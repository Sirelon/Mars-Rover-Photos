package com.sirelon.marsroverphotos.domain.support

/**
 * One purchasable "Support the developer" tier, as the store currently sells it.
 *
 * @property id the store product identifier (`tip_coffee`, `support_medium` …), what the store is asked to sell.
 * @property packageId the RevenueCat package the product sits in (`support_small` … `support_huge`);
 *   stable across stores and product renames, so the UI keys icons and wording on it.
 * @property kind whether the tier is a repeatable tip or a one-off ad-free unlock.
 * @property title the store's localized product title with the shared "Support: " / "Tip: " prefix stripped.
 * @property price the store's localized, formatted price ("$0.99").
 */
data class SupportTier(
    val id: String,
    val packageId: String,
    val kind: SupportTierKind,
    val title: String,
    val price: String,
)

enum class SupportTierKind {
    /**
     * A consumable: buyable any number of times, each one keeps ads off for [TIP_AD_FREE_PERIOD]
     * (stacking). Not attached to the `ad_free` entitlement, which would make it permanent; stores do
     * not restore consumables, so the window is also lost on reinstall or a new device.
     */
    TIP,

    /** A non-consumable: bought once per store account, grants the `ad_free` entitlement, restorable. */
    AD_FREE,
}
