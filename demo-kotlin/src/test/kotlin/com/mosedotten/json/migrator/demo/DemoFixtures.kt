package com.mosedotten.json.migrator.demo

import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import tools.jackson.databind.node.ObjectNode
import tools.jackson.module.kotlin.kotlinModule

internal abstract class DemoFixtures {

    // A real application is lenient about fields it does not model, so it tolerates newer producers.
    protected val mapper: JsonMapper = JsonMapper.builder()
        .addModule(kotlinModule())
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .build()

    /** A credit-application document as it might sit in the database (or arrive on Kafka) at [version]. */
    protected fun storedDocument(version: Int): ObjectNode =
        mapper.readTree(resource("credit-application-v$version.json")) as ObjectNode

    private fun resource(name: String): String =
        requireNotNull(javaClass.getResource("/$name")) { "missing test resource $name" }.readText()
}
