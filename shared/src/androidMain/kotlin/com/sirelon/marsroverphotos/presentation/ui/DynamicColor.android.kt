package com.sirelon.marsroverphotos.presentation.ui

import android.os.Build

actual fun supportsDynamicColor(): Boolean {
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
}
