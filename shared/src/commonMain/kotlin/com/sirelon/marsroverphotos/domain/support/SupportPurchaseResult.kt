package com.sirelon.marsroverphotos.domain.support

sealed interface SupportPurchaseResult {
    data object Success : SupportPurchaseResult

    /** The user backed out of the store sheet — not an error, nothing to show. */
    data object Cancelled : SupportPurchaseResult

    /**
     * @property message short, user-facing explanation (the store's developer-facing text never
     *   reaches the screen).
     * @property code the billing SDK's error code name, for analytics.
     */
    data class Failure(val message: String, val code: String) : SupportPurchaseResult
}
