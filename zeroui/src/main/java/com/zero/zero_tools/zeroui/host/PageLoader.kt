package com.zero.zero_tools.zeroui.host

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.concurrent.ConcurrentHashMap

/** Loads a ZeroUI page schema by name. Implementations should throw if the page is missing. */
public fun interface PageLoader {
    public fun load(name: String): String
}

public class AssetsPageLoader(
    private val context: Context,
    private val basePath: String = "pages"
) : PageLoader {
    override fun load(name: String): String {
        return context.assets.open("$basePath/$name.json")
            .bufferedReader()
            .use { it.readText() }
    }
}

public class InMemoryCachedPageLoader(
    private val delegate: PageLoader
) : PageLoader {
    private val cache = ConcurrentHashMap<String, String>()

    override fun load(name: String): String {
        return cache.computeIfAbsent(name) { delegate.load(it) }
    }

    public fun preload(name: String, json: String): Unit {
        cache[name] = json
    }

    public fun clear(): Unit {
        cache.clear()
    }
}

@Composable
public fun rememberAssetsPageLoader(basePath: String = "pages"): PageLoader {
    val context = LocalContext.current
    return remember(context, basePath) {
        AssetsPageLoader(context, basePath)
    }
}
