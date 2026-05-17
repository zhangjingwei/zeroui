package com.zero.zero_tools

import com.zero.zero_tools.zeroui.action.Action
import com.zero.zero_tools.zeroui.condition.Condition
import com.zero.zero_tools.zeroui.core.parseZeroUiPage
import com.zero.zero_tools.zeroui.effect.Effect
import com.zero.zero_tools.zeroui.node.ImageContentScale
import com.zero.zero_tools.zeroui.node.ImageSource
import com.zero.zero_tools.zeroui.node.ButtonVariant
import com.zero.zero_tools.zeroui.node.HorizontalAlignment
import com.zero.zero_tools.zeroui.node.IconSource
import com.zero.zero_tools.zeroui.node.Node
import com.zero.zero_tools.zeroui.node.VerticalAlignment
import com.zero.zero_tools.zeroui.page.ZeroUiSchemaVersion
import com.zero.zero_tools.zeroui.state.StateOwner
import com.zero.zero_tools.zeroui.text.Text
import com.zero.zero_tools.zeroui.text.TextStyle
import com.zero.zero_tools.zeroui.text.Tone
import com.zero.zero_tools.zeroui.value.Value
import com.zero.zero_tools.zeroui.value.ValueSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests cover every Node / Action / Effect / Condition / ValueSource subtype
 * the protocol currently supports, plus the Fallback (unknown node) path.
 */
class ZeroUiParserTest {

    @Test
    fun parsesMinimalPageWithInitialState() {
        val json = """
            {
              "initialState": {
                "title": "Hello",
                "count": { "value": 3, "owner": "shared" }
              },
              "root": { "type": "column", "children": [] }
            }
        """.trimIndent()

        val page = parseZeroUiPage(json)

        assertEquals(Value.Text("Hello"), page.initialState.values["title"]?.value)
        assertEquals(Value.Number(3), page.initialState.values["count"]?.value)
        assertEquals(StateOwner.Shared, page.initialState.values["count"]?.owner)
        assertEquals(StateOwner.Client, page.initialState.values["title"]?.owner)
        assertEquals(ZeroUiSchemaVersion.Current, page.schemaVersion)
        assertTrue(page.root is Node.Column)
    }

    @Test
    fun parsesExplicitSchemaVersion() {
        val page = parseZeroUiPage(
            """
              {
                "schemaVersion": 7,
                "root": { "type": "column", "children": [] }
              }
            """.trimIndent()
        )

        assertEquals(7, page.schemaVersion)
    }

