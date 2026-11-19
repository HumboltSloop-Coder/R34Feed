package com.example.r34viewer.data.local.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface TagTypeDao {
    @Query("SELECT * FROM tag_types WHERE name IN (:names)")
    suspend fun getTagTypes(names: List<String>): List<TagTypeEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tagTypes: List<TagTypeEntity>)

    @Query("SELECT EXISTS(SELECT 1 FROM tag_catalog_import WHERE id = 1)")
    suspend fun isBundledCatalogImported(): Boolean

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markBundledCatalogImported(state: TagCatalogImportEntity)
}
