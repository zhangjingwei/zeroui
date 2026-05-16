# ZeroUI

[中文](README.md) | English

ZeroUI is a lightweight Server-Driven UI runtime for Android that lets apps dynamically deliver JSON descriptions and render native Jetpack Compose screens. Build campaign pages, settings screens, dynamic forms, and internal tools without frequent app releases, while keeping native performance, state management, action dispatch, and host capability integration.

## 5 Minute Start

Add the `zeroui` Android library module or published artifact:

```kotlin
dependencies {
    implementation("com.zero.zero-tools:zeroui:0.1.0")
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
    onUnknownNode = { typeName, raw -> /* report unsupported schema */ }
)
```

Use `PageLoader` to load schemas from assets, memory, or a prefilled cache. It is intentionally synchronous in `0.1.x`; remote schemas should be prefetched by the host and served from a cache-backed loader.

Use `HttpClient` to replace the default `HttpURLConnection` implementation with your own network stack. Use `Tracker` to forward `track` effects into your analytics system.

Use `ZeroImageLoader` to plug in your image-loading stack. The bundled `rememberDefaultZeroImageLoader()` decodes drawable resources and HTTP(S) URLs with `inSampleSize` downsampling, a small in-memory LRU cache, and an SDK-side `http`/`https` scheme allowlist. For lazy lists, animated formats, disk caching, or request prioritisation, plug in a Coil- or Glide-backed `ZeroImageLoader` instead:

```kotlin
class CoilZeroImageLoader(...) : ZeroImageLoader {
    override fun load(request: ZeroImageRequest, onResult: (ZeroImageResult) -> Unit): Cancelable {
        // route request.source.url / .name into Coil; post onResult on the main thread.
    }
}
```

## Skin System

ZeroUI exposes semantic `style` and `tone` values in JSON, while Android implementation details stay inside `ZeroSkin`.

Current component token coverage:

- Button
- TextField
- ChipGroup
- Switch
- Card

The sample `assets/pages/showcase.json` page exercises these skin-driven components for visual review.

## Publishing

The `zeroui` module publishes a release AAR with sources using Maven coordinates:

```text
groupId: com.zero.zero-tools
artifactId: zeroui
version: 0.1.0
```

`publishReleasePublicationToMavenLocal` is configured for local/team validation. For JitPack or a private Maven repository, point the repository service at the `:zeroui` release publication and reuse the same coordinates.

## Protocol Quick Reference

Nodes:

| Node | Stability |
| --- | --- |
| `column`, `row`, `text`, `textField`, `switch`, `button`, `chipGroup`, `card`, `spacer`, `condition`, `image` | Stable for `0.1.x` internal/team use (`image` requires a `ZeroImageLoader`; the default loader covers resources + HTTP/HTTPS URLs) |
| `forEach`, `lazyColumn`, `dialog` | Experimental in `0.1.x`; keep behind server-side capability gates |

Actions:
`setState`, `incrementState`, `toggleState`, `batch`.

Effects:
`toast`, `log`, `track`, `navigate`, `back`, `http`.

Text styles:
`title`, `sectionTitle`, `body`, `label`.

Tones:
`default`, `muted`, `primary`, `success`, `error`, `warning`.

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
`lazyColumn`, `dialog`, and `forEach`. Treat these as early surface area and gate rollout by client capability.

## API Boundary

The `zeroui` module compiles its main sources with Kotlin explicit API mode. Public declarations are intentional SDK surface; implementation details stay `internal` or `private`.
