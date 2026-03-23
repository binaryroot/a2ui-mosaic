package org.a2ui.mosaic.render

import kotlinx.serialization.json.*
import org.a2ui.mosaic.focus.FocusManager
import org.a2ui.mosaic.model.*
import org.a2ui.mosaic.state.SurfaceState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class InputHandlerTest {

    private fun createFormSurface(): Triple<SurfaceState, FocusManager, InputHandler> {
        val surface = SurfaceState("test", "basic", sendDataModel = true)
        surface.updateComponents(listOf(
            A2uiComponent.Column(
                id = "root",
                children = ChildList.StaticList(listOf("name-field", "check1", "btn1"))
            ),
            A2uiComponent.TextField(
                id = "name-field",
                label = DynamicValue.LiteralString("Name"),
                value = DynamicValue.Path("/name"),
                variant = "shortText"
            ),
            A2uiComponent.CheckBox(
                id = "check1",
                label = DynamicValue.LiteralString("Accept"),
                value = DynamicValue.Path("/accepted")
            ),
            A2uiComponent.Button(
                id = "btn1",
                child = "btn-text",
                action = A2uiAction.Event(
                    name = "submit",
                    context = mapOf("name" to DynamicValue.Path("/name"))
                ),
                variant = "primary"
            ),
            A2uiComponent.Text(
                id = "btn-text",
                text = DynamicValue.LiteralString("Submit")
            )
        ))
        surface.updateDataModel(null, buildJsonObject {
            put("name", "")
            put("accepted", false)
        })

        val focusManager = FocusManager()
        focusManager.rebuildFocusOrder(surface)
        val inputHandler = InputHandler(surface, focusManager)
        return Triple(surface, focusManager, inputHandler)
    }

    @Test
    fun `typing in text field updates data model`() {
        val (surface, focusManager, inputHandler) = createFormSurface()
        assertEquals("name-field", focusManager.focusedId)

        inputHandler.handleKeyEvent("H")
        inputHandler.handleKeyEvent("i")
        assertEquals("Hi", surface.resolveString(DynamicValue.Path("/name")))
    }

    @Test
    fun `backspace in text field removes last character`() {
        val (surface, focusManager, inputHandler) = createFormSurface()
        inputHandler.handleKeyEvent("A")
        inputHandler.handleKeyEvent("B")
        inputHandler.handleKeyEvent("C")
        assertEquals("ABC", surface.resolveString(DynamicValue.Path("/name")))

        inputHandler.handleKeyEvent("Backspace")
        assertEquals("AB", surface.resolveString(DynamicValue.Path("/name")))
    }

    @Test
    fun `tab navigates to next component`() {
        val (_, focusManager, inputHandler) = createFormSurface()
        assertEquals("name-field", focusManager.focusedId)

        inputHandler.handleKeyEvent("Tab")
        assertEquals("check1", focusManager.focusedId)
    }

    @Test
    fun `shift tab navigates to previous component`() {
        val (_, focusManager, inputHandler) = createFormSurface()
        assertEquals("name-field", focusManager.focusedId)

        inputHandler.handleKeyEvent("ShiftTab")
        assertEquals("btn1", focusManager.focusedId)
    }

    @Test
    fun `space toggles checkbox`() {
        val (surface, focusManager, inputHandler) = createFormSurface()
        focusManager.setFocus("check1")

        assertFalse(surface.resolveBoolean(DynamicValue.Path("/accepted")))
        inputHandler.handleKeyEvent(" ")
        assertTrue(surface.resolveBoolean(DynamicValue.Path("/accepted")))
        inputHandler.handleKeyEvent(" ")
        assertFalse(surface.resolveBoolean(DynamicValue.Path("/accepted")))
    }

    @Test
    fun `enter on button dispatches action`() {
        val (surface, focusManager, inputHandler) = createFormSurface()

        // Type a name first
        inputHandler.handleKeyEvent("J")
        inputHandler.handleKeyEvent("o")
        inputHandler.handleKeyEvent("h")
        inputHandler.handleKeyEvent("n")

        // Navigate to button
        focusManager.setFocus("btn1")

        var receivedAction: A2uiClientMessage? = null
        surface.onAction = { receivedAction = it }

        inputHandler.handleKeyEvent("Enter")

        assertNotNull(receivedAction)
        assertTrue(receivedAction is A2uiClientMessage.Action)
        val action = receivedAction as A2uiClientMessage.Action
        assertEquals("submit", action.name)
        assertEquals("John", action.context["name"])
    }

    @Test
    fun `enter in short text field moves to next field`() {
        val (_, focusManager, inputHandler) = createFormSurface()
        assertEquals("name-field", focusManager.focusedId)

        inputHandler.handleKeyEvent("Enter")
        assertEquals("check1", focusManager.focusedId)
    }
}
