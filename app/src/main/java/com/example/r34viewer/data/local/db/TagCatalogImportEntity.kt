package com.example.r34viewer.data.local.db

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tag_catalog_import")
data class TagCatalogImportEntity(
    @PrimaryKey val id: Int = SINGLETON_ID,
    val importedAtEpochDay: Long
) {
    companion object {
        const val SINGLETON_ID = 1
    }
}
