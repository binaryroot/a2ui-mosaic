# A2UI Mosaic

A2UI Mosaic is a Kotlin library that bridges the [A2UI 0.9 specification](https://a2ui.org/) with [JakeWharton/mosaic](https://github.com/JakeWharton/mosaic), enabling you to render interactive, server-driven Compose UI forms directly in the terminal.

## Features

- **Full A2UI v0.9 Support**: Implements the A2UI protocol including `createSurface`, `updateComponents`, `updateDataModel`, and `deleteSurface`.
- **Interactive Terminal UI**: Renders A2UI components using Mosaic's Compose runtime for the terminal.
- **Keyboard Navigation**: Built-in focus management with `Tab` / `Shift+Tab` navigation between interactive elements.
- **Data Binding**: Resolves A2UI JSON Pointer paths (`/path/to/data`) against a reactive data model.
- **Action Dispatching**: Handles user interactions (button clicks, form submissions) and generates A2UI client-to-server messages.

## Supported Components

| Component | Terminal Rendering |
|---|---|
| `Text` | Styled text with h1/h2/h3/caption variants |
| `TextField` | Bordered input box with cursor, supports `shortText`, `longText`, `obscured` |
| `Button` | Bracketed label with focus highlight, supports `primary`, `default`, `borderless` |
| `CheckBox` | `[x]` / `[ ]` toggle with label |
| `ChoicePicker` | Radio `(*)` or checkbox `[x]` list, single/multi selection |
| `Slider` | ASCII progress bar with value display |
| `DateTimeInput` | Text input with date/time type hint |
| `Row` / `Column` | Horizontal / vertical layout |
| `Card` | Bordered container |
| `Divider` | Horizontal or vertical line |
| `Tabs` | Tab header bar with active content panel |
| `Icon` | ASCII icon representation (e.g., `[v]`, `[x]`, `[*]`) |
| `Image` | URL placeholder text |
| `List` | Vertical or horizontal list layout |
| `Modal` | Shows entry point child |

## Running

> **Important**: Mosaic requires a real interactive terminal (TTY) to handle keyboard input and ANSI rendering. Gradle's `run` task does **not** provide a TTY.

### Option 1: Build and run the distribution (recommended)

```bash
./gradlew installDist
./build/install/a2ui-mosaic/bin/a2ui-mosaic
```

### Option 2: Run with a custom A2UI JSON file

```bash
./build/install/a2ui-mosaic/bin/a2ui-mosaic path/to/your-form.json
```

### Option 3: Non-interactive mode (for CI, testing, or Gradle)

This renders the form once as a static snapshot and exits:

```bash
./gradlew run --args="--non-interactive"
./gradlew run --args="--non-interactive path/to/form.json"
```

### Keyboard Controls

| Key | Action |
|---|---|
| `Tab` | Move focus to next interactive element |
| `Shift+Tab` | Move focus to previous interactive element |
| `Enter` | Activate button / submit / move to next field |
| `Space` | Toggle checkbox / activate button |
| `Backspace` | Delete last character in text field |
| Any character | Type into focused text field |
| `Ctrl+C` | Quit |

## Using as a Library

```kotlin
import com.jakewharton.mosaic.NonInteractivePolicy
import com.jakewharton.mosaic.runMosaicBlocking
import org.a2ui.mosaic.A2uiMosaicApp
import org.a2ui.mosaic.state.SurfaceManager

fun main() {
    val manager = SurfaceManager()
    
    // Load A2UI messages from JSON
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
                "component": "Column",
                "children": ["greeting", "name-field"]
              },
              {
                "id": "greeting",
                "component": "Text",
                "text": "Hello Terminal!",
                "variant": "h1"
              },
              {
                "id": "name-field",
                "component": "TextField",
                "label": "Your Name",
                "value": {"path": "/name"}
              }
            ]
          }
        },
        {
          "updateDataModel": {
            "surfaceId": "my-form",
            "value": { "name": "" }
          }
        }
      ]
    }
    """)

    runMosaicBlocking {
        A2uiMosaicApp(
            surfaceManager = manager,
            onAction = { clientMessageJson ->
                println("Action: $clientMessageJson")
            }
        )
    }
}
```

## Architecture

```
org.a2ui.mosaic
├── model/           # A2UI v0.9 JSON schema models (kotlinx.serialization)
│   ├── DynamicValue.kt   # Literal values, path references, function calls
│   ├── Action.kt          # Event and function call actions
│   ├── Component.kt       # All 16 component types with custom deserializer
│   └── Message.kt         # Server-to-client and client-to-server messages
├── state/           # Reactive state management
│   ├── SurfaceState.kt    # Per-surface state with data binding
│   └── SurfaceManager.kt  # Multi-surface orchestration
├── render/          # Mosaic @Composable renderers
│   ├── A2uiRenderer.kt    # Component-to-terminal rendering
│   └── InputHandler.kt    # Keyboard input routing
├── focus/           # Focus traversal
│   └── FocusManager.kt    # Tab/Shift+Tab navigation
├── A2uiMosaic.kt    # Main composable entry point
└── sample/          # Example applications
    ├── Main.kt             # CLI runner
    └── ProgrammaticExample.kt  # API usage example
```

## Building

```bash
# Compile
./gradlew build

# Run tests (58 tests)
./gradlew test

# Build distribution
./gradlew installDist
```

## License

This project is licensed under the MIT License.
