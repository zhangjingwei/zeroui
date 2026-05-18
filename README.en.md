# ZeroUI

[中文](README.md) | English

ZeroUI is a lightweight Server-Driven UI runtime for Android that lets apps dynamically deliver JSON descriptions and render native Jetpack Compose screens. Build campaign pages, settings screens, dynamic forms, and internal tools without frequent app releases, while keeping native performance, state management, action dispatch, and host capability integration.

## 5 Minute Start

Add the JitPack repository:

```kotlin
dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven("https://jitpack.io")
    }
}
```

Then add the `zeroui` Android library dependency:

```kotlin
dependencies {
    implementation("com.github.zhangjingwei:zeroui:v0.1.4")
}
```

For local validation before publishing to a team repository:

```bash
./gradlew :zeroui:publishReleasePublicationToMavenLocal
```

Then render a page from `assets/pages/home.json`:

```kotlin
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MyAppTheme {
                ZeroSkinProvider {
                    ZeroUiHost(startPage = "home")
                }
            }
        }
    }
}
```

Place JSON at `src/main/assets/pages/home.json`:

```json
{
  "schemaVersion": 1,
  "root": {
    "type": "column",
    "children": [
      {
        "type": "text",
        "style": "title",
        "text": { "type": "value", "value": "Hello ZeroUI" }
      }
    ]
  }
}
```

## Host Hooks

`ZeroUiHost` accepts the main SDK extension points:

```kotlin
ZeroUiHost(
    startPage = "home",
    pageLoader = AssetsPageLoader(context),
    httpClient = rememberDefaultHttpClient(),
    imageLoader = rememberDefaultZeroImageLoader(),
    tracker = LogcatTracker,
    externalNavigator = object : Navigator {
        override fun navigate(target: String, kind: NavigationTargetKind) {
            if (kind == NavigationTargetKind.Route) {
                navController.navigate(target)
            }
        }

        override fun navigate(target: String) {
            navController.navigate(target)
        }

        override fun back() {
            navController.popBackStack()
        }
    },
    onUnknownNode = { typeName, raw -> /* report unsupported schema */ }
)
```

Use `PageLoader` to load schemas from assets, memory, or a prefilled cache. It is intentionally synchronous in `0.1.x`; remote schemas should be prefetched by the host and served from a cache-backed loader.

Use `HttpClient` to replace the default `HttpURLConnection` implementation with your own network stack. Use `Tracker` to forward `track` effects into your analytics system.

Use `externalNavigator` to forward `navigate` `route` / `url` / `external` targets into host navigation such as Compose Navigation. `page` targets still use `ZeroUiHost`'s JSON page stack; when the ZeroUI page stack cannot pop, `back` delegates to `externalNavigator.back()`.

Use `ZeroImageLoader` to plug in your image/icon loading stack. The bundled `rememberDefaultZeroImageLoader()` constrains drawable resources (including vector drawables) and HTTP(S) URLs, keeps a small in-memory LRU cache, and enforces an SDK-side `http`/`https` scheme allowlist. For lazy lists, animated formats, disk caching, or request prioritisation, plug in a Coil- or Glide-backed `ZeroImageLoader` instead:

```kotlin
class CoilZeroImageLoader(...) : ZeroImageLoader {
    override fun load(request: ZeroImageRequest, onResult: (ZeroImageResult) -> Unit): Cancelable {
        // route request.source.url / .name into Coil; post onResult on the main thread.
    }
}
```

## Skin System

ZeroUI exposes semantic `style` and `tone` values in JSON, while Android implementation details stay inside `ZeroSkin`.

`ZeroSkinProvider` accepts a `ZeroSkin` object constructed by the host or a brand integration package. It does not accept JSON strings, file paths, or remote loaders. Brand packages should expose `rememberBrandZeroSkin(): ZeroSkin`, then the host passes it in:

