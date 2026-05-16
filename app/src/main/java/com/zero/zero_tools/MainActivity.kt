package com.zero.zero_tools

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.zero.zero_tools.home.readPageJson
import com.zero.zero_tools.host.PageEntryRegistry
import com.zero.zero_tools.host.fireOnMountIfAny
import com.zero.zero_tools.host.pageScopedFollowUp
import com.zero.zero_tools.http.UrlConnectionHttpClient
import com.zero.zero_tools.zeroui.core.ZeroUiRenderer
import com.zero.zero_tools.zeroui.core.parseZeroUiPage
import com.zero.zero_tools.zeroui.effect.executeEffects
import com.zero.zero_tools.zeroui.interaction.Interaction
import com.zero.zero_tools.zeroui.navigation.Navigator
import com.zero.zero_tools.zeroui.page.Page
import com.zero.zero_tools.zeroui.state.State
import com.zero.zero_tools.zeroui.state.asText
import com.zero.zero_tools.zeroui.state.reduceState
import com.zero.zero_tools.zeroui.tracking.Tracker
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.ui.theme.ZerotoolsTheme
import java.util.concurrent.atomic.AtomicLong

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ZerotoolsTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    ZeroUiHost(
                        startPage = "home",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
        }
    }
}

/**
 * Top-level host that maintains a stack of ZeroUI pages and wires
 * Navigator / Tracker / HttpClient into [executeEffects].
 *
 * Two-layer defence for async HTTP correctness:
 *
 * 1. **[PageEntryRegistry] (Wave 4)** — every request started by an entry is
 *    registered against that entry's stable id. When the entry pops from the
 *    stack, every registered [com.zero.zero_tools.zeroui.http.Cancelable] is
 *    cancelled, freeing network / CPU. When the host itself is disposed, all
 *    entries' cancelables are cancelled.
 * 2. **[pageScopedFollowUp] (Wave 3.5)** — even if cancellation is best-effort
 *    (e.g. the underlying HttpURLConnection is not cooperatively cancellable),
 *    any late response is checked against the current top entry id and dropped
 *    if it would mutate a different page.
 *
 * Both layers stay engaged; the registry handles 99% of cases, the guard catches
 * races and any HttpClient that can't truly abort its I/O.
 */
@Composable
fun ZeroUiHost(startPage: String, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // Stack of (pageName, parsed Page, current State). Last entry is the visible page.
    val stack = remember {
        mutableStateListOf<PageStackEntry>().apply {
            add(loadPageEntry(name = startPage, loader = context::readPageJson))
        }
    }

    val registry = remember { PageEntryRegistry() }

    // Forward-reference holder: navigator (created via remember) needs to call
    // handleInteraction to fire onMount on push, and handleInteraction needs to
    // pass navigator to executeEffects. We break the cycle by having the navigator
    // capture a stable holder whose `.value` is filled in later in the same
    // composition. The holder is null only during the brief window before the
    // first composition completes — at that point no user interactions can fire yet.
    //
    // We use a plain holder (not mutableStateOf) so reassignment doesn't trigger
    // recomposition — `handlerRef.value = ::handleInteraction` happens every
    // composition, which with mutableStateOf would loop infinitely.
    val handlerRef = remember { Holder<(Interaction, Value?) -> Unit>() }

    val navigator = remember {
        object : Navigator {
            override fun navigate(target: String) {
                val entry = loadPageEntry(name = target, loader = context::readPageJson)
                stack.add(entry)
                // Explicit "load-time" trigger per Wave 5 spec: fire onMount right after
                // the push so the new entry can self-fetch / set up state.
                fireOnMountIfAny(entry.page.onMount, handlerRef.value)
            }

            override fun back() {
                if (stack.size > 1) {
                    val popped = stack.removeAt(stack.lastIndex)
                    registry.cancelAll(popped.id)
                }
            }
        }
    }

    val tracker = remember { LogcatTracker }
    val httpClient = remember(scope) { UrlConnectionHttpClient(scope) }

    // Activity-level safety net: cancel every in-flight cancelable when the host
    // composable leaves composition (e.g. Activity destroyed). rememberCoroutineScope
    // would also cancel the coroutines on its own, but going through the registry
    // ensures any Cancelable that isn't backed by a coroutine (future HttpClients,
    // WebSocket subscriptions, ...) gets the same treatment.
    DisposableEffect(Unit) {
        onDispose { registry.cancelAll() }
    }

    // Single handler that ZeroUiRenderer feeds (user interactions) AND that
    // executeEffects feeds back into (Http response follow-ups). Reads `stack.last()`
    // freshly each call so late HTTP callbacks never clobber a stale `top` reference.
    // The onFollowUp is wrapped with a page-scope guard: a late HTTP response is
    // only applied if the entry that issued it is still top of the stack.
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
            onCancelable = { cancelable -> registry.register(originEntryId, cancelable) }
        )
    }

    // Publish handleInteraction into the holder so the navigator's onMount trigger
    // can dispatch through it. Assignment runs every composition, but the lambda
    // body always uses stable remembered captures, so identity drift is irrelevant.
    handlerRef.value = ::handleInteraction

    // Start-page onMount: fires exactly once when the host first enters composition.
    // LaunchedEffect(Unit) is safe here because the key never changes and the body
    // is dedup'd by Compose — unlike LaunchedEffect(currentTop.id), which would
    // re-fire onMount on back navigation.
    LaunchedEffect(Unit) {
        fireOnMountIfAny(stack.first().page.onMount, handlerRef.value)
    }

    val top = stack.last()
    Surface(modifier = modifier.fillMaxSize()) {
        ZeroUiRenderer(
            node = top.page.root,
            state = top.state,
            onInteraction = ::handleInteraction,
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(24.dp)
        )
    }
}

private data class PageStackEntry(
    val id: Long,
    val name: String,
    val page: Page,
    val state: State
)

/**
 * Plain mutable single-slot holder. Used by [ZeroUiHost] to break the navigator <→
 * handleInteraction cycle without going through `mutableStateOf` — which would
 * loop recomposition since the holder is reassigned every composition with a
 * fresh local-function reference.
 */
private class Holder<T : Any> {
    var value: T? = null
}

private val entryIdSeq = AtomicLong(0L)

private fun loadPageEntry(name: String, loader: (String) -> String): PageStackEntry {
    val page = parseZeroUiPage(loader(name))
    return PageStackEntry(
        id = entryIdSeq.incrementAndGet(),
        name = name,
        page = page,
        state = page.initialState
    )
}

private const val TrackerLogTag = "ZeroUiTrack"
private const val LateResponseLogTag = "ZeroUiLateResponse"

private val LogcatTracker: Tracker = Tracker { event, params ->
    if (params.isEmpty()) {
        Log.i(TrackerLogTag, event)
    } else {
        val rendered = params.entries.joinToString(prefix = "{", postfix = "}") { (k, v) ->
            "$k=${v.asText()}"
        }
        Log.i(TrackerLogTag, "$event $rendered")
    }
}

@Preview(showBackground = true)
@Composable
fun ZeroUiHostPreview() {
    ZerotoolsTheme {
        ZeroUiHost(startPage = "home")
    }
}
