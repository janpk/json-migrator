package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.Transform
import com.mosedotten.json.migrator.engine.test.util.TestFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.IntNode
import tools.jackson.databind.node.NullNode
import tools.jackson.databind.node.StringNode

@DisplayName("When transforming a value")
@Suppress("LargeClass") // Happy-path plus lenient and failure cases accumulate; acceptable for test classes
internal class TransformTest : TestFixtures() {

    @Test
    fun `transforms a number in place`() {
        assertMigrates(
            """{"age":30}""",
            """{"age":31}""",
        ) {
            Transform("/age") { IntNode.valueOf(asInt() + 1) }.apply(this)
        }
    }

    @Test
    fun `transforms a string in place`() {
        assertMigrates(
            """{"name":"john"}""",
            """{"name":"JOHN"}""",
        ) {
            Transform("/name") { StringNode.valueOf(asString().uppercase()) }.apply(this)
        }
    }

    @Test
    fun `transforms a boolean in place`() {
        assertMigrates(
            """{"active":true}""",
            """{"active":false}""",
        ) {
            Transform("/active") { BooleanNode.valueOf(!asBoolean()) }.apply(this)
        }
    }

    @Test
    fun `can change the type of the value`() {
        assertMigrates(
            """{"age":30}""",
            """{"age":"30"}""",
        ) {
            Transform("/age") { StringNode.valueOf(asInt().toString()) }.apply(this)
        }
    }

    @Test
    fun `can transform a value into null`() {
        assertMigrates(
            """{"age":30}""",
            """{"age":null}""",
        ) {
            Transform("/age") { NullNode.instance }.apply(this)
        }
    }

    @Test
    fun `transforms a nested value`() {
        assertMigrates(
            """{"user":{"age":30}}""",
            """{"user":{"age":31}}""",
        ) {
            Transform("/user/age") { IntNode.valueOf(asInt() + 1) }.apply(this)
        }
    }

    @Test
    fun `leaves sibling fields unchanged`() {
        assertMigrates(
            """{"age":30,"name":"John"}""",
            """{"age":31,"name":"John"}""",
        ) {
            Transform("/age") { IntNode.valueOf(asInt() + 1) }.apply(this)
        }
    }

    @Test
    fun `runs the lambda on a present null value`() {
        assertMigrates(
            """{"middleName":null}""",
            """{"middleName":"filled"}""",
        ) {
            Transform("/middleName") { StringNode.valueOf("filled") }.apply(this)
        }
    }

    @Test
    fun `fails when the field is missing`() {
        assertMigratesThrows<MissingFieldException>("""{"name":"John"}""") {
            Transform("/age") { IntNode.valueOf(asInt() + 1) }.apply(this)
        }
    }

    @Test
    fun `fails when a nested field is missing`() {
        assertMigratesThrows<MissingFieldException>("""{"user":{}}""") {
            Transform("/user/age") { IntNode.valueOf(asInt() + 1) }.apply(this)
        }
    }

    @Test
    fun `fails when an intermediate ancestor is missing`() {
        assertMigratesThrows<MissingFieldException>("""{"name":"John"}""") {
            Transform("/user/age") { IntNode.valueOf(asInt() + 1) }.apply(this)
        }
    }

    @Test
    fun `fails when an intermediate ancestor is a scalar`() {
        assertMigratesThrows<MissingFieldException>("""{"user":"John"}""") {
            Transform("/user/age") { IntNode.valueOf(asInt() + 1) }.apply(this)
        }
    }

    @Test
    fun `with lenient is a no-op when the field is missing`() {
        assertMigrates(
            """{"name":"John"}""",
            """{"name":"John"}""",
        ) {
            Transform("/age", lenient = true) { IntNode.valueOf(asInt() + 1) }.apply(this)
        }
    }

    @Test
    fun `with lenient still transforms when the field is present`() {
        assertMigrates(
            """{"age":30}""",
            """{"age":31}""",
        ) {
            Transform("/age", lenient = true) { IntNode.valueOf(asInt() + 1) }.apply(this)
        }
    }

    @Test
    fun `with lenient is a no-op when an intermediate ancestor is missing`() {
        assertMigrates(
            """{"name":"John"}""",
            """{"name":"John"}""",
        ) {
            Transform("/user/age", lenient = true) { IntNode.valueOf(asInt() + 1) }.apply(this)
        }
    }
}
