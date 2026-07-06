package com.mosedotten.json.migrator.engine.dsl

import tools.jackson.databind.node.ObjectNode

fun interface ExecutionStrategy {
    fun execute(rootNode: ObjectNode, block: () -> ObjectNode): ObjectNode

    companion object {
        @Suppress("TooGenericExceptionCaught") // rollback boundary: restore then rethrow every failure
        val Atomic = ExecutionStrategy { rootNode, block ->
            val snapshot = rootNode.deepCopy()
            try {
                block()
            } catch (failure: Throwable) {
                rootNode.removeAll()
                rootNode.setAll(snapshot)
                throw failure
            }
        }

        val NonAtomic = ExecutionStrategy { _, block -> block() }
    }
}
