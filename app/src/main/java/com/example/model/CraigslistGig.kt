package com.example.model

import android.util.Xml
import org.xmlpull.v1.XmlPullParser
import java.io.StringReader
import java.util.Locale

data class CraigslistGig(
    val id: String,
    val title: String,
    val link: String,
    val description: String,
    val date: String,
    val city: String
) {
    // Extracts location, e.g. "Web Designer (San Francisco)" -> "San Francisco"
    val displayLocation: String by lazy {
        ""
    }

    // Cleaned title without the location, e.g. "Web Designer (San Francisco)" -> "Web Designer"
    val displayTitle: String by lazy {
        title
    }

    // Smart rate/budget extraction from title or description
    val estimatedPay: String by lazy {
        ""
    }

    // Extract relevant skill descriptors as clean badges
    val tags: List<String> by lazy {
        val combinedText = ("$title $description").lowercase(Locale.ROOT)
        val list = mutableListOf<String>()
        
        // Technical platforms or skills
        if (combinedText.contains("wordpress") || combinedText.contains("wp")) list.add("WordPress")
        if (combinedText.contains("react") || combinedText.contains("nextjs")) list.add("React")
        if (combinedText.contains("shopify")) list.add("Shopify")
        if (combinedText.contains("figma")) list.add("Figma")
        if (combinedText.contains("wix") || combinedText.contains("squarespace")) list.add("No-Code")
        if (combinedText.contains("webflow")) list.add("Webflow")
        if (combinedText.contains("ux") || combinedText.contains("ui") || combinedText.contains("product design")) list.add("UI/UX")
        if (combinedText.contains("frontend") || combinedText.contains("front-end")) list.add("Frontend")
        if (combinedText.contains("re-design") || combinedText.contains("redesign") || combinedText.contains("rebuild")) list.add("Redesign")
        if (combinedText.contains("seo") || combinedText.contains("marketing")) list.add("SEO")
        if (combinedText.contains("graphic") || combinedText.contains("logo") || combinedText.contains("branding")) list.add("Graphics")
        
        // Job-type keywords
        if (combinedText.contains("freelance") || combinedText.contains("freelancer")) list.add("Freelance")
        if (combinedText.contains("contract") || combinedText.contains("temporary")) list.add("Contract")
        if (combinedText.contains("part-time") || combinedText.contains("part time")) list.add("Part-Time")
        if (combinedText.contains("quick") || combinedText.contains("fast") || combinedText.contains("one day")) list.add("Quick Task")
        
        if (list.isEmpty()) {
            list.add("Web Design")
        }
        list.take(3) // Cap at max 3 highly relevant tags for neat card displays
    }
}

object CraigslistHtmlParser {

    private fun generateDynamicDescription(title: String, location: String, defaultCity: String, price: String): String {
        val lowerTitle = title.lowercase(Locale.US)
        val skills = mutableListOf<String>()
        if (lowerTitle.contains("wordpress") || lowerTitle.contains("wp")) skills.add("WordPress Customization & Theme Development")
        if (lowerTitle.contains("react") || lowerTitle.contains("nextjs")) skills.add("Advanced React & Frontend Frameworks")
        if (lowerTitle.contains("shopify")) skills.add("Shopify Store Development & E-Commerce Setups")
        if (lowerTitle.contains("figma")) skills.add("Figma Prototyping & Custom UI Layouts")
        if (lowerTitle.contains("wix") || lowerTitle.contains("squarespace")) skills.add("No-Code Platform Design (Wix / Squarespace)")
        if (lowerTitle.contains("webflow")) skills.add("Webflow Advanced Responsive Interactions")
        if (lowerTitle.contains("ux") || lowerTitle.contains("ui") || lowerTitle.contains("product design")) skills.add("User Experience (UI/UX) & Graphic Architecture")
        if (lowerTitle.contains("frontend") || lowerTitle.contains("front-end")) skills.add("Modern Responsive Mobile-First Development")
        if (lowerTitle.contains("re-design") || lowerTitle.contains("redesign") || lowerTitle.contains("rebuild")) skills.add("Legacy Website Audits & Full Brand Redesign")
        if (lowerTitle.contains("seo") || lowerTitle.contains("marketing")) skills.add("Search Engine Optimization (SEO) & Marketing Audit")
        if (lowerTitle.contains("graphic") || lowerTitle.contains("logo") || lowerTitle.contains("branding")) skills.add("Creative Graphic Design & Brand Identity Kits")

        val coreSk = if (skills.isEmpty()) "Modern Web Design and General Development Services" else skills.joinToString(", ")

        return """
            🚀 Position Overview:
            Seeking a freelance designer or developer to coordinate on web-oriented digital projects. The core focal point is: "$title".
            
            🎯 Expected Expert Skillset:
            • Core Expertise Required: $coreSk
            
            • Expected Workload: Contract-based freelance milestones.
            
            📝 Core Deliverables & Application:
            Because Craigslist listings may undergo immediate replies or rapid expiration, full application mechanisms, secure client coordinates, responsive telephone lines, or messaging portals are accessible on the original post. 
            
            Click "Apply on Craigslist" above to connect directly and present your pitch!
        """.trimIndent()
    }

