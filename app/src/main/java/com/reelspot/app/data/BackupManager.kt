package com.reelspot.app.data

import android.content.Context
import android.net.Uri
import androidx.room.withTransaction
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class ImportMode {
    MERGE,
    REPLACE
}

data class ExportResult(
    val placesCount: Int,
    val foldersCount: Int
)

data class ImportResult(
    val placesImported: Int,
    val foldersImported: Int,
    val mode: ImportMode
)

data class ParsedBackup(
    val formatVersion: Int,
    val appVersion: String,
    val places: List<SavedPlaceBackupItem>,
    val folders: List<PlaceFolderBackupItem>
)

data class SavedPlaceBackupItem(
    val originalId: Long,
    val name: String,
    val description: String,
    val category: String,
    val latitude: Double,
    val longitude: Double,
    val radius: Float,
    val reelUrl: String?,
    val createdAt: Long,
    val tags: String,
    val folderId: Long?,
    val folderName: String?
)

data class PlaceFolderBackupItem(
    val originalId: Long,
    val name: String,
    val createdAt: Long
)

class BackupManager(
    private val database: AppDatabase,
    private val placeDao: SavedPlaceDao,
    private val folderDao: PlaceFolderDao
) {

    /**
     * Exports all user-created places and folders to the destination URI chosen by the user.
     * Guaranteed NOT to export API keys, auth credentials, or internal secrets.
     */
    suspend fun exportBackup(context: Context, destinationUri: Uri): Result<ExportResult> {
        return try {
            val places = placeDao.getAllPlacesList()
            val folders = folderDao.getAllFoldersList()
            val folderMap = folders.associateBy { it.id }

            val appVersion = try {
                val pInfo = context.packageManager.getPackageInfo(context.packageName, 0)
                pInfo.versionName ?: "1.0"
            } catch (e: Exception) {
                "1.0"
            }

            val rootJson = JSONObject().apply {
                put("backupFormatVersion", CURRENT_BACKUP_FORMAT_VERSION)
                put("appVersion", appVersion)
                put("exportedAt", System.currentTimeMillis())
                put(
                    "exportedAtFormatted",
                    SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
                )
                put("placesCount", places.size)
                put("foldersCount", folders.size)

                // Serialize Folders
                val foldersArray = JSONArray()
                for (folder in folders) {
                    val folderObj = JSONObject().apply {
                        put("id", folder.id)
                        put("name", folder.name)
                        put("createdAt", folder.createdAt)
                    }
                    foldersArray.put(folderObj)
                }
                put("folders", foldersArray)

                // Serialize Places
                val placesArray = JSONArray()
                for (place in places) {
                    val placeObj = JSONObject().apply {
                        put("id", place.id)
                        put("name", place.name)
                        put("description", place.description)
                        put("category", place.category)
                        put("latitude", place.latitude)
                        put("longitude", place.longitude)
                        put("radius", place.radius.toDouble())
                        put("reelUrl", place.reelUrl ?: JSONObject.NULL)
                        put("createdAt", place.createdAt)
                        put("tags", place.tags)
                        put("folderId", place.folderId ?: JSONObject.NULL)
                        put("folderName", place.folderId?.let { folderMap[it]?.name } ?: JSONObject.NULL)
                    }
                    placesArray.put(placeObj)
                }
                put("places", placesArray)
            }

            val jsonString = rootJson.toString(2)
            context.contentResolver.openOutputStream(destinationUri)?.use { outputStream ->
                outputStream.write(jsonString.toByteArray(Charsets.UTF_8))
                outputStream.flush()
            } ?: return Result.failure(IllegalStateException("Unable to open output stream for destination file."))

            Result.success(ExportResult(placesCount = places.size, foldersCount = folders.size))
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    /**
     * Validates and imports a ReelSpot backup file using atomic database transactions.
     * In case of any error or invalid data, the transaction rolls back cleanly.
     */
    suspend fun importBackup(context: Context, sourceUri: Uri, mode: ImportMode): Result<ImportResult> {
        val jsonString = try {
            context.contentResolver.openInputStream(sourceUri)?.use { inputStream ->
                BufferedReader(InputStreamReader(inputStream, Charsets.UTF_8)).use { reader ->
                    reader.readText()
                }
            } ?: return Result.failure(IllegalStateException("Unable to open file for reading."))
        } catch (e: Exception) {
            return Result.failure(IllegalArgumentException("Failed to read backup file: ${e.message}"))
        }

        val parsed = parseAndValidateJson(jsonString).getOrElse { error ->
            return Result.failure(error)
        }

        return try {
            database.withTransaction {
                if (mode == ImportMode.REPLACE) {
                    // Safe replace: delete all existing records inside transaction
                    placeDao.deleteAllPlaces()
                    folderDao.deleteAllFolders()

                    // Re-insert folders and record newly assigned IDs
                    val folderIdMap = mutableMapOf<Long, Long>()
                    for (folderItem in parsed.folders) {
                        val newFolderId = folderDao.insertFolder(
                            PlaceFolder(
                                id = 0L,
                                name = folderItem.name,
                                createdAt = folderItem.createdAt
                            )
                        )
                        folderIdMap[folderItem.originalId] = newFolderId
                    }

                    // Insert places with remapped folder IDs
                    for (placeItem in parsed.places) {
                        val resolvedFolderId = placeItem.folderId?.let { folderIdMap[it] }
                        placeDao.insertPlace(
                            SavedPlace(
                                id = 0L,
                                name = placeItem.name,
                                description = placeItem.description,
                                category = placeItem.category,
                                latitude = placeItem.latitude,
                                longitude = placeItem.longitude,
                                radius = placeItem.radius,
                                reelUrl = placeItem.reelUrl,
                                createdAt = placeItem.createdAt,
                                tags = placeItem.tags,
                                folderId = resolvedFolderId
                            )
                        )
                    }
                } else {
                    // MERGE mode: preserves existing data, safely deduplicates
                    val folderIdMap = mutableMapOf<Long, Long>()

                    for (folderItem in parsed.folders) {
                        val existing = folderDao.getFolderByName(folderItem.name)
                        val folderId = existing?.id ?: folderDao.insertFolder(
                            PlaceFolder(
                                id = 0L,
                                name = folderItem.name,
                                createdAt = folderItem.createdAt
                            )
                        )
                        folderIdMap[folderItem.originalId] = folderId
                    }

                    for (placeItem in parsed.places) {
                        // Check if folder name can match an existing folder if original ID not found
                        val resolvedFolderId = placeItem.folderId?.let { folderIdMap[it] }
                            ?: placeItem.folderName?.let { fName ->
                                folderDao.getFolderByName(fName)?.id
                            }

                        // Check duplicate place by Reel URL
                        val existingPlace = if (!placeItem.reelUrl.isNullOrBlank()) {
                            placeDao.findPlaceByReelUrl(
                                placeItem.reelUrl,
                                ReelProcessor.cleanUrl(placeItem.reelUrl)
                            )
                        } else null

                        if (existingPlace != null) {
                            // Update existing place with merged notes/tags
                            val mergedTags = if (placeItem.tags.isNotBlank()) {
                                (existingPlace.tagList + placeItem.tags.split(",").map { it.trim() })
                                    .filter { it.isNotEmpty() }.distinct().joinToString(",")
                            } else existingPlace.tags

                            val mergedDesc = if (existingPlace.description.isBlank()) {
                                placeItem.description
                            } else existingPlace.description

                            placeDao.updatePlace(
                                existingPlace.copy(
                                    description = mergedDesc,
                                    tags = mergedTags,
                                    folderId = resolvedFolderId ?: existingPlace.folderId
                                )
                            )
                        } else {
                            // Insert as a new place
                            placeDao.insertPlace(
                                SavedPlace(
                                    id = 0L,
                                    name = placeItem.name,
                                    description = placeItem.description,
                                    category = placeItem.category,
                                    latitude = placeItem.latitude,
                                    longitude = placeItem.longitude,
                                    radius = placeItem.radius,
                                    reelUrl = placeItem.reelUrl,
                                    createdAt = placeItem.createdAt,
                                    tags = placeItem.tags,
                                    folderId = resolvedFolderId
                                )
                            )
                        }
                    }
                }
            }

            Result.success(
                ImportResult(
                    placesImported = parsed.places.size,
                    foldersImported = parsed.folders.size,
                    mode = mode
                )
            )
        } catch (e: Exception) {
            e.printStackTrace()
            Result.failure(e)
        }
    }

    companion object {
        const val CURRENT_BACKUP_FORMAT_VERSION = 1

        fun generateSuggestedFileName(): String {
            val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
            return "ReelSpot_Backup_$dateStr.json"
        }

        /**
         * Parses and strictly validates backup JSON.
         * Detects corrupted, malformed, or invalid files gracefully without touching database.
         */
        fun parseAndValidateJson(jsonString: String): Result<ParsedBackup> {
            if (jsonString.isBlank()) {
                return Result.failure(IllegalArgumentException("Backup file is empty."))
            }

            val rootJson = try {
                JSONObject(jsonString)
            } catch (e: JSONException) {
                return Result.failure(IllegalArgumentException("Invalid JSON format: ${e.message}"))
            }

            val formatVersion = rootJson.optInt("backupFormatVersion", 1)
            val appVersion = rootJson.optString("appVersion", "Unknown")

            if (!rootJson.has("places")) {
                return Result.failure(IllegalArgumentException("Invalid ReelSpot backup: Missing 'places' list."))
            }

            val placesArray = try {
                rootJson.getJSONArray("places")
            } catch (e: Exception) {
                return Result.failure(IllegalArgumentException("Invalid 'places' format in backup file."))
            }

            val parsedPlaces = mutableListOf<SavedPlaceBackupItem>()
            for (i in 0 until placesArray.length()) {
                val placeObj = placesArray.optJSONObject(i)
                    ?: return Result.failure(IllegalArgumentException("Invalid place entry at index $i."))

                val name = placeObj.optString("name", "").trim()
                if (name.isEmpty()) {
                    return Result.failure(IllegalArgumentException("Place at index $i is missing a valid name."))
                }

                if (!placeObj.has("latitude") || !placeObj.has("longitude")) {
                    return Result.failure(IllegalArgumentException("Place \"$name\" is missing coordinates."))
                }

                val lat = placeObj.optDouble("latitude", Double.NaN)
                val lng = placeObj.optDouble("longitude", Double.NaN)

                if (lat.isNaN() || lat < -90.0 || lat > 90.0) {
                    return Result.failure(IllegalArgumentException("Place \"$name\" has an invalid latitude ($lat)."))
                }
                if (lng.isNaN() || lng < -180.0 || lng > 180.0) {
                    return Result.failure(IllegalArgumentException("Place \"$name\" has an invalid longitude ($lng)."))
                }

                val desc = placeObj.optString("description", "")
                val category = placeObj.optString("category", "General").ifBlank { "General" }
                val radius = placeObj.optDouble("radius", 500.0).toFloat().coerceIn(100f, 10000f)
                val reelUrlRaw = if (placeObj.isNull("reelUrl")) null else placeObj.optString("reelUrl", "").ifBlank { null }
                val createdAt = placeObj.optLong("createdAt", System.currentTimeMillis()).let {
                    if (it <= 0L) System.currentTimeMillis() else it
                }
                val tags = placeObj.optString("tags", "")
                val folderId = if (placeObj.isNull("folderId")) null else placeObj.optLong("folderId")
                val folderName = if (placeObj.isNull("folderName")) null else placeObj.optString("folderName", "").ifBlank { null }

                parsedPlaces.add(
                    SavedPlaceBackupItem(
                        originalId = placeObj.optLong("id", 0L),
                        name = name,
                        description = desc,
                        category = category,
                        latitude = lat,
                        longitude = lng,
                        radius = radius,
                        reelUrl = reelUrlRaw,
                        createdAt = createdAt,
                        tags = tags,
                        folderId = folderId,
                        folderName = folderName
                    )
                )
            }

            val parsedFolders = mutableListOf<PlaceFolderBackupItem>()
            val foldersArray = rootJson.optJSONArray("folders")
            if (foldersArray != null) {
                for (i in 0 until foldersArray.length()) {
                    val folderObj = foldersArray.optJSONObject(i) ?: continue
                    val fName = folderObj.optString("name", "").trim()
                    if (fName.isNotEmpty()) {
                        parsedFolders.add(
                            PlaceFolderBackupItem(
                                originalId = folderObj.optLong("id", 0L),
                                name = fName,
                                createdAt = folderObj.optLong("createdAt", System.currentTimeMillis())
                            )
                        )
                    }
                }
            }

            return Result.success(
                ParsedBackup(
                    formatVersion = formatVersion,
                    appVersion = appVersion,
                    places = parsedPlaces,
                    folders = parsedFolders
                )
            )
        }
    }
}
