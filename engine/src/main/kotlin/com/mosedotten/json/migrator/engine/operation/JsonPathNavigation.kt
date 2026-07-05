package com.mosedotten.json.migrator.engine.operation

import tools.jackson.databind.node.ObjectNode

internal fun JsonPath.parentObjectIn(root: ObjectNode) = findObjectIn(root, segments.dropLast(1))

internal fun JsonPath.parentObjectOrCreateIn(root: ObjectNode) = objectIn(root, segments.dropLast(1))

private fun findObjectIn(root: ObjectNode, path: List<String>) =
    path.fold(root as ObjectNode?) { current, segment -> current?.get(segment) as? ObjectNode }

private fun objectIn(root: ObjectNode, path: List<String>) =
    path.fold(root) { current, segment -> current.get(segment) as? ObjectNode ?: current.putObject(segment) }
