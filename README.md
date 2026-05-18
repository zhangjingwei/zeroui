# ZeroUI

中文 | [English](README.en.md)

ZeroUI 是一个面向 Android 的轻量级 Server-Driven UI 运行时，让应用能够通过 JSON 动态下发并渲染原生 Compose 页面。无需频繁发版，即可快速搭建活动页、配置页、动态表单和业务工具界面，同时保留原生性能、状态管理、动作分发与宿主能力接入。

## 5 分钟开始

添加 JitPack 仓库：

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

然后添加 `zeroui` Android library 依赖：

```kotlin
dependencies {
    implementation("com.github.zhangjingwei:zeroui:v0.1.4")
}
```

在发布到团队仓库前，可以先在本地验证：

```bash
./gradlew :zeroui:publishReleasePublicationToMavenLocal
```

然后从 `assets/pages/home.json` 渲染页面：

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

将 JSON 放到 `src/main/assets/pages/home.json`：

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

## 宿主扩展点

`ZeroUiHost` 接收主要的 SDK 扩展点：

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

使用 `PageLoader` 从 assets、内存或预填充缓存中加载 schema。它在 `0.1.x` 中有意保持同步；远程 schema 应由宿主预取，并通过带缓存的 loader 提供。

使用 `HttpClient` 将默认的 `HttpURLConnection` 实现替换为你自己的网络栈。使用 `Tracker` 将 `track` effect 转发到你的埋点系统。

使用 `externalNavigator` 将 `navigate` 的 `route` / `url` / `external` target 转发给宿主导航，例如 Compose Navigation。`page` target 仍由 `ZeroUiHost` 自己的 JSON 页栈处理；当 ZeroUI 页栈不能继续后退时，`back` 会转交给 `externalNavigator.back()`。

使用 `ZeroImageLoader` 接入你的图片/图标加载栈。内置的 `rememberDefaultZeroImageLoader()` 会对 drawable 资源（含 vector drawable）和 HTTP(S) URL 做尺寸约束、维护一份小型内存 LRU，并强制 `http`/`https` 的 scheme 白名单。如果你的业务依赖懒加载列表、动图、磁盘缓存或请求优先级，请改为通过 `ZeroUiHost(imageLoader = ...)` 接入 Coil/Glide 实现：

```kotlin
class CoilZeroImageLoader(...) : ZeroImageLoader {
    override fun load(request: ZeroImageRequest, onResult: (ZeroImageResult) -> Unit): Cancelable {
        // 把 request.source.url / .name 路由进 Coil；onResult 必须在主线程回调。
    }
}
```

## 皮肤系统

ZeroUI 在 JSON 中暴露语义化的 `style` 和 `tone`，Android 实现细节则保留在 `ZeroSkin` 内。

`ZeroSkinProvider` 接收宿主或品牌集成包构造好的 `ZeroSkin` 对象；它不接收 JSON 字符串、文件路径或远程 loader。外部品牌包推荐导出 `rememberBrandZeroSkin(): ZeroSkin`，再由宿主传入：

```kotlin
ZeroSkinProvider(
    skin = rememberBrandZeroSkin(darkTheme)
) {
    ZeroUiHost(startPage = "home")
}
```

字段级说明见 [docs/skin.md](docs/skin.md)，可复制模板见 [examples/brand-skin-integration/BrandZeroSkin.kt](examples/brand-skin-integration/BrandZeroSkin.kt)。给 AI agent 的接入约束见 [docs/ai-integration.md](docs/ai-integration.md)。

当前组件 token 覆盖：

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

示例页面 `assets/pages/showcase.json` 会覆盖这些由皮肤驱动的组件，便于视觉走查。

## 发布

`zeroui` 模块会通过 JitPack 发布带 sources 的 release AAR：

```text
groupId: com.github.zhangjingwei
artifactId: zeroui
version: v0.1.4
```

发布前先在本地验证：

```bash
./gradlew :zeroui:publishReleasePublicationToMavenLocal
```

然后在 GitHub 创建 release：

- Tag: `v0.1.4`
- Target: `main`
- Release title: `v0.1.4`
- Release notes: `ZeroUI 0.1.4 增强 HTTP 请求状态、取消、缓存、重试和响应映射能力。`
- 不勾选 `Set as a pre-release`

