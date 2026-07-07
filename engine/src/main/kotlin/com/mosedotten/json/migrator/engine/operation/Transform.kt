package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import tools.jackson.databind.JsonNode

class Transform(
    private val path: String,
    private val lenient: Boolean = false,
    private val transformation: JsonNode.() -> JsonNode,
) : Operation {
    private val jsonPath = JsonPath.parse(path)

    override fun apply(document: Document) {
        val current = document.get(jsonPath) ?: return validateMissingField()
        document.set(jsonPath, current.transformation())
    }

    private fun validateMissingField() {
        if (!lenient) throw MissingFieldException(path)
    }

    override fun describe() = "transform(\"$path\")"
}
