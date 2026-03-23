package org.a2ui.mosaic.state

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import org.a2ui.mosaic.model.*

/**
 * Manages all active A2UI surfaces and processes incoming messages.
 * This is the main entry point for the A2UI protocol handling.
 */
class SurfaceManager {
    private val _surfaces = mutableMapOf<String, SurfaceState>()

    /** Observable version counter for recomposition */
    var version by mutableStateOf(0L)
        private set

    /** Callback for outgoing client-to-server messages */
    var onClientMessage: ((String) -> Unit)? = null

    /** Get all active surfaces */
    val surfaces: Map<String, SurfaceState> get() = _surfaces

    /** Get the most recently created surface (convenience for single-surface apps) */
    val activeSurface: SurfaceState? get() = _surfaces.values.lastOrNull()

    /**
     * Process a single A2UI server-to-client message.
     */
    fun processMessage(message: A2uiMessage) {
        when (message) {
            is A2uiMessage.CreateSurface -> {
                val surface = SurfaceState(
                    surfaceId = message.surfaceId,
                    catalogId = message.catalogId,
                    sendDataModel = message.sendDataModel
                )
                surface.onAction = { clientMsg ->
                    handleClientMessage(clientMsg)
                }
                _surfaces[message.surfaceId] = surface
                version++
            }
            is A2uiMessage.UpdateComponents -> {
                val surface = _surfaces[message.surfaceId]
                    ?: throw IllegalStateException("Surface ${message.surfaceId} not found. Send createSurface first.")
                surface.updateComponents(message.components)
                version++
            }
            is A2uiMessage.UpdateDataModel -> {
                val surface = _surfaces[message.surfaceId]
                    ?: throw IllegalStateException("Surface ${message.surfaceId} not found. Send createSurface first.")
                surface.updateDataModel(message.path, message.value)
                version++
            }
            is A2uiMessage.DeleteSurface -> {
                _surfaces.remove(message.surfaceId)
                version++
            }
        }
    }

    /**
     * Process multiple messages (e.g., from an example file).
     */
    fun processMessages(messages: List<A2uiMessage>) {
        messages.forEach { processMessage(it) }
    }

    /**
     * Load and process an A2UI example file.
     */
    fun loadExample(jsonString: String) {
        val example = A2uiMessageParser.parseExample(jsonString)
        processMessages(example.messages)
    }

    /**
     * Load and process a raw JSON message or array of messages.
     */
    fun loadMessages(jsonString: String) {
        val messages = A2uiMessageParser.parseMessages(jsonString)
        processMessages(messages)
    }

    private fun handleClientMessage(clientMsg: A2uiClientMessage) {
        val json = when (clientMsg) {
            is A2uiClientMessage.Action -> A2uiMessageParser.serializeAction(clientMsg)
            is A2uiClientMessage.ValidationError -> A2uiMessageParser.serializeError(clientMsg)
        }
        onClientMessage?.invoke(json)
    }
}
