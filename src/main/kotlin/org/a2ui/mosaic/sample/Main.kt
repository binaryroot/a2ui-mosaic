package org.a2ui.mosaic.sample

import com.jakewharton.mosaic.NonInteractivePolicy
import com.jakewharton.mosaic.runMosaicBlocking
import org.a2ui.mosaic.*
import org.a2ui.mosaic.state.SurfaceManager

/**
 * Main entry point for the A2UI Mosaic terminal renderer.
 *
 * IMPORTANT: Mosaic requires a real TTY (interactive terminal) to function.
 * Gradle's `run` task does NOT provide a TTY. You must either:
 *
 *   1. Build and run the distribution:
 *      ./gradlew installDist
 *      ./build/install/a2ui-mosaic/bin/a2ui-mosaic
 *
 *   2. Or run the fat JAR directly:
 *      ./gradlew shadowJar
 *      java -jar build/libs/a2ui-mosaic-0.1.0-all.jar
 *
 *   3. Or use --non-interactive flag for a static one-shot render:
 *      ./gradlew run --args="--non-interactive"
 *
 * Usage:
 *   a2ui-mosaic                               # Run with built-in login form example
 *   a2ui-mosaic path/to/file.json             # Run with a custom A2UI example file
 *   a2ui-mosaic --non-interactive              # Render once without TTY (for CI/testing)
 *   a2ui-mosaic --non-interactive file.json    # Render a file once without TTY
 */
fun main(args: Array<String>) {
    val argList = args.toMutableList()
    val nonInteractive = argList.remove("--non-interactive") || argList.remove("-n")

    val jsonContent = if (argList.isNotEmpty()) {
        val file = java.io.File(argList[0])
        if (!file.exists()) {
            System.err.println("Error: File not found: ${argList[0]}")
            System.exit(1)
        }
        file.readText()
    } else {
        object {}.javaClass.getResource("/examples/login-form.json")?.readText()
            ?: getBuiltInLoginForm()
    }

    val surfaceManager = SurfaceManager()
    surfaceManager.loadExample(jsonContent)

    surfaceManager.onClientMessage = { message ->
        System.err.println("[A2UI Action] $message")
    }

    if (nonInteractive) {
        // Non-interactive mode: render once and exit.
        // Uses Mosaic's Ignore policy so it runs with a fake terminal.
        System.err.println("A2UI Mosaic Terminal Renderer v0.1.0 (non-interactive mode)")
        runMosaicBlocking(onNonInteractive = NonInteractivePolicy.Ignore) {
            A2uiMosaicApp(
                surfaceManager = surfaceManager,
                onAction = { message ->
                    System.err.println("[A2UI Action] $message")
                },
                onQuit = {}
            )
        }
    } else {
        // Interactive mode: requires a real TTY.
        // If no TTY is available, prints a helpful error message.
        val success = runMosaicBlocking(onNonInteractive = NonInteractivePolicy.Return) {
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

        if (!success) {
            System.err.println("""
                |
                |ERROR: No interactive terminal (TTY) detected.
                |
                |Mosaic requires a real terminal to handle keyboard input and rendering.
                |Gradle's `run` task does NOT provide a TTY.
                |
                |To run interactively, build the distribution first:
                |
                |  ./gradlew installDist
                |  ./build/install/a2ui-mosaic/bin/a2ui-mosaic
                |
                |Or use --non-interactive for a static one-shot render:
                |
                |  ./gradlew run --args="--non-interactive"
                |
            """.trimMargin())
            System.exit(1)
        }
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
