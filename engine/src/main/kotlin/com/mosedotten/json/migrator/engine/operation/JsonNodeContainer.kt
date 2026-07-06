package com.mosedotten.json.migrator.engine.operation

import tools.jackson.databind.JsonNode
import tools.jackson.databind.node.ArrayNode
import tools.jackson.databind.node.ObjectNode

internal fun JsonNode.isEmptyContainer() = isEmptyObject() || isEmptyArray()

internal fun JsonNode.isEmptyObject() = this is ObjectNode && size() == 0

internal fun JsonNode.isEmptyArray() = this is ArrayNode && size() == 0
