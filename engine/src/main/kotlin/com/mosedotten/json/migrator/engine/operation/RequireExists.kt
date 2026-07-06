package com.mosedotten.json.migrator.engine.operation

class RequireExists(private val path: String) : Operation {
    private val jsonPath = JsonPath.parse(path)

    override fun apply(document: Document) {
        document.require(jsonPath, "Required field")
    }

    override fun describe() = "requireExists(\"$path\")"
}
