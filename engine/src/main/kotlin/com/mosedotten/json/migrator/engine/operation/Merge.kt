package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import tools.jackson.databind.node.StringNode

class Merge(
    private val sources: List<String>,
    private val target: String,
    private val joiner: ValueJoinerStrategy = ValueJoinerStrategy.SpaceSeparated,
) : CompositeOperation() {
    private val sourcePaths = sources.map(JsonPath::parse)

    companion object {
        private const val MIN_SOURCES = 2
    }

    init {
        requireAtLeast(sources.size, MIN_SOURCES, "Merge", "sources")
    }

    override fun steps(document: Document): List<Operation> {
        validateSources(document)
        return listOf(Add(target, StringNode.valueOf(joinSources(document)))) + sources.map { Remove(it) }
    }

    private fun joinSources(document: Document) = joiner.join(sourcePaths.map { document.require(it, "Source field") })

    private fun validateSources(document: Document) {
        sourcePaths.firstOrNull { !document.exists(it) }
            ?.let { missing -> throw MissingFieldException(missing.raw, "Source field") }
    }

    override fun describe() = "merge(${sources.joinToString { "\"$it\"" }}) into \"$target\""
}
