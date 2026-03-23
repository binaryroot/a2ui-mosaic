package org.a2ui.mosaic.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Represents an A2UI v0.9 action that can be either:
 * - A server-side event trigger
 * - A local client-side function call
 */
sealed class A2uiAction {
    data class Event(
        val name: String,
        val context: Map<String, DynamicValue> = emptyMap()
    ) : A2uiAction()

    data class FunctionCallAction(
        val functionCall: DynamicValue.FunctionCall
    ) : A2uiAction()
}

object ActionSerializer : KSerializer<A2uiAction> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("A2uiAction", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: A2uiAction) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is A2uiAction.Event -> jsonEncoder.encodeJsonElement(buildJsonObject {
                putJsonObject("event") {
                    put("name", value.name)
                    if (value.context.isNotEmpty()) {
                        putJsonObject("context") {
                            // simplified serialization
                        }
                    }
                }
            })
            is A2uiAction.FunctionCallAction -> jsonEncoder.encodeJsonElement(buildJsonObject {
                putJsonObject("functionCall") {
                    put("call", value.functionCall.call)
                }
            })
        }
    }

    override fun deserialize(decoder: Decoder): A2uiAction {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement().jsonObject
        return when {
            "event" in element -> {
                val event = element["event"]!!.jsonObject
                val name = event["name"]!!.jsonPrimitive.content
                val context = event["context"]?.jsonObject?.mapValues { (_, v) ->
                    parseDynamicValue(v)
                } ?: emptyMap()
                A2uiAction.Event(name, context)
            }
            "functionCall" in element -> {
                val fc = element["functionCall"]!!.jsonObject
                val call = fc["call"]!!.jsonPrimitive.content
                val returnType = fc["returnType"]?.jsonPrimitive?.contentOrNull ?: "void"
                val args = fc["args"]?.jsonObject?.mapValues { (_, v) ->
                    parseDynamicValue(v)
                } ?: emptyMap()
                A2uiAction.FunctionCallAction(
                    DynamicValue.FunctionCall(call, args, returnType)
                )
            }
            else -> throw SerializationException("Unknown action type: $element")
        }
    }
}
