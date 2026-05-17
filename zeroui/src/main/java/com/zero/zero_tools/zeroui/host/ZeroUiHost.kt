package com.zero.zero_tools.zeroui.host

import android.util.Log
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.zero.zero_tools.zeroui.core.LocalZeroUiUnknownNodeHandler
import com.zero.zero_tools.zeroui.core.ZeroUiRenderer
import com.zero.zero_tools.zeroui.core.parseZeroUiPage
import com.zero.zero_tools.zeroui.effect.executeEffects
import com.zero.zero_tools.zeroui.http.HttpClient
import com.zero.zero_tools.zeroui.http.UrlConnectionHttpClient
import com.zero.zero_tools.zeroui.image.LocalZeroImageLoader
import com.zero.zero_tools.zeroui.image.ZeroImageLoader
import com.zero.zero_tools.zeroui.image.rememberDefaultZeroImageLoader
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.effect.NavigationTargetKind
import com.zero.zero_tools.zeroui.navigation.Navigator
import com.zero.zero_tools.zeroui.node.Node
import com.zero.zero_tools.zeroui.page.Page
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.StateEntry
import com.zero.zero_tools.zeroui.state.StateOwner
import com.zero.zero_tools.zeroui.state.reduceState
import com.zero.zero_tools.zeroui.tracking.Tracker
import com.zero.zero_tools.zeroui.value.Value
import java.util.concurrent.atomic.AtomicLong

@Composable
public fun ZeroUiHost(
    startPage: String,
    modifier: Modifier = Modifier,
    rootPadding: Int = 24,
    pageLoader: PageLoader = rememberAssetsPageLoader(),
    httpClient: HttpClient = rememberDefaultHttpClient(),
    imageLoader: ZeroImageLoader = rememberDefaultZeroImageLoader(),
    tracker: Tracker = LogcatTracker,
    onUnknownNode: ((typeName: String, raw: String) -> Unit)? = null,
    externalNavigator: Navigator = Navigator.Noop
) {
    val context = LocalContext.current

    val pageStack = rememberSaveable(
        startPage,
        pageLoader,
        saver = restorablePageStackSaver(loader = pageLoader, startPage = startPage)
    ) {
        RestorablePageStack(
            entries = mutableStateListOf<PageStackEntry>().apply {
                add(loadPageEntry(name = startPage, loader = pageLoader))
            },
            restored = false
        )
    }
    val stack = pageStack.entries
    val registry = remember { PageEntryRegistry() }
    val handlerRef = remember { Holder<(Interaction, Value?) -> Unit>() }

    val navigator = remember(pageLoader, externalNavigator) {
        object : Navigator {
            override fun navigate(target: String) {
                val entry = loadPageEntry(name = target, loader = pageLoader)
                stack.add(entry)
                fireOnMountIfAny(entry.page.onMount, handlerRef.value)
            }

            override fun navigate(target: String, kind: NavigationTargetKind) {
                if (kind == NavigationTargetKind.Page) {
                    navigate(target)
                } else {
                    externalNavigator.navigate(target = target, kind = kind)
                }
            }

            override fun back() {
                if (stack.size > 1) {
                    val popped = stack.removeAt(stack.lastIndex)
                    registry.cancelAll(popped.id)
                } else {
                    externalNavigator.back()
                }
            }
        }
    }

    DisposableEffect(Unit) {
        onDispose { registry.cancelAll() }
    }

    BackHandler(enabled = stack.size > 1) {
        navigator.back()
    }

    fun handleInteraction(interaction: Interaction, eventValue: Value?) {
        val current = stack.last()
        val nextState = reduceState(current.state, interaction, eventValue)
        stack[stack.lastIndex] = current.copy(state = nextState)

        val originEntryId = current.id
        val guardedFollowUp = pageScopedFollowUp(
            originEntryId = originEntryId,
            currentTopId = { stack.lastOrNull()?.id },
            onAllow = ::handleInteraction,
            onDrop = { origin, now ->
                Log.w(
                    LateResponseLogTag,
                    "Dropped page-scoped follow-up: origin=$origin currentTop=$now (${stack.lastOrNull()?.name})"
                )
            }
        )

        executeEffects(
            context = context,
            state = nextState,
            effects = interaction.effects,
            eventValue = eventValue,
            navigator = navigator,
            tracker = tracker,
            httpClient = httpClient,
            onFollowUp = guardedFollowUp,
            onCancelable = { cancelable ->
                registry.register(
                    entryId = originEntryId,
                    id = registry.nextId(),
                    cancelable = cancelable
                )
            }
        )
    }

    handlerRef.value = ::handleInteraction

    LaunchedEffect(startPage, pageLoader, pageStack.restored) {
        if (!pageStack.restored) {
            fireOnMountIfAny(stack.first().page.onMount, handlerRef.value)
        }
    }

    val top = stack.last()
    val rootModifier = Modifier
        .fillMaxSize()
        .then(
            if (top.page.root is Node.LazyColumn) {
                Modifier
            } else {
                Modifier.verticalScroll(rememberScrollState())
            }
        )
        .padding(rootPadding.dp)

    Surface(modifier = modifier.fillMaxSize()) {
        CompositionLocalProvider(
            LocalZeroUiUnknownNodeHandler provides onUnknownNode,
            LocalZeroImageLoader provides imageLoader
        ) {
            ZeroUiRenderer(
                node = top.page.root,
                state = top.state,
                onInteraction = ::handleInteraction,
                modifier = rootModifier
            )
        }
    }
}

