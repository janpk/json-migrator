package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.ExistingFieldException
import tools.jackson.databind.JsonNode

class Add(private val path: String, private val value: JsonNode) : Operation {
    private val jsonPath = JsonPath.parse(path)

    override fun apply(document: Document) {
        if (document.exists(jsonPath)) throw ExistingFieldException(path)
        document.set(jsonPath, value)
    }

    override fun describe() = "add(\"$path\")"
}
