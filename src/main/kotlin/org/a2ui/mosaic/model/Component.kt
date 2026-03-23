package org.a2ui.mosaic.model

import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.*

/**
 * Represents the children of a layout component.
 * Can be either a static list of component IDs or a template for dynamic generation.
 */
sealed class ChildList {
    data class StaticList(val ids: List<String>) : ChildList()
    data class Template(val componentId: String, val path: String) : ChildList()
}

object ChildListSerializer : KSerializer<ChildList> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("ChildList", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: ChildList) {
        val jsonEncoder = encoder as JsonEncoder
        when (value) {
            is ChildList.StaticList -> jsonEncoder.encodeJsonElement(
                JsonArray(value.ids.map { JsonPrimitive(it) })
            )
            is ChildList.Template -> jsonEncoder.encodeJsonElement(buildJsonObject {
                put("componentId", value.componentId)
                put("path", value.path)
            })
        }
    }

    override fun deserialize(decoder: Decoder): ChildList {
        val jsonDecoder = decoder as JsonDecoder
        val element = jsonDecoder.decodeJsonElement()
        return when (element) {
            is JsonArray -> ChildList.StaticList(element.map { it.jsonPrimitive.content })
            is JsonObject -> ChildList.Template(
                componentId = element["componentId"]!!.jsonPrimitive.content,
                path = element["path"]!!.jsonPrimitive.content
            )
            else -> throw SerializationException("Invalid ChildList: $element")
        }
    }
}

/**
 * A single option in a ChoicePicker component.
 */
data class ChoiceOption(
    val label: DynamicValue,
    val value: String
)

/**
 * A validation check rule.
 */
data class CheckRule(
    val condition: DynamicValue,
    val message: String
)

/**
 * Represents any A2UI v0.9 basic catalog component.
 * Each variant corresponds to a component type in the spec.
 */
sealed class A2uiComponent {
    abstract val id: String

    data class Text(
        override val id: String,
        val text: DynamicValue,
        val variant: String = "body",
        val weight: Double? = null
    ) : A2uiComponent()

    data class Image(
        override val id: String,
        val url: DynamicValue,
        val fit: String = "fill",
        val variant: String = "mediumFeature",
        val weight: Double? = null
    ) : A2uiComponent()

    data class Icon(
        override val id: String,
        val name: DynamicValue,
        val weight: Double? = null
    ) : A2uiComponent()

    data class Row(
        override val id: String,
        val children: ChildList,
        val justify: String = "start",
        val align: String = "stretch",
        val weight: Double? = null
    ) : A2uiComponent()

    data class Column(
        override val id: String,
        val children: ChildList,
        val justify: String = "start",
        val align: String = "stretch",
        val weight: Double? = null
    ) : A2uiComponent()

    data class Card(
        override val id: String,
        val child: String,
        val weight: Double? = null
    ) : A2uiComponent()

    data class Button(
        override val id: String,
        val child: String,
        val action: A2uiAction,
        val variant: String = "default",
        val weight: Double? = null
    ) : A2uiComponent()

    data class TextField(
        override val id: String,
        val label: DynamicValue,
        val value: DynamicValue? = null,
        val variant: String = "shortText",
        val validationRegexp: String? = null,
        val checks: List<CheckRule> = emptyList(),
        val weight: Double? = null
    ) : A2uiComponent()

    data class CheckBox(
        override val id: String,
        val label: DynamicValue,
        val value: DynamicValue,
        val checks: List<CheckRule> = emptyList(),
        val weight: Double? = null
    ) : A2uiComponent()

    data class ChoicePicker(
        override val id: String,
        val options: List<ChoiceOption>,
        val value: DynamicValue,
        val label: DynamicValue? = null,
        val variant: String = "mutuallyExclusive",
        val displayStyle: String = "checkbox",
        val filterable: Boolean = false,
        val checks: List<CheckRule> = emptyList(),
        val weight: Double? = null
    ) : A2uiComponent()

    data class Slider(
        override val id: String,
        val value: DynamicValue,
        val max: Double,
        val min: Double = 0.0,
        val label: DynamicValue? = null,
        val checks: List<CheckRule> = emptyList(),
        val weight: Double? = null
    ) : A2uiComponent()

    data class DateTimeInput(
        override val id: String,
        val value: DynamicValue,
        val enableDate: Boolean = false,
        val enableTime: Boolean = false,
        val label: DynamicValue? = null,
        val min: DynamicValue? = null,
        val max: DynamicValue? = null,
        val checks: List<CheckRule> = emptyList(),
        val weight: Double? = null
    ) : A2uiComponent()

    data class Divider(
        override val id: String,
        val axis: String = "horizontal",
        val weight: Double? = null
    ) : A2uiComponent()

