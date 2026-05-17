# ZeroUI Skin Integration

ZeroUI skinning is code-first in `0.1.x`.

`ZeroSkinProvider` accepts a resolved `ZeroSkin` object. It does not parse JSON, read files, or call a loader. Brand/theme integration packages should export a small function such as `rememberBrandZeroSkin()` and let the host app pass that object into `ZeroSkinProvider`.

```kotlin
ZeroSkinProvider(
    skin = rememberBrandZeroSkin(darkTheme)
) {
    ZeroUiHost(startPage = "home")
}
```

## Public Shape

The main public type is:

```kotlin
data class ZeroSkin(
    val palette: ZeroPalette,
    val typography: ZeroTypography,
    val shapes: ZeroShapes = ZeroShapes(),
    val spacing: ZeroSpacing = ZeroSpacing(),
    val density: ZeroDensity = ZeroDensity.Comfortable,
    val components: ZeroComponentTokens = ZeroComponentTokens.fromPalette(palette, density)
)
```

Most integrations only provide `palette`, `typography`, and optionally `shapes` / `density`. `components` can usually be omitted; ZeroUI derives button, field, chip, switch, and card tokens from `palette` and `density`.

## Palette

`ZeroPalette` is the semantic color set consumed by ZeroUI nodes and component tokens:

```kotlin
ZeroPalette(
    content = Color(0xFF10141F),
    mutedContent = Color(0xFF596273),
    primaryContent = Color(0xFF255CDE),
    successContent = Color(0xFF1C8F67),
    errorContent = Color(0xFFC84444),
    warningContent = Color(0xFF9A6A00),
    container = Color(0xFFF7F9FC),
    mutedContainer = Color(0xFFE6EBF2),
    primaryContainer = Color(0xFFDDE7FF),
    successContainer = Color(0xFFDDF5EC),
    errorContainer = Color(0xFFFFE2E0),
    warningContainer = Color(0xFFFFF1C7),
    outline = Color(0xFFC8D0DC),
    mutedOutline = Color(0xFFDCE2EA),
    focusedOutline = Color(0xFF255CDE),
    errorOutline = Color(0xFFC84444),
    unknownContainer = Color(0x1A000000),
    inverseContent = Color(0xFFF7F9FC),
    inverseContainer = Color(0xFF10141F)
)
```

These fields are not Material color role names. They are ZeroUI roles:

| Field | Used For |
| --- | --- |
| `content` | Default text/content |
| `mutedContent` | Secondary text/content |
| `primaryContent` | Primary actions and emphasized content |
| `successContent` | Success tone content |
| `errorContent` | Error tone content |
| `warningContent` | Warning tone content |
| `container` | Default cards/surfaces |
| `mutedContainer` | Muted cards/surfaces |
| `primaryContainer` | Primary tone containers |
| `successContainer` | Success tone containers |
| `errorContainer` | Error tone containers |
| `warningContainer` | Warning tone containers |
| `outline` | Default borders |
| `mutedOutline` | Low-emphasis borders |
| `focusedOutline` | Focused input border/cursor accent |
| `errorOutline` | Error input border |
| `unknownContainer` | Unknown node and unavailable placeholder background |
| `inverseContent` | Content rendered on inverse containers |
| `inverseContainer` | High-contrast inverse containers |

Text nodes can also set `surfaceTone` in JSON. It uses these same semantic roles for a local text background container. Text color remains controlled by `tone`; when `tone` is omitted, `surfaceTone` provides the matching default content color.

## Typography

`ZeroTypography` maps ZeroUI's schema-level text styles to Compose text styles:

```kotlin
ZeroTypography(
    display = typography.displayLarge.copy(
        fontSize = 48.sp,
        fontWeight = FontWeight.Black,
        letterSpacing = (-1.2).sp
    ),
    title = typography.headlineMedium,
    sectionTitle = typography.titleMedium,
    body = typography.bodyMedium,
    label = typography.labelLarge,
    support = typography.bodySmall
)
```

JSON pages can refer to `style: "display" | "title" | "sectionTitle" | "body" | "label" | "support"`. Unknown style values fall back to `body` so a future or misspelled style does not fail page parsing.

## Shapes

`ZeroShapes` is not Material3's `small` / `medium` / `large` shape set. It is ZeroUI's own small semantic token surface:

```kotlin
ZeroShapes(
    cardCornerRadius = 8.dp
)
```

Component-specific shapes are derived inside `ZeroComponentTokens` unless explicitly overridden.

## Spacing

`ZeroSpacing` currently covers SDK-owned fallback UI:

```kotlin
ZeroSpacing(
    unknownNodePadding = 12.dp,
    unknownNodeSpacing = 4.dp
)
```

Page layout spacing remains part of the JSON protocol (`spacing`, `layout.padding`, etc.), not the skin.

## Density

`ZeroDensity` controls derived component sizing:

```kotlin
ZeroDensity.Comfortable
ZeroDensity.Compact
```

`ZeroComponentTokens.fromPalette(palette, density)` uses this value to derive button, field, and chip dimensions.

## Custom Component Tokens

Only override `components` when the default derived tokens are not enough:

```kotlin
ZeroSkin(
    palette = palette,
    typography = typography,
    density = ZeroDensity.Compact,
    components = ZeroComponentTokens.fromPalette(palette, ZeroDensity.Compact)
)
```

For most brand packages, prefer palette/typography/density. That keeps the integration smaller and leaves new ZeroUI component defaults available as the SDK evolves.

## Non-Goals In 0.1.x

ZeroUI skin integration does not include:

- JSON skin schema
- Remote skin loading
- Per-page token overrides
- App-private skin protocols

Those can be designed later if real multi-brand or remote configuration needs appear. For now, brand packages should ship Kotlin code that constructs `ZeroSkin`.
