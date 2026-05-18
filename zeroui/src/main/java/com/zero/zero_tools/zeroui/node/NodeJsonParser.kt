package com.zero.zero_tools.zeroui.node

import com.zero.zero_tools.zeroui.condition.parseCondition
import com.zero.zero_tools.zeroui.interaction.mapObjects
import com.zero.zero_tools.zeroui.interaction.parseInteraction
import com.zero.zero_tools.zeroui.page.Layout
import com.zero.zero_tools.zeroui.text.Text
import com.zero.zero_tools.zeroui.text.optTextStyle
import com.zero.zero_tools.zeroui.text.optTone
import com.zero.zero_tools.zeroui.text.optToneOrNull
import com.zero.zero_tools.zeroui.text.parseText
import org.json.JSONArray
import org.json.JSONObject

internal fun parseNode(json: JSONObject): Node {
    return when (val type = json.getString("type")) {
        "column" -> Node.Column(
            spacing = json.optInt("spacing", 0),
            horizontalAlignment = json.optHorizontalAlignment("horizontalAlignment"),
            layout = json.optLayout(),
            children = json.getChildren()
        )

        "row" -> Node.Row(
            spacing = json.optInt("spacing", 0),
            verticalAlignment = json.optVerticalAlignment("verticalAlignment"),
            arrangement = json.optRowArrangement("arrangement"),
            layout = json.optLayout(),
            children = json.getChildren()
        )

        "lazyColumn" -> Node.LazyColumn(
            spacing = json.optInt("spacing", 0),
            layout = json.optLayout(),
            children = json.optChildren(),
            itemsKey = json.optStringOrNull("itemsKey"),
            item = json.optJSONObject("item")?.let(::parseNode),
            emptyChild = json.optJSONObject("emptyChild")?.let(::parseNode)
        )

        "lazyRow" -> Node.LazyRow(
            spacing = json.optInt("spacing", 0),
            verticalAlignment = json.optVerticalAlignment("verticalAlignment"),
            layout = json.optLayout(),
            children = json.optChildren(),
            itemsKey = json.optStringOrNull("itemsKey"),
            item = json.optJSONObject("item")?.let(::parseNode),
            emptyChild = json.optJSONObject("emptyChild")?.let(::parseNode)
        )

        "text" -> Node.Text(
            text = parseText(json.getJSONObject("text")),
            style = json.optTextStyle(),
            tone = json.optToneOrNull("tone"),
            surfaceTone = json.optToneOrNull("surfaceTone"),
            layout = json.optLayout(),
            onClick = json.optJSONObject("onClick")?.let(::parseInteraction)
        )

        "image" -> Node.Image(
            source = json.getJSONObject("source").parseImageSource(),
            contentDescription = json.optStringOrNull("contentDescription"),
            contentScale = json.optImageContentScale(),
            aspectRatio = json.optFloatOrNull("aspectRatio"),
            cornerRadius = json.optInt("cornerRadius", 0),
            layout = json.optLayout(default = Layout(fillMaxWidth = true)),
            onClick = json.optJSONObject("onClick")?.let(::parseInteraction)
        )

        "icon" -> Node.Icon(
            source = json.getJSONObject("source").parseIconSource(),
            contentDescription = json.optStringOrNull("contentDescription"),
            tone = json.optTone(),
            size = json.optInt("size", 24),
            tint = json.optBoolean("tint", true),
            layout = json.optLayout(),
            onClick = json.optJSONObject("onClick")?.let(::parseInteraction)
        )

        "textField" -> Node.TextField(
            label = json.getString("label"),
            value = json.getJSONObject("value").parseTextBinding(),
            onValueChange = parseInteraction(json.getJSONObject("onValueChange")),
            enabledKey = json.optStringOrNull("enabledKey"),
            layout = json.optLayout(default = Layout(fillMaxWidth = true))
        )

        "switch" -> Node.Switch(
            text = json.getString("text"),
            checkedKey = json.getString("checkedKey"),
            onCheckedChange = parseInteraction(json.getJSONObject("onCheckedChange")),
            enabledKey = json.optStringOrNull("enabledKey"),
            layout = json.optLayout()
        )

        "button" -> Node.Button(
            text = json.getString("text"),
            onClick = parseInteraction(json.getJSONObject("onClick")),
            variant = json.optButtonVariant(),
            icon = json.optJSONObject("icon")?.parseIconSource(),
            enabledKey = json.optStringOrNull("enabledKey"),
            layout = json.optLayout()
        )

        "chipGroup" -> Node.ChipGroup(
            selectedKey = json.getString("selectedKey"),
            options = json.getJSONArray("options").toChipOptions(),
            onSelected = parseInteraction(json.getJSONObject("onSelected")),
            spacing = json.optInt("spacing", 8),
            enabledKey = json.optStringOrNull("enabledKey"),
            layout = json.optLayout()
        )

        "card" -> Node.Card(
            tone = json.optTone(),
            padding = json.optInt("padding", 16),
            spacing = json.optInt("spacing", 8),
            layout = json.optLayout(default = Layout(fillMaxWidth = true)),
            onClick = json.optJSONObject("onClick")?.let(::parseInteraction),
            children = json.getChildren()
        )

        "spacer" -> Node.Spacer(
            height = json.optInt("height", 0),
            width = json.optInt("width", 0)
        )

        "condition" -> Node.Condition(
            condition = parseCondition(json.getJSONObject("condition")),
            child = parseNode(json.getJSONObject("child"))
        )

        "forEach" -> Node.ForEach(
            itemsKey = json.getString("itemsKey"),
            child = parseNode(json.getJSONObject("child")),
            spacing = json.optInt("spacing", 8),
            layout = json.optLayout()
        )

        "dialog" -> Node.Dialog(
            visibleKey = json.getString("visibleKey"),
            onDismiss = json.optJSONObject("onDismiss")?.let(::parseInteraction) ?: com.zero.zero_tools.zeroui.interaction.Interaction(),
            title = json.optJSONObject("title")?.let(::parseText),
            spacing = json.optInt("spacing", 8),
            padding = json.optInt("padding", 20),
            children = json.getChildren()
        )

        "box" -> Node.Box(
            contentAlignment = json.optBoxAlignment(),
            layout = json.optLayout(),
            onClick = json.optJSONObject("onClick")?.let(::parseInteraction),
            children = json.getChildren()
        )

        "divider" -> Node.Divider(
            thickness = json.optInt("thickness", 1),
            tone = json.optToneOrNull("tone"),
            layout = json.optLayout(default = Layout(fillMaxWidth = true))
        )

        "checkbox" -> Node.Checkbox(
            text = json.getString("text"),
            checkedKey = json.getString("checkedKey"),
            onCheckedChange = parseInteraction(json.getJSONObject("onCheckedChange")),
            enabledKey = json.optStringOrNull("enabledKey"),
            layout = json.optLayout()
        )

        "radioGroup" -> Node.RadioGroup(
            selectedKey = json.getString("selectedKey"),
            options = json.getJSONArray("options").toRadioOptions(),
            onSelected = parseInteraction(json.getJSONObject("onSelected")),
            spacing = json.optInt("spacing", 8),
            enabledKey = json.optStringOrNull("enabledKey"),
            layout = json.optLayout()
        )

        "progress" -> Node.Progress(
            variant = json.optProgressVariant(),
            progressKey = json.optStringOrNull("progressKey"),
            tone = json.optTone(),
            layout = json.optLayout()
        )

        "slider" -> Node.Slider(
            valueKey = json.getString("valueKey"),
            onValueChange = parseInteraction(json.getJSONObject("onValueChange")),
            min = json.optFloat("min", 0f),
            max = json.optFloat("max", 1f),
            steps = json.optInt("steps", 0),
            enabledKey = json.optStringOrNull("enabledKey"),
            layout = json.optLayout(default = Layout(fillMaxWidth = true))
        )

        "select" -> Node.Select(
            selectedKey = json.getString("selectedKey"),
            options = json.getJSONArray("options").toSelectOptions(),
            onSelected = parseInteraction(json.getJSONObject("onSelected")),
            label = json.optStringOrNull("label"),
            enabledKey = json.optStringOrNull("enabledKey"),
            layout = json.optLayout(default = Layout(fillMaxWidth = true))
        )

        "snackbar" -> Node.Snackbar(
            visibleKey = json.getString("visibleKey"),
            message = parseText(json.getJSONObject("message")),
            actionLabel = json.optStringOrNull("actionLabel"),
            onAction = json.optJSONObject("onAction")?.let(::parseInteraction),
            onDismiss = json.optJSONObject("onDismiss")?.let(::parseInteraction) ?: com.zero.zero_tools.zeroui.interaction.Interaction()
        )

        "bottomSheet" -> Node.BottomSheet(
            visibleKey = json.getString("visibleKey"),
            onDismiss = json.optJSONObject("onDismiss")?.let(::parseInteraction) ?: com.zero.zero_tools.zeroui.interaction.Interaction(),
            title = json.optJSONObject("title")?.let(::parseText),
            spacing = json.optInt("spacing", 8),
            padding = json.optInt("padding", 20),
            children = json.getChildren()
        )

        else -> Node.Unknown(
            typeName = type,
            raw = json.toString()
        )
    }
}

