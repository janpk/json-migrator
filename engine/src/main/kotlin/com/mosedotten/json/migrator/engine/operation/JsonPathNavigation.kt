package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.InvalidFieldTypeException
import tools.jackson.databind.node.ObjectNode

internal fun JsonPath.parentObjectIn(root: ObjectNode) = findObjectIn(root, segments.dropLast(1))

internal fun JsonPath.parentObjectOrCreateIn(root: ObjectNode) = objectOrCreateIn(root, segments.dropLast(1))

internal fun ensureObjectPathIn(root: ObjectNode, path: JsonPath) {
    path.segments.fold(root) { current, segment -> current.objectChildOrCreate(path.raw, segment) }
}

private fun findObjectIn(root: ObjectNode, path: List<String>) =
    path.fold(root as ObjectNode?) { current, segment -> current?.get(segment) as? ObjectNode }

private fun objectOrCreateIn(root: ObjectNode, path: List<String>) =
    path.fold(root) { current, segment -> current.get(segment) as? ObjectNode ?: current.putObject(segment) }

private fun ObjectNode.objectChildOrCreate(rawPath: String, segment: String) =
    existingObject(segment) ?: createMissingObject(rawPath, segment)

private fun ObjectNode.existingObject(segment: String) = get(segment) as? ObjectNode

private fun ObjectNode.createMissingObject(rawPath: String, segment: String): ObjectNode {
    val child = get(segment)
    if (child != null) throw InvalidFieldTypeException(rawPath, "OBJECT", child.javaClass.simpleName)
    return putObject(segment)
}
