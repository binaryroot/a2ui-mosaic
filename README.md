# A2UI Mosaic

A2UI Mosaic is a Kotlin library that bridges the [A2UI 0.9 specification](https://a2ui.org/) with [JakeWharton/mosaic](https://github.com/JakeWharton/mosaic), enabling you to render interactive, server-driven Compose UI forms directly in the terminal.

## Features

- **Full A2UI v0.9 Support**: Implements the A2UI protocol including `createSurface`, `updateComponents`, and `updateDataModel`.
- **Interactive Terminal UI**: Renders A2UI components using Mosaic's Compose runtime for the terminal.
- **Keyboard Navigation**: Built-in focus management with `Tab` / `Shift+Tab` navigation between interactive elements.
- **Data Binding**: Resolves A2UI JSON Pointer paths (`/path/to/data`) against a reactive data model.
- **Action Dispatching**: Handles user interactions (button clicks, form submissions) and generates A2UI client-to-server messages.

## Supported Components

- `Text` (with h1, h2, h3, caption variants)
- `TextField` (shortText, longText, obscured)
- `Button` (primary, default, borderless)
- `CheckBox`
- `ChoicePicker` (single and multiple selection)
- `Slider`
- `DateTimeInput`
- `Row` & `Column`
- `Card`
- `Divider`
- `Tabs`
- `Icon` & `Image` (rendered as text placeholders)

## Usage

### Running the Sample Application

The project includes a sample application that renders a login form defined in A2UI JSON format.

```bash
# Run the built-in login form example
./gradlew run

# Run with a custom A2UI JSON file
./gradlew run --args="path/to/your-form.json"
```

### Using as a Library

You can use A2UI Mosaic programmatically to build terminal UIs dynamically:

```kotlin
import com.jakewharton.mosaic.runMosaicBlocking
import org.a2ui.mosaic.A2uiMosaicApp
import org.a2ui.mosaic.state.SurfaceManager

fun main() {
    // 1. Create a SurfaceManager
    val manager = SurfaceManager()
    
    // 2. Load A2UI messages (from JSON string or programmatically)
    manager.loadExample("""
    {
      "messages": [
        {
          "createSurface": {
            "surfaceId": "my-form",
            "catalogId": "basic"
          }
        },
        {
          "updateComponents": {
            "surfaceId": "my-form",
            "components": [
              {
                "id": "root",
                "component": "Text",
                "text": "Hello Terminal!"
              }
            ]
          }
        }
      ]
    }
    """)

    // 3. Render the UI using Mosaic
    runMosaicBlocking {
        A2uiMosaicApp(
            surfaceManager = manager,
            onAction = { clientMessageJson ->
                // Send this JSON back to your server
                println("Action triggered: $clientMessageJson")
            },
            onQuit = {
                // Handle quit (Ctrl+Q)
            }
        )
    }
}
```

## Architecture

- **`org.a2ui.mosaic.model`**: Kotlinx Serialization models for the A2UI v0.9 JSON schema.
- **`org.a2ui.mosaic.state`**: State management (`SurfaceManager`, `SurfaceState`) that handles data binding and reactive updates.
- **`org.a2ui.mosaic.render`**: Mosaic `@Composable` functions that translate A2UI components into terminal UI elements.
- **`org.a2ui.mosaic.focus`**: Keyboard input handling and focus traversal logic.

## Building

```bash
# Compile the project
./gradlew build

# Run tests
./gradlew test
```

## License

This project is licensed under the MIT License.
