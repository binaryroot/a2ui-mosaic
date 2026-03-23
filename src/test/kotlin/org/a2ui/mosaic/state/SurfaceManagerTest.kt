package org.a2ui.mosaic.state

import org.a2ui.mosaic.model.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

class SurfaceManagerTest {

    @Test
    fun `create surface via message`() {
        val manager = SurfaceManager()
        manager.processMessage(
            A2uiMessage.CreateSurface(
                surfaceId = "test",
                catalogId = "basic"
            )
        )

        assertNotNull(manager.activeSurface)
        assertEquals("test", manager.activeSurface!!.surfaceId)
    }

    @Test
    fun `update components via message`() {
        val manager = SurfaceManager()
        manager.processMessage(A2uiMessage.CreateSurface(surfaceId = "test", catalogId = "basic"))
        manager.processMessage(A2uiMessage.UpdateComponents(
            surfaceId = "test",
            components = listOf(
                A2uiComponent.Text(id = "root", text = DynamicValue.LiteralString("Hello"))
            )
        ))

        val root = manager.activeSurface!!.getRootComponent()
        assertNotNull(root)
        assertTrue(root is A2uiComponent.Text)
    }

    @Test
    fun `delete surface`() {
        val manager = SurfaceManager()
        manager.processMessage(A2uiMessage.CreateSurface(surfaceId = "test", catalogId = "basic"))
        assertNotNull(manager.activeSurface)

        manager.processMessage(A2uiMessage.DeleteSurface(surfaceId = "test"))
        assertNull(manager.activeSurface)
    }

    @Test
    fun `load example from JSON`() {
        val json = """
        {
            "name": "Test",
            "description": "Test example",
            "messages": [
                {
                    "createSurface": {
                        "surfaceId": "s1",
                        "catalogId": "basic"
                    }
                },
                {
                    "updateComponents": {
                        "surfaceId": "s1",
                        "components": [
                            {
                                "id": "root",
                                "component": "Text",
                                "text": "Hello World"
                            }
                        ]
                    }
                },
                {
                    "updateDataModel": {
                        "surfaceId": "s1",
                        "value": {
                            "name": "Test"
                        }
                    }
                }
            ]
        }
        """.trimIndent()

        val manager = SurfaceManager()
        manager.loadExample(json)

        assertNotNull(manager.activeSurface)
        assertEquals("s1", manager.activeSurface!!.surfaceId)
        assertNotNull(manager.activeSurface!!.getRootComponent())
        assertEquals("Test", manager.activeSurface!!.resolveString(DynamicValue.Path("/name")))
    }

    @Test
    fun `client message callback is invoked`() {
        val manager = SurfaceManager()
        var receivedJson: String? = null
        manager.onClientMessage = { receivedJson = it }

        manager.processMessage(A2uiMessage.CreateSurface(surfaceId = "test", catalogId = "basic", sendDataModel = true))
        manager.processMessage(A2uiMessage.UpdateComponents(
            surfaceId = "test",
            components = listOf(
                A2uiComponent.Button(
                    id = "btn",
                    child = "btn-text",
                    action = A2uiAction.Event(name = "click"),
                    variant = "primary"
                )
            )
        ))

        manager.activeSurface!!.dispatchAction("btn", A2uiAction.Event(name = "click"))

        assertNotNull(receivedJson)
        assertTrue(receivedJson!!.contains("click"))
    }

    @Test
    fun `multiple surfaces`() {
        val manager = SurfaceManager()
        manager.processMessage(A2uiMessage.CreateSurface(surfaceId = "s1", catalogId = "basic"))
        manager.processMessage(A2uiMessage.CreateSurface(surfaceId = "s2", catalogId = "basic"))

        assertEquals(2, manager.surfaces.size)
        // activeSurface should be the last one
        assertEquals("s2", manager.activeSurface!!.surfaceId)
    }

    @Test
    fun `update components on non-existent surface throws`() {
        val manager = SurfaceManager()
        assertThrows(IllegalStateException::class.java) {
            manager.processMessage(A2uiMessage.UpdateComponents(
                surfaceId = "nonexistent",
                components = emptyList()
            ))
        }
    }
}
