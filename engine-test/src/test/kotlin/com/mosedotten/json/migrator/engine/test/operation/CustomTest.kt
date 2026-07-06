package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.operation.Custom
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.IntNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

@DisplayName("When applying a custom operation")
@Suppress("LargeClass") // Escape-hatch scenarios accumulate; acceptable for test classes
internal class CustomTest : TestFixtures() {

    @Test
    fun `adds a new field`() {
        assertMigrates(
            """{"name":"John"}""",
            """{"name":"John","enabled":true}""",
        ) {
            Custom { node -> node.set("enabled", BooleanNode.TRUE) }.apply(this)
        }
    }

    @Test
    fun `removes a field`() {
        assertMigrates(
            """{"name":"John","deprecated":true}""",
            """{"name":"John"}""",
        ) {
            Custom { node -> node.remove("deprecated") }.apply(this)
        }
    }

    @Test
    fun `overwrites an existing field`() {
        assertMigrates(
            """{"age":30}""",
            """{"age":31}""",
        ) {
            Custom { node -> node.set("age", IntNode.valueOf(31)) }.apply(this)
        }
    }

    @Test
    fun `reads and modifies a value`() {
        assertMigrates(
            """{"age":30}""",
            """{"age":31}""",
        ) {
            Custom { node -> node.set("age", IntNode.valueOf(node.get("age").asInt() + 1)) }.apply(this)
        }
    }

    @Test
    fun `renames a field by restructuring the document`() {
        assertMigrates(
            """{"name":"John"}""",
            """{"fullName":"John"}""",
        ) {
            Custom { node ->
                node.set("fullName", node.get("name"))
                node.remove("name")
            }.apply(this)
        }
    }

    @Test
    fun `adds multiple fields in one operation`() {
        assertMigrates(
            """{}""",
            """{"a":1,"b":2}""",
        ) {
            Custom { node ->
                node.set("a", IntNode.valueOf(1))
                node.set("b", IntNode.valueOf(2))
            }.apply(this)
        }
    }

    @Test
    fun `mutates a nested object`() {
        assertMigrates(
            """{"address":{"city":"Oslo"}}""",
            """{"address":{"city":"Oslo","country":"NO"}}""",
        ) {
            Custom { node ->
                (node.get("address") as ObjectNode).set("country", StringNode.valueOf("NO"))
            }.apply(this)
        }
    }

    @Test
    @Suppress("LongMethod") // Test methods are naturally longer
    fun `applies conditional logic based on document content`() {
        assertMigrates(
            """{"legacyId":"123"}""",
            """{"id":"123"}""",
        ) {
            Custom { node ->
                if (node.has("legacyId")) {
                    node.set("id", node.get("legacyId"))
                    node.remove("legacyId")
                }
            }.apply(this)
        }
    }

    @Test
    fun `is a no-op when the block does nothing`() {
        assertMigrates(
            """{"name":"John","age":30}""",
            """{"name":"John","age":30}""",
        ) {
            Custom { }.apply(this)
        }
    }

    @Test
    fun `applies multiple custom operations in order`() {
        assertMigrates(
            """{"n":1}""",
            """{"n":3}""",
        ) {
            Custom { node -> node.set("n", IntNode.valueOf(node.get("n").asInt() + 1)) }.apply(this)
            Custom { node -> node.set("n", IntNode.valueOf(node.get("n").asInt() + 1)) }.apply(this)
        }
    }

    @Test
    fun `propagates exceptions thrown by the block`() {
        assertMigratesThrows<IllegalStateException>("""{"name":"John"}""") {
            Custom { error("boom") }.apply(this)
        }
    }
}
