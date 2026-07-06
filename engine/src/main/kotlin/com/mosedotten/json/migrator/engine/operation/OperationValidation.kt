package com.mosedotten.json.migrator.engine.operation

import com.mosedotten.json.migrator.engine.exception.InvalidOperationException

internal fun requireAtLeast(count: Int, min: Int, operation: String, subject: String) {
    if (count < min) {
        throw InvalidOperationException("$operation requires at least $min $subject, but got $count")
    }
}
