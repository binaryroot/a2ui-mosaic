package org.a2ui.mosaic.focus

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.a2ui.mosaic.model.A2uiComponent
import org.a2ui.mosaic.model.ChildList
import org.a2ui.mosaic.state.SurfaceState

/**
 * Manages focus navigation between interactive components in the terminal.
 * Supports Tab/Shift+Tab cycling through focusable components.
 */
class FocusManager {
    /** The currently focused component ID */
    var focusedId by mutableStateOf<String?>(null)
        private set

    /** Ordered list of focusable component IDs */
    private var focusableIds = listOf<String>()

    /**
     * Rebuild the focusable component list from the surface state.
     * This should be called whenever the component tree changes.
     */
    fun rebuildFocusOrder(surface: SurfaceState) {
        val root = surface.getRootComponent() ?: return
        val ids = mutableListOf<String>()
        collectFocusableIds(root, surface, ids)
        focusableIds = ids

        // If current focus is invalid, reset to first focusable
        if (focusedId == null || focusedId !in focusableIds) {
            focusedId = focusableIds.firstOrNull()
        }
    }

    /**
     * Move focus to the next focusable component.
     */
    fun focusNext() {
        if (focusableIds.isEmpty()) return
        val currentIndex = focusableIds.indexOf(focusedId)
        val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % focusableIds.size
        focusedId = focusableIds[nextIndex]
    }

    /**
     * Move focus to the previous focusable component.
     */
    fun focusPrevious() {
        if (focusableIds.isEmpty()) return
        val currentIndex = focusableIds.indexOf(focusedId)
        val prevIndex = if (currentIndex <= 0) focusableIds.size - 1 else currentIndex - 1
        focusedId = focusableIds[prevIndex]
    }

    /**
     * Set focus to a specific component.
     */
    fun setFocus(id: String) {
        if (id in focusableIds) {
            focusedId = id
        }
    }

    /**
     * Check if a component is currently focused.
     */
    fun isFocused(id: String): Boolean = focusedId == id

    /**
     * Get the number of focusable components.
     */
    fun focusableCount(): Int = focusableIds.size

    private fun collectFocusableIds(
        component: A2uiComponent,
        surface: SurfaceState,
        result: MutableList<String>
    ) {
        // Check if this component is focusable
        when (component) {
            is A2uiComponent.TextField,
            is A2uiComponent.Button,
            is A2uiComponent.CheckBox,
            is A2uiComponent.ChoicePicker,
            is A2uiComponent.Slider,
            is A2uiComponent.DateTimeInput -> {
                result.add(component.id)
            }
            else -> { /* not focusable */ }
        }

        // Recurse into children
        when (component) {
            is A2uiComponent.Row -> {
                when (val children = component.children) {
                    is ChildList.StaticList -> {
                        children.ids.forEach { childId ->
                            surface.getComponent(childId)?.let { collectFocusableIds(it, surface, result) }
                        }
                    }
                    is ChildList.Template -> { /* dynamic children handled at render time */ }
                }
            }
            is A2uiComponent.Column -> {
                when (val children = component.children) {
                    is ChildList.StaticList -> {
                        children.ids.forEach { childId ->
                            surface.getComponent(childId)?.let { collectFocusableIds(it, surface, result) }
                        }
                    }
                    is ChildList.Template -> { /* dynamic children handled at render time */ }
                }
            }
            is A2uiComponent.Card -> {
                surface.getComponent(component.child)?.let { collectFocusableIds(it, surface, result) }
            }
            is A2uiComponent.ListComponent -> {
                when (val children = component.children) {
                    is ChildList.StaticList -> {
                        children.ids.forEach { childId ->
                            surface.getComponent(childId)?.let { collectFocusableIds(it, surface, result) }
                        }
                    }
                    is ChildList.Template -> { /* dynamic children handled at render time */ }
                }
            }
            is A2uiComponent.Tabs -> {
                component.tabItems.forEach { tab ->
                    surface.getComponent(tab.child)?.let { collectFocusableIds(it, surface, result) }
                }
            }
            is A2uiComponent.Modal -> {
                surface.getComponent(component.entryPointChild)?.let { collectFocusableIds(it, surface, result) }
            }
            else -> { /* leaf component, no children */ }
        }
    }
}
