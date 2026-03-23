package org.a2ui.mosaic.render

import androidx.compose.runtime.*
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.AnnotatedString.Builder
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.*
import org.a2ui.mosaic.focus.FocusManager
import org.a2ui.mosaic.model.*
import org.a2ui.mosaic.state.SurfaceState

/**
 * Terminal color palette for the A2UI renderer.
 * Uses Mosaic's built-in Color constants plus custom RGB colors.
 */
object TerminalTheme {
    val primaryColor = Color.Blue
    val textColor = Color.White
    val dimColor = Color(128, 128, 128) // gray
    val errorColor = Color.Red
    val successColor = Color.Green
    val accentColor = Color.Cyan
    val focusBorderColor = Color.Yellow
    val cardBorderColor = Color(100, 100, 100) // dark gray
    val buttonColor = Color.Blue
    val buttonPrimaryColor = Color.Green
    val inputBgColor = Color.Black
    val headerColor = Color(220, 220, 220) // bright white
}

/**
 * Root composable that renders an entire A2UI surface.
 */
@Composable
fun A2uiSurface(
    surface: SurfaceState,
    focusManager: FocusManager
) {
    // Trigger recomposition when surface version changes
    val v = surface.version

    // Rebuild focus order when components change
    LaunchedEffect(v) {
        focusManager.rebuildFocusOrder(surface)
    }

    val root = surface.getRootComponent()
    if (root != null) {
        A2uiComponentView(root, surface, focusManager)
    } else {
        Text("Loading...", color = TerminalTheme.dimColor)
    }
}

/**
 * Routes to the appropriate renderer based on component type.
 */
@Composable
fun A2uiComponentView(
    component: A2uiComponent,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    when (component) {
        is A2uiComponent.Text -> A2uiTextView(component, surface)
        is A2uiComponent.Row -> A2uiRowView(component, surface, focusManager)
        is A2uiComponent.Column -> A2uiColumnView(component, surface, focusManager)
        is A2uiComponent.Card -> A2uiCardView(component, surface, focusManager)
        is A2uiComponent.Button -> A2uiButtonView(component, surface, focusManager)
        is A2uiComponent.TextField -> A2uiTextFieldView(component, surface, focusManager)
        is A2uiComponent.CheckBox -> A2uiCheckBoxView(component, surface, focusManager)
        is A2uiComponent.ChoicePicker -> A2uiChoicePickerView(component, surface, focusManager)
        is A2uiComponent.Slider -> A2uiSliderView(component, surface, focusManager)
        is A2uiComponent.Divider -> A2uiDividerView(component)
        is A2uiComponent.Icon -> A2uiIconView(component, surface)
        is A2uiComponent.Image -> A2uiImageView(component, surface)
        is A2uiComponent.DateTimeInput -> A2uiDateTimeInputView(component, surface, focusManager)
        is A2uiComponent.Tabs -> A2uiTabsView(component, surface, focusManager)
        is A2uiComponent.ListComponent -> A2uiListView(component, surface, focusManager)
        is A2uiComponent.Modal -> A2uiModalView(component, surface, focusManager)
        is A2uiComponent.Unknown -> Text("[Unknown: ${component.componentType}]", color = TerminalTheme.errorColor)
    }
}

// ============================================================
// Individual Component Renderers
// ============================================================

@Composable
fun A2uiTextView(component: A2uiComponent.Text, surface: SurfaceState) {
    val text = surface.resolveString(component.text)
    val color = when (component.variant) {
        "h1", "h2", "h3" -> TerminalTheme.headerColor
        "h4", "h5" -> TerminalTheme.textColor
        "caption" -> TerminalTheme.dimColor
        else -> TerminalTheme.textColor
    }
    val prefix = when (component.variant) {
        "h1" -> "# "
        "h2" -> "## "
        "h3" -> "### "
        else -> ""
    }
    Text(
        buildAnnotatedString {
            if (prefix.isNotEmpty()) {
                withStyle(SpanStyle(color = TerminalTheme.accentColor)) {
                    append(prefix)
                }
            }
            withStyle(SpanStyle(color = color)) {
                append(text)
            }
        }
    )
}