@Composable
public fun rememberDefaultHttpClient(): HttpClient {
    val scope = rememberCoroutineScope()
    return remember(scope) { UrlConnectionHttpClient(scope) }
}

private data class PageStackEntry(
    val id: Long,
    val name: String,
    val page: Page,
    val state: State
)

private data class RestorablePageStack(
    val entries: SnapshotStateList<PageStackEntry>,
    val restored: Boolean
)

private class Holder<T : Any> {
    var value: T? = null
}

private val entryIdSeq = AtomicLong(0L)

private fun loadPageEntry(name: String, loader: PageLoader): PageStackEntry {
    val page = parseZeroUiPage(loader.load(name))
    return PageStackEntry(
        id = entryIdSeq.incrementAndGet(),
        name = name,
        page = page,
        state = page.initialState
    )
}

private fun loadPageEntry(name: String, loader: PageLoader, state: State): PageStackEntry {
    val page = parseZeroUiPage(loader.load(name))
    return PageStackEntry(
        id = entryIdSeq.incrementAndGet(),
        name = name,
        page = page,
        state = state
    )
}

private fun restorablePageStackSaver(
    loader: PageLoader,
    startPage: String
): Saver<RestorablePageStack, Any> {
    return Saver(
        save = { pageStack -> pageStack.entries.map(::savePageStackEntry) },
        restore = { saved ->
            val entries = (saved as? List<*>)
                ?.mapNotNull { restorePageStackEntry(it, loader) }
                .orEmpty()

            RestorablePageStack(
                entries = mutableStateListOf<PageStackEntry>().apply {
                    if (entries.isEmpty()) {
                        add(loadPageEntry(name = startPage, loader = loader))
                    } else {
                        addAll(entries)
                    }
                },
                restored = entries.isNotEmpty()
            )
        }
    )
}

private fun savePageStackEntry(entry: PageStackEntry): List<Any> {
    return listOf(entry.name, saveState(entry.state))
}

private fun restorePageStackEntry(saved: Any?, loader: PageLoader): PageStackEntry? {
    val entry = saved as? List<*> ?: return null
    val name = entry.getOrNull(0) as? String ?: return null
    val state = restoreState(entry.getOrNull(1)) ?: return null
    return runCatching { loadPageEntry(name = name, loader = loader, state = state) }.getOrNull()
}

private fun saveState(state: State): List<Any> {
    return state.values.entries
        .sortedBy { it.key }
        .map { (key, entry) ->
            listOf(key, entry.owner.name, saveValue(entry.value))
        }
}

private fun restoreState(saved: Any?): State? {
    val entries = saved as? List<*> ?: return null
    val values = entries.mapNotNull { savedEntry ->
        val entry = savedEntry as? List<*> ?: return@mapNotNull null
        val key = entry.getOrNull(0) as? String ?: return@mapNotNull null
        val ownerName = entry.getOrNull(1) as? String ?: return@mapNotNull null
        val value = restoreValue(entry.getOrNull(2)) ?: return@mapNotNull null
        val owner = runCatching { StateOwner.valueOf(ownerName) }.getOrNull() ?: return@mapNotNull null
        key to StateEntry(value = value, owner = owner)
    }.toMap()
    return State(values)
}

private fun saveValue(value: Value): List<Any> {
    return when (value) {
        is Value.Text -> listOf("text", value.value)
        is Value.Number -> listOf("number", value.value)
        is Value.Bool -> listOf("bool", value.value)
        is Value.List -> listOf("list", value.items.map(::saveValue))
        is Value.Record -> listOf(
            "record",
            value.fields.entries
                .sortedBy { it.key }
                .map { (key, childValue) -> listOf(key, saveValue(childValue)) }
        )
    }
}

private fun restoreValue(saved: Any?): Value? {
    val value = saved as? List<*> ?: return null
    return when (value.getOrNull(0) as? String) {
        "text" -> (value.getOrNull(1) as? String)?.let(Value::Text)
        "number" -> (value.getOrNull(1) as? Number)?.toInt()?.let(Value::Number)
        "bool" -> (value.getOrNull(1) as? Boolean)?.let(Value::Bool)
        "list" -> {
            val items = value.getOrNull(1) as? List<*> ?: return null
            Value.List(items.mapNotNull(::restoreValue))
        }
        "record" -> {
            val fields = value.getOrNull(1) as? List<*> ?: return null
            Value.Record(
                fields.mapNotNull { savedField ->
                    val field = savedField as? List<*> ?: return@mapNotNull null
                    val key = field.getOrNull(0) as? String ?: return@mapNotNull null
                    val childValue = restoreValue(field.getOrNull(1)) ?: return@mapNotNull null
                    key to childValue
                }.toMap()
            )
        }
        else -> null
    }
}

private const val LateResponseLogTag = "ZeroUiLateResponse"
