package com.reelspot.app.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupManagerTest {

    @Test
    fun parseAndValidateJson_validPayload_succeeds() {
        val json = """
            {
              "backupFormatVersion": 1,
              "appVersion": "1.0",
              "exportedAt": 1728730000000,
              "places": [
                {
                  "id": 10,
                  "name": "Eiffel Tower",
                  "description": "Iconic landmark",
                  "category": "Attraction",
                  "latitude": 48.8584,
                  "longitude": 2.2945,
                  "radius": 500.0,
                  "reelUrl": "https://www.instagram.com/reel/C8XYZ123",
                  "createdAt": 1728700000000,
                  "tags": "Paris,Sightseeing",
                  "folderId": 1,
                  "folderName": "France 2026"
                }
              ],
              "folders": [
                {
                  "id": 1,
                  "name": "France 2026",
                  "createdAt": 1728700000000
                }
              ]
            }
        """.trimIndent()

        val result = BackupManager.parseAndValidateJson(json)
        assertTrue("Parsing valid JSON should succeed", result.isSuccess)

        val parsed = result.getOrNull()
        assertNotNull(parsed)
        assertEquals(1, parsed!!.formatVersion)
        assertEquals("1.0", parsed.appVersion)
        assertEquals(1, parsed.places.size)
        assertEquals("Eiffel Tower", parsed.places[0].name)
        assertEquals(48.8584, parsed.places[0].latitude, 0.0001)
        assertEquals(2.2945, parsed.places[0].longitude, 0.0001)
        assertEquals("Paris,Sightseeing", parsed.places[0].tags)
        assertEquals(1, parsed.folders.size)
        assertEquals("France 2026", parsed.folders[0].name)
    }

    @Test
    fun parseAndValidateJson_olderFormatWithoutFolders_succeedsGracefully() {
        // Backwards compatibility test for older backups or minimal export
        val json = """
            {
              "backupFormatVersion": 1,
              "places": [
                {
                  "name": "Central Park",
                  "latitude": 40.785091,
                  "longitude": -73.968285
                }
              ]
            }
        """.trimIndent()

        val result = BackupManager.parseAndValidateJson(json)
        assertTrue("Older format with minimal fields should parse", result.isSuccess)

        val parsed = result.getOrNull()!!
        assertEquals(1, parsed.places.size)
        assertEquals("Central Park", parsed.places[0].name)
        assertEquals("General", parsed.places[0].category)
        assertEquals("", parsed.places[0].description)
        assertNull(parsed.places[0].reelUrl)
        assertEquals(0, parsed.folders.size)
    }

    @Test
    fun parseAndValidateJson_invalidJsonSyntax_failsGracefully() {
        val corruptedJson = "{ not valid json: 123 "
        val result = BackupManager.parseAndValidateJson(corruptedJson)
        assertTrue("Malformed JSON syntax must fail", result.isFailure)
    }

    @Test
    fun parseAndValidateJson_missingPlacesArray_failsGracefully() {
        val invalidPayload = """
            {
              "backupFormatVersion": 1,
              "appVersion": "1.0"
            }
        """.trimIndent()

        val result = BackupManager.parseAndValidateJson(invalidPayload)
        assertTrue("Missing 'places' key must fail validation", result.isFailure)
    }

    @Test
    fun parseAndValidateJson_invalidCoordinates_failsGracefully() {
        val invalidLat = """
            {
              "places": [
                {
                  "name": "Invalid Place",
                  "latitude": 150.0,
                  "longitude": 20.0
                }
              ]
            }
        """.trimIndent()

        val result = BackupManager.parseAndValidateJson(invalidLat)
        assertTrue("Latitude > 90 must fail validation", result.isFailure)
    }

    @Test
    fun parseAndValidateJson_blankName_failsGracefully() {
        val blankName = """
            {
              "places": [
                {
                  "name": "   ",
                  "latitude": 10.0,
                  "longitude": 20.0
                }
              ]
            }
        """.trimIndent()

        val result = BackupManager.parseAndValidateJson(blankName)
        assertTrue("Blank place name must fail validation", result.isFailure)
    }
}
