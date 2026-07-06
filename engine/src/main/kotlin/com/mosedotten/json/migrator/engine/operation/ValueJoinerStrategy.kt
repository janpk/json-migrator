package com.mosedotten.json.migrator.engine.operation

import tools.jackson.databind.JsonNode

fun interface ValueJoinerStrategy {
    fun join(values: List<JsonNode>): String

    companion object {
        val SpaceSeparated = ValueJoinerStrategy { values -> values.joinToString(" ") { it.asString() } }
    }
}