```kotlin
ZeroSkinProvider(
    skin = rememberBrandZeroSkin(darkTheme)
) {
    ZeroUiHost(startPage = "home")
}
```

See [docs/skin.md](docs/skin.md) for the field-level contract, [examples/brand-skin-integration/BrandZeroSkin.kt](examples/brand-skin-integration/BrandZeroSkin.kt) for a copyable template, and [docs/ai-integration.md](docs/ai-integration.md) for coding-agent constraints.

Current component token coverage:

- Button
- TextField
- ChipGroup
- Switch
- Card
- Checkbox
- Radio
- Slider
- Progress
- Snackbar
- Divider
- BottomSheet

The sample `assets/pages/showcase.json` page exercises these skin-driven components for visual review.

## Publishing

The `zeroui` module publishes a release AAR with sources through JitPack:

```text
groupId: com.github.zhangjingwei
artifactId: zeroui
version: v0.1.4
```

Validate the publication locally before creating a release:

```bash
./gradlew :zeroui:publishReleasePublicationToMavenLocal
```

Then create a GitHub release:

- Tag: `v0.1.4`
- Target: `main`
- Release title: `v0.1.4`
- Release notes: `ZeroUI 0.1.4 enhances HTTP request state, cancellation, caching, retry, and response mapping capabilities.`
- Leave `Set as a pre-release` unchecked

