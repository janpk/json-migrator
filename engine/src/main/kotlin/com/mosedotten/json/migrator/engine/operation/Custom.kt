package com.mosedotten.json.migrator.engine.operation

import tools.jackson.databind.node.ObjectNode

class Custom(private val block: (ObjectNode) -> Unit) : Operation {
    override fun apply(document: Document) = document.mutate(block)

    override fun describe() = "custom"
}