    @Test
    fun parsesTextValueAndBinding() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "column",
                  "children": [
                    { "type": "text", "text": { "type": "value", "value": "Hi" }, "style": "title", "tone": "primary" },
                    {
                      "type": "text",
                      "text": {
                        "type": "binding",
                        "key": "user",
                        "fallback": "Guest",
                        "format": "user: {value}"
                      }
                    }
                  ]
                }
              }
            """.trimIndent()
        )

        val column = page.root as Node.Column
        val first = column.children[0] as Node.Text
        assertEquals(Text.Value("Hi"), first.text)
        assertEquals(TextStyle.Title, first.style)
        assertEquals(Tone.Primary, first.tone)

        val second = column.children[1] as Node.Text
        val binding = second.text as Text.Binding
        assertEquals("user", binding.key)
        assertEquals("Guest", binding.fallback)
        assertEquals("user: {value}", binding.format)
    }

    @Test
    fun parsesSupportTextStyleAndFallsBackForUnknownStyle() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "column",
                  "children": [
                    {
                      "type": "text",
                      "style": "support",
                      "text": { "type": "value", "value": "Helper copy" }
                    },
                    {
                      "type": "text",
                      "style": "captionFromFuture",
                      "text": { "type": "value", "value": "Future copy" }
                    }
                  ]
                }
              }
            """.trimIndent()
        )

        val column = page.root as Node.Column
        assertEquals(TextStyle.Support, (column.children[0] as Node.Text).style)
        assertEquals(TextStyle.Body, (column.children[1] as Node.Text).style)
    }

    @Test
    fun parsesImageLazyColumnAndDialogNodes() {
        val page = parseZeroUiPage(
            """
              {
                "initialState": {
                  "showDialog": true,
                  "photos": [
                    { "url": "https://example.test/a.png", "title": "A" },
                    { "url": "https://example.test/b.png", "title": "B" }
                  ]
                },
                "root": {
                  "type": "lazyColumn",
                  "spacing": 10,
                  "children": [
                    {
                      "type": "image",
                      "source": { "type": "resource", "name": "hero" },
                      "contentDescription": "Hero",
                      "contentScale": "crop",
                      "aspectRatio": 1.7,
                      "cornerRadius": 14
                    }
                  ],
                  "itemsKey": "photos",
                  "item": {
                    "type": "image",
                    "source": { "type": "binding", "key": "item.url", "fallback": "" },
                    "contentScale": "fillWidth"
                  }
                }
              }
            """.trimIndent()
        )

        val lazyColumn = page.root as Node.LazyColumn
        assertEquals(10, lazyColumn.spacing)
        assertEquals("photos", lazyColumn.itemsKey)
        assertTrue(lazyColumn.item is Node.Image)

        val hero = lazyColumn.children.single() as Node.Image
        assertEquals(ImageSource.Resource("hero"), hero.source)
        assertEquals("Hero", hero.contentDescription)
        assertEquals(ImageContentScale.Crop, hero.contentScale)
        assertEquals(1.7f, hero.aspectRatio)
        assertEquals(14, hero.cornerRadius)

        val itemImage = lazyColumn.item as Node.Image
        assertEquals(ImageSource.Binding("item.url", ""), itemImage.source)
        assertEquals(ImageContentScale.FillWidth, itemImage.contentScale)

        val photos = page.initialState.values["photos"]?.value as Value.List
        assertEquals(2, photos.items.size)
    }

    @Test
    fun parsesDialogNode() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "dialog",
                  "visibleKey": "showConfirm",
                  "title": { "type": "value", "value": "Confirm" },
                  "spacing": 12,
                  "padding": 24,
                  "onDismiss": {
                    "actions": [{ "type": "setState", "key": "showConfirm", "value": { "type": "literal", "value": false } }]
                  },
                  "children": [
                    { "type": "text", "text": { "type": "value", "value": "Are you sure?" } }
                  ]
                }
              }
            """.trimIndent()
        )

        val dialog = page.root as Node.Dialog
        assertEquals("showConfirm", dialog.visibleKey)
        assertEquals(Text.Value("Confirm"), dialog.title)
        assertEquals(12, dialog.spacing)
        assertEquals(24, dialog.padding)
        assertTrue(dialog.onDismiss.actions.single() is Action.SetState)
        assertTrue(dialog.children.single() is Node.Text)
    }

    @Test
    fun parsesTextFieldSwitchAndButton() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "column",
                  "children": [
                    {
                      "type": "textField",
                      "label": "Name",
                      "value": { "type": "binding", "key": "name", "fallback": "" },
                      "onValueChange": {
                        "actions": [{ "type": "setState", "key": "name", "value": { "type": "event" } }]
                      }
                    },
                    {
                      "type": "switch",
                      "text": "Notify",
                      "checkedKey": "notify",
                      "onCheckedChange": {
                        "actions": [{ "type": "toggleState", "key": "notify" }]
                      }
                    },
                    {
                      "type": "button",
                      "text": "Submit",
                      "variant": "secondary",
                      "onClick": {
                        "actions": [{ "type": "incrementState", "key": "clicks", "step": 2 }]
                      }
                    }
                  ]
                }
              }
            """.trimIndent()
        )

        val column = page.root as Node.Column
        val tf = column.children[0] as Node.TextField
        assertEquals("Name", tf.label)
        assertEquals("name", tf.value.key)

        val sw = column.children[1] as Node.Switch
        assertEquals("notify", sw.checkedKey)
        assertTrue(sw.onCheckedChange.actions.single() is Action.ToggleState)

        val btn = column.children[2] as Node.Button
        assertEquals(ButtonVariant.Secondary, btn.variant)
        val inc = btn.onClick.actions.single() as Action.IncrementState
        assertEquals(2, inc.step)
    }

    @Test
    fun parsesExtendedLayoutListsClickableNodesAndIcon() {
        val page = parseZeroUiPage(
            """
              {
                "initialState": {
                  "icons": [
                    { "name": "ic_one", "label": "One" },
                    { "name": "ic_two", "label": "Two" }
                  ]
                },
                "root": {
                  "type": "column",
                  "spacing": 12,
                  "horizontalAlignment": "center",
                  "layout": {
                    "fillMaxWidth": true,
                    "fillMaxHeight": true,
                    "padding": { "start": 8, "top": 10, "end": 12, "bottom": 14 }
                  },
                  "children": [
                    {
                      "type": "row",
                      "verticalAlignment": "center",
                      "children": [
                        {
                          "type": "text",
                          "text": { "type": "value", "value": "Tap" },
                          "layout": { "weight": 1.5, "minHeight": 24 },
                          "onClick": { "effects": [{ "type": "toast", "message": { "type": "literal", "value": "text" } }] }
                        },
                        {
                          "type": "icon",
                          "source": { "type": "resource", "name": "ic_zero" },
                          "tone": "primary",
                          "size": 20,
                          "onClick": { "effects": [{ "type": "track", "event": "icon" }] }
                        }
                      ]
                    },
                    {
                      "type": "lazyRow",
                      "spacing": 6,
                      "itemsKey": "icons",
                      "item": {
                        "type": "card",
                        "onClick": { "effects": [{ "type": "track", "event": "card" }] },
                        "children": [
                          {
                            "type": "image",
                            "source": { "type": "binding", "key": "item.name", "fallback": "" },
                            "onClick": { "effects": [{ "type": "track", "event": "image" }] }
                          }
                        ]
                      }
                    }
                  ]
                }
              }
            """.trimIndent()
        )

        val column = page.root as Node.Column
        assertEquals(HorizontalAlignment.Center, column.horizontalAlignment)
        assertEquals(12, column.spacing)
        assertEquals(true, column.layout.fillMaxWidth)
        assertEquals(true, column.layout.fillMaxHeight)
        assertEquals(8, column.layout.paddingStart)
        assertEquals(10, column.layout.paddingTop)
        assertEquals(12, column.layout.paddingEnd)
        assertEquals(14, column.layout.paddingBottom)

        val row = column.children[0] as Node.Row
        assertEquals(VerticalAlignment.Center, row.verticalAlignment)
        val text = row.children[0] as Node.Text
        assertEquals(1.5f, text.layout.weight, 0.001f)
        assertEquals(24, text.layout.minHeight)
        assertTrue(text.onClick?.effects?.single() is Effect.Toast)
        val icon = row.children[1] as Node.Icon
        assertEquals(IconSource.Resource("ic_zero"), icon.source)
        assertTrue(icon.onClick?.effects?.single() is Effect.Track)

        val lazyRow = column.children[1] as Node.LazyRow
        assertEquals("icons", lazyRow.itemsKey)
        val card = lazyRow.item as Node.Card
        assertTrue(card.onClick?.effects?.single() is Effect.Track)
        assertTrue((card.children.single() as Node.Image).onClick?.effects?.single() is Effect.Track)
    }

    @Test
    fun parsesChipGroupAndCardAndConditionAndSpacer() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "column",
                  "children": [
                    {
                      "type": "chipGroup",
                      "selectedKey": "topic",
                      "options": [
                        { "label": "A", "value": "a" },
                        { "label": "B", "value": "b" }
                      ],
                      "onSelected": {
                        "actions": [{ "type": "setState", "key": "topic", "value": { "type": "event" } }]
                      }
                    },
                    { "type": "spacer", "height": 12 },
                    {
                      "type": "condition",
                      "condition": { "type": "truthy", "key": "showCard" },
                      "child": {
                        "type": "card",
                        "tone": "success",
                        "children": [
                          { "type": "text", "text": { "type": "value", "value": "Hidden gem" } }
                        ]
                      }
                    }
                  ]
                }
              }
            """.trimIndent()
        )

        val column = page.root as Node.Column
        val chip = column.children[0] as Node.ChipGroup
        assertEquals(2, chip.options.size)
        assertEquals("a", chip.options[0].value)

        val spacer = column.children[1] as Node.Spacer
        assertEquals(12, spacer.height)

        val cond = column.children[2] as Node.Condition
        assertTrue(cond.condition is Condition.Truthy)
        val card = cond.child as Node.Card
        assertEquals(Tone.Success, card.tone)
    }

    @Test
    fun parsesErrorWarningAndInverseTones() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "column",
                  "children": [
                    { "type": "text", "tone": "error", "text": { "type": "value", "value": "Error" } },
                    { "type": "card", "tone": "warning", "children": [] },
                    { "type": "text", "tone": "inverse", "text": { "type": "value", "value": "Inverse" } }
                  ]
                }
              }
            """.trimIndent()
        )

        val column = page.root as Node.Column
        val text = column.children[0] as Node.Text
        val card = column.children[1] as Node.Card
        val inverse = column.children[2] as Node.Text

        assertEquals(Tone.Error, text.tone)
        assertEquals(Tone.Warning, card.tone)
        assertEquals(Tone.Inverse, inverse.tone)
    }

    @Test
    fun parsesAllValueSourceVariants() {
        // Wrapped inside a setState action so we can drive parseValueSource through the real parser.
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "button",
                  "text": "X",
                  "onClick": {
                    "actions": [
                      { "type": "setState", "key": "a", "value": { "type": "literal", "value": 7 } },
                      { "type": "setState", "key": "b", "value": { "type": "event" } },
                      { "type": "setState", "key": "c", "value": { "type": "state", "key": "src" } },
                      { "type": "setState", "key": "d", "value": { "type": "template", "value": "hi {event}" } }
                    ]
                  }
                }
              }
            """.trimIndent()
        )

        val actions = (page.root as Node.Button).onClick.actions
        assertEquals(ValueSource.Literal(Value.Number(7)), (actions[0] as Action.SetState).value)
        assertEquals(ValueSource.EventValue, (actions[1] as Action.SetState).value)
        assertEquals(ValueSource.StateValue("src"), (actions[2] as Action.SetState).value)
        assertEquals(ValueSource.Template("hi {event}"), (actions[3] as Action.SetState).value)
    }

    @Test
    fun parsesBatchAndEffects() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "button",
                  "text": "Go",
                  "onClick": {
                    "actions": [
                      {
                        "type": "batch",
                        "actions": [
                          { "type": "incrementState", "key": "n" },
                          { "type": "toggleState", "key": "flag" }
                        ]
                      }
                    ],
                    "effects": [
                      { "type": "log", "message": { "type": "literal", "value": "did it" } },
                      { "type": "toast", "message": { "type": "template", "value": "n={event}" } }
                    ]
                  }
                }
              }
            """.trimIndent()
        )

        val interaction = (page.root as Node.Button).onClick
        val batch = interaction.actions.single() as Action.Batch
        assertEquals(2, batch.actions.size)
        assertTrue(batch.actions[0] is Action.IncrementState)
        assertTrue(batch.actions[1] is Action.ToggleState)

        assertTrue(interaction.effects[0] is Effect.Log)
        assertTrue(interaction.effects[1] is Effect.Toast)
    }

    @Test
    fun parsesNotBlankCondition() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "condition",
                  "condition": { "type": "notBlank", "key": "search" },
                  "child": { "type": "text", "text": { "type": "value", "value": "results" } }
                }
              }
            """.trimIndent()
        )

        val cond = page.root as Node.Condition
        assertEquals(Condition.NotBlank("search"), cond.condition)
    }

    @Test
    fun parsesForEachWithChildAndSpacing() {
        val page = parseZeroUiPage(
            """
              {
                "initialState": {
                  "todos": [
                    { "title": "buy milk", "done": false },
                    { "title": "ship code", "done": true }
                  ]
                },
                "root": {
                  "type": "forEach",
                  "itemsKey": "todos",
                  "spacing": 6,
                  "child": {
                    "type": "text",
                    "text": { "type": "binding", "key": "item.title", "fallback": "" }
                  }
                }
              }
            """.trimIndent()
        )

        val fe = page.root as Node.ForEach
        assertEquals("todos", fe.itemsKey)
        assertEquals(6, fe.spacing)
        assertTrue(fe.child is Node.Text)

        val todos = page.initialState.values["todos"]?.value as Value.List
        assertEquals(2, todos.items.size)
        val firstItem = todos.items[0] as Value.Record
        assertEquals(Value.Text("buy milk"), firstItem.fields["title"])
        assertEquals(Value.Bool(false), firstItem.fields["done"])
    }

    @Test
    fun unknownNodeTypeFallsBackToUnknown() {
        val page = parseZeroUiPage(
            """
              {
                "root": {
                  "type": "column",
                  "children": [
                    { "type": "text", "text": { "type": "value", "value": "before" } },
                    { "type": "future", "anyField": 42 },
                    { "type": "text", "text": { "type": "value", "value": "after" } }
                  ]
                }
              }
            """.trimIndent()
        )

        val column = page.root as Node.Column
        assertEquals(3, column.children.size)
        val unknown = column.children[1] as Node.Unknown
        assertEquals("future", unknown.typeName)
        assertTrue("raw payload preserved", unknown.raw.contains("\"future\""))
        assertTrue(column.children[0] is Node.Text)
        assertTrue(column.children[2] is Node.Text)
    }

    @Test
    fun rawArrayBecomesValueListInState() {
        val page = parseZeroUiPage(
            """
              {
                "initialState": { "tags": ["a", "b", "c"] },
                "root": { "type": "column", "children": [] }
              }
            """.trimIndent()
        )

        val tags = page.initialState.values["tags"]?.value as Value.List
        assertEquals(3, tags.items.size)
        assertEquals(Value.Text("a"), tags.items[0])
    }
}
