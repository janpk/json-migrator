package com.mosedotten.json.migrator.engine.operation

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.BooleanNode
import tools.jackson.databind.node.NullNode
import tools.jackson.databind.node.NumericNode
import tools.jackson.databind.node.ObjectNode
import tools.jackson.databind.node.StringNode

enum class JsonType(internal val matches: (JsonNode) -> Boolean) {
    STRING({ it is StringNode }),
    NUMBER({ it is NumericNode }),
    BOOLEAN({ it is BooleanNode }),
    OBJECT({ it is ObjectNode }),
    ARRAY({ it is ArrayNode }),
    NULL({ it is NullNode }),
}
