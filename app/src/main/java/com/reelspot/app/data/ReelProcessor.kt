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

    /**
     * Extracts and cleans a URL from potentially mixed shared text.
     *
     * Instagram's "Share" action sometimes sends text like:
     *   "Check out this place!\nhttps://www.instagram.com/reel/XYZ/?igsh=abc"
     *
     * This function:
     * 1. Scans for the first HTTP/HTTPS URL in the shared text.
     * 2. Strips tracking query parameters (e.g. ?igsh=...) from the URL.
     * 3. Falls back to the original trimmed text if no URL is found.
     */
    fun cleanUrl(sharedText: String): String {
        val trimmed = sharedText.trim()

        // Try to find the first http/https URL in the text
        val urlRegex = Regex("""https?://[^\s]+""")
        val match = urlRegex.find(trimmed)
        val urlCandidate = match?.value ?: trimmed

        // Strip query string and fragment
        val withoutQuery = urlCandidate.substringBefore("?").substringBefore("#")

        return withoutQuery.trimEnd('/')
    }
}
