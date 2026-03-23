package org.a2ui.mosaic.sample

import org.a2ui.mosaic.model.*
import org.a2ui.mosaic.state.SurfaceManager

/**
 * Demonstrates how to use the A2UI Mosaic library programmatically
 * to build and process A2UI messages without loading from JSON files.
 *
 * This is useful for:
 * - AI agents that generate A2UI messages dynamically
 * - Testing and development
 * - Integration with other systems
 */
object ProgrammaticExample {

    /**
     * Creates a simple survey form using A2UI messages.
     */
    fun createSurveyForm(): SurfaceManager {
        val manager = SurfaceManager()

        // Step 1: Create the surface
        manager.processMessage(
            A2uiMessage.CreateSurface(
                surfaceId = "survey-form",
                catalogId = "https://a2ui.org/specification/v0_9/basic_catalog.json",
                sendDataModel = true
            )
        )

        // Step 2: Define the components
        manager.processMessage(
            A2uiMessage.UpdateComponents(
                surfaceId = "survey-form",
                components = listOf(
                    A2uiComponent.Card(id = "root", child = "main-column"),
                    A2uiComponent.Column(
                        id = "main-column",
                        children = ChildList.StaticList(listOf(
                            "title",
                            "name-field",
                            "email-field",
                            "rating-slider",
                            "feedback-field",
                            "newsletter-checkbox",
                            "submit-btn"
                        ))
                    ),
                    A2uiComponent.Text(
                        id = "title",
                        text = DynamicValue.LiteralString("Customer Satisfaction Survey"),
                        variant = "h2"
                    ),
                    A2uiComponent.TextField(
                        id = "name-field",
                        label = DynamicValue.LiteralString("Your Name"),
                        value = DynamicValue.Path("/name"),
                        variant = "shortText"
                    ),
                    A2uiComponent.TextField(
                        id = "email-field",
                        label = DynamicValue.LiteralString("Email Address"),
                        value = DynamicValue.Path("/email"),
                        variant = "shortText"
                    ),
                    A2uiComponent.Slider(
                        id = "rating-slider",
                        label = DynamicValue.LiteralString("Overall Rating"),
                        value = DynamicValue.Path("/rating"),
                        min = 1.0,
                        max = 10.0
                    ),
                    A2uiComponent.TextField(
                        id = "feedback-field",
                        label = DynamicValue.LiteralString("Additional Feedback"),
                        value = DynamicValue.Path("/feedback"),
                        variant = "longText"
                    ),
                    A2uiComponent.CheckBox(
                        id = "newsletter-checkbox",
                        label = DynamicValue.LiteralString("Subscribe to newsletter"),
                        value = DynamicValue.Path("/newsletter")
                    ),
                    A2uiComponent.Button(
                        id = "submit-btn",
                        child = "submit-btn-text",
                        action = A2uiAction.Event(
                            name = "submit_survey",
                            context = mapOf(
                                "name" to DynamicValue.Path("/name"),
                                "email" to DynamicValue.Path("/email"),
                                "rating" to DynamicValue.Path("/rating"),
                                "feedback" to DynamicValue.Path("/feedback"),
                                "newsletter" to DynamicValue.Path("/newsletter")
                            )
                        ),
                        variant = "primary"
                    ),
                    A2uiComponent.Text(
                        id = "submit-btn-text",
                        text = DynamicValue.LiteralString("Submit Survey")
                    )
                )
            )
        )

        // Step 3: Initialize the data model
        manager.processMessage(
            A2uiMessage.UpdateDataModel(
                surfaceId = "survey-form",
                value = kotlinx.serialization.json.buildJsonObject {
                    put("name", kotlinx.serialization.json.JsonPrimitive(""))
                    put("email", kotlinx.serialization.json.JsonPrimitive(""))
                    put("rating", kotlinx.serialization.json.JsonPrimitive(5))
                    put("feedback", kotlinx.serialization.json.JsonPrimitive(""))
                    put("newsletter", kotlinx.serialization.json.JsonPrimitive(false))
                }
            )
        )

        return manager
    }

    /**
     * Creates a multi-choice preferences form.
     */
    fun createPreferencesForm(): SurfaceManager {
        val manager = SurfaceManager()

        manager.processMessage(
            A2uiMessage.CreateSurface(
                surfaceId = "preferences",
                catalogId = "https://a2ui.org/specification/v0_9/basic_catalog.json",
                sendDataModel = true
            )
        )

        manager.processMessage(
            A2uiMessage.UpdateComponents(
                surfaceId = "preferences",
                components = listOf(
                    A2uiComponent.Column(
                        id = "root",
                        children = ChildList.StaticList(listOf(
                            "title",
                            "divider1",
                            "language-picker",
                            "divider2",
                            "interests-picker",
                            "save-btn"
                        ))
                    ),
                    A2uiComponent.Text(
                        id = "title",
                        text = DynamicValue.LiteralString("User Preferences"),
                        variant = "h1"
                    ),
                    A2uiComponent.Divider(id = "divider1"),
                    A2uiComponent.ChoicePicker(
                        id = "language-picker",
                        label = DynamicValue.LiteralString("Preferred Language"),
                        options = listOf(
                            ChoiceOption(DynamicValue.LiteralString("English"), "en"),
                            ChoiceOption(DynamicValue.LiteralString("Spanish"), "es"),
                            ChoiceOption(DynamicValue.LiteralString("French"), "fr"),
                            ChoiceOption(DynamicValue.LiteralString("German"), "de"),
                            ChoiceOption(DynamicValue.LiteralString("Japanese"), "ja")
                        ),
                        value = DynamicValue.Path("/language"),
                        variant = "mutuallyExclusive"
                    ),
                    A2uiComponent.Divider(id = "divider2"),
                    A2uiComponent.ChoicePicker(
                        id = "interests-picker",
                        label = DynamicValue.LiteralString("Your Interests"),
                        options = listOf(
                            ChoiceOption(DynamicValue.LiteralString("Technology"), "tech"),
                            ChoiceOption(DynamicValue.LiteralString("Science"), "science"),
                            ChoiceOption(DynamicValue.LiteralString("Arts"), "arts"),
                            ChoiceOption(DynamicValue.LiteralString("Sports"), "sports"),
                            ChoiceOption(DynamicValue.LiteralString("Music"), "music")
                        ),
                        value = DynamicValue.Path("/interests"),
                        variant = "multipleSelection"
                    ),
                    A2uiComponent.Button(
                        id = "save-btn",
                        child = "save-btn-text",
                        action = A2uiAction.Event(name = "save_preferences"),
                        variant = "primary"
                    ),
                    A2uiComponent.Text(
                        id = "save-btn-text",
                        text = DynamicValue.LiteralString("Save Preferences")
                    )
                )
            )
        )

        manager.processMessage(
            A2uiMessage.UpdateDataModel(
                surfaceId = "preferences",
                value = kotlinx.serialization.json.buildJsonObject {
                    put("language", kotlinx.serialization.json.JsonArray(
                        listOf(kotlinx.serialization.json.JsonPrimitive("en"))
                    ))
                    put("interests", kotlinx.serialization.json.JsonArray(emptyList()))
                }
            )
        )

        return manager
    }
}
