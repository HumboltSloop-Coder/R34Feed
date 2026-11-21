package com.example.r34viewer.data.repository

import android.content.Context
import androidx.room.withTransaction
import com.example.r34viewer.data.local.db.AppDatabase
import com.example.r34viewer.data.local.db.TagCatalogImportEntity
import com.example.r34viewer.data.local.db.TagTypeDao
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.IOException
import java.time.LocalDate
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.coroutineContext

@Singleton
class TagCatalogImporter @Inject constructor(
    @ApplicationContext private val context: Context,
    private val database: AppDatabase,
    private val tagTypeDao: TagTypeDao
) {
    private val importMutex = Mutex()

    suspend fun importIfNeeded(): Boolean = importMutex.withLock {
        if (tagTypeDao.isBundledCatalogImported()) return false

        val importedCount = withContext(Dispatchers.IO) {
            importBundledRows()
        }
        if (importedCount == 0L) {
            throw IOException("The bundled Rule34 tag catalog is empty")
        }

        tagTypeDao.markBundledCatalogImported(
            TagCatalogImportEntity(importedAtEpochDay = LocalDate.now().toEpochDay())
        )
        true
    }

    private suspend fun importBundledRows(): Long {
        var importedCount = 0L
        val statementSql =
            "INSERT OR REPLACE INTO `tag_types` (`name`, `type`, `apiCheckedAtEpochDay`) VALUES (?, ?, NULL)"

        context.assets.open(CATALOG_ASSET).bufferedReader(Charsets.UTF_8).use { reader ->
            while (true) {
                coroutineContext.ensureActive()
                val batch = readBatch(reader)
                if (batch.isEmpty()) break

                database.withTransaction {
                    database.openHelper.writableDatabase
                        .compileStatement(statementSql)
                        .use { statement ->
                            batch.forEach { (name, type) ->
                                statement.clearBindings()
                                statement.bindString(1, name)
                                statement.bindLong(2, type.toLong())
                                statement.executeInsert()
                            }
                        }
                }
                importedCount += batch.size
            }
        }

        return importedCount
    }

    private fun readBatch(reader: BufferedReader): List<Pair<String, Int>> =
        buildList(BATCH_SIZE) {
            while (size < BATCH_SIZE) {
                val line = reader.readLine() ?: break
                val separator = line.lastIndexOf('\t')
                if (separator <= 0 || separator == line.lastIndex) {
                    throw IOException("Invalid entry in the bundled Rule34 tag catalog")
                }

                val name = line.substring(0, separator)
                if (name.isBlank()) {
                    throw IOException("Empty tag name in the bundled Rule34 tag catalog")
                }
                val type = line.substring(separator + 1).toIntOrNull()
                    ?: throw IOException("Invalid tag type in the bundled Rule34 tag catalog")
                if (type !in VALID_TAG_TYPES) {
                    throw IOException("Unsupported tag type in the bundled Rule34 tag catalog")
                }
                add(name to type)
            }
        }

    private companion object {
        const val CATALOG_ASSET = "rule34_tags.tsv"
        const val BATCH_SIZE = 5_000
        val VALID_TAG_TYPES = setOf(0, 1, 3, 4, 5)
    }
}
