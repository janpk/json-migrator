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
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.NullNode

@DisplayName("When describing operations")
@TestInstance(Lifecycle.PER_CLASS)
internal class OperationDescribeTest {

    @ParameterizedTest(name = "{1}")
    @MethodSource("descriptions")
    fun `describes operations with user-facing DSL syntax`(operation: Operation, expected: String) =
        assertEquals(expected, operation.describe())

    @Test
    fun `falls back to class name when operation does not override describe`() {
        assertEquals("FakeOperation", FakeOperation().describe())
    }

    @Suppress("LongMethod") // MethodSource providers are data tables, not logic
    fun descriptions() = listOf(
        Arguments.of(Add("/enabled", BooleanNode.TRUE), "add(\"/enabled\")"),
        Arguments.of(Copy("/id", "/legacyId"), "copy(\"/id\") to \"/legacyId\""),
        Arguments.of(Move("/name", "/fullName"), "move(\"/name\") to \"/fullName\""),
        Arguments.of(Remove("/deprecated"), "remove(\"/deprecated\")"),
        Arguments.of(Set("/enabled", BooleanNode.TRUE), "set(\"/enabled\")"),
        Arguments.of(RemoveIfEmpty("/deprecated"), "removeIfEmpty(\"/deprecated\", cascade = false)"),
        Arguments.of(RemoveIfEmpty("/address", cascade = true), "removeIfEmpty(\"/address\", cascade = true)"),
        Arguments.of(RequireExists("/deprecated"), "requireExists(\"/deprecated\")"),
        Arguments.of(RequireType("/deprecated", STRING), "requireType(\"/deprecated\", STRING)"),
        Arguments.of(Transform("/deprecated") { NullNode.instance }, "transform(\"/deprecated\")"),
        Arguments.of(Custom { }, "custom"),
        Arguments.of(ForEach("/users", emptyList()), "forEach(\"/users\")"),
        Arguments.of(
            Merge(listOf("/firstName", "/lastName"), "/fullName"),
            "merge(\"/firstName\", \"/lastName\") into \"/fullName\"",
        ),
        Arguments.of(
            Split("/fullName", listOf("/firstName", "/lastName")),
            "split(\"/fullName\").into(\"/firstName\", \"/lastName\")",
        ),
    )

    private class FakeOperation : Operation {
        override fun apply(document: Document) = Unit
    }
}