@Composable
fun A2uiRowView(
    component: A2uiComponent.Row,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    Row {
        when (val children = component.children) {
            is ChildList.StaticList -> {
                children.ids.forEachIndexed { index, childId ->
                    val child = surface.getComponent(childId)
                    if (child != null) {
                        A2uiComponentView(child, surface, focusManager)
                        if (index < children.ids.size - 1) {
                            Text(" ", color = TerminalTheme.textColor)
                        }
                    }
                }
            }
            is ChildList.Template -> {
                Text("[Template list]", color = TerminalTheme.dimColor)
            }
        }
    }
}

@Composable
fun A2uiColumnView(
    component: A2uiComponent.Column,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    Column {
        when (val children = component.children) {
            is ChildList.StaticList -> {
                children.ids.forEach { childId ->
                    val child = surface.getComponent(childId)
                    if (child != null) {
                        A2uiComponentView(child, surface, focusManager)
                    }
                }
            }
            is ChildList.Template -> {
                Text("[Template list]", color = TerminalTheme.dimColor)
            }
        }
    }
}

@Composable
fun A2uiCardView(
    component: A2uiComponent.Card,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    val child = surface.getComponent(component.child)
    Column {
        Text("+" + "-".repeat(50) + "+", color = TerminalTheme.cardBorderColor)
        Row {
            Text("| ", color = TerminalTheme.cardBorderColor)
            if (child != null) {
                A2uiComponentView(child, surface, focusManager)
            }
        }
        Text("+" + "-".repeat(50) + "+", color = TerminalTheme.cardBorderColor)
    }
}

@Composable
fun A2uiButtonView(
    component: A2uiComponent.Button,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    val isFocused = focusManager.isFocused(component.id)
    val childComponent = surface.getComponent(component.child)
    val label = when (childComponent) {
        is A2uiComponent.Text -> surface.resolveString(childComponent.text)
        else -> "[Button]"
    }

    val textColor = when {
        isFocused -> Color.Black
        component.variant == "primary" -> TerminalTheme.successColor
        component.variant == "borderless" -> TerminalTheme.accentColor
        else -> TerminalTheme.textColor
    }

    val bgColor = when {
        isFocused && component.variant == "primary" -> TerminalTheme.buttonPrimaryColor
        isFocused -> TerminalTheme.buttonColor
        else -> Color.Unspecified
    }

    val prefix = if (isFocused) "> " else "  "
    val brackets = if (component.variant == "borderless") {
        Pair("", "")
    } else {
        Pair("[ ", " ]")
    }

    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = if (isFocused) TerminalTheme.focusBorderColor else TerminalTheme.dimColor)) {
                append(prefix)
            }
            withStyle(SpanStyle(color = textColor, background = bgColor)) {
                append("${brackets.first}$label${brackets.second}")
            }
        }
    )
}

@Composable
fun A2uiTextFieldView(
    component: A2uiComponent.TextField,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    val isFocused = focusManager.isFocused(component.id)
    val label = surface.resolveString(component.label)
    val currentValue = if (component.value != null) {
        surface.resolveString(component.value)
    } else {
        ""
    }

    val isObscured = component.variant == "obscured"
    val displayValue = if (isObscured) "*".repeat(currentValue.length) else currentValue

    val focusIndicator = if (isFocused) "> " else "  "
    val borderColor = if (isFocused) TerminalTheme.focusBorderColor else TerminalTheme.cardBorderColor
    val cursor = if (isFocused) "_" else ""

    Column {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = if (isFocused) TerminalTheme.focusBorderColor else TerminalTheme.dimColor)) {
                    append(focusIndicator)
                }
                withStyle(SpanStyle(color = TerminalTheme.dimColor)) {
                    append(label)
                }
            }
        )
        Text(
            buildAnnotatedString {
                append("  ")
                withStyle(SpanStyle(color = borderColor)) {
                    append("+" + "-".repeat(40) + "+")
                }
            }
        )
        Text(
            buildAnnotatedString {
                append("  ")
                withStyle(SpanStyle(color = borderColor)) {
                    append("| ")
                }
                withStyle(SpanStyle(color = TerminalTheme.textColor)) {
                    append(displayValue)
                }
                withStyle(SpanStyle(color = TerminalTheme.focusBorderColor)) {
                    append(cursor)
                }
                // Pad to fill the box
                val padding = 38 - displayValue.length - cursor.length
                if (padding > 0) {
                    append(" ".repeat(padding))
                }
                withStyle(SpanStyle(color = borderColor)) {
                    append(" |")
                }
            }
        )
        Text(
            buildAnnotatedString {
                append("  ")
                withStyle(SpanStyle(color = borderColor)) {
                    append("+" + "-".repeat(40) + "+")
                }
            }
        )
    }
}