发布后打开 [JitPack](https://jitpack.io/#zhangjingwei/zeroui/v0.1.4)，点击 `Get it` 触发构建。构建成功后即可通过上面的 Gradle 依赖引入。

## 协议速查

`schemaVersion: 1` 继续向后兼容。ZeroUI 在 v1 内通过可选字段扩展能力，旧页面无需迁移。

通用 layout 字段可用于多数可见节点：

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

`column` 支持 `spacing` 和 `horizontalAlignment: "start" | "center" | "end"`；`row` 支持 `spacing`、`verticalAlignment: "top" | "center" | "bottom" | "baseline"`，以及可选 `arrangement: "start" | "center" | "end" | "spaceBetween" | "spaceAround" | "spaceEvenly"`。`arrangement` 控制水平排列，`verticalAlignment` 控制垂直对齐；`baseline` 会对 row 子节点应用 Compose `alignByBaseline()`，适合数字和单位混排。未设置 `arrangement` 时，`row` 使用 `spacing` 渲染间距；设置后直接透传 Compose `Arrangement.Horizontal`。子节点可通过 `layout.weight` 在父 `row` / `column` 中分配剩余空间。

`lazyColumn` 与 `lazyRow` 都支持静态 `children`、`itemsKey` 和可复用 `item` 模板；模板内可通过 `item.*` 与 `itemIndex` 读取当前项。

`card` 支持 `padding` 和 `spacing`。`padding` 控制内容内边距，`spacing` 控制子节点之间的垂直间距。

`text`、`image`、`card`、`button` 使用一致的 `onClick` 交互模型。`button.onClick` 仍为必填；`text` / `image` / `card` 的 `onClick` 为可选。

`text.surfaceTone` 可选，取值同 `tone`，控制文本背景 container，并自带默认 inside padding（水平 10dp、垂直 4dp）。文字颜色仍由 `tone` 控制；如果没有显式设置 `tone`，才会使用 `surfaceTone` 匹配的 content 作为默认文字色。`layout.padding` 仍是 outside padding，可与 surface inside padding 叠加。

`icon` 是通用图标节点，不包含业务专用 `iconText` / `iconButton`。它使用通用 source：

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

`navigate` 支持旧写法，也支持 ZeroUI 通用 target 语义：

```json
{
  "type": "navigate",
  "target": {
    "type": "page",
    "name": { "type": "literal", "value": "detail" }
  }
}
```

`type` 可为 `page`、`route`、`url`、`external`。内置 `ZeroUiHost` 使用自己的页栈消费 `page` target；`route` / `url` / `external` target 会转发给 `ZeroUiHost(externalNavigator = ...)`，可接 Compose Navigation、浏览器或宿主业务导航。不引入 `page://`、`sdui://` 之类 app 私有 scheme。

节点：

| Node | 稳定性 |
| --- | --- |
| `column`, `row`, `box`, `text`, `textField`, `switch`, `button`, `chipGroup`, `card`, `spacer`, `condition`, `image`, `icon`, `divider` | 面向 `0.1.x` 内部或团队使用保持稳定（`image` / `icon` 需要 `ZeroImageLoader`；默认 loader 覆盖 drawable 资源与 HTTP/HTTPS URL）|
| `checkbox`, `radioGroup`, `slider`, `progress`, `select`, `snackbar` | 面向 `0.1.x` 内部或团队使用保持稳定；通过 `enabledKey` 绑定布尔状态控制禁用态 |
| `forEach`, `lazyColumn`, `lazyRow`, `dialog`, `bottomSheet` | `0.1.x` 实验能力；应放在服务端能力开关后 |

动作：
`setState`, `appendState`, `incrementState`, `toggleState`, `clearState`, `resetState`, `validate`, `batch`。`setState` / `clearState` 支持点路径键（如 `form.name`、`items[0].status`）直接读写嵌套 Record。

效果：
`toast`, `log`, `track`, `navigate`, `back`, `http`。

交互策略：交互对象可以设置 `debounceMillis` 防抖和 `throttleMillis` 节流，防止按钮快速连击或搜索输入触发过多请求。

`http` 支持 `params` 查询参数构建、`timeoutMs`、`retryCount` / `retryDelayMs`、`requestKey` + `cancelPrevious`、`cachePolicy: "networkOnly" | "cacheFirst"`、`stateKey` 标准请求状态、`responseMode: "body" | "full"`、`map` 响应字段映射，以及 `onStart` / `onSuccess` / `onError` / `onFinally` 生命周期。`map` 数组项可用 `mode: "replace" | "append"`，用于刷新列表或 load-more 分页追加。

文本样式：
`display`, `title`, `sectionTitle`, `body`, `label`, `support`。未知 `style` 会回落到 `body`，避免单个样式拼写问题导致页面解析失败。

色调：
`default`, `muted`, `primary`, `success`, `error`, `warning`, `inverse`, `info`, `accent`, `disabled`。

## 协议版本

页面可以包含顶层 `schemaVersion` 整数。缺失时默认使用 `1`。

ZeroUI `0.1.x` 当前支持协议版本 `1`。解析器会在 `Page.schemaVersion` 中保留页面版本；宿主可以将其用于遥测、预检或服务端灰度控制。

`0.1.x` 的兼容策略：

- 在 schema version `1` 内保持现有协议字段向后兼容。
- 允许添加字段，前提是旧客户端可以忽略。
- 不支持的节点 `type` 会渲染为 `Node.Unknown` 占位，并可通过 `ZeroUiHost(onUnknownNode = ...)` 观察。
- 破坏性协议变更应使用新的 `schemaVersion`，并通过客户端能力在服务端进行开关控制。

## 已知限制

ZeroUI `0.1.x` 已适合内部源码或模块复用，以及早期 Maven 风格的团队内使用。它还不是通用的公开 SDK。

当前仍缺少的一些能力：
配置变更或进程死亡后的状态恢复、二进制兼容性校验，以及正式发布流水线。

`0.1.x` 中已经提供但仍处于实验阶段的协议和渲染能力：
`lazyColumn`, `lazyRow`, `dialog`, `forEach`。请将它们视为早期能力，并按客户端能力进行灰度。

## API 边界

`zeroui` 模块的 main sources 使用 Kotlin explicit API mode 编译。公开声明都是有意暴露的 SDK surface；实现细节保持为 `internal` 或 `private`。
