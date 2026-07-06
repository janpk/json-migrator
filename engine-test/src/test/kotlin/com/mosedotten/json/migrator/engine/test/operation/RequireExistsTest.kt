package com.mosedotten.json.migrator.engine.test.operation

import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import com.mosedotten.json.migrator.engine.operation.RequireExists
import com.mosedotten.json.migrator.engine.test.util.JsonFixtures
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.TestInstance.Lifecycle
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource

@DisplayName("When requiring a field exists")
@TestInstance(Lifecycle.PER_CLASS)
internal class RequireExistsTest : JsonFixtures() {

    @ParameterizedTest(name = "passes: {1} present in {0}")
    @MethodSource("presentFields")
    fun `passes when the field is present, leaving the document unchanged`(json: String, path: String) =
        assertUnchanged(json) { RequireExists(path).apply(this) }

    @ParameterizedTest(name = "fails: {1} not resolvable in {0}")
    @MethodSource("unresolvableFields")
    fun `fails when the field cannot be resolved`(json: String, path: String) =
        assertMigratesThrows<MissingFieldException>(json) { RequireExists(path).apply(this) }

    fun presentFields() = listOf(
        Arguments.of("""{"id":"123"}""", "/id"),
        Arguments.of("""{"address":{"city":"Oslo"}}""", "/address/city"),
        Arguments.of("""{"a":{"b":{"c":"value"}}}""", "/a/b/c"),
        Arguments.of("""{"middleName":null}""", "/middleName"),
        Arguments.of("""{"address":{}}""", "/address"),
        Arguments.of("""{"items":[]}""", "/items"),
        Arguments.of("""{"enabled":false}""", "/enabled"),
        Arguments.of("""{"count":0}""", "/count"),
        Arguments.of("""{"id":"123","name":"John","address":{"city":"Oslo"}}""", "/id"),
    )

    fun unresolvableFields() = listOf(
        Arguments.of("""{"name":"John"}""", "/id"),
        Arguments.of("""{"address":{"city":"Oslo"}}""", "/address/zip"),
        Arguments.of("""{"name":"John"}""", "/address/city"),
        Arguments.of("""{"address":"Oslo"}""", "/address/city"),
        Arguments.of("""{"address":[1,2,3]}""", "/address/city"),
    )
}