@Composable
fun A2uiCheckBoxView(
    component: A2uiComponent.CheckBox,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    val isFocused = focusManager.isFocused(component.id)
    val label = surface.resolveString(component.label)
    val checked = surface.resolveBoolean(component.value)

    val focusIndicator = if (isFocused) "> " else "  "
    val checkMark = if (checked) "[x]" else "[ ]"
    val checkColor = if (checked) TerminalTheme.successColor else TerminalTheme.dimColor

    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = if (isFocused) TerminalTheme.focusBorderColor else TerminalTheme.dimColor)) {
                append(focusIndicator)
            }
            withStyle(SpanStyle(color = checkColor)) {
                append("$checkMark ")
            }
            withStyle(SpanStyle(color = TerminalTheme.textColor)) {
                append(label)
            }
        }
    )
}

@Composable
fun A2uiChoicePickerView(
    component: A2uiComponent.ChoicePicker,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    val isFocused = focusManager.isFocused(component.id)
    val selectedValues = surface.resolveStringList(component.value)
    val label = component.label?.let { surface.resolveString(it) }
    val isMultiple = component.variant == "multipleSelection"

    Column {
        if (label != null) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (isFocused) TerminalTheme.focusBorderColor else TerminalTheme.dimColor)) {
                        append(if (isFocused) "> " else "  ")
                    }
                    withStyle(SpanStyle(color = TerminalTheme.dimColor)) {
                        append(label)
                    }
                }
            )
        }
        component.options.forEachIndexed { index, option ->
            val optionLabel = surface.resolveString(option.label)
            val isSelected = option.value in selectedValues
            val marker = if (isMultiple) {
                if (isSelected) "  [x] " else "  [ ] "
            } else {
                if (isSelected) "  (*) " else "  ( ) "
            }
            val markerColor = if (isSelected) TerminalTheme.successColor else TerminalTheme.dimColor

            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = markerColor)) {
                        append(marker)
                    }
                    withStyle(SpanStyle(color = TerminalTheme.textColor)) {
                        append(optionLabel)
                    }
                }
            )
        }
    }
}

@Composable
fun A2uiSliderView(
    component: A2uiComponent.Slider,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    val isFocused = focusManager.isFocused(component.id)
    val value = surface.resolveNumber(component.value)
    val label = component.label?.let { surface.resolveString(it) }
    val min = component.min
    val max = component.max

    val range = max - min
    val normalized = if (range > 0) ((value - min) / range).coerceIn(0.0, 1.0) else 0.0
    val barWidth = 30
    val filledWidth = (normalized * barWidth).toInt()

    Column {
        if (label != null) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(color = if (isFocused) TerminalTheme.focusBorderColor else TerminalTheme.dimColor)) {
                        append(if (isFocused) "> " else "  ")
                    }
                    withStyle(SpanStyle(color = TerminalTheme.dimColor)) {
                        append(label)
                    }
                }
            )
        }
        Text(
            buildAnnotatedString {
                append("  ")
                withStyle(SpanStyle(color = TerminalTheme.primaryColor)) {
                    append("#".repeat(filledWidth))
                }
                withStyle(SpanStyle(color = TerminalTheme.dimColor)) {
                    append("-".repeat(barWidth - filledWidth))
                }
                withStyle(SpanStyle(color = TerminalTheme.textColor)) {
                    append(" ${String.format("%.0f", value)}")
                }
            }
        )
    }
}

@Composable
fun A2uiDividerView(component: A2uiComponent.Divider) {
    val char = if (component.axis == "horizontal") "-" else "|"
    val length = if (component.axis == "horizontal") 50 else 1
    Text(char.repeat(length), color = TerminalTheme.cardBorderColor)
}

@Composable
fun A2uiIconView(component: A2uiComponent.Icon, surface: SurfaceState) {
    val name = surface.resolveString(component.name)
    // Map icon names to ASCII representations
    val icon = when (name) {
        "check" -> "[v]"
        "close" -> "[x]"
        "add" -> "[+]"
        "delete" -> "[del]"
        "edit" -> "[edit]"
        "search" -> "[search]"
        "settings" -> "[settings]"
        "home" -> "[home]"
        "mail" -> "[mail]"
        "star" -> "[*]"
        "starOff" -> "[ ]"
        "warning" -> "[!]"
        "error" -> "[X]"
        "info" -> "[i]"
        "help" -> "[?]"
        "arrowBack" -> "[<-]"
        "arrowForward" -> "[->]"
        "person" -> "[user]"
        "lock" -> "[lock]"
        "lockOpen" -> "[unlock]"
        "send" -> "[send]"
        else -> "[$name]"
    }
    Text(icon, color = TerminalTheme.accentColor)
}

