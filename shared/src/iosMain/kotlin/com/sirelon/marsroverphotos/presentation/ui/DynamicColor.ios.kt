package com.sirelon.marsroverphotos.presentation.ui

actual fun supportsDynamicColor(): Boolean {
    return false  // iOS doesn't support Material 3 dynamic color
}