After publishing, open [JitPack](https://jitpack.io/#zhangjingwei/zeroui/v0.1.4) and click `Get it` to trigger the build. Once it succeeds, consumers can use the Gradle dependency above.

## Protocol Quick Reference

`schemaVersion: 1` remains backward compatible. ZeroUI extends v1 through optional fields, so existing pages do not need a migration.

Common `layout` fields work on most visible nodes:

```json
{
  "layout": {
    "fillMaxWidth": true,
    "fillMaxHeight": false,
    "weight": 1,
    "padding": { "start": 12, "top": 8, "end": 12, "bottom": 8 },
    "width": 120,
    "height": 48,
    "minWidth": 80,
    "minHeight": 40,
    "maxWidth": 320,
    "maxHeight": 480
  }
}
```

`column` supports `spacing` and `horizontalAlignment: "start" | "center" | "end"`; `row` supports `spacing`, `verticalAlignment: "top" | "center" | "bottom" | "baseline"`, and optional `arrangement: "start" | "center" | "end" | "spaceBetween" | "spaceAround" | "spaceEvenly"`. `arrangement` controls horizontal placement and `verticalAlignment` controls vertical alignment; `baseline` applies Compose `alignByBaseline()` to row children, which fits number-and-unit typography. When `arrangement` is omitted, `row` renders spacing through `spacing`; when present, it is passed through to Compose `Arrangement.Horizontal`. Children can use `layout.weight` to share remaining space in a parent `row` / `column`.

`lazyColumn` and `lazyRow` both support static `children`, `itemsKey`, and a reusable `item` template. Templates read the current item through `item.*` and `itemIndex`.

`card` supports `padding` and `spacing`. `padding` controls inner content padding, while `spacing` controls vertical gaps between child nodes.

`text`, `image`, `card`, and `button` share the same `onClick` interaction model. `button.onClick` is still required; `text` / `image` / `card` `onClick` is optional.

`text.surfaceTone` is optional and uses the same values as `tone`; it controls the text background container and adds default inside padding (10dp horizontal, 4dp vertical). Text color is still controlled by `tone`; when `tone` is omitted, ZeroUI uses the matching `surfaceTone` content color as the default text color. `layout.padding` remains outside padding and can stack with the surface inside padding.

`icon` is a generic icon node, not a business-specific `iconText` / `iconButton`. It uses the common source shape:

```json
{
  "type": "icon",
  "source": { "type": "resource", "name": "ic_launcher_foreground" },
  "tone": "primary",
  "size": 24,
  "tint": true,
  "onClick": { "effects": [{ "type": "track", "event": "icon_clicked" }] }
}
```

`navigate` supports the legacy shape and ZeroUI-level target semantics:

```json
{
  "type": "navigate",
  "target": {
    "type": "page",
    "name": { "type": "literal", "value": "detail" }
  }
}
```

`type` can be `page`, `route`, `url`, or `external`. The bundled `ZeroUiHost` consumes `page` targets with its own page stack; `route` / `url` / `external` targets are forwarded to `ZeroUiHost(externalNavigator = ...)`, which can bridge to Compose Navigation, a browser, or host-owned navigation. ZeroUI does not introduce app-private schemes such as `page://` or `sdui://`.

Nodes:

| Node | Stability |
| --- | --- |
| `column`, `row`, `box`, `text`, `textField`, `switch`, `button`, `chipGroup`, `card`, `spacer`, `condition`, `image`, `icon`, `divider` | Stable for `0.1.x` internal/team use (`image` / `icon` require a `ZeroImageLoader`; the default loader covers drawable resources + HTTP/HTTPS URLs) |
| `checkbox`, `radioGroup`, `slider`, `progress`, `select`, `snackbar` | Stable for `0.1.x` internal/team use; bind a boolean state via `enabledKey` for disabled state |
| `forEach`, `lazyColumn`, `lazyRow`, `dialog`, `bottomSheet` | Experimental in `0.1.x`; keep behind server-side capability gates |

Actions:
`setState`, `appendState`, `incrementState`, `toggleState`, `clearState`, `resetState`, `validate`, `batch`. `setState` / `clearState` support dotted-path keys such as `form.name` or `items[0].status` to read and write nested Record fields.

Effects:
`toast`, `log`, `track`, `navigate`, `back`, `http`.

Interaction policies: interactions can set `debounceMillis` and `throttleMillis` to prevent rapid-tap double-submit or excessive search requests.

`http` supports `params` query building, `timeoutMs`, `retryCount` / `retryDelayMs`, `requestKey` + `cancelPrevious`, `cachePolicy: "networkOnly" | "cacheFirst"`, standard request state via `stateKey`, `responseMode: "body" | "full"`, response `map`, and `onStart` / `onSuccess` / `onError` / `onFinally` lifecycle hooks. `map` array entries can use `mode: "replace" | "append"` for refresh or load-more pagination flows.

Text styles:
`display`, `title`, `sectionTitle`, `body`, `label`, `support`. Unknown `style` values fall back to `body` so one typo does not fail page parsing.

Tones:
`default`, `muted`, `primary`, `success`, `error`, `warning`, `inverse`, `info`, `accent`, `disabled`.

## Protocol Versioning

Pages may include a top-level `schemaVersion` integer. Missing versions default to `1`.

ZeroUI `0.1.x` currently supports protocol version `1`. The parser preserves the page's version on `Page.schemaVersion`; hosts can use it for telemetry, preflight checks, or server-side rollout guards.

Compatibility policy for `0.1.x`:

- Keep existing protocol fields backward compatible within schema version `1`.
- Additive fields are allowed when older clients can ignore them.
- Unsupported node `type` values render as `Node.Unknown` placeholders and can be observed via `ZeroUiHost(onUnknownNode = ...)`.
- Breaking protocol changes should use a new `schemaVersion` and be gated server-side by client capability.

## Known Limits

ZeroUI `0.1.x` is ready for internal source/module reuse and early Maven-style team use. It is not yet a general-purpose public SDK.

Notable missing pieces:
state restoration after configuration changes/process death, binary compatibility validation, and a formal publishing pipeline.

Experimental protocol/rendering pieces available in `0.1.x`:
`lazyColumn`, `lazyRow`, `dialog`, and `forEach`. Treat these as early surface area and gate rollout by client capability.

## API Boundary

The `zeroui` module compiles its main sources with Kotlin explicit API mode. Public declarations are intentional SDK surface; implementation details stay `internal` or `private`.
