package com.mosedotten.json.migrator.engine.test.util

import com.mosedotten.json.migrator.engine.dsl.SchemaBuilder
import com.mosedotten.json.migrator.engine.dsl.schema
import com.mosedotten.json.migrator.engine.operation.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper

internal abstract class TestFixtures {

    protected val mapper = jacksonObjectMapper()

    protected fun obj(json: String): ObjectNode = mapper.readTree(json) as ObjectNode

    protected fun assertMigrates(input: String, expected: String, operation: Document.() -> Unit) {
        val root = obj(input)
        Document(root).apply(operation)
        assertEquals(mapper.readTree(expected), root)
    }
    protected inline fun <reified T : Throwable> assertMigratesThrows(
        input: String,
        noinline operation: Document.() -> Unit,
    ) {
        assertThrows<T> { Document(obj(input)).apply(operation) }
    }

    protected fun assertUnchanged(input: String, operation: Document.() -> Unit) =
        assertMigrates(input, input, operation)

    protected fun assertSchemaMigrates(
        input: String,
        expected: String,
        allowNoVersionField: Boolean = false,
        block: SchemaBuilder.() -> Unit,
    ) {
        val actual = schema(obj(input), allowNoVersionField = allowNoVersionField, block = block)
        assertEquals(mapper.readTree(expected), actual)
    }
}
