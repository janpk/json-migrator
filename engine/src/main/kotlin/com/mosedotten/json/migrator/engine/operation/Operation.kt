package com.mosedotten.json.migrator.engine.operation

interface Operation {
    fun apply(document: Document)
}
