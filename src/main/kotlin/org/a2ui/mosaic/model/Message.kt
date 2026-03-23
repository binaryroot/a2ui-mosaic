package org.a2ui.mosaic.model

import kotlinx.serialization.*
import kotlinx.serialization.json.*

/**
 * Represents an A2UI v0.9 server-to-client message.
 */
sealed class A2uiMessage {
    abstract val version: String

    data class CreateSurface(
        override val version: String = "v0.9",
        val surfaceId: String,
        val catalogId: String,
        val sendDataModel: Boolean = false,
        val theme: JsonObject? = null
    ) : A2uiMessage()

    data class UpdateComponents(
        override val version: String = "v0.9",
        val surfaceId: String,
        val components: List<A2uiComponent>
    ) : A2uiMessage()

    data class UpdateDataModel(
        override val version: String = "v0.9",
        val surfaceId: String,
        val path: String? = null,
        val value: JsonElement? = null
    ) : A2uiMessage()

    data class DeleteSurface(
        override val version: String = "v0.9",
        val surfaceId: String
    ) : A2uiMessage()
}

/**
 * Represents an A2UI v0.9 client-to-server message.
 */
sealed class A2uiClientMessage {
    data class Action(
        val name: String,
        val surfaceId: String,
        val sourceComponentId: String,
        val timestamp: String,
        val context: Map<String, Any?> = emptyMap()
    ) : A2uiClientMessage()

    data class ValidationError(
        val surfaceId: String,
        val path: String,
        val message: String
    ) : A2uiClientMessage()
}

/**
 * An example file container that wraps a name, description, and list of messages.
 */
data class A2uiExample(
    val name: String,
    val description: String,
    val messages: List<A2uiMessage>
)

/**
 * Parser for A2UI v0.9 JSON messages.
 */
object A2uiMessageParser {
    private val json = Json {
        ignoreUnknownKeys = true
        isLenient = true
    }

    /**
     * Parse a single server-to-client message from a JSON string.
     */
    fun parseMessage(jsonString: String): A2uiMessage {
        val element = json.parseToJsonElement(jsonString).jsonObject
        return parseMessageFromObject(element)
    }

    /**
     * Parse an example file containing multiple messages.
     */
    fun parseExample(jsonString: String): A2uiExample {
        val element = json.parseToJsonElement(jsonString).jsonObject
        val name = element["name"]?.jsonPrimitive?.contentOrNull ?: "Unnamed"
        val description = element["description"]?.jsonPrimitive?.contentOrNull ?: ""
        val messages = element["messages"]?.jsonArray?.map { msg ->
            parseMessageFromObject(msg.jsonObject)
        } ?: emptyList()
        return A2uiExample(name, description, messages)
    }

    /**
     * Parse multiple messages from a JSON array string (streaming scenario).
     */
    fun parseMessages(jsonString: String): List<A2uiMessage> {
        val element = json.parseToJsonElement(jsonString)
        return when (element) {
            is JsonArray -> element.map { parseMessageFromObject(it.jsonObject) }
            is JsonObject -> listOf(parseMessageFromObject(element))
            else -> emptyList()
        }
    }

    private fun parseMessageFromObject(obj: JsonObject): A2uiMessage {
        return when {
            "createSurface" in obj -> {
                val cs = obj["createSurface"]!!.jsonObject
                A2uiMessage.CreateSurface(
                    surfaceId = cs["surfaceId"]!!.jsonPrimitive.content,
                    catalogId = cs["catalogId"]!!.jsonPrimitive.content,
                    sendDataModel = cs["sendDataModel"]?.jsonPrimitive?.booleanOrNull ?: false,
                    theme = cs["theme"]?.jsonObject
                )
            }
            "updateComponents" in obj -> {
                val uc = obj["updateComponents"]!!.jsonObject
                val surfaceId = uc["surfaceId"]!!.jsonPrimitive.content
                val components = uc["components"]!!.jsonArray.map { compElement ->
                    json.decodeFromJsonElement(A2uiComponentSerializer, compElement)
                }
                A2uiMessage.UpdateComponents(surfaceId = surfaceId, components = components)
            }
            "updateDataModel" in obj -> {
                val udm = obj["updateDataModel"]!!.jsonObject
                A2uiMessage.UpdateDataModel(
                    surfaceId = udm["surfaceId"]!!.jsonPrimitive.content,
                    path = udm["path"]?.jsonPrimitive?.contentOrNull,
                    value = udm["value"]
                )
            }
            "deleteSurface" in obj -> {
                val ds = obj["deleteSurface"]!!.jsonObject
                A2uiMessage.DeleteSurface(
                    surfaceId = ds["surfaceId"]!!.jsonPrimitive.content
                )
            }
            else -> throw SerializationException("Unknown message type: ${obj.keys}")
        }
    }

    /**
     * Serialize a client-to-server action message.
     */
    fun serializeAction(action: A2uiClientMessage.Action): String {
        return buildJsonObject {
            put("version", "v0.9")
            putJsonObject("action") {
                put("name", action.name)
                put("surfaceId", action.surfaceId)
                put("sourceComponentId", action.sourceComponentId)
                put("timestamp", action.timestamp)
                putJsonObject("context") {
                    action.context.forEach { (key, value) ->
                        when (value) {
                            is String -> put(key, value)
                            is Number -> put(key, value.toDouble())
                            is Boolean -> put(key, value)
                            null -> put(key, JsonNull)
                            else -> put(key, value.toString())
                        }
                    }
                }
            }
        }.toString()
    }

    /**
     * Serialize a client-to-server validation error message.
     */
    fun serializeError(error: A2uiClientMessage.ValidationError): String {
        return buildJsonObject {
            put("version", "v0.9")
            putJsonObject("error") {
                put("code", "VALIDATION_FAILED")
                put("surfaceId", error.surfaceId)
                put("path", error.path)
                put("message", error.message)
            }
        }.toString()
    }
}
