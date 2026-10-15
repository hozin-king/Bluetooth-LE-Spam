package de.simon.dankelmann.bluetoothlespam.Enums

import de.simon.dankelmann.bluetoothlespam.R

enum class AdvertisementTarget {
    ADVERTISEMENT_TARGET_KITCHEN_SINK,
    ADVERTISEMENT_TARGET_UNDEFINED,
    ADVERTISEMENT_TARGET_ANDROID,
    ADVERTISEMENT_TARGET_IOS,
    ADVERTISEMENT_TARGET_WINDOWS,
    ADVERTISEMENT_TARGET_SAMSUNG,
    ADVERTISEMENT_TARGET_XIAOMI,
    ADVERTISEMENT_TARGET_SONY,
    ADVERTISEMENT_TARGET_JBL,
    ADVERTISEMENT_TARGET_HUAWEI,
    ADVERTISEMENT_TARGET_LOVESPOUSE
}

fun AdvertisementTarget.getDrawableId(): Int {
    return when (this) {
        AdvertisementTarget.ADVERTISEMENT_TARGET_SAMSUNG -> R.drawable.samsung
        AdvertisementTarget.ADVERTISEMENT_TARGET_ANDROID -> R.drawable.ic_android
        AdvertisementTarget.ADVERTISEMENT_TARGET_IOS -> R.drawable.apple
        AdvertisementTarget.ADVERTISEMENT_TARGET_UNDEFINED -> R.drawable.bluetooth
        AdvertisementTarget.ADVERTISEMENT_TARGET_WINDOWS -> R.drawable.microsoft
        AdvertisementTarget.ADVERTISEMENT_TARGET_KITCHEN_SINK -> R.drawable.shuffle
        AdvertisementTarget.ADVERTISEMENT_TARGET_LOVESPOUSE -> R.drawable.heart
        // All four vendor Fast Pair targets reuse the generic Android icon: these devices'
        // pairing popups only ever appear on Android handsets (Google Fast Pair is an
        // Android-side feature), and there are no vendor-specific drawables for them.
        AdvertisementTarget.ADVERTISEMENT_TARGET_XIAOMI -> R.drawable.ic_android
        AdvertisementTarget.ADVERTISEMENT_TARGET_SONY -> R.drawable.ic_android
        AdvertisementTarget.ADVERTISEMENT_TARGET_JBL -> R.drawable.ic_android
        AdvertisementTarget.ADVERTISEMENT_TARGET_HUAWEI -> R.drawable.ic_android
    }
}
