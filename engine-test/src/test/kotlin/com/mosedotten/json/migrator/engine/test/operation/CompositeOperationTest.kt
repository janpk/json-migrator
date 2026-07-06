package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.Add
import com.mosedotten.json.migrator.engine.operation.CompositeOperation
import com.mosedotten.json.migrator.engine.operation.Document
import com.mosedotten.json.migrator.engine.operation.Operation
import com.mosedotten.json.migrator.engine.operation.Remove
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.IntNode

@DisplayName("When applying a composite operation")
internal class CompositeOperationTest : TestFixtures() {

    private fun composite(vararg steps: Operation) = object : CompositeOperation() {
        override fun steps(document: Document) = steps.toList()
        override fun describe() = "test"
    }

    @Test
    fun `applies its steps in order`() {
        // Removing "/a" only succeeds if the preceding add ran first.
        assertMigrates("""{}""", """{}""") {
            composite(Add("/a", IntNode.valueOf(1)), Remove("/a")).apply(this)
        }
    }

    @Test
    fun `a failing step propagates and stops the sequence`() {
        assertMigratesThrows<MissingFieldException>("""{}""") {
            composite(Add("/a", IntNode.valueOf(1)), Remove("/missing")).apply(this)
        }
    }
}
