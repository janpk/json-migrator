package com.mosedotten.json.migrator.engine.operation

class CreateObject(private val path: String) : Operation {
    private val jsonPath = JsonPath.parse(path)

    override fun apply(document: Document) = document.ensureObject(jsonPath)

    override fun describe() = "createObject(\"$path\")"
}
