package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.ExistingFieldException
import com.mosedotten.json.migrator.engine.operation.Add
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.IntNode

@DisplayName("When adding a field")
internal class AddTest : TestFixtures() {
    @Test
    fun `expect success if it doesn't exist`() {
        assertMigrates("""{"name": "John Doe"}""", """{"name" : "John Doe", "newWithDefault" : true}""") {
            Add("/newWithDefault", BooleanNode.TRUE).apply(this)
        }
    }

    @Test
    fun `expect exception if it exist`() {
        assertMigratesThrows<ExistingFieldException>("{\"name\": \"John Doe\"}") {
            Add("/name", IntNode.valueOf(30)).apply(this)
        }
    }

    @Test
    fun `to missing parent creates nested path`() {
        assertMigrates(
            """{"name":"John Doe","age":30}""",
            """{"name":"John Doe","age":30,"contact":{"verified":true}}""",
        ) {
            Add("/contact/verified", BooleanNode.TRUE).apply(this)
        }
    }
}
