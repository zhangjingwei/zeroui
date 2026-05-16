package com.zero.zero_tools.home

import android.content.Context

private const val PAGES_DIR = "pages"

/** Read `assets/pages/{name}.json` as a single string. `name` should not include `.json`. */
fun Context.readPageJson(name: String): String {
    return assets.open("$PAGES_DIR/$name.json")
        .bufferedReader()
        .use { it.readText() }
}

fun Context.readHomePageJson(): String = readPageJson("home")
