package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.ExistingFieldException
import com.mosedotten.json.migrator.engine.exception.MissingFieldException

class Copy(private val from: String, private val to: String) : Operation {
    private val source = JsonPath.parse(from)
    private val target = JsonPath.parse(to)

    override fun apply(document: Document) {
        validateSource(document)
        validateTarget(document)
        document.set(target, document.require(source, "Source field"))
    }

    private fun validateSource(document: Document) {
        if (!document.exists(source)) throw MissingFieldException(from, "Source field")
    }

    private fun validateTarget(document: Document) {
        if (document.exists(target)) throw ExistingFieldException(to, "Target field")
    }

    override fun describe() = "copy(\"$from\") to \"$to\""
}
