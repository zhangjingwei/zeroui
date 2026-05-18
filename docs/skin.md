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
    val elevation: ZeroElevation = ZeroElevation(),
    val stateLayer: ZeroStateLayer = ZeroStateLayer(),
    val components: ZeroComponentTokens = ZeroComponentTokens.fromPalette(palette, density)
)
```

Most integrations only provide `palette`, `typography`, and optionally `shapes` / `density`. `elevation`, `stateLayer`, and `components` have sensible defaults and can be omitted for brand packages that only need color and type customisation.

## Palette

`ZeroPalette` is the semantic color set consumed by ZeroUI nodes and component tokens. Required fields must be set; optional fields default to values derived from nearby required fields.

```kotlin
ZeroPalette(
    // required
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
    // optional — defaults shown
    inverseContent = container,
    inverseContainer = content,
    infoContent = primaryContent,
    infoContainer = primaryContainer,
    accentContent = primaryContent,
    accentContainer = primaryContainer,
    disabledContent = content.copy(alpha = 0.38f),
    disabledContainer = mutedContainer.copy(alpha = 0.12f)
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
| `infoContent` / `infoContainer` | Info tone — neutral informational, distinct from primary |
| `accentContent` / `accentContainer` | Accent tone — brand secondary color, defaults to primary |
| `disabledContent` / `disabledContainer` | Disabled tone — non-interactive surfaces and text |

Text nodes can also set `surfaceTone` in JSON. It uses these same semantic roles for a local text background container and applies a built-in inside padding of 10dp horizontal and 4dp vertical. Text color remains controlled by `tone`; when `tone` is omitted, `surfaceTone` provides the matching default content color. `layout.padding` remains outside padding.

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

`ZeroShapes` provides a six-step corner radius scale plus a legacy `cardCornerRadius` field for backward compatibility:

```kotlin
ZeroShapes(
    extraSmall = 4.dp,
    small = 8.dp,
    medium = 12.dp,    // default
    large = 16.dp,
    extraLarge = 28.dp,
    full = 999.dp,
    cardCornerRadius = 12.dp  // kept for compat; prefer medium
)
```

Component-specific shapes are derived inside `ZeroComponentTokens` unless explicitly overridden.

## Spacing

`ZeroSpacing` provides a named scale for use in component and layout code:

```kotlin
ZeroSpacing(
    xs = 4.dp,
    s = 8.dp,
    m = 12.dp,
    l = 16.dp,
    xl = 24.dp,
    xxl = 32.dp,
    unknownNodePadding = 12.dp,  // SDK fallback UI
    unknownNodeSpacing = 4.dp
)
```

Page layout spacing remains part of the JSON protocol (`spacing`, `layout.padding`, etc.), not the skin.

## Elevation

`ZeroElevation` provides shadow levels for raised surfaces:

```kotlin
ZeroElevation(
    level0 = 0.dp,
    level1 = 1.dp,
    level2 = 3.dp,
    level3 = 6.dp,
    level4 = 8.dp
)
```

Use `level1`–`level2` for cards and chips, `level3`–`level4` for sheets and menus.

## State Layer

`ZeroStateLayer` defines the alpha values applied to the content color to produce pressed, focused, hovered, selected, and disabled overlays:

```kotlin
ZeroStateLayer(
    pressedAlpha = 0.12f,
    focusedAlpha = 0.12f,
    hoveredAlpha = 0.08f,
    selectedAlpha = 0.08f,
    draggedAlpha = 0.16f,
    disabledContentAlpha = 0.38f,
    disabledContainerAlpha = 0.12f
)
```

Component renderers read `skin.stateLayer` to apply consistent interaction feedback without hardcoding alphas.

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

`ZeroComponentTokens.fromPalette` derives tokens for: Button, TextField, ChipGroup, Switch, Card, Checkbox, Radio, Slider, Progress, Snackbar, Divider, and BottomSheet. For most brand packages, prefer palette/typography/density. That keeps the integration smaller and leaves new ZeroUI component defaults available as the SDK evolves.

## Non-Goals In 0.1.x

ZeroUI skin integration does not include:

- JSON skin schema
- Remote skin loading
- Per-page token overrides
- App-private skin protocols

Those can be designed later if real multi-brand or remote configuration needs appear. For now, brand packages should ship Kotlin code that constructs `ZeroSkin`.
