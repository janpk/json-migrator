package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.InvalidFieldTypeException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.Add
import com.mosedotten.json.migrator.engine.operation.ForEach
import com.mosedotten.json.migrator.engine.operation.Move
import com.mosedotten.json.migrator.engine.operation.Remove
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import tools.jackson.databind.node.BooleanNode

@DisplayName("When applying forEach over an array")
internal class ForEachTest : JsonFixtures() {

    @Test
    fun `applies an operation to every element`() {
        assertMigrates(
            """{"users":[{"name":"John"},{"name":"Jane"}]}""",
            """{"users":[{"fullName":"John"},{"fullName":"Jane"}]}""",
        ) {
            ForEach("/users", listOf(Move("/name", "/fullName"))).apply(this)
        }
    }

    @Test
    fun `applies multiple operations to each element in order`() {
        assertMigrates(
            """{"users":[{"name":"John","tmp":1}]}""",
            """{"users":[{"name":"John","active":true}]}""",
        ) {
            ForEach("/users", listOf(Add("/active", BooleanNode.TRUE), Remove("/tmp"))).apply(this)
        }
    }

    @Test
    fun `adds a new field to every element`() {
        assertMigrates(
            """{"users":[{"name":"John"},{"name":"Jane"}]}""",
            """{"users":[{"name":"John","active":true},{"name":"Jane","active":true}]}""",
        ) {
            ForEach("/users", listOf(Add("/active", BooleanNode.TRUE))).apply(this)
        }
    }

    @Test
    fun `operates on nested fields within each element`() {
        assertMigrates(
            """{"users":[{"contact":{"email":"a@b.com"}}]}""",
            """{"users":[{"contact":{},"email":"a@b.com"}]}""",
        ) {
            ForEach("/users", listOf(Move("/contact/email", "/email"))).apply(this)
        }
    }

    @Test
    fun `an empty array is a no-op`() {
        assertMigrates(
            """{"users":[]}""",
            """{"users":[]}""",
        ) {
            ForEach("/users", listOf(Move("/name", "/fullName"))).apply(this)
        }
    }

    @Test
    fun `fields outside the array are left untouched`() {
        assertMigrates(
            """{"users":[{"name":"John"}],"count":1}""",
            """{"users":[{"fullName":"John"}],"count":1}""",
        ) {
            ForEach("/users", listOf(Move("/name", "/fullName"))).apply(this)
        }
    }

    @Test
    fun `iterates an array at a nested path`() {
        assertMigrates(
            """{"data":{"users":[{"name":"John"}]}}""",
            """{"data":{"users":[{"fullName":"John"}]}}""",
        ) {
            ForEach("/data/users", listOf(Move("/name", "/fullName"))).apply(this)
        }
    }

    @Test
    fun `nested forEach transforms elements of an inner array`() {
        assertMigrates(
            """{"orders":[{"items":[{"qty":"1"},{"qty":"2"}]}]}""",
            """{"orders":[{"items":[{"quantity":"1"},{"quantity":"2"}]}]}""",
        ) {
            ForEach("/orders", listOf(ForEach("/items", listOf(Move("/qty", "/quantity"))))).apply(this)
        }
    }

    @Test
    fun `a missing path fails the operation`() {
        assertMigratesThrows<MissingFieldException>("""{"count":1}""") {
            ForEach("/users", listOf(Move("/name", "/fullName"))).apply(this)
        }
    }

    @Test
    fun `a path that is an object rather than an array fails the operation`() {
        assertMigratesThrows<InvalidFieldTypeException>("""{"users":{"name":"John"}}""") {
            ForEach("/users", listOf(Move("/name", "/fullName"))).apply(this)
        }
    }

    @Test
    fun `a path that is a scalar rather than an array fails the operation`() {
        assertMigratesThrows<InvalidFieldTypeException>("""{"users":"John"}""") {
            ForEach("/users", listOf(Move("/name", "/fullName"))).apply(this)
        }
    }

    @Test
    fun `a non-object element fails the operation`() {
        assertMigratesThrows<InvalidFieldTypeException>("""{"users":["John","Jane"]}""") {
            ForEach("/users", listOf(Move("/name", "/fullName"))).apply(this)
        }
    }

    @Test
    fun `a nested operation failing on any element fails the whole operation`() {
        assertMigratesThrows<MissingFieldException>("""{"users":[{"name":"John"},{"other":"x"}]}""") {
            ForEach("/users", listOf(Move("/name", "/fullName"))).apply(this)
        }
    }
}
