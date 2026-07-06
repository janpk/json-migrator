package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.InvalidFieldTypeException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException
import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode

class Document(private val root: ObjectNode) {
    internal fun exists(path: JsonPath) = path.parentObjectIn(root)?.has(path.leaf) == true
    internal fun set(path: JsonPath, value: JsonNode) {
        path.parentObjectOrCreateIn(root).set(path.leaf, value)
    }
    internal fun get(path: JsonPath): JsonNode? = path.parentObjectIn(root)?.get(path.leaf)
    internal fun require(path: JsonPath, label: String = "Field") =
        get(path) ?: throw MissingFieldException(path.raw, label)
    internal fun remove(path: JsonPath) {
        path.parentObjectIn(root)?.remove(path.leaf)
    }
    internal fun children(path: JsonPath): List<Document> {
        val target = require(path)
        if (target !is ArrayNode) throw InvalidFieldTypeException(path.raw, "ARRAY")
        return target.mapIndexed { index, element -> childDocument(path, index, element) }
    }
    private fun childDocument(path: JsonPath, index: Int, element: JsonNode): Document {
        if (element !is ObjectNode) throw InvalidFieldTypeException("${path.raw}[$index]", "OBJECT")
        return Document(element)
    }
    internal fun ensureObject(path: JsonPath) = ensureObjectPathIn(root, path)
}
