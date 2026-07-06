package com.mosedotten.json.migrator.engine.operation

abstract class CompositeOperation : Operation {
    protected abstract fun steps(document: Document): List<Operation>

    final override fun apply(document: Document) = steps(document).forEach { it.apply(document) }
}
