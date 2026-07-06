package com.mosedotten.json.migrator.engine.operation

import tools.jackson.databind.JsonNode

fun interface ValueSplitterStrategy {
    fun split(value: JsonNode): List<String>

    companion object {
        val SpaceSeparated = ValueSplitterStrategy { value -> value.asString().split(" ") }
    }
}
