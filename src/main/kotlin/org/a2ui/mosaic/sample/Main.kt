package org.a2ui.mosaic.sample

import com.jakewharton.mosaic.runMosaicBlocking
import org.a2ui.mosaic.*
import org.a2ui.mosaic.state.SurfaceManager

/**
 * Main entry point for the A2UI Mosaic terminal renderer.
 *
 * Usage:
 *   ./gradlew run                           # Run with built-in login form example
 *   ./gradlew run --args="path/to/file.json" # Run with a custom A2UI example file
 */
fun main(args: Array<String>) {
    val jsonContent = if (args.isNotEmpty()) {
        // Load from file path
        java.io.File(args[0]).readText()
    } else {
        // Load built-in login form example
        object {}.javaClass.getResource("/examples/login-form.json")?.readText()
            ?: getBuiltInLoginForm()
    }

    println("A2UI Mosaic Terminal Renderer v0.1.0")
    println("Loading A2UI surface...")

    val surfaceManager = SurfaceManager()
    surfaceManager.loadExample(jsonContent)

    // Set up action handler to print to stderr
    surfaceManager.onClientMessage = { message ->
        System.err.println("[A2UI Action] $message")
    }

    runMosaicBlocking {
        A2uiMosaicApp(
            surfaceManager = surfaceManager,
            onAction = { message ->
                System.err.println("[A2UI Action] $message")
            },
            onQuit = {
                System.err.println("Quitting A2UI Mosaic...")
            }
        )
    }
}

/**
 * Built-in login form example for when no file is provided.
 */
private fun getBuiltInLoginForm(): String = """
{
  "name": "Login Form",
  "description": "A simple login form example",
  "messages": [
    {
      "createSurface": {
        "surfaceId": "login-surface",
        "catalogId": "https://a2ui.org/specification/v0_9/basic_catalog.json",
        "sendDataModel": true
      }
    },
    {
      "updateComponents": {
        "surfaceId": "login-surface",
        "components": [
          {
            "id": "root",
            "component": "Card",
            "child": "main-col"
          },
          {
            "id": "main-col",
            "component": "Column",
            "children": ["title", "subtitle", "username-field", "password-field", "remember-check", "button-row"]
          },
          {
            "id": "title",
            "component": "Text",
            "text": "Welcome Back",
            "variant": "h1"
          },
          {
            "id": "subtitle",
            "component": "Text",
            "text": "Please sign in to continue",
            "variant": "caption"
          },
          {
            "id": "username-field",
            "component": "TextField",
            "label": "Username",
            "value": {"path": "/username"},
            "variant": "shortText"
          },
          {
            "id": "password-field",
            "component": "TextField",
            "label": "Password",
            "value": {"path": "/password"},
            "variant": "obscured"
          },
          {
            "id": "remember-check",
            "component": "CheckBox",
            "label": "Remember me",
            "value": {"path": "/rememberMe"}
          },
          {
            "id": "button-row",
            "component": "Row",
            "children": ["login-btn", "forgot-btn"]
          },
          {
            "id": "login-btn",
            "component": "Button",
            "child": "login-btn-text",
            "variant": "primary",
            "action": {
              "event": {
                "name": "login",
                "context": {
                  "username": {"path": "/username"},
                  "password": {"path": "/password"},
                  "rememberMe": {"path": "/rememberMe"}
                }
              }
            }
          },
          {
            "id": "login-btn-text",
            "component": "Text",
            "text": "Sign In"
          },
          {
            "id": "forgot-btn",
            "component": "Button",
            "child": "forgot-btn-text",
            "variant": "borderless",
            "action": {
              "event": {
                "name": "forgot_password"
              }
            }
          },
          {
            "id": "forgot-btn-text",
            "component": "Text",
            "text": "Forgot Password?"
          }
        ]
      }
    },
    {
      "updateDataModel": {
        "surfaceId": "login-surface",
        "value": {
          "username": "",
          "password": "",
          "rememberMe": false
        }
      }
    }
  ]
}
""".trimIndent()
