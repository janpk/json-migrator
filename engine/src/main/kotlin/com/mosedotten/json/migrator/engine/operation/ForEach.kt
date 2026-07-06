package com.mosedotten.json.migrator.engine.operation

class ForEach(private val path: String, private val operations: List<Operation>) : Operation {
    private val jsonPath = JsonPath.parse(path)

    override fun apply(document: Document) {
        document.children(jsonPath).forEach { child -> operations.forEach { it.apply(child) } }
    }

    override fun describe() = "forEach(\"$path\")"
}
