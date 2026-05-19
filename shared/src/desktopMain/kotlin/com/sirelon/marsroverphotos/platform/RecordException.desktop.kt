package com.sirelon.marsroverphotos.platform

import co.touchlab.kermit.Logger

actual fun recordException(t: Throwable) {
    Logger.e("RecordException", t) { "Exception recorded: ${t.message}" }
}