private fun JSONObject.parseTextBinding(): Text.Binding {
    return parseText(this) as? Text.Binding
        ?: error("Expected ZeroUI text binding for textField value")
}

private fun JSONObject.getChildren(): List<Node> {
    return getJSONArray("children").mapObjects(::parseNode)
}

private fun JSONObject.optChildren(): List<Node> {
    return optJSONArray("children")?.mapObjects(::parseNode).orEmpty()
}

private fun JSONArray.toChipOptions(): List<ChipOption> {
    return mapObjects { json ->
        ChipOption(
            label = json.getString("label"),
            value = json.getString("value"),
            icon = json.optJSONObject("icon")?.parseIconSource()
        )
    }
}

private fun JSONArray.toRadioOptions(): List<RadioOption> {
    return mapObjects { json ->
        RadioOption(
            label = json.getString("label"),
            value = json.getString("value")
        )
    }
}

private fun JSONArray.toSelectOptions(): List<SelectOption> {
    return mapObjects { json ->
        SelectOption(
            label = json.getString("label"),
            value = json.getString("value")
        )
    }
}

private fun JSONObject.parseImageSource(): ImageSource {
    return when (val type = getString("type")) {
        "url" -> ImageSource.Url(getString("url"))
        "resource" -> ImageSource.Resource(getString("name"))
        "binding" -> ImageSource.Binding(
            key = getString("key"),
            fallback = optString("fallback", "")
        )

        else -> error("Unsupported ZeroUI image source type: $type")
    }
}