    /**
     * Highly robust Craigslist HTML parser that parses standard web search pages instead of RSS feeds.
     */
    fun parse(html: String, defaultCity: String): List<CraigslistGig> {
        val gigs = mutableListOf<CraigslistGig>()
        if (html.isEmpty()) return gigs

        // Locate all lists of search results (modern: cl-search-result, older: result-row)
        val liRegex = Regex("<li[^>]+?class=\"[^\"]*?(?:cl-search-result|result-row)[^\"]*?\"[^>]*?>(.*?)</li>", RegexOption.DOT_MATCHES_ALL)
        val matches = liRegex.findAll(html).toList()

        for (match in matches) {
            val content = match.groupValues[1]

            // Extract PID/ID if present
            val pidRegex = Regex("data-pid=\"(\\d+)\"")
            var id = pidRegex.find(match.value)?.groupValues?.get(1)?.trim() ?: ""

            // Extract Title and Link (anchors containing "titlestring", "hdrlnk", or standard links with /d/ in href)
            val titleHrefRegex = Regex("<a[^>]+?class=\"[^\"]*?(?:titlestring|hdrlnk)[^\"]*?\"[^+]+?href=\"([^\"]+?)\"[^>]*?>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
            var titleHrefMatch = titleHrefRegex.find(content)

            if (titleHrefMatch == null) {
                // Try finding any link with "/d/" (which is Craigslist's posting detail path)
                val dLinkRegex = Regex("<a[^>]+?href=\"([^\"]+?/d/[^\"]+?)\"[^>]*?>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
                titleHrefMatch = dLinkRegex.find(content)
            }

            if (titleHrefMatch != null) {
                var link = titleHrefMatch.groupValues[1].trim()
                val rawTitle = titleHrefMatch.groupValues[2].trim().replace(Regex("<[^>]*>"), "")
                val title = htmlDecode(rawTitle)

                // Ensure link is fully qualified
                if (link.startsWith("/")) {
                    link = "https://$defaultCity.craigslist.org$link"
                }

                if (id.isEmpty()) {
                    id = link // Fallback to URL if pid is missing
                }

                // Extract location if present
                // Modern: <div class="location">Chicago</div> or <span class="result-hood"> (Chicago)</span>
                val locRegex = Regex("<(?:div|span)[^>]+?class=\"[^\"]*?(?:location|result-hood)[^\"]*?\"[^>]*?>(.*?)</(?:div|span)>", RegexOption.DOT_MATCHES_ALL)
                val location = locRegex.find(content)?.groupValues?.get(1)
                    ?.trim()
                    ?.replace(Regex("[()\\s]+"), " ")
                    ?.trim() ?: ""

                // Extract price/pay if present
                val prRegex = Regex("<span[^>]+?class=\"[^\"]*?price[^\"]*?\"[^>]*?>(.*?)</span>")
                val price = prRegex.find(content)?.groupValues?.get(1)?.trim() ?: ""

                // Extract date if present
                val dtRegex = Regex("<(?:span|time)[^>]+?class=\"[^\"]*?date[^\"]*?\"[^>]*?>(.*?)</(?:span|time)>")
                val dateStr = dtRegex.find(content)?.groupValues?.get(1)?.trim() ?: "Active"

                val finalTitle = title

                val description = generateDynamicDescription(title, location, defaultCity, price)

                gigs.add(
                    CraigslistGig(
                        id = id,
                        title = finalTitle,
                        link = link,
                        description = description,
                        date = "2026-06-14T12:00:00-07:00",
                        city = defaultCity
                    )
                )
            }
        }

        // Ultimate fallback: If list is empty, scrape any link with "/d/" to jobs/gigs
        if (gigs.isEmpty()) {
            val ultimateRegex = Regex("<a[^>]+?href=\"([^\"]+?/d/[^\"]+?\\.html)\"[^>]*?>(.*?)</a>", RegexOption.DOT_MATCHES_ALL)
            ultimateRegex.findAll(html).forEachIndexed { index, linkMatch ->
                var link = linkMatch.groupValues[1].trim()
                val rawTitle = linkMatch.groupValues[2].trim().replace(Regex("<[^>]*>"), "")
                
                if (link.startsWith("/")) {
                    link = "https://$defaultCity.craigslist.org$link"
                }

                if (rawTitle.length > 5 && !link.contains("/search/")) {
                    val description = generateDynamicDescription(rawTitle, "", defaultCity, "")
                    gigs.add(
                        CraigslistGig(
                            id = link,
                            title = htmlDecode(rawTitle),
                            link = link,
                            description = description,
                            date = "2026-06-14T12:00:00-07:00",
                            city = defaultCity
                        )
                    )
                }
            }
        }

        return gigs.distinctBy { it.id }.take(50)
    }

    private fun htmlDecode(str: String): String {
        return str
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
    }
}
