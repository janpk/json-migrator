package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.InvalidFieldTypeException
import com.mosedotten.json.migrator.engine.operation.CreateObject
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("When creating an object")
@TestInstance(Lifecycle.PER_CLASS)
internal class CreateObjectTest : JsonFixtures() {

    @ParameterizedTest(name = "creates object(s) for path {1}")
    @MethodSource("creations")
    fun `creates objects along the path`(json: String, path: String, expected: String) =
        assertMigrates(json, expected) { CreateObject(path).apply(this) }

    @ParameterizedTest(name = "no-op when {1} already holds an object")
    @MethodSource("existingObjects")
    fun `is a no-op when the path already holds an object`(json: String, path: String) =
        assertUnchanged(json) { CreateObject(path).apply(this) }

    @ParameterizedTest(name = "fails when {1} hits a non-object")
    @MethodSource("nonObjectCollisions")
    fun `fails when the path or an ancestor is not an object`(json: String, path: String) =
        assertMigratesThrows<InvalidFieldTypeException>(json) { CreateObject(path).apply(this) }

    @Suppress("LongMethod") // MethodSource providers are data tables, not logic
    fun creations() = listOf(
        Arguments.of("""{"name":"John"}""", "/address", """{"name":"John","address":{}}"""),
        Arguments.of("""{"name":"John","age":30}""", "/address", """{"name":"John","age":30,"address":{}}"""),
        Arguments.of(
            """{"name":"John"}""",
            "/contact/address/city",
            """{"name":"John","contact":{"address":{"city":{}}}}""",
        ),
        Arguments.of("""{"data":{"users":{}}}""", "/data/users/primary", """{"data":{"users":{"primary":{}}}}"""),
        Arguments.of("""{"root":{"existing":{}}}""", "/root/new", """{"root":{"existing":{},"new":{}}}"""),
        Arguments.of("""{}""", "/address", """{"address":{}}"""),
    )

    fun existingObjects() = listOf(
        Arguments.of("""{"name":"John","address":{"city":"Oslo"}}""", "/address"),
        Arguments.of("""{"address":{}}""", "/address"),
    )

    fun nonObjectCollisions() = listOf(
        Arguments.of("""{"address":"123"}""", "/address"),
        Arguments.of("""{"address":null}""", "/address"),
        Arguments.of("""{"address":[]}""", "/address"),
        Arguments.of("""{"address":42}""", "/address"),
        Arguments.of("""{"address":true}""", "/address"),
        Arguments.of("""{"parent":"string"}""", "/parent/child"),
        Arguments.of("""{"root":{"middle":123}}""", "/root/middle/deep"),
    )
}
