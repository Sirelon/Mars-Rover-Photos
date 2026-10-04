package com.sirelon.marsroverphotos.domain.support

/**
 * One purchasable "Support the developer" tier, as the store currently sells it.
 *
 * @property id the store product identifier (`support_small` … `support_huge`), stable across stores.
 * @property title the store's localized product title with the shared "Support: " prefix stripped.
 * @property price the store's localized, formatted price ("$0.99").
 */
data class SupportTier(
    val id: String,
    val title: String,
    val price: String,
)
