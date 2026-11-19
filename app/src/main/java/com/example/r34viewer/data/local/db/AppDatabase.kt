package com.example.r34viewer.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import java.time.LocalDate

@Database(
    entities = [
        FavoritePostEntity::class,
        SearchHistoryEntity::class,
        FavoriteFolderEntity::class,
        FavoriteProfileEntity::class,
        TagTypeEntity::class,
        TagCatalogImportEntity::class
    ],
    version = 5,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun favoritesDao(): FavoritesDao
    abstract fun searchHistoryDao(): SearchHistoryDao
    abstract fun tagTypeDao(): TagTypeDao

    companion object {
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `favorite_folders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    "CREATE UNIQUE INDEX IF NOT EXISTS `index_favorite_folders_name` ON `favorite_folders` (`name`)"
                )
                db.execSQL(
                    "ALTER TABLE `favorite_posts` ADD COLUMN `folderId` INTEGER DEFAULT NULL"
                )
            }
        }

        val MIGRATION_2_3 = object : Migration(2, 3) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tag_types` (
                        `name` TEXT NOT NULL,
                        `type` INTEGER,
                        PRIMARY KEY(`name`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_3_4 = object : Migration(3, 4) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `tag_types` ADD COLUMN `apiCheckedAtEpochDay` INTEGER")
                db.execSQL(
                    "UPDATE `tag_types` SET `apiCheckedAtEpochDay` = ?",
                    arrayOf(LocalDate.now().toEpochDay())
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `tag_catalog_import` (
                        `id` INTEGER NOT NULL,
                        `importedAtEpochDay` INTEGER NOT NULL,
                        PRIMARY KEY(`id`)
                    )
                    """.trimIndent()
                )
            }
        }

        val MIGRATION_4_5 = object : Migration(4, 5) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE `favorite_posts` RENAME TO `favorite_posts_legacy`")
                db.execSQL(
                    """
                    CREATE TABLE `favorite_posts` (
                        `id` INTEGER NOT NULL,
                        `score` INTEGER NOT NULL,
                        `rating` TEXT NOT NULL,
                        `tags` TEXT NOT NULL,
                        `fileUrl` TEXT NOT NULL,
                        `sampleUrl` TEXT NOT NULL,
                        `previewUrl` TEXT NOT NULL,
                        `width` INTEGER NOT NULL,
                        `height` INTEGER NOT NULL,
                        `sampleWidth` INTEGER NOT NULL,
                        `sampleHeight` INTEGER NOT NULL,
                        `previewWidth` INTEGER NOT NULL,
                        `previewHeight` INTEGER NOT NULL,
                        `owner` TEXT NOT NULL,
                        `source` TEXT NOT NULL,
                        `hasNotes` INTEGER NOT NULL,
                        `createdAt` TEXT,
                        `savedAt` INTEGER NOT NULL,
                        `folderId` INTEGER,
                        `profileUserId` TEXT NOT NULL,
                        PRIMARY KEY(`id`, `profileUserId`)
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `favorite_posts` (
                        `id`, `score`, `rating`, `tags`, `fileUrl`, `sampleUrl`, `previewUrl`,
                        `width`, `height`, `sampleWidth`, `sampleHeight`, `previewWidth`,
                        `previewHeight`, `owner`, `source`, `hasNotes`, `createdAt`, `savedAt`,
                        `folderId`, `profileUserId`
                    )
                    SELECT `id`, `score`, `rating`, `tags`, `fileUrl`, `sampleUrl`, `previewUrl`,
                        `width`, `height`, `sampleWidth`, `sampleHeight`, `previewWidth`,
                        `previewHeight`, `owner`, `source`, `hasNotes`, `createdAt`, `savedAt`,
                        `folderId`, ''
                    FROM `favorite_posts_legacy`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `favorite_posts_legacy`")

                db.execSQL("DROP INDEX IF EXISTS `index_favorite_folders_name`")
                db.execSQL("ALTER TABLE `favorite_folders` RENAME TO `favorite_folders_legacy`")
                db.execSQL(
                    """
                    CREATE TABLE `favorite_folders` (
                        `id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
                        `name` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        `profileUserId` TEXT NOT NULL
                    )
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    INSERT INTO `favorite_folders` (`id`, `name`, `createdAt`, `profileUserId`)
                    SELECT `id`, `name`, `createdAt`, '' FROM `favorite_folders_legacy`
                    """.trimIndent()
                )
                db.execSQL("DROP TABLE `favorite_folders_legacy`")
                db.execSQL(
                    """
                    CREATE UNIQUE INDEX `index_favorite_folders_profileUserId_name`
                    ON `favorite_folders` (`profileUserId`, `name`)
                    """.trimIndent()
                )
                db.execSQL(
                    """
                    CREATE TABLE IF NOT EXISTS `favorite_profiles` (
                        `userId` TEXT NOT NULL,
                        `createdAt` INTEGER NOT NULL,
                        PRIMARY KEY(`userId`)
                    )
                    """.trimIndent()
                )
            }
        }
    }
}
