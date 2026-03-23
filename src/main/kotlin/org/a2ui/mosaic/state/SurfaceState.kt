package org.a2ui.mosaic.state

import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.serialization.json.*
import org.a2ui.mosaic.model.*

/**
 * Manages the state of a single A2UI surface.
 * Holds the component tree, data model, and provides data binding resolution.
 */
class SurfaceState(
    val surfaceId: String,
    val catalogId: String,
    val sendDataModel: Boolean = false
) {
    /** All registered components by ID */
    private val _components = mutableMapOf<String, A2uiComponent>()
    val components: Map<String, A2uiComponent> get() = _components

    /** The data model as a mutable JSON-like tree */
    private var _dataModel: JsonElement = JsonObject(emptyMap())

    /** Compose-observable version counter to trigger recomposition */
    var version by mutableStateOf(0L)
        private set

    /** Callback for client-to-server messages */
    var onAction: ((A2uiClientMessage) -> Unit)? = null

    /**
     * Get the root component (must have id "root").
     */
    fun getRootComponent(): A2uiComponent? = _components["root"]

    /**
     * Get a component by ID.
     */
    fun getComponent(id: String): A2uiComponent? = _components[id]

    /**
     * Apply an updateComponents message.
     */
    fun updateComponents(components: List<A2uiComponent>) {
        for (component in components) {
            _components[component.id] = component
        }
        version++
    }

    /**
     * Apply an updateDataModel message.
     */
    fun updateDataModel(path: String?, value: JsonElement?) {
        if (path == null || path == "/" || path.isEmpty()) {
            // Replace entire data model
            _dataModel = value ?: JsonObject(emptyMap())
        } else {
            // Patch at specific path
            _dataModel = setValueAtPath(_dataModel, path, value)
        }
        version++
    }

    /**
     * Resolve a DynamicValue against the current data model.
     */
    fun resolveString(dv: DynamicValue?): String {
        if (dv == null) return ""
        return when (dv) {
            is DynamicValue.LiteralString -> dv.value
            is DynamicValue.LiteralNumber -> dv.value.toString()
            is DynamicValue.LiteralBoolean -> dv.value.toString()
            is DynamicValue.LiteralStringList -> dv.value.joinToString(", ")
            is DynamicValue.Path -> getStringAtPath(dv.path)
            is DynamicValue.FunctionCall -> resolveFunctionCall(dv)
        }
    }

    /**
     * Resolve a DynamicValue to a boolean.
     */
    fun resolveBoolean(dv: DynamicValue?): Boolean {
        if (dv == null) return false
        return when (dv) {
            is DynamicValue.LiteralBoolean -> dv.value
            is DynamicValue.LiteralString -> dv.value.toBoolean()
            is DynamicValue.Path -> getBooleanAtPath(dv.path)
            else -> false
        }
    }

    /**
     * Resolve a DynamicValue to a number.
     */
    fun resolveNumber(dv: DynamicValue?): Double {
        if (dv == null) return 0.0
        return when (dv) {
            is DynamicValue.LiteralNumber -> dv.value
            is DynamicValue.LiteralString -> dv.value.toDoubleOrNull() ?: 0.0
            is DynamicValue.Path -> getNumberAtPath(dv.path)
            else -> 0.0
        }
    }

    /**
     * Resolve a DynamicValue to a string list.
     */
    fun resolveStringList(dv: DynamicValue?): List<String> {
        if (dv == null) return emptyList()
        return when (dv) {
            is DynamicValue.LiteralStringList -> dv.value
            is DynamicValue.Path -> getStringListAtPath(dv.path)
            else -> emptyList()
        }
    }

    /**
     * Update a value in the data model at the given JSON Pointer path.
     * Used when interactive components (TextField, CheckBox, etc.) change their value.
     */
    fun setDataValue(path: String, value: JsonElement) {
        _dataModel = setValueAtPath(_dataModel, path, value)
        version++
    }

    /**
     * Set a string value in the data model.
     */
    fun setStringValue(path: String, value: String) {
        setDataValue(path, JsonPrimitive(value))
    }

    /**
     * Set a boolean value in the data model.
     */
    fun setBooleanValue(path: String, value: Boolean) {
        setDataValue(path, JsonPrimitive(value))
    }

    /**
     * Set a number value in the data model.
     */
    fun setNumberValue(path: String, value: Double) {
        setDataValue(path, JsonPrimitive(value))
    }

    /**
     * Set a string list value in the data model.
     */
    fun setStringListValue(path: String, value: List<String>) {
        setDataValue(path, JsonArray(value.map { JsonPrimitive(it) }))
    }

    /**
     * Get the full data model as a resolved map (for sendDataModel).
     */
    fun getDataModelAsMap(): Map<String, Any?> {
        return jsonElementToMap(_dataModel)
    }

    /**
     * Dispatch an action event from a component.
     */
    fun dispatchAction(componentId: String, action: A2uiAction.Event) {
        val resolvedContext = action.context.mapValues { (_, dv) ->
            when (dv) {
                is DynamicValue.LiteralString -> dv.value
                is DynamicValue.LiteralNumber -> dv.value
                is DynamicValue.LiteralBoolean -> dv.value
                is DynamicValue.Path -> getValueAtPath(dv.path)?.let { jsonElementToAny(it) }
                else -> resolveString(dv)
            }
        }

        val clientAction = A2uiClientMessage.Action(
            name = action.name,
            surfaceId = surfaceId,
            sourceComponentId = componentId,
            timestamp = java.time.Instant.now().toString(),
            context = if (sendDataModel) {
                resolvedContext + ("_dataModel" to getDataModelAsMap())
            } else {
                resolvedContext
            }
        )
        onAction?.invoke(clientAction)
    }

    // --- Private helpers ---

    private fun getStringAtPath(path: String): String {
        val element = getValueAtPath(path) ?: return ""
        return when (element) {
            is JsonPrimitive -> element.contentOrNull ?: ""
            else -> element.toString()
        }
    }

    private fun getBooleanAtPath(path: String): Boolean {
        val element = getValueAtPath(path) ?: return false
        return when (element) {
            is JsonPrimitive -> element.booleanOrNull ?: false
            else -> false
        }
    }

    private fun getNumberAtPath(path: String): Double {
        val element = getValueAtPath(path) ?: return 0.0
        return when (element) {
            is JsonPrimitive -> element.doubleOrNull ?: 0.0
            else -> 0.0
        }
    }

    private fun getStringListAtPath(path: String): List<String> {
        val element = getValueAtPath(path) ?: return emptyList()
        return when (element) {
            is JsonArray -> element.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
            else -> emptyList()
        }
    }

    private fun getValueAtPath(path: String): JsonElement? {
        if (path.isEmpty() || path == "/") return _dataModel
        val segments = path.trimStart('/').split('/')
        var current: JsonElement = _dataModel
        for (segment in segments) {
            current = when (current) {
                is JsonObject -> current[segment] ?: return null
                is JsonArray -> {
                    val index = segment.toIntOrNull() ?: return null
                    current.getOrNull(index) ?: return null
                }
                else -> return null
            }
        }
        return current
    }

    private fun setValueAtPath(root: JsonElement, path: String, value: JsonElement?): JsonElement {
        if (path.isEmpty() || path == "/") return value ?: JsonObject(emptyMap())
        val segments = path.trimStart('/').split('/')
        return setValueRecursive(root, segments, 0, value)
    }

    private fun setValueRecursive(
        current: JsonElement,
        segments: List<String>,
        index: Int,
        value: JsonElement?
    ): JsonElement {
        if (index >= segments.size) return value ?: JsonNull

        val segment = segments[index]

        if (current is JsonObject || current is JsonNull) {
            val obj = if (current is JsonObject) current else JsonObject(emptyMap())
            val entries = obj.toMutableMap()
            if (index == segments.size - 1) {
                if (value != null) {
                    entries[segment] = value
                } else {
                    entries.remove(segment)
                }
            } else {
                val child = entries[segment] ?: JsonObject(emptyMap())
                entries[segment] = setValueRecursive(child, segments, index + 1, value)
            }
            return JsonObject(entries)
        }

        return current
    }

    private fun resolveFunctionCall(fc: DynamicValue.FunctionCall): String {
        // Basic built-in function support
        return when (fc.call) {
            "formatString" -> {
                val template = resolveString(fc.args["value"])
                resolveFormatString(template)
            }
            else -> "[${fc.call}()]"
        }
    }

    private fun resolveFormatString(template: String): String {
        // Replace ${/path} references with data model values
        val regex = Regex("""\$\{(/[^}]*)\}""")
        return regex.replace(template) { match ->
            val path = match.groupValues[1]
            getStringAtPath(path)
        }
    }

    private fun jsonElementToMap(element: JsonElement): Map<String, Any?> {
        return when (element) {
            is JsonObject -> element.mapValues { (_, v) -> jsonElementToAny(v) }
            else -> emptyMap()
        }
    }

    private fun jsonElementToAny(element: JsonElement): Any? {
        return when (element) {
            is JsonNull -> null
            is JsonPrimitive -> {
                element.booleanOrNull ?: element.doubleOrNull ?: element.contentOrNull
            }
            is JsonArray -> element.map { jsonElementToAny(it) }
            is JsonObject -> element.mapValues { (_, v) -> jsonElementToAny(v) }
        }
    }
}