    data class Tabs(
        override val id: String,
        val tabItems: List<TabItem>,
        val weight: Double? = null
    ) : A2uiComponent()

    data class Modal(
        override val id: String,
        val entryPointChild: String,
        val contentChild: String,
        val weight: Double? = null
    ) : A2uiComponent()

    data class ListComponent(
        override val id: String,
        val children: ChildList,
        val direction: String = "vertical",
        val alignment: String = "start",
        val weight: Double? = null
    ) : A2uiComponent()

    /** Fallback for unknown component types */
    data class Unknown(
        override val id: String,
        val componentType: String,
        val rawJson: JsonObject
    ) : A2uiComponent()
}

data class TabItem(
    val title: DynamicValue,
    val child: String
)

/**
 * Custom serializer that reads the "component" discriminator field
 * to determine which A2uiComponent subclass to instantiate.
 */
object A2uiComponentSerializer : KSerializer<A2uiComponent> {
    override val descriptor: SerialDescriptor =
        PrimitiveSerialDescriptor("A2uiComponent", PrimitiveKind.STRING)

    override fun serialize(encoder: Encoder, value: A2uiComponent) {
        val jsonEncoder = encoder as JsonEncoder
        jsonEncoder.encodeJsonElement(buildJsonObject {
            put("id", value.id)
            when (value) {
                is A2uiComponent.Text -> put("component", "Text")
                is A2uiComponent.Row -> put("component", "Row")
                is A2uiComponent.Column -> put("component", "Column")
                is A2uiComponent.Card -> put("component", "Card")
                is A2uiComponent.Button -> put("component", "Button")
                is A2uiComponent.TextField -> put("component", "TextField")
                is A2uiComponent.CheckBox -> put("component", "CheckBox")
                is A2uiComponent.ChoicePicker -> put("component", "ChoicePicker")
                is A2uiComponent.Slider -> put("component", "Slider")
                is A2uiComponent.DateTimeInput -> put("component", "DateTimeInput")
                is A2uiComponent.Divider -> put("component", "Divider")
                is A2uiComponent.Image -> put("component", "Image")
                is A2uiComponent.Icon -> put("component", "Icon")
                is A2uiComponent.Tabs -> put("component", "Tabs")
                is A2uiComponent.Modal -> put("component", "Modal")
                is A2uiComponent.ListComponent -> put("component", "List")
                is A2uiComponent.Unknown -> put("component", value.componentType)
            }
        })
    }

