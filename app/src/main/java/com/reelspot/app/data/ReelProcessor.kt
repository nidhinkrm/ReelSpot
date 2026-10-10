package com.reelspot.app.data

/**
 * Service placeholder for Instagram Reel processing.
 * Currently validates Reel URLs and extracts basic metadata structure.
 * Designed for Version 2 extension (AI location extraction & metadata scraping).
 */
object ReelProcessor {

    fun isValidInstagramUrl(url: String): Boolean {
        val trimmed = url.trim()
        return trimmed.startsWith("https://www.instagram.com/reel/") ||
                trimmed.startsWith("https://instagram.com/reel/") ||
                trimmed.startsWith("https://www.instagram.com/p/") ||
                trimmed.startsWith("https://instagram.com/p/")
    }

    fun cleanUrl(url: String): String {
        val trimmed = url.trim()
        val queryIndex = trimmed.indexOf("?")
        return if (queryIndex != -1) trimmed.substring(0, queryIndex) else trimmed
    }
}
