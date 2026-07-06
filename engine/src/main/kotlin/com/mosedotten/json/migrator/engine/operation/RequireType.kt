package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.InvalidFieldTypeException

class RequireType(private val path: String, private val type: JsonType) : Operation {
    private val jsonPath = JsonPath.parse(path)

    override fun apply(document: Document) {
        val value = document.require(jsonPath, "Required field")
        if (!type.matches(value)) throw InvalidFieldTypeException(path, type.name, value.javaClass.simpleName)
    }

    override fun describe() = "requireType(\"$path\", $type)"
}
