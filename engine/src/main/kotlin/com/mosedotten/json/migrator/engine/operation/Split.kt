package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.ExistingFieldException
import com.mosedotten.json.migrator.engine.exception.InvalidFieldValueException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import tools.jackson.databind.node.StringNode

class Split(
    private val source: String,
    private val targets: List<String>,
    private val splitter: ValueSplitterStrategy = ValueSplitterStrategy.SpaceSeparated,
) : CompositeOperation() {
    companion object {
        private const val MIN_TARGETS = 2
    }
    private val sourcePath = JsonPath.parse(source)
    private val targetPaths = targets.map(JsonPath::parse)

    init {
        requireAtLeast(targets.size, MIN_TARGETS, "Split", "targets")
    }

    override fun steps(document: Document): List<Operation> {
        if (!document.exists(sourcePath)) throw MissingFieldException(source, "Source field")
        val pieces = splitter.split(document.require(sourcePath, "Source field"))
        validatePieces(pieces)
        validateTargetsAreMissing(document)
        return targets.zip(pieces).map { (target, piece) -> Add(target, StringNode.valueOf(piece)) } + Remove(source)
    }

    private fun validatePieces(pieces: List<String>) {
        if (pieces.size != targets.size) {
            throw InvalidFieldValueException(
                source,
                "split produced ${pieces.size} pieces, but ${targets.size} targets were given",
            )
        }
    }

    private fun validateTargetsAreMissing(document: Document) {
        targetPaths.forEach { if (document.exists(it)) throw ExistingFieldException(it.raw, "Target field") }
    }

    override fun describe() = "split(\"$source\").into(${targets.joinToString { "\"$it\"" }})"
}
