package org.a2ui.mosaic.focus

import org.a2ui.mosaic.model.*
import org.a2ui.mosaic.state.SurfaceState
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class FocusManagerTest {

    private fun createSurfaceWithForm(): SurfaceState {
        val surface = SurfaceState("test", "basic")
        surface.updateComponents(listOf(
            A2uiComponent.Column(
                id = "root",
                children = ChildList.StaticList(listOf("field1", "field2", "btn1", "check1"))
            ),
            A2uiComponent.TextField(
                id = "field1",
                label = DynamicValue.LiteralString("Name"),
                value = DynamicValue.Path("/name")
            ),
            A2uiComponent.TextField(
                id = "field2",
                label = DynamicValue.LiteralString("Email"),
                value = DynamicValue.Path("/email")
            ),
            A2uiComponent.Button(
                id = "btn1",
                child = "btn-text",
                action = A2uiAction.Event(name = "submit"),
                variant = "primary"
            ),
            A2uiComponent.CheckBox(
                id = "check1",
                label = DynamicValue.LiteralString("Accept"),
                value = DynamicValue.Path("/accepted")
            )
        ))
        return surface
    }

    @Test
    fun `rebuild focus order from surface`() {
        val surface = createSurfaceWithForm()
        val focusManager = FocusManager()
        focusManager.rebuildFocusOrder(surface)

        assertEquals(4, focusManager.focusableCount())
        assertEquals("field1", focusManager.focusedId)
    }

    @Test
    fun `focus next cycles through components`() {
        val surface = createSurfaceWithForm()
        val focusManager = FocusManager()
        focusManager.rebuildFocusOrder(surface)

        assertEquals("field1", focusManager.focusedId)
        focusManager.focusNext()
        assertEquals("field2", focusManager.focusedId)
        focusManager.focusNext()
        assertEquals("btn1", focusManager.focusedId)
        focusManager.focusNext()
        assertEquals("check1", focusManager.focusedId)
        focusManager.focusNext()
        assertEquals("field1", focusManager.focusedId) // wraps around
    }

    @Test
    fun `focus previous cycles backwards`() {
        val surface = createSurfaceWithForm()
        val focusManager = FocusManager()
        focusManager.rebuildFocusOrder(surface)

        assertEquals("field1", focusManager.focusedId)
        focusManager.focusPrevious()
        assertEquals("check1", focusManager.focusedId) // wraps to end
        focusManager.focusPrevious()
        assertEquals("btn1", focusManager.focusedId)
    }

    @Test
    fun `set focus to specific component`() {
        val surface = createSurfaceWithForm()
        val focusManager = FocusManager()
        focusManager.rebuildFocusOrder(surface)

        focusManager.setFocus("btn1")
        assertEquals("btn1", focusManager.focusedId)
    }

    @Test
    fun `set focus to non-focusable component is ignored`() {
        val surface = createSurfaceWithForm()
        val focusManager = FocusManager()
        focusManager.rebuildFocusOrder(surface)

        focusManager.setFocus("root") // Column is not focusable
        assertEquals("field1", focusManager.focusedId) // unchanged
    }

    @Test
    fun `isFocused returns correct value`() {
        val surface = createSurfaceWithForm()
        val focusManager = FocusManager()
        focusManager.rebuildFocusOrder(surface)

        assertTrue(focusManager.isFocused("field1"))
        assertFalse(focusManager.isFocused("field2"))
    }

    @Test
    fun `empty surface has no focusable components`() {
        val surface = SurfaceState("test", "basic")
        surface.updateComponents(listOf(
            A2uiComponent.Text(id = "root", text = DynamicValue.LiteralString("Hello"))
        ))

        val focusManager = FocusManager()
        focusManager.rebuildFocusOrder(surface)

        assertEquals(0, focusManager.focusableCount())
        assertNull(focusManager.focusedId)
    }

    @Test
    fun `nested focusable components are found`() {
        val surface = SurfaceState("test", "basic")
        surface.updateComponents(listOf(
            A2uiComponent.Card(id = "root", child = "col1"),
            A2uiComponent.Column(
                id = "col1",
                children = ChildList.StaticList(listOf("row1"))
            ),
            A2uiComponent.Row(
                id = "row1",
                children = ChildList.StaticList(listOf("field1", "btn1"))
            ),
            A2uiComponent.TextField(
                id = "field1",
                label = DynamicValue.LiteralString("Name"),
                value = DynamicValue.Path("/name")
            ),
            A2uiComponent.Button(
                id = "btn1",
                child = "btn-text",
                action = A2uiAction.Event(name = "submit")
            )
        ))

        val focusManager = FocusManager()
        focusManager.rebuildFocusOrder(surface)

        assertEquals(2, focusManager.focusableCount())
        assertEquals("field1", focusManager.focusedId)
    }
}
