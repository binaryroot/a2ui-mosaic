package org.a2ui.mosaic

import androidx.compose.runtime.*
import com.jakewharton.mosaic.layout.KeyEvent
import com.jakewharton.mosaic.layout.onKeyEvent
import com.jakewharton.mosaic.modifier.Modifier
import com.jakewharton.mosaic.text.SpanStyle
import com.jakewharton.mosaic.text.buildAnnotatedString
import com.jakewharton.mosaic.text.withStyle
import com.jakewharton.mosaic.ui.*
import org.a2ui.mosaic.focus.FocusManager
import org.a2ui.mosaic.model.*
import org.a2ui.mosaic.render.*
import org.a2ui.mosaic.state.SurfaceManager
import org.a2ui.mosaic.state.SurfaceState

/**
 * The main composable that renders an A2UI surface with full keyboard interaction.
 * This is the primary entry point for integrating A2UI with Mosaic.
 *
 * @param surfaceManager The manager holding all active surfaces
 * @param onAction Callback invoked when a client-to-server message is generated
 * @param onQuit Callback invoked when the user presses 'q' or Ctrl+C to quit
 */
@Composable
fun A2uiMosaicApp(
    surfaceManager: SurfaceManager,
    onAction: ((String) -> Unit)? = null,
    onQuit: (() -> Unit)? = null
) {
    val focusManager = remember { FocusManager() }
    val inputHandler = remember(surfaceManager.activeSurface) {
        surfaceManager.activeSurface?.let { InputHandler(it, focusManager) }
    }

    // Wire up the action callback
    LaunchedEffect(onAction) {
        surfaceManager.onClientMessage = onAction
    }

    // Read version to trigger recomposition
    val v = surfaceManager.version

    Column(
        modifier = Modifier.onKeyEvent { event ->
            val key = event.key
            // Tab navigation
            if (key == "Tab") {
                if (event.shift) {
                    focusManager.focusPrevious()
                } else {
                    focusManager.focusNext()
                }
                return@onKeyEvent true
            }
            // Quit
            if (key == "q" && event.ctrl) {
                onQuit?.invoke()
                return@onKeyEvent true
            }
            // Delegate to input handler
            inputHandler?.handleKeyEvent(key) ?: false
        }
    ) {
        // Header
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = TerminalTheme.accentColor)) {
                    append("A2UI Terminal Renderer")
                }
                withStyle(SpanStyle(color = TerminalTheme.dimColor)) {
                    append(" v0.9 | ")
                }
                withStyle(SpanStyle(color = TerminalTheme.dimColor)) {
                    append("Tab: navigate | Enter/Space: interact | Ctrl+Q: quit")
                }
            }
        )
        Text("=".repeat(60), color = TerminalTheme.cardBorderColor)

        // Render the active surface
        val surface = surfaceManager.activeSurface
        if (surface != null) {
            A2uiSurface(surface, focusManager)
        } else {
            Text("")
            Text("  No active surface. Waiting for A2UI messages...", color = TerminalTheme.dimColor)
            Text("")
        }

        // Footer with status
        Text("=".repeat(60), color = TerminalTheme.cardBorderColor)
        val focusInfo = focusManager.focusedId?.let { "Focus: $it" } ?: "No focus"
        Text(focusInfo, color = TerminalTheme.dimColor)
    }
}

/**
 * Convenience function to create and configure a SurfaceManager from JSON.
 */
fun createSurfaceManagerFromJson(json: String): SurfaceManager {
    val manager = SurfaceManager()
    manager.loadExample(json)
    return manager
}

/**
 * Convenience function to create a SurfaceManager from individual messages.
 */
fun createSurfaceManagerFromMessages(messages: String): SurfaceManager {
    val manager = SurfaceManager()
    manager.loadMessages(messages)
    return manager
}
