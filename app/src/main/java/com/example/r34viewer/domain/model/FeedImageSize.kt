package com.example.r34viewer.domain.model

/**
 * Feed image size preference. The number of grid columns is derived from it:
 * large = 1 column, medium = 2 columns, small = 3 columns.
 */
enum class FeedImageSize(val columns: Int, val label: String) {
    LARGE(1, "Large"),
    MEDIUM(2, "Medium"),
    SMALL(3, "Small");

    companion object {
        val DEFAULT: FeedImageSize = MEDIUM

        fun fromStoredValue(value: String?): FeedImageSize =
            entries.firstOrNull { it.name == value?.uppercase() } ?: DEFAULT
    }
}
