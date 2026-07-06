package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.operation.Add
import com.mosedotten.json.migrator.engine.operation.Copy
import com.mosedotten.json.migrator.engine.operation.Custom
import com.mosedotten.json.migrator.engine.operation.Document
import com.mosedotten.json.migrator.engine.operation.ForEach
import com.mosedotten.json.migrator.engine.operation.JsonType.STRING
import com.mosedotten.json.migrator.engine.operation.Merge
import com.mosedotten.json.migrator.engine.operation.Move
import com.mosedotten.json.migrator.engine.operation.Operation
import com.mosedotten.json.migrator.engine.operation.Remove
import com.mosedotten.json.migrator.engine.operation.RemoveIfEmpty
import com.mosedotten.json.migrator.engine.operation.RequireExists
import com.mosedotten.json.migrator.engine.operation.RequireType
import com.mosedotten.json.migrator.engine.operation.Set
import com.mosedotten.json.migrator.engine.operation.Split
import com.mosedotten.json.migrator.engine.operation.Transform
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.NullNode

@DisplayName("When describing operations")
internal class OperationDescribeTest {

    @Test
    @Suppress("LongMethod") // Acceptable for a test method with one purpose
    fun `describes simple operations with user-facing DSL syntax`() {
        assertEquals("add(\"/enabled\")", Add("/enabled", BooleanNode.TRUE).describe())
        assertEquals("copy(\"/id\") to \"/legacyId\"", Copy("/id", "/legacyId").describe())
        assertEquals("move(\"/name\") to \"/fullName\"", Move("/name", "/fullName").describe())
        assertEquals("remove(\"/deprecated\")", Remove("/deprecated").describe())
        assertEquals("set(\"/enabled\")", Set("/enabled", BooleanNode.TRUE).describe())
        assertEquals("removeIfEmpty(\"/deprecated\", cascade = false)", RemoveIfEmpty("/deprecated").describe())
        assertEquals("requireExists(\"/deprecated\")", RequireExists("/deprecated").describe())
        assertEquals("requireType(\"/deprecated\", STRING)", RequireType("/deprecated", STRING).describe())
        assertEquals("transform(\"/deprecated\")", Transform("/deprecated") { NullNode.instance }.describe())
        assertEquals("custom", Custom { }.describe())
    }

    @Test
    fun `describes composite operations with user-facing DSL syntax`() {
        assertEquals("forEach(\"/users\")", ForEach("/users", emptyList()).describe())
        assertEquals("merge(\"/firstName\", \"/lastName\") into \"/fullName\"", merge().describe())
        assertEquals("split(\"/fullName\").into(\"/firstName\", \"/lastName\")", split().describe())
        assertEquals(
            "removeIfEmpty(\"/address\", cascade = true)",
            RemoveIfEmpty("/address", cascade = true).describe(),
        )
    }

    @Test
    fun `falls back to class name when operation does not override describe`() {
        assertEquals("FakeOperation", FakeOperation().describe())
    }

    private fun merge() = Merge(listOf("/firstName", "/lastName"), "/fullName")

    private fun split() = Split("/fullName", listOf("/firstName", "/lastName"))

    private class FakeOperation : Operation {
        override fun apply(document: Document) = Unit
    }
}
