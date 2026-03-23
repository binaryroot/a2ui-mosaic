package org.a2ui.mosaic.model

import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Represents a dynamic value in A2UI v0.9 that can be:
 * - A literal value (string, number, boolean, array)
 * - A data binding path (JSON Pointer)
 * - A function call
 */
sealed class DynamicValue {
    data class LiteralString(val value: String) : DynamicValue()
    data class LiteralNumber(val value: Double) : DynamicValue()
    data class LiteralBoolean(val value: Boolean) : DynamicValue()
    data class LiteralStringList(val value: List<String>) : DynamicValue()
    data class Path(val path: String) : DynamicValue()
    data class FunctionCall(
        val call: String,
        val args: Map<String, DynamicValue> = emptyMap(),
        val returnType: String = "boolean"
    ) : DynamicValue()
}

object DynamicValueSerializer : KSerializer<DynamicValue> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("DynamicValue", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: DynamicValue) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is DynamicValue.LiteralString -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is DynamicValue.LiteralNumber -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is DynamicValue.LiteralBoolean -> jsonEncoder.encodeJsonElement(JsonPrimitive(value.value))
            is DynamicValue.LiteralStringList -> jsonEncoder.encodeJsonElement(
                JsonArray(value.value.map { JsonPrimitive(it) })
            )
            is DynamicValue.Path -> jsonEncoder.encodeJsonElement(
                buildJsonObject { put("path", value.path) }
            )
            is DynamicValue.FunctionCall -> jsonEncoder.encodeJsonElement(
                buildJsonObject {
                    put("call", value.call)
                    put("returnType", value.returnType)
                }
            )
        }
    }

    override fun deserialize(decoder: Decoder): DynamicValue {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return parseDynamicValue(element)
    }
}

fun parseDynamicValue(element: JsonElement): DynamicValue {
    return when (element) {
        is JsonPrimitive -> {
            when {
                element.isString -> DynamicValue.LiteralString(element.content)
                element.booleanOrNull != null -> DynamicValue.LiteralBoolean(element.boolean)
                element.doubleOrNull != null -> DynamicValue.LiteralNumber(element.double)
                else -> DynamicValue.LiteralString(element.content)
            }
        }
        is JsonArray -> {
            DynamicValue.LiteralStringList(element.map { (it as JsonPrimitive).content })
        }
        is JsonObject -> {
            when {
                "path" in element -> DynamicValue.Path(element["path"]!!.jsonPrimitive.content)
                "call" in element -> {
                    val call = element["call"]!!.jsonPrimitive.content
                    val returnType = element["returnType"]?.jsonPrimitive?.contentOrNull ?: "boolean"
                    val args = element["args"]?.jsonObject?.mapValues { (_, v) ->
                        parseDynamicValue(v)
                    } ?: emptyMap()
                    DynamicValue.FunctionCall(call, args, returnType)
                }
                else -> DynamicValue.LiteralString(element.toString())
            }
        }
    }
}
