package com.mosedotten.json.migrator.engine.operation

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ObjectNode

class Document(private val root: ObjectNode) {
    internal fun exists(path: JsonPath) = path.parentObjectIn(root)?.has(path.leaf) == true
    internal fun set(path: JsonPath, value: JsonNode) {
        path.parentObjectOrCreateIn(root).set(path.leaf, value)
    }
}
