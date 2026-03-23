package org.a2ui.mosaic.render

import com.jakewharton.mosaic.layout.KeyEvent
import org.a2ui.mosaic.focus.FocusManager
import org.a2ui.mosaic.model.*
import org.a2ui.mosaic.state.SurfaceState

/**
 * Handles keyboard input for the A2UI terminal renderer.
 * Processes key events and dispatches them to the appropriate component handlers.
 */
class InputHandler(
    private val surface: SurfaceState,
    private val focusManager: FocusManager
) {
    /**
     * Process a key event. Returns true if the event was consumed.
     */
    fun handleKeyEvent(key: String): Boolean {
        // Global navigation keys
        when (key) {
            "Tab", "\t" -> {
                focusManager.focusNext()
                return true
            }
            "ShiftTab", "BackTab" -> {
                focusManager.focusPrevious()
                return true
            }
        }

        // Delegate to focused component
        val focusedId = focusManager.focusedId ?: return false
        val component = surface.getComponent(focusedId) ?: return false

        return when (component) {
            is A2uiComponent.TextField -> handleTextFieldInput(component, key)
            is A2uiComponent.Button -> handleButtonInput(component, key)
            is A2uiComponent.CheckBox -> handleCheckBoxInput(component, key)
            is A2uiComponent.ChoicePicker -> handleChoicePickerInput(component, key)
            is A2uiComponent.Slider -> handleSliderInput(component, key)
            is A2uiComponent.DateTimeInput -> handleDateTimeInput(component, key)
            else -> false
        }
    }

    private fun handleTextFieldInput(component: A2uiComponent.TextField, key: String): Boolean {
        val pathDv = component.value
        if (pathDv !is DynamicValue.Path) return false
        val path = pathDv.path

        val currentValue = surface.resolveString(component.value)

        when {
            key.length == 1 && key[0].code >= 32 -> {
                // Printable character
                surface.setStringValue(path, currentValue + key)
                return true
            }
            key == "Backspace" || key == "Delete" -> {
                if (currentValue.isNotEmpty()) {
                    surface.setStringValue(path, currentValue.dropLast(1))
                }
                return true
            }
            key == "Enter" || key == "Return" -> {
                if (component.variant == "longText") {
                    surface.setStringValue(path, currentValue + "\n")
                    return true
                }
                // For short text, Enter moves to next field
                focusManager.focusNext()
                return true
            }
        }
        return false
    }

    private fun handleButtonInput(component: A2uiComponent.Button, key: String): Boolean {
        if (key == "Enter" || key == "Return" || key == " ") {
            val action = component.action
            if (action is A2uiAction.Event) {
                surface.dispatchAction(component.id, action)
            }
            return true
        }
        return false
    }

    private fun handleCheckBoxInput(component: A2uiComponent.CheckBox, key: String): Boolean {
        if (key == "Enter" || key == "Return" || key == " ") {
            val pathDv = component.value
            if (pathDv is DynamicValue.Path) {
                val currentValue = surface.resolveBoolean(component.value)
                surface.setBooleanValue(pathDv.path, !currentValue)
            }
            return true
        }
        return false
    }

    private fun handleChoicePickerInput(component: A2uiComponent.ChoicePicker, key: String): Boolean {
        val pathDv = component.value
        if (pathDv !is DynamicValue.Path) return false

        val selectedValues = surface.resolveStringList(component.value).toMutableList()
        val isMultiple = component.variant == "multipleSelection"

        // Use number keys or arrow keys to select options
        val optionIndex = when {
            key.length == 1 && key[0].isDigit() -> key[0].digitToInt() - 1
            key == "ArrowUp" -> {
                val currentIdx = component.options.indexOfFirst { it.value in selectedValues }
                if (currentIdx > 0) currentIdx - 1 else component.options.size - 1
            }
            key == "ArrowDown" -> {
                val currentIdx = component.options.indexOfFirst { it.value in selectedValues }
                if (currentIdx < component.options.size - 1) currentIdx + 1 else 0
            }
            key == " " || key == "Enter" || key == "Return" -> {
                // Toggle current selection (for display, use first selected)
                val currentIdx = component.options.indexOfFirst { it.value in selectedValues }
                if (currentIdx >= 0) currentIdx else 0
            }
            else -> return false
        }

        if (optionIndex in component.options.indices) {
            val optionValue = component.options[optionIndex].value
            if (isMultiple) {
                if (optionValue in selectedValues) {
                    selectedValues.remove(optionValue)
                } else {
                    selectedValues.add(optionValue)
                }
            } else {
                selectedValues.clear()
                selectedValues.add(optionValue)
            }
            surface.setStringListValue(pathDv.path, selectedValues)
            return true
        }
        return false
    }

    private fun handleSliderInput(component: A2uiComponent.Slider, key: String): Boolean {
        val pathDv = component.value
        if (pathDv !is DynamicValue.Path) return false

        val currentValue = surface.resolveNumber(component.value)
        val step = (component.max - component.min) / 20.0 // 5% steps

        when (key) {
            "ArrowRight", "ArrowUp" -> {
                val newValue = (currentValue + step).coerceAtMost(component.max)
                surface.setNumberValue(pathDv.path, newValue)
                return true
            }
            "ArrowLeft", "ArrowDown" -> {
                val newValue = (currentValue - step).coerceAtLeast(component.min)
                surface.setNumberValue(pathDv.path, newValue)
                return true
            }
        }
        return false
    }

    private fun handleDateTimeInput(component: A2uiComponent.DateTimeInput, key: String): Boolean {
        val pathDv = component.value
        if (pathDv !is DynamicValue.Path) return false

        val currentValue = surface.resolveString(component.value)

        when {
            key.length == 1 && (key[0].isDigit() || key[0] == '-' || key[0] == ':' || key[0] == 'T') -> {
                surface.setStringValue(pathDv.path, currentValue + key)
                return true
            }
            key == "Backspace" || key == "Delete" -> {
                if (currentValue.isNotEmpty()) {
                    surface.setStringValue(pathDv.path, currentValue.dropLast(1))
                }
                return true
            }
        }
        return false
    }
}
