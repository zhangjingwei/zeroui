package com.zero.zero_tools.zeroui.node

import com.zero.zero_tools.zeroui.condition.parseCondition
import com.zero.zero_tools.zeroui.interaction.mapObjects
import com.zero.zero_tools.zeroui.interaction.parseInteraction
import com.zero.zero_tools.zeroui.page.Layout
import com.zero.zero_tools.zeroui.text.Text
import com.zero.zero_tools.zeroui.text.optTextStyle
import com.zero.zero_tools.zeroui.text.optTone
import com.zero.zero_tools.zeroui.text.parseText
import org.json.JSONArray
import org.json.JSONObject

internal fun parseNode(json: JSONObject): Node {
    return when (val type = json.getString("type")) {
        "column" -> Node.Column(
            spacing = json.optInt("spacing", 0),
            layout = json.optLayout(),
            children = json.getChildren()
        )

        "row" -> Node.Row(
            spacing = json.optInt("spacing", 0),
            layout = json.optLayout(),
            children = json.getChildren()
        )

        "lazyColumn" -> Node.LazyColumn(
            spacing = json.optInt("spacing", 0),
            layout = json.optLayout(),
            children = json.optChildren(),
            itemsKey = json.optStringOrNull("itemsKey"),
            item = json.optJSONObject("item")?.let(::parseNode)
        )

        "text" -> Node.Text(
            text = parseText(json.getJSONObject("text")),
            style = json.optTextStyle(),
            tone = json.optTone(),
            layout = json.optLayout()
        )

        "image" -> Node.Image(
            source = json.getJSONObject("source").parseImageSource(),
            contentDescription = json.optStringOrNull("contentDescription"),
            contentScale = json.optImageContentScale(),
            aspectRatio = json.optFloatOrNull("aspectRatio"),
            cornerRadius = json.optInt("cornerRadius", 0),
            layout = json.optLayout(default = Layout(fillMaxWidth = true))
        )

        "textField" -> Node.TextField(
            label = json.getString("label"),
            value = json.getJSONObject("value").parseTextBinding(),
            onValueChange = parseInteraction(json.getJSONObject("onValueChange")),
            layout = json.optLayout(default = Layout(fillMaxWidth = true))
        )

        "switch" -> Node.Switch(
            text = json.getString("text"),
            checkedKey = json.getString("checkedKey"),
            onCheckedChange = parseInteraction(json.getJSONObject("onCheckedChange")),
            layout = json.optLayout()
        )

        "button" -> Node.Button(
            text = json.getString("text"),
            onClick = parseInteraction(json.getJSONObject("onClick")),
            variant = json.optButtonVariant(),
            layout = json.optLayout()
        )

        "chipGroup" -> Node.ChipGroup(
            selectedKey = json.getString("selectedKey"),
            options = json.getJSONArray("options").toChipOptions(),
            onSelected = parseInteraction(json.getJSONObject("onSelected")),
            spacing = json.optInt("spacing", 8),
            layout = json.optLayout()
        )

        "card" -> Node.Card(
            tone = json.optTone(),
            padding = json.optInt("padding", 16),
            spacing = json.optInt("spacing", 8),
            layout = json.optLayout(default = Layout(fillMaxWidth = true)),
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

    return Layout(
        fillMaxWidth = json.optBoolean("fillMaxWidth", default.fillMaxWidth),
        padding = json.optInt("padding", default.padding),
        maxHeight = json.optInt("maxHeight", default.maxHeight)
    )
}

private fun JSONObject.optButtonVariant(): ButtonVariant {
    return optString("variant", "primary").toButtonVariant()
}

private fun String.toButtonVariant(): ButtonVariant {
    return when (this) {
        "primary" -> ButtonVariant.Primary
        "secondary" -> ButtonVariant.Secondary
        else -> error("Unsupported ZeroUI button variant: $this")
    }
}