private fun JSONObject.parseIconSource(): IconSource {
    return when (val type = getString("type")) {
        "url" -> IconSource.Url(getString("url"))
        "resource" -> IconSource.Resource(getString("name"))
        "binding" -> IconSource.Binding(
            key = getString("key"),
            fallback = optString("fallback", "")
        )

        else -> error("Unsupported ZeroUI icon source type: $type")
    }
}

private fun JSONObject.optImageContentScale(): ImageContentScale {
    return when (val value = optString("contentScale", "fit")) {
        "fit" -> ImageContentScale.Fit
        "crop" -> ImageContentScale.Crop
        "fillWidth" -> ImageContentScale.FillWidth
        else -> error("Unsupported ZeroUI image contentScale: $value")
    }
}

private fun JSONObject.optStringOrNull(name: String): String? {
    return if (has(name) && !isNull(name)) getString(name) else null
}

private fun JSONObject.optFloatOrNull(name: String): Float? {
    return if (has(name) && !isNull(name)) getDouble(name).toFloat() else null
}

private fun JSONObject.optLayout(default: Layout = Layout()): Layout {
    val json = optJSONObject("layout") ?: return default
    val padding = json.optPadding(default.padding)

    return Layout(
        fillMaxWidth = json.optBoolean("fillMaxWidth", default.fillMaxWidth),
        fillMaxHeight = json.optBoolean("fillMaxHeight", default.fillMaxHeight),
        weight = json.optFloat("weight", default.weight),
        padding = padding.all,
        paddingStart = padding.start ?: default.paddingStart,
        paddingTop = padding.top ?: default.paddingTop,
        paddingEnd = padding.end ?: default.paddingEnd,
        paddingBottom = padding.bottom ?: default.paddingBottom,
        width = json.optInt("width", default.width),
        height = json.optInt("height", default.height),
        minWidth = json.optInt("minWidth", default.minWidth),
        minHeight = json.optInt("minHeight", default.minHeight),
        maxWidth = json.optInt("maxWidth", default.maxWidth),
        maxHeight = json.optInt("maxHeight", default.maxHeight),
        background = json.optToneOrNull("background")
    )
}

