package com.example.data.repository

import com.example.data.local.BookmarkDao
import com.example.data.local.BookmarkedGig
import com.example.data.local.toBookmark
import com.example.data.local.toGig
import com.example.model.CraigslistHtmlParser
import com.example.model.CraigslistGig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.IOException

class GigRepository(
    private val bookmarkDao: BookmarkDao,
    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .readTimeout(15, java.util.concurrent.TimeUnit.SECONDS)
        .build()
) {
    // Standard User-Agent to avoid Craigslist scraping blocks (403 errors)
    private val userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"

    /**
     * Fetches web design gigs from Craigslist standard HTML search with optional distance/area queries
     * @param cityPrefix Craigslist city domain, e.g., "sfbay", "newyork", "fresno"
     * @param category Search category, e.g., "wdg" (Web Design Gigs), "web" (Web/Tech Jobs)
     */
    suspend fun fetchGigs(
        cityPrefix: String,
        category: String,
        query: String? = null,
        bundleDuplicates: Boolean = false,
        postal: String? = null,
        searchDistance: Int? = null,
        sort: String? = null
    ): List<CraigslistGig> = withContext(Dispatchers.IO) {
        val cleanPrefix = cityPrefix.trim().lowercase()
        
        val builder = java.lang.StringBuilder("https://$cleanPrefix.craigslist.org/search/$category")
        val params = mutableListOf<String>()
        
        if (!query.isNullOrBlank()) {
            try {
                params.add("query=${java.net.URLEncoder.encode(query, "UTF-8")}")
            } catch (e: Exception) {
                params.add("query=$query")
            }
        }
        if (bundleDuplicates) {
            params.add("bundleDuplicates=1")
        }
        if (!postal.isNullOrBlank()) {
            params.add("postal=$postal")
        }
        if (searchDistance != null) {
            params.add("search_distance=$searchDistance")
        }
        if (!sort.isNullOrBlank()) {
            params.add("sort=$sort")
        }
        
        val url = if (params.isNotEmpty()) {
            builder.append("?").append(params.joinToString("&")).toString()
        } else {
            builder.toString()
        }
        
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .build()
            
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    throw IOException("HTTP error response: ${response.code} for URL: $url")
                }
                val bodyString = response.body?.string() ?: ""
                if (bodyString.isEmpty()) {
                    throw IOException("Received empty response body from Craigslist")
                }
                
                CraigslistHtmlParser.parse(bodyString, cleanPrefix)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            throw e
        }
    }

    /**
     * Fetches the real description/posting body from a Craigslist detail page
     */
    suspend fun fetchDescription(link: String): String = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(link)
            .header("User-Agent", userAgent)
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,*/*;q=0.8")
            .header("Accept-Language", "en-US,en;q=0.5")
            .build()
        try {
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext ""
                val html = response.body?.string() ?: ""
                val postingBodyStart = html.indexOf("<section id=\"postingbody\">")
                if (postingBodyStart == -1) return@withContext ""
                
                val sub = html.substring(postingBodyStart)
                val endSection = sub.indexOf("</section>")
                if (endSection == -1) return@withContext ""
                
                var sectionHtml = sub.substring(0, endSection)
                
                // Strip QR code blocks
                sectionHtml = sectionHtml.replace(Regex("<div[^>]*?class=\"[^\"]*?print-qrcode-container[^\"]*?\"[^>]*?>.*?</div>", RegexOption.DOT_MATCHES_ALL), "")
                
                // Strip all other HTML tags
                sectionHtml = sectionHtml.replace(Regex("<[^>]*?>"), "")
                
                // Decode HTML entities
                val decoded = sectionHtml
                    .replace("&lt;", "<")
                    .replace("&gt;", ">")
                    .replace("&amp;", "&")
                    .replace("&quot;", "\"")
                    .replace("&apos;", "'")
                    .replace("&#39;", "'")
                    .replace("&#34;", "\"")
                    .replace("&#x9;", " ")
                    .replace("&#xA;", "\n")
                    .replace("&#xD;", "\r")
                
                var clean = decoded.trim()
                if (clean.startsWith("QR Code Link to This Post")) {
                    clean = clean.removePrefix("QR Code Link to This Post").trim()
                }
                clean
            }
        } catch (e: Exception) {
            e.printStackTrace()
            ""
        }
    }

    // Local Dao Bookmarks queries
    val allBookmarks: Flow<List<CraigslistGig>> = bookmarkDao.getAllBookmarks()
        .map { list -> list.map { it.toGig() } }

    suspend fun addBookmark(gig: CraigslistGig) = withContext(Dispatchers.IO) {
        bookmarkDao.insertBookmark(gig.toBookmark())
    }

    suspend fun removeBookmarkById(id: String) = withContext(Dispatchers.IO) {
        bookmarkDao.deleteBookmarkById(id)
    }

    fun observeIsBookmarked(id: String): Flow<Boolean> {
        return bookmarkDao.observeIsBookmarked(id)
    }
}
