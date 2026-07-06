package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.operation.Custom
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.IntNode

@DisplayName("When applying a custom operation")
internal class CustomTest : TestFixtures() {

    @Test
    fun `runs the block against the root and persists its mutations`() {
        assertMigrates(
            """{"name":"John"}""",
            """{"name":"John","enabled":true}""",
        ) {
            Custom { node -> node.set("enabled", BooleanNode.TRUE) }.apply(this)
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
