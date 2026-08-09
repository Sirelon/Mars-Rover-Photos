package com.sirelon.marsroverphotos.presentation.ui

import com.sirelon.marsroverphotos.domain.releasenotes.ChangeType

/**
 * UI representation of a release-note [ChangeType].
 *
 * Lives in the UI layer, not on the domain enum: `domain/` must stay free of Compose-facing types
 * (see docs/ARCHITECTURE.md › Domain never imports presentation).
 */
fun ChangeType.toIcon(): MaterialSymbol = when (this) {
    ChangeType.MISSION_INFO -> MaterialSymbol.Rocket
    ChangeType.MULTIPLATFORM -> MaterialSymbol.Devices
    ChangeType.OFFLINE_CACHE -> MaterialSymbol.CloudOff
    ChangeType.FAVORITES -> MaterialSymbol.Favorite
    ChangeType.POPULAR -> MaterialSymbol.LocalFireDepartment
    ChangeType.REDESIGN -> MaterialSymbol.Star
}
