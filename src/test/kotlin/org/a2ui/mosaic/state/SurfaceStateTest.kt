package org.a2ui.mosaic.state

import kotlinx.serialization.json.*
import org.a2ui.mosaic.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SurfaceStateTest {

    @Test
    fun `create surface and add components`() {
        val surface = SurfaceState("test", "basic")
        surface.updateComponents(listOf(
            A2uiComponent.Text(
                id = "root",
                text = DynamicValue.LiteralString("Hello"),
                variant = "h1"
            )
        ))

        val root = surface.getRootComponent()
        assertNotNull(root)
        assertTrue(root is A2uiComponent.Text)
        assertEquals("Hello", surface.resolveString((root as A2uiComponent.Text).text))
    }

    @Test
    fun `resolve literal string`() {
        val surface = SurfaceState("test", "basic")
        val result = surface.resolveString(DynamicValue.LiteralString("hello"))
        assertEquals("hello", result)
    }

    @Test
    fun `resolve literal number`() {
        val surface = SurfaceState("test", "basic")
        val result = surface.resolveNumber(DynamicValue.LiteralNumber(42.0))
        assertEquals(42.0, result)
    }

    @Test
    fun `resolve literal boolean`() {
        val surface = SurfaceState("test", "basic")
        val result = surface.resolveBoolean(DynamicValue.LiteralBoolean(true))
        assertTrue(result)
    }

    @Test
    fun `resolve path from data model`() {
        val surface = SurfaceState("test", "basic")
        surface.updateDataModel(null, buildJsonObject {
            put("name", "John")
            put("age", 30)
        })

        assertEquals("John", surface.resolveString(DynamicValue.Path("/name")))
        assertEquals(30.0, surface.resolveNumber(DynamicValue.Path("/age")))
    }

    @Test
    fun `resolve nested path from data model`() {
        val surface = SurfaceState("test", "basic")
        surface.updateDataModel(null, buildJsonObject {
            putJsonObject("user") {
                put("name", "Jane")
                putJsonObject("address") {
                    put("city", "NYC")
                }
            }
        })

        assertEquals("Jane", surface.resolveString(DynamicValue.Path("/user/name")))
        assertEquals("NYC", surface.resolveString(DynamicValue.Path("/user/address/city")))
    }

    @Test
    fun `set string value in data model`() {
        val surface = SurfaceState("test", "basic")
        surface.updateDataModel(null, buildJsonObject {
            put("name", "")
        })

        surface.setStringValue("/name", "Alice")
        assertEquals("Alice", surface.resolveString(DynamicValue.Path("/name")))
    }

    @Test
    fun `set boolean value in data model`() {
        val surface = SurfaceState("test", "basic")
        surface.updateDataModel(null, buildJsonObject {
            put("checked", false)
        })

        surface.setBooleanValue("/checked", true)
        assertTrue(surface.resolveBoolean(DynamicValue.Path("/checked")))
    }

    @Test
    fun `set number value in data model`() {
        val surface = SurfaceState("test", "basic")
        surface.updateDataModel(null, buildJsonObject {
            put("score", 0)
        })

        surface.setNumberValue("/score", 95.5)
        assertEquals(95.5, surface.resolveNumber(DynamicValue.Path("/score")))
    }

    @Test
    fun `set string list value in data model`() {
        val surface = SurfaceState("test", "basic")
        surface.updateDataModel(null, buildJsonObject {
            put("tags", JsonArray(emptyList()))
        })

        surface.setStringListValue("/tags", listOf("a", "b", "c"))
        assertEquals(listOf("a", "b", "c"), surface.resolveStringList(DynamicValue.Path("/tags")))
    }

    @Test
    fun `update data model at specific path`() {
        val surface = SurfaceState("test", "basic")
        surface.updateDataModel(null, buildJsonObject {
            putJsonObject("user") {
                put("name", "Old")
            }
        })

        surface.updateDataModel("/user/name", JsonPrimitive("New"))
        assertEquals("New", surface.resolveString(DynamicValue.Path("/user/name")))
    }

    @Test
    fun `resolve null dynamic value returns empty`() {
        val surface = SurfaceState("test", "basic")
        assertEquals("", surface.resolveString(null))
        assertFalse(surface.resolveBoolean(null))
        assertEquals(0.0, surface.resolveNumber(null))
        assertEquals(emptyList<String>(), surface.resolveStringList(null))
    }

    @Test
    fun `resolve missing path returns default`() {
        val surface = SurfaceState("test", "basic")
        surface.updateDataModel(null, buildJsonObject {
            put("name", "test")
        })

        assertEquals("", surface.resolveString(DynamicValue.Path("/nonexistent")))
        assertFalse(surface.resolveBoolean(DynamicValue.Path("/nonexistent")))
        assertEquals(0.0, surface.resolveNumber(DynamicValue.Path("/nonexistent")))
    }

    @Test
    fun `version increments on updates`() {
        val surface = SurfaceState("test", "basic")
        val v0 = surface.version

        surface.updateComponents(listOf(
            A2uiComponent.Text(id = "root", text = DynamicValue.LiteralString("test"))
        ))
        val v1 = surface.version
        assertTrue(v1 > v0)

        surface.updateDataModel(null, buildJsonObject { put("x", 1) })
        val v2 = surface.version
        assertTrue(v2 > v1)
    }

    @Test
    fun `get data model as map`() {
        val surface = SurfaceState("test", "basic", sendDataModel = true)
        surface.updateDataModel(null, buildJsonObject {
            put("name", "John")
            put("age", 30)
            put("active", true)
        })

        val map = surface.getDataModelAsMap()
        assertEquals("John", map["name"])
        assertEquals(30.0, map["age"])
        assertEquals(true, map["active"])
    }

    @Test
    fun `dispatch action with context resolution`() {
        val surface = SurfaceState("test", "basic", sendDataModel = true)
        surface.updateDataModel(null, buildJsonObject {
            put("username", "john")
            put("password", "secret")
        })

        var receivedMessage: A2uiClientMessage? = null
        surface.onAction = { receivedMessage = it }

        val action = A2uiAction.Event(
            name = "login",
            context = mapOf(
                "username" to DynamicValue.Path("/username"),
                "password" to DynamicValue.Path("/password")
            )
        )

        surface.dispatchAction("btn1", action)

        assertNotNull(receivedMessage)
        assertTrue(receivedMessage is A2uiClientMessage.Action)
        val clientAction = receivedMessage as A2uiClientMessage.Action
        assertEquals("login", clientAction.name)
        assertEquals("test", clientAction.surfaceId)
        assertEquals("btn1", clientAction.sourceComponentId)
        assertEquals("john", clientAction.context["username"])
        assertEquals("secret", clientAction.context["password"])
    }
}
