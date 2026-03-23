package org.a2ui.mosaic.model

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class MessageTest {

    @Test
    fun `parse createSurface message`() {
        val json = """
        {
            "createSurface": {
                "surfaceId": "test-surface",
                "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json",
                "sendDataModel": true
            }
        }
        """.trimIndent()

        val message = A2uiMessageParser.parseMessage(json)
        assertTrue(message is A2uiMessage.CreateSurface)
        val cs = message as A2uiMessage.CreateSurface
        assertEquals("test-surface", cs.surfaceId)
        assertEquals("https://a2ui.org/specification/v0_9/basic_catalog.json", cs.catalogId)
        assertTrue(cs.sendDataModel)
    }

    @Test
    fun `parse updateComponents message with Text component`() {
        val json = """
        {
            "updateComponents": {
                "surfaceId": "test-surface",
                "components": [
                    {
                        "id": "title",
                        "component": "Text",
                        "text": "Hello World",
                        "variant": "h1"
                    }
                ]
            }
        }
        """.trimIndent()

        val message = A2uiMessageParser.parseMessage(json)
        assertTrue(message is A2uiMessage.UpdateComponents)
        val uc = message as A2uiMessage.UpdateComponents
        assertEquals("test-surface", uc.surfaceId)
        assertEquals(1, uc.components.size)

        val text = uc.components[0]
        assertTrue(text is A2uiComponent.Text)
        assertEquals("title", text.id)
        assertEquals("h1", (text as A2uiComponent.Text).variant)
    }

    @Test
    fun `parse updateComponents with TextField and path binding`() {
        val json = """
        {
            "updateComponents": {
                "surfaceId": "test-surface",
                "components": [
                    {
                        "id": "name-field",
                        "component": "TextField",
                        "label": "Your Name",
                        "value": {"path": "/name"},
                        "variant": "shortText"
                    }
                ]
            }
        }
        """.trimIndent()

        val message = A2uiMessageParser.parseMessage(json)
        assertTrue(message is A2uiMessage.UpdateComponents)
        val uc = message as A2uiMessage.UpdateComponents
        val tf = uc.components[0] as A2uiComponent.TextField
        assertEquals("name-field", tf.id)
        assertTrue(tf.value is DynamicValue.Path)
        assertEquals("/name", (tf.value as DynamicValue.Path).path)
    }

    @Test
    fun `parse updateDataModel message`() {
        val json = """
        {
            "updateDataModel": {
                "surfaceId": "test-surface",
                "value": {
                    "name": "John",
                    "age": 30
                }
            }
        }
        """.trimIndent()

        val message = A2uiMessageParser.parseMessage(json)
        assertTrue(message is A2uiMessage.UpdateDataModel)
        val udm = message as A2uiMessage.UpdateDataModel
        assertEquals("test-surface", udm.surfaceId)
        assertNotNull(udm.value)
    }

    @Test
    fun `parse deleteSurface message`() {
        val json = """
        {
            "deleteSurface": {
                "surfaceId": "test-surface"
            }
        }
        """.trimIndent()

        val message = A2uiMessageParser.parseMessage(json)
        assertTrue(message is A2uiMessage.DeleteSurface)
        assertEquals("test-surface", (message as A2uiMessage.DeleteSurface).surfaceId)
    }

    @Test
    fun `parse example file with multiple messages`() {
        val json = """
        {
            "name": "Test Example",
            "description": "A test",
            "messages": [
                {
                    "createSurface": {
                        "surfaceId": "s1",
                        "catalogId": "basic"
                    }
                },
                {
                    "updateComponents": {
                        "surfaceId": "s1",
                        "components": [
                            {
                                "id": "root",
                                "component": "Text",
                                "text": "Hello"
                            }
                        ]
                    }
                }
            ]
        }
        """.trimIndent()

        val example = A2uiMessageParser.parseExample(json)
        assertEquals("Test Example", example.name)
        assertEquals(2, example.messages.size)
        assertTrue(example.messages[0] is A2uiMessage.CreateSurface)
        assertTrue(example.messages[1] is A2uiMessage.UpdateComponents)
    }

    @Test
    fun `parse Button with event action`() {
        val json = """
        {
            "updateComponents": {
                "surfaceId": "test",
                "components": [
                    {
                        "id": "btn",
                        "component": "Button",
                        "child": "btn-text",
                        "variant": "primary",
                        "action": {
                            "event": {
                                "name": "submit",
                                "context": {
                                    "field1": {"path": "/data/field1"}
                                }
                            }
                        }
                    }
                ]
            }
        }
        """.trimIndent()

        val message = A2uiMessageParser.parseMessage(json)
        val uc = message as A2uiMessage.UpdateComponents
        val btn = uc.components[0] as A2uiComponent.Button
        assertEquals("btn", btn.id)
        assertEquals("primary", btn.variant)
        assertTrue(btn.action is A2uiAction.Event)
        val event = btn.action as A2uiAction.Event
        assertEquals("submit", event.name)
        assertTrue(event.context["field1"] is DynamicValue.Path)
    }

    @Test
    fun `parse CheckBox component`() {
        val json = """
        {
            "updateComponents": {
                "surfaceId": "test",
                "components": [
                    {
                        "id": "cb",
                        "component": "CheckBox",
                        "label": "Accept terms",
                        "value": {"path": "/accepted"}
                    }
                ]
            }
        }
        """.trimIndent()

        val message = A2uiMessageParser.parseMessage(json)
        val uc = message as A2uiMessage.UpdateComponents
        val cb = uc.components[0] as A2uiComponent.CheckBox
        assertEquals("cb", cb.id)
        assertTrue(cb.value is DynamicValue.Path)
    }

    @Test
    fun `parse ChoicePicker component`() {
        val json = """
        {
            "updateComponents": {
                "surfaceId": "test",
                "components": [
                    {
                        "id": "picker",
                        "component": "ChoicePicker",
                        "label": "Color",
                        "options": [
                            {"label": "Red", "value": "red"},
                            {"label": "Blue", "value": "blue"}
                        ],
                        "value": {"path": "/color"},
                        "variant": "mutuallyExclusive"
                    }
                ]
            }
        }
        """.trimIndent()

        val message = A2uiMessageParser.parseMessage(json)
        val uc = message as A2uiMessage.UpdateComponents
        val picker = uc.components[0] as A2uiComponent.ChoicePicker
        assertEquals(2, picker.options.size)
        assertEquals("red", picker.options[0].value)
    }

    @Test
    fun `parse Row with children`() {
        val json = """
        {
            "updateComponents": {
                "surfaceId": "test",
                "components": [
                    {
                        "id": "row1",
                        "component": "Row",
                        "children": ["child1", "child2", "child3"]
                    }
                ]
            }
        }
        """.trimIndent()

        val message = A2uiMessageParser.parseMessage(json)
        val uc = message as A2uiMessage.UpdateComponents
        val row = uc.components[0] as A2uiComponent.Row
        val children = row.children as ChildList.StaticList
        assertEquals(3, children.ids.size)
        assertEquals("child1", children.ids[0])
    }

    @Test
    fun `serialize client action message`() {
        val action = A2uiClientMessage.Action(
            name = "submit",
            surfaceId = "test-surface",
            sourceComponentId = "btn1",
            timestamp = "2024-01-01T00:00:00Z",
            context = mapOf("key" to "value")
        )
        val json = A2uiMessageParser.serializeAction(action)
        assertTrue(json.contains("submit"))
        assertTrue(json.contains("test-surface"))
        assertTrue(json.contains("btn1"))
    }
}
