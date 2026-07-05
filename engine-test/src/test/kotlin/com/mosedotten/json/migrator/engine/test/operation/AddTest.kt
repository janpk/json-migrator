package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.ExistingFieldException
import com.mosedotten.json.migrator.engine.operation.Add
import com.mosedotten.json.migrator.engine.operation.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.IntNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper

@DisplayName("When adding a field")
internal class AddTest {
    private val mapper = jacksonObjectMapper()
    private fun document(json: String): ObjectNode = mapper.readTree(json) as ObjectNode

    @Test
    fun `expect success if it doesn't exist`() {
        val root = document("""{"name": "John Doe"}""")
        val expected = document("""{"name" : "John Doe", "newWithDefault" : true}""")
        Add("/newWithDefault", BooleanNode.TRUE)
            .apply(Document(root)).also {
                assertEquals(expected, root)
            }
    }

    @Test
    fun `expect exception if it exist`() {
        val root = document("{\"name\": \"John Doe\"}")
        assertThrows<ExistingFieldException> {
            Add("/name", IntNode.valueOf(30)).apply(Document(root))
        }
    }

    @Test
    fun `to missing parent, creates nested path`() {
        val root = document("""{"name":"John Doe","age":30}""")
        val expected = document("""{"name":"John Doe","age":30,"contact":{"verified":true}}""")
        Add("/contact/verified", BooleanNode.TRUE).apply(Document(root)).also {
            assertEquals(expected, root)
        }
    }
}
