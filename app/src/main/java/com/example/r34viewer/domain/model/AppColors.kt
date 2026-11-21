package com.example.r34viewer.domain.model

data class AppColors(
    val primary: String? = null,
    val secondary: String? = null,
    val background: String? = null
) {
    fun with(slot: CustomColorSlot, hex: String?): AppColors = when (slot) {
        CustomColorSlot.PRIMARY -> copy(primary = hex)
        CustomColorSlot.SECONDARY -> copy(secondary = hex)
        CustomColorSlot.BACKGROUND -> copy(background = hex)
    }

    companion object {
        fun normalizeHex(value: String?): String? {
            val raw = value?.trim()?.removePrefix("#") ?: return null
            if (raw.length != 6) return null
            if (raw.any { it !in '0'..'9' && it !in 'a'..'f' && it !in 'A'..'F' }) return null
            return "#${raw.uppercase()}"
        }
    }
}

enum class CustomColorSlot {
    PRIMARY,
    SECONDARY,
    BACKGROUND
}
