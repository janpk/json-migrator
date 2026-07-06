package com.mosedotten.json.migrator.engine.operation

class RemoveIfEmpty(private val path: String, private val cascade: Boolean = false) : Operation {
    private val jsonPath = JsonPath.parse(path)

    override fun apply(document: Document) {
        removeIfEmpty(document, jsonPath)
    }

    private fun removeIfEmpty(document: Document, path: JsonPath) {
        document.get(path)
            ?.takeIf { it.isEmptyContainer() }
            ?.also { document.remove(path) }
            ?.also { removeParentIfCascade(document, path) }
    }

    private fun removeParentIfCascade(document: Document, path: JsonPath) {
        if (cascade) removeParent(document, path)
    }

    private fun removeParent(document: Document, path: JsonPath) {
        path.parent?.let { removeIfEmpty(document, it) }
    }

    override fun describe() = "removeIfEmpty(\"$path\", cascade = $cascade)"
}
