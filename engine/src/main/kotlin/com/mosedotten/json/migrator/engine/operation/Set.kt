package com.mosedotten.json.migrator.engine.operation

import tools.jackson.databind.JsonNode

class Set(val path: String, private val value: JsonNode) : Operation {
    private val jsonPath = JsonPath.parse(path)

    override fun apply(document: Document) {
        document.set(jsonPath, value)
    }

    override fun describe() = "set(\"$path\")"
}
