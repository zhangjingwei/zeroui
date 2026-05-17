# Changelog

## 0.1.2

- Added `display` text style support with a 48sp black-weight default slot and updated default chip/secondary-button derived tokens to use neutral and inverse roles.
- Added `ZeroUiHost(externalNavigator = ...)` so `route` / `url` / `external` navigation targets can bridge to host navigation while `page` stays on the ZeroUI page stack.
- Added optional text `surfaceTone`, row `arrangement`, and row baseline `verticalAlignment` fields to schemaVersion 1; `card.onClick` remains on the shared interaction schema.
- Added `inverse` tone support across JSON parsing, skin palette tokens, text/icon foreground color, and card container tokens.
- Added JSON `support` text style parsing and made unknown text styles fall back to `body` instead of failing page parsing.
- Extended schemaVersion 1 with additive ZeroUI public protocol fields for layout alignment/weight/dimensions/padding, `lazyRow`, generic `icon`, optional `onClick` on text/image/card, and structured navigation target kinds.
- Updated the app showcase assets so new public protocol capabilities are consumed through `ZeroUiHost`.
- Added skin integration docs, AI integration notes, and a copyable brand skin template so external packages can build `ZeroSkin` without reading local SDK sources.
- Added vector drawable fallback support to the default image loader so drawable resources work for `icon` as well as `image`.
- Corrected the JitPack consumer coordinate to `com.github.zhangjingwei:zeroui`.

## 0.1.0

- Introduced reusable `ZeroUiHost` in the `zeroui` module.
- Added synchronous `PageLoader`, `AssetsPageLoader`, and `InMemoryCachedPageLoader`.
- Moved page-scoped follow-up guarding and cancelable registry into `zeroui.host`.
- Added default `UrlConnectionHttpClient` and `rememberDefaultHttpClient()`.
- Added `LogcatTracker` and `onUnknownNode` reporting hook.
- Kept app module as a thin sample shell around `ZeroUiHost`.
- Added initial README with setup, protocol quick reference, host hooks, and known limits.
- Enabled Kotlin explicit API mode for `zeroui` main sources and marked the public SDK surface explicitly.
- Added Maven Publish configuration for the `zeroui` release AAR and sources jar.
- Expanded `ZeroSkin` component tokens to cover TextField, ChipGroup, Switch, and Card.
- Updated the showcase page to include token-driven field, chip, switch, button, and card samples.
- Added top-level page `schemaVersion` parsing, defaulting missing versions to protocol version 1.
- Documented the initial protocol compatibility policy.
- Published Compose API dependencies on the compile classpath for external consumers.
- Added system back handling for the internal ZeroUI page stack.
- Fixed `startPage` changes so the new root page's `onMount` hook fires.
- Documented experimental `image`, `lazyColumn`, `dialog`, and `forEach` protocol nodes.
- Rewrote the `image` node on top of a pluggable `ZeroImageLoader`; the default implementation downsamples via `inSampleSize`, holds a small in-memory LRU cache, and enforces an `http`/`https` scheme allowlist.
- Moved the unknown-node and image placeholder strings into `values/values-zh-rCN` resources so the SDK no longer ships hardcoded Chinese labels.
