package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.operation.Add
import com.mosedotten.json.migrator.engine.operation.Copy
import com.mosedotten.json.migrator.engine.operation.Document
import com.mosedotten.json.migrator.engine.operation.Move
import com.mosedotten.json.migrator.engine.operation.Operation
import com.mosedotten.json.migrator.engine.operation.Remove
import com.mosedotten.json.migrator.engine.operation.Set
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.BooleanNode

@DisplayName("When describing operations")
internal class OperationDescribeTest {

    @Test
    fun `describes simple operations with user-facing DSL syntax`() {
        assertEquals("add(\"/enabled\")", Add("/enabled", BooleanNode.TRUE).describe())
        assertEquals("copy(\"/id\") to \"/legacyId\"", Copy("/id", "/legacyId").describe())
        assertEquals("move(\"/name\") to \"/fullName\"", Move("/name", "/fullName").describe())
        assertEquals("remove(\"/deprecated\")", Remove("/deprecated").describe())
        assertEquals("set(\"/enabled\")", Set("/enabled", BooleanNode.TRUE).describe())
    }

    @Test
    fun `falls back to class name when operation does not override describe`() {
        assertEquals("FakeOperation", FakeOperation().describe())
    }

    private class FakeOperation : Operation {
        override fun apply(document: Document) = Unit
    }
}
