# AI Integration Notes

This file is for coding agents working with ZeroUI integrations.

ZeroUI is a general Android SDK. Do not depend on private app code, deleted private protocols, or local source folders being mounted into the user's workspace. External users should be able to integrate through the published `zeroui` AAR plus public docs and examples.

## Hard Rules

- Keep `schemaVersion = 1` compatible.
- Do not migrate or recreate deleted private v2 protocols.
- Do not introduce app-private navigation schemes such as `page://` or `sdui://`.
- Do not design `iconText` / `iconButton` business-specific nodes; use the generic `icon` node.
- Do not design JSON skin loading unless the user explicitly asks for remote/configurable skin support.
- Treat `ZeroUiHost` as the public host integration point for app consumption.

## Skin Integration

`ZeroSkinProvider` accepts a resolved `ZeroSkin` object:

```kotlin
ZeroSkinProvider(
    skin = rememberBrandZeroSkin(darkTheme)
) {
    ZeroUiHost(startPage = "home")
}
```

It does not accept:

- JSON strings
- file paths
- remote loaders
- app-private token maps

Brand/theme integration packages should export one or more functions that build `ZeroSkin`, for example:

```kotlin
@Composable
fun rememberBrandZeroSkin(darkTheme: Boolean): ZeroSkin
```

Use `docs/skin.md` and `examples/brand-skin-integration/BrandZeroSkin.kt` as the canonical template.

## Protocol Integration

The JSON protocol exposes semantic UI fields. Android/Compose implementation details stay in the SDK.

Use:

- `column` / `row` with `layout`, `spacing`, alignment, and `weight`
- `lazyColumn` / `lazyRow` with `itemsKey` and reusable `item`
- shared `onClick` interactions on clickable nodes
- structured `navigate` target kinds
- generic `icon` source

Avoid:

- Material-specific field names in JSON
- app-specific route schemes
- business-only node names
- schemaVersion bumps for additive v1 fields

## How To Work In This Repo

Before answering codebase questions, query the graph:

```bash
graphify query "your question"
```

After modifying code, update the graph:

```bash
graphify update .
```

When implementing SDK behavior:

- Start in `zeroui/src/main/java/com/zero/zero_tools/zeroui`.
- Keep public API declarations explicit.
- Keep app-specific sample consumption in `app/src/main/assets/pages`.
- Document new public protocol/API behavior in `README.md`, `README.en.md`, or `docs/`.
- Add parser tests for new protocol fields.

## Common Task Patterns

### Add A New Public Node Field

1. Update `NodeModels.kt`.
2. Parse it in `NodeJsonParser.kt` with a backward-compatible default.
3. Render it in `NodeRenderers.kt`.
4. Add a parser test.
5. Document the field.
6. Update app sample JSON only through public ZeroUI protocol.

### Add A Skin Capability

1. Prefer extending `ZeroSkin` or derived component tokens.
2. Keep `ZeroSkinProvider` accepting `ZeroSkin`.
3. Do not add JSON/loader entry points by default.
4. Update `docs/skin.md` and the brand skin example.

### Add Navigation Semantics

1. Keep target semantics generic.
2. Use `NavigationTargetKind` or another ZeroUI-level public type.
3. Let `ZeroUiHost` handle page-stack behavior.
4. Do not encode app-private route schemes into JSON.
