# ZeroUI

中文 | [English](README.en.md)

ZeroUI 是一个面向 Android 的轻量级 Server-Driven UI 运行时，让应用能够通过 JSON 动态下发并渲染原生 Compose 页面。无需频繁发版，即可快速搭建活动页、配置页、动态表单和业务工具界面，同时保留原生性能、状态管理、动作分发与宿主能力接入。

## 5 分钟开始

添加 `zeroui` Android library 模块，或使用已发布的产物：

```kotlin
dependencies {
    implementation("com.zero.zero-tools:zeroui:0.1.0")
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
    onUnknownNode = { typeName, raw -> /* report unsupported schema */ }
)
```

使用 `PageLoader` 从 assets、内存或预填充缓存中加载 schema。它在 `0.1.x` 中有意保持同步；远程 schema 应由宿主预取，并通过带缓存的 loader 提供。

使用 `HttpClient` 将默认的 `HttpURLConnection` 实现替换为你自己的网络栈。使用 `Tracker` 将 `track` effect 转发到你的埋点系统。

使用 `ZeroImageLoader` 接入你的图片加载栈。内置的 `rememberDefaultZeroImageLoader()` 会对 drawable 资源和 HTTP(S) URL 做 `inSampleSize` 下采样、维护一份小型内存 LRU，并强制 `http`/`https` 的 scheme 白名单。如果你的业务依赖懒加载列表、动图、磁盘缓存或请求优先级，请改为通过 `ZeroUiHost(imageLoader = ...)` 接入 Coil/Glide 实现：

```kotlin
class CoilZeroImageLoader(...) : ZeroImageLoader {
    override fun load(request: ZeroImageRequest, onResult: (ZeroImageResult) -> Unit): Cancelable {
        // 把 request.source.url / .name 路由进 Coil；onResult 必须在主线程回调。
    }
}
```

## 皮肤系统

ZeroUI 在 JSON 中暴露语义化的 `style` 和 `tone`，Android 实现细节则保留在 `ZeroSkin` 内。

当前组件 token 覆盖：

- Button
- TextField
- ChipGroup
- Switch
- Card

示例页面 `assets/pages/showcase.json` 会覆盖这些由皮肤驱动的组件，便于视觉走查。

## 发布

`zeroui` 模块会使用以下 Maven 坐标发布带 sources 的 release AAR：

```text
groupId: com.zero.zero-tools
artifactId: zeroui
version: 0.1.0
```

`publishReleasePublicationToMavenLocal` 已配置用于本地和团队内验证。对于 JitPack 或私有 Maven 仓库，让仓库服务指向 `:zeroui` 的 release publication，并复用同一组坐标即可。

## 协议速查

节点：

| Node | 稳定性 |
| --- | --- |
| `column`, `row`, `text`, `textField`, `switch`, `button`, `chipGroup`, `card`, `spacer`, `condition`, `image` | 面向 `0.1.x` 内部或团队使用保持稳定（`image` 需要 `ZeroImageLoader`；默认 loader 覆盖资源与 HTTP/HTTPS URL）|
| `forEach`, `lazyColumn`, `dialog` | `0.1.x` 实验能力；应放在服务端能力开关后 |

动作：
`setState`, `incrementState`, `toggleState`, `batch`。

效果：
`toast`, `log`, `track`, `navigate`, `back`, `http`。

文本样式：
`title`, `sectionTitle`, `body`, `label`。

色调：
`default`, `muted`, `primary`, `success`, `error`, `warning`。

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
`lazyColumn`, `dialog`, `forEach`。请将它们视为早期能力，并按客户端能力进行灰度。

## API 边界

`zeroui` 模块的 main sources 使用 Kotlin explicit API mode 编译。公开声明都是有意暴露的 SDK surface；实现细节保持为 `internal` 或 `private`。