@Composable
fun A2uiImageView(component: A2uiComponent.Image, surface: SurfaceState) {
    val url = surface.resolveString(component.url)
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(color = TerminalTheme.dimColor)) {
                append("[Image: ")
            }
            withStyle(SpanStyle(color = TerminalTheme.accentColor)) {
                append(url.take(40))
                if (url.length > 40) append("...")
            }
            withStyle(SpanStyle(color = TerminalTheme.dimColor)) {
                append("]")
            }
        }
    )
}

@Composable
fun A2uiDateTimeInputView(
    component: A2uiComponent.DateTimeInput,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    val isFocused = focusManager.isFocused(component.id)
    val value = surface.resolveString(component.value)
    val label = component.label?.let { surface.resolveString(it) }

    val typeHint = buildString {
        if (component.enableDate) append("date")
        if (component.enableDate && component.enableTime) append("+")
        if (component.enableTime) append("time")
        if (isEmpty()) append("datetime")
    }

    Column {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = if (isFocused) TerminalTheme.focusBorderColor else TerminalTheme.dimColor)) {
                    append(if (isFocused) "> " else "  ")
                }
                withStyle(SpanStyle(color = TerminalTheme.dimColor)) {
                    append(label ?: "Date/Time")
                    append(" ($typeHint)")
                }
            }
        )
        Text(
            buildAnnotatedString {
                append("  ")
                withStyle(SpanStyle(color = TerminalTheme.cardBorderColor)) {
                    append("[ ")
                }
                withStyle(SpanStyle(color = TerminalTheme.textColor)) {
                    append(value.ifEmpty { "Not set" })
                }
                if (isFocused) {
                    withStyle(SpanStyle(color = TerminalTheme.focusBorderColor)) {
                        append("_")
                    }
                }
                withStyle(SpanStyle(color = TerminalTheme.cardBorderColor)) {
                    append(" ]")
                }
            }
        )
    }
}

@Composable
fun A2uiTabsView(
    component: A2uiComponent.Tabs,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    var activeTab by remember { mutableStateOf(0) }

    Column {
        // Tab headers
        Row {
            component.tabItems.forEachIndexed { index, tab ->
                val title = surface.resolveString(tab.title)
                val isActive = index == activeTab
                Text(
                    buildAnnotatedString {
                        if (isActive) {
                            withStyle(SpanStyle(color = TerminalTheme.primaryColor)) {
                                append("[ $title ]")
                            }
                        } else {
                            withStyle(SpanStyle(color = TerminalTheme.dimColor)) {
                                append("  $title  ")
                            }
                        }
                    }
                )
            }
        }
        Text("-".repeat(50), color = TerminalTheme.cardBorderColor)
        // Active tab content
        if (activeTab < component.tabItems.size) {
            val childId = component.tabItems[activeTab].child
            val child = surface.getComponent(childId)
            if (child != null) {
                A2uiComponentView(child, surface, focusManager)
            }
        }
    }
}

@Composable
fun A2uiListView(
    component: A2uiComponent.ListComponent,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    when (val children = component.children) {
        is ChildList.StaticList -> {
            if (component.direction == "horizontal") {
                Row {
                    children.ids.forEach { childId ->
                        val child = surface.getComponent(childId)
                        if (child != null) {
                            A2uiComponentView(child, surface, focusManager)
                            Text(" ")
                        }
                    }
                }
            } else {
                Column {
                    children.ids.forEach { childId ->
                        val child = surface.getComponent(childId)
                        if (child != null) {
                            A2uiComponentView(child, surface, focusManager)
                        }
                    }
                }
            }
        }
        is ChildList.Template -> {
            Text("[Dynamic list]", color = TerminalTheme.dimColor)
        }
    }
}

@Composable
fun A2uiModalView(
    component: A2uiComponent.Modal,
    surface: SurfaceState,
    focusManager: FocusManager
) {
    // In terminal, show the entry point child
    val child = surface.getComponent(component.entryPointChild)
    if (child != null) {
        A2uiComponentView(child, surface, focusManager)
    }
}