private data class ParsedPadding(
    val all: Int,
    val start: Int? = null,
    val top: Int? = null,
    val end: Int? = null,
    val bottom: Int? = null
)

private fun JSONObject.optPadding(default: Int): ParsedPadding {
    val raw = opt("padding") ?: return ParsedPadding(default)
    val json = raw as? JSONObject ?: return ParsedPadding(optInt("padding", default))
    val all = json.optInt("all", default)
    return ParsedPadding(
        all = all,
        start = json.optNullableInt("start") ?: json.optNullableInt("left"),
        top = json.optNullableInt("top"),
        end = json.optNullableInt("end") ?: json.optNullableInt("right"),
        bottom = json.optNullableInt("bottom")
    )
}

private fun JSONObject.optNullableInt(name: String): Int? {
    return if (has(name) && !isNull(name)) getInt(name) else null
}

private fun JSONObject.optFloat(name: String, default: Float): Float {
    return if (has(name) && !isNull(name)) getDouble(name).toFloat() else default
}

private fun JSONObject.optHorizontalAlignment(name: String): HorizontalAlignment {
    return when (val value = optString(name, "start")) {
        "start" -> HorizontalAlignment.Start
        "center" -> HorizontalAlignment.Center
        "end" -> HorizontalAlignment.End
        else -> error("Unsupported ZeroUI horizontalAlignment: $value")
    }
}

private fun JSONObject.optVerticalAlignment(name: String): VerticalAlignment {
    return when (val value = optString(name, "top")) {
        "top" -> VerticalAlignment.Top
        "center" -> VerticalAlignment.Center
        "bottom" -> VerticalAlignment.Bottom
        "baseline" -> VerticalAlignment.Baseline
        else -> error("Unsupported ZeroUI verticalAlignment: $value")
    }
}

private fun JSONObject.optRowArrangement(name: String): RowArrangement? {
    if (!has(name) || isNull(name)) return null
    return when (val value = getString(name)) {
        "start" -> RowArrangement.Start
        "center" -> RowArrangement.Center
        "end" -> RowArrangement.End
        "spaceBetween" -> RowArrangement.SpaceBetween
        "spaceAround" -> RowArrangement.SpaceAround
        "spaceEvenly" -> RowArrangement.SpaceEvenly
        else -> error("Unsupported ZeroUI row arrangement: $value")
    }
}

private fun JSONObject.optButtonVariant(): ButtonVariant {
    return optString("variant", "primary").toButtonVariant()
}

private fun JSONObject.optProgressVariant(): ProgressVariant {
    return when (val value = optString("variant", "linear")) {
        "linear" -> ProgressVariant.Linear
        "circular" -> ProgressVariant.Circular
        else -> error("Unsupported ZeroUI progress variant: $value")
    }
}

private fun JSONObject.optBoxAlignment(): BoxAlignment {
    return when (val value = optString("contentAlignment", "topStart")) {
        "topStart" -> BoxAlignment.TopStart
        "topCenter" -> BoxAlignment.TopCenter
        "topEnd" -> BoxAlignment.TopEnd
        "centerStart" -> BoxAlignment.CenterStart
        "center" -> BoxAlignment.Center
        "centerEnd" -> BoxAlignment.CenterEnd
        "bottomStart" -> BoxAlignment.BottomStart
        "bottomCenter" -> BoxAlignment.BottomCenter
        "bottomEnd" -> BoxAlignment.BottomEnd
        else -> error("Unsupported ZeroUI box contentAlignment: $value")
    }
}

private fun String.toButtonVariant(): ButtonVariant {
    return when (this) {
        "primary" -> ButtonVariant.Primary
        "secondary" -> ButtonVariant.Secondary
        else -> error("Unsupported ZeroUI button variant: $this")
    }
}
