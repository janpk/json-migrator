package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.MissingFieldException

class Remove(private val path: String) : Operation {
    private val jsonPath = JsonPath.parse(path)

    override fun apply(document: Document) {
        if (!document.exists(jsonPath)) throw MissingFieldException(path)
        document.remove(jsonPath)
    }

    override fun describe() = "remove(\"$path\")"
}