    override fun deserialize(decoder: Decoder): A2uiComponent {
        val jsonDecoder = decoder as JsonDecoder
        val obj = jsonDecoder.decodeJsonElement().jsonObject
        val id = obj["id"]!!.jsonPrimitive.content
        val componentType = obj["component"]?.jsonPrimitive?.contentOrNull ?: "Unknown"
        val json = Json { ignoreUnknownKeys = true }

        return try {
            when (componentType) {
                "Text" -> A2uiComponent.Text(
                    id = id,
                    text = parseDynamicValue(obj["text"] ?: JsonPrimitive("")),
                    variant = obj["variant"]?.jsonPrimitive?.contentOrNull ?: "body",
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "Image" -> A2uiComponent.Image(
                    id = id,
                    url = parseDynamicValue(obj["url"]!!),
                    fit = obj["fit"]?.jsonPrimitive?.contentOrNull ?: "fill",
                    variant = obj["variant"]?.jsonPrimitive?.contentOrNull ?: "mediumFeature",
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "Icon" -> A2uiComponent.Icon(
                    id = id,
                    name = parseDynamicValue(obj["name"]!!),
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "Row" -> A2uiComponent.Row(
                    id = id,
                    children = parseChildList(obj["children"]!!),
                    justify = obj["justify"]?.jsonPrimitive?.contentOrNull ?: "start",
                    align = obj["align"]?.jsonPrimitive?.contentOrNull ?: "stretch",
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "Column" -> A2uiComponent.Column(
                    id = id,
                    children = parseChildList(obj["children"]!!),
                    justify = obj["justify"]?.jsonPrimitive?.contentOrNull ?: "start",
                    align = obj["align"]?.jsonPrimitive?.contentOrNull ?: "stretch",
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "Card" -> A2uiComponent.Card(
                    id = id,
                    child = obj["child"]!!.jsonPrimitive.content,
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "Button" -> A2uiComponent.Button(
                    id = id,
                    child = obj["child"]!!.jsonPrimitive.content,
                    action = json.decodeFromJsonElement(ActionSerializer, obj["action"]!!),
                    variant = obj["variant"]?.jsonPrimitive?.contentOrNull ?: "default",
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "TextField" -> A2uiComponent.TextField(
                    id = id,
                    label = parseDynamicValue(obj["label"]!!),
                    value = obj["value"]?.let { parseDynamicValue(it) },
                    variant = obj["variant"]?.jsonPrimitive?.contentOrNull ?: "shortText",
                    validationRegexp = obj["validationRegexp"]?.jsonPrimitive?.contentOrNull,
                    checks = parseChecks(obj["checks"]),
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "CheckBox" -> A2uiComponent.CheckBox(
                    id = id,
                    label = parseDynamicValue(obj["label"]!!),
                    value = parseDynamicValue(obj["value"]!!),
                    checks = parseChecks(obj["checks"]),
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "ChoicePicker" -> A2uiComponent.ChoicePicker(
                    id = id,
                    options = parseChoiceOptions(obj["options"]!!.jsonArray),
                    value = parseDynamicValue(obj["value"]!!),
                    label = obj["label"]?.let { parseDynamicValue(it) },
                    variant = obj["variant"]?.jsonPrimitive?.contentOrNull ?: "mutuallyExclusive",
                    displayStyle = obj["displayStyle"]?.jsonPrimitive?.contentOrNull ?: "checkbox",
                    filterable = obj["filterable"]?.jsonPrimitive?.booleanOrNull ?: false,
                    checks = parseChecks(obj["checks"]),
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "Slider" -> A2uiComponent.Slider(
                    id = id,
                    value = parseDynamicValue(obj["value"]!!),
                    max = obj["max"]!!.jsonPrimitive.double,
                    min = obj["min"]?.jsonPrimitive?.doubleOrNull ?: 0.0,
                    label = obj["label"]?.let { parseDynamicValue(it) },
                    checks = parseChecks(obj["checks"]),
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "DateTimeInput" -> A2uiComponent.DateTimeInput(
                    id = id,
                    value = parseDynamicValue(obj["value"]!!),
                    enableDate = obj["enableDate"]?.jsonPrimitive?.booleanOrNull ?: false,
                    enableTime = obj["enableTime"]?.jsonPrimitive?.booleanOrNull ?: false,
                    label = obj["label"]?.let { parseDynamicValue(it) },
                    min = obj["min"]?.let { parseDynamicValue(it) },
                    max = obj["max"]?.let { parseDynamicValue(it) },
                    checks = parseChecks(obj["checks"]),
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "Divider" -> A2uiComponent.Divider(
                    id = id,
                    axis = obj["axis"]?.jsonPrimitive?.contentOrNull ?: "horizontal",
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "Tabs" -> A2uiComponent.Tabs(
                    id = id,
                    tabItems = parseTabItems(obj["tabItems"]!!.jsonArray),
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "Modal" -> A2uiComponent.Modal(
                    id = id,
                    entryPointChild = obj["entryPointChild"]!!.jsonPrimitive.content,
                    contentChild = obj["contentChild"]!!.jsonPrimitive.content,
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                "List" -> A2uiComponent.ListComponent(
                    id = id,
                    children = parseChildList(obj["children"]!!),
                    direction = obj["direction"]?.jsonPrimitive?.contentOrNull ?: "vertical",
                    alignment = obj["alignment"]?.jsonPrimitive?.contentOrNull ?: "start",
                    weight = obj["weight"]?.jsonPrimitive?.doubleOrNull
                )
                else -> A2uiComponent.Unknown(id, componentType, obj)
            }
        } catch (e: Exception) {
            A2uiComponent.Unknown(id, componentType, obj)
        }
    }
}

private fun parseChildList(element: JsonElement): ChildList {
    return when (element) {
        is JsonArray -> ChildList.StaticList(element.map { it.jsonPrimitive.content })
        is JsonObject -> ChildList.Template(
            componentId = element["componentId"]!!.jsonPrimitive.content,
            path = element["path"]!!.jsonPrimitive.content
        )
        else -> ChildList.StaticList(emptyList())
    }
}

private fun parseChoiceOptions(array: JsonArray): List<ChoiceOption> {
    return array.map { element ->
        val obj = element.jsonObject
        ChoiceOption(
            label = parseDynamicValue(obj["label"]!!),
            value = obj["value"]!!.jsonPrimitive.content
        )
    }
}

private fun parseTabItems(array: JsonArray): List<TabItem> {
    return array.map { element ->
        val obj = element.jsonObject
        TabItem(
            title = parseDynamicValue(obj["title"]!!),
            child = obj["child"]!!.jsonPrimitive.content
        )
    }
}

private fun parseChecks(element: JsonElement?): List<CheckRule> {
    if (element == null || element !is JsonArray) return emptyList()
    return element.map { item ->
        val obj = item.jsonObject
        CheckRule(
            condition = parseDynamicValue(obj["condition"]!!),
            message = obj["message"]!!.jsonPrimitive.content
        )
    }
}
