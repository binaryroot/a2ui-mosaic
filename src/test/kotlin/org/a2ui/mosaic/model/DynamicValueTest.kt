package org.a2ui.mosaic.model

import kotlinx.serialization.json.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class DynamicValueTest {

    @Test
    fun `parse literal string`() {
        val element = JsonPrimitive("hello")
        val result = parseDynamicValue(element)
        assertTrue(result is DynamicValue.LiteralString)
        assertEquals("hello", (result as DynamicValue.LiteralString).value)
    }

    @Test
    fun `parse literal number`() {
        val element = JsonPrimitive(42.0)
        val result = parseDynamicValue(element)
        assertTrue(result is DynamicValue.LiteralNumber)
        assertEquals(42.0, (result as DynamicValue.LiteralNumber).value)
    }

    @Test
    fun `parse literal boolean true`() {
        val element = JsonPrimitive(true)
        val result = parseDynamicValue(element)
        assertTrue(result is DynamicValue.LiteralBoolean)
        assertEquals(true, (result as DynamicValue.LiteralBoolean).value)
    }

    @Test
    fun `parse literal boolean false`() {
        val element = JsonPrimitive(false)
        val result = parseDynamicValue(element)
        assertTrue(result is DynamicValue.LiteralBoolean)
        assertEquals(false, (result as DynamicValue.LiteralBoolean).value)
    }

    @Test
    fun `parse literal string list`() {
        val element = JsonArray(listOf(JsonPrimitive("a"), JsonPrimitive("b"), JsonPrimitive("c")))
        val result = parseDynamicValue(element)
        assertTrue(result is DynamicValue.LiteralStringList)
        assertEquals(listOf("a", "b", "c"), (result as DynamicValue.LiteralStringList).value)
    }

    @Test
    fun `parse path reference`() {
        val element = buildJsonObject { put("path", "/user/name") }
        val result = parseDynamicValue(element)
        assertTrue(result is DynamicValue.Path)
        assertEquals("/user/name", (result as DynamicValue.Path).path)
    }

    @Test
    fun `parse function call`() {
        val element = buildJsonObject {
            put("call", "formatString")
            put("returnType", "string")
            putJsonObject("args") {
                put("value", "hello")
            }
        }
        val result = parseDynamicValue(element)
        assertTrue(result is DynamicValue.FunctionCall)
        val fc = result as DynamicValue.FunctionCall
        assertEquals("formatString", fc.call)
        assertEquals("string", fc.returnType)
        assertTrue(fc.args["value"] is DynamicValue.LiteralString)
    }

    @Test
    fun `parse integer as number`() {
        val element = JsonPrimitive(5)
        val result = parseDynamicValue(element)
        assertTrue(result is DynamicValue.LiteralNumber)
        assertEquals(5.0, (result as DynamicValue.LiteralNumber).value)
    }

    @Test
    fun `parse unknown object as literal string`() {
        val element = buildJsonObject { put("unknown", "value") }
        val result = parseDynamicValue(element)
        assertTrue(result is DynamicValue.LiteralString)
    }
}
