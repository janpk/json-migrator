package com.mosedotten.json.migrator.engine.test.util

import com.mosedotten.json.migrator.engine.operation.Document
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.assertThrows
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.jacksonObjectMapper

internal abstract class JsonFixtures {

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
}
