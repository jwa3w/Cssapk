package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.repository.GigRepository
import com.example.model.CraigslistGig
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

sealed class GigsUiState {
    object Idle : GigsUiState()
    object Loading : GigsUiState()
    data class Success(val list: List<CraigslistGig>) : GigsUiState()
    data class Error(val message: String, val attemptedUrl: String) : GigsUiState()
}

data class UserProfile(
    val fullName: String = "John Designer",
    val portfolioUrl: String = "https://behance.net/johnportfolio",
    val skills: String = "UI/UX, Tailwind CSS, React, Framer Motion, Shopify, WordPress",
    val experience: String = "3+ years designing clean, fast-loading, responsive business websites"
)

class GigViewModel(private val repository: GigRepository) : ViewModel() {

    // Selected city indicator
    private val _selectedCity = MutableStateFlow("all")
    val selectedCity = _selectedCity.asStateFlow()

    // Selected category, defaulting to "wdg" (Web Design Gigs)
    private val _selectedCategory = MutableStateFlow("wdg") 
    val selectedCategory = _selectedCategory.asStateFlow()

    // Query for text searches in title / description
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()

    // Core list fetching states
    private val _uiState = MutableStateFlow<GigsUiState>(GigsUiState.Idle)
    val uiState = _uiState.asStateFlow()

    // Reactive Bookmarks from Local DB
    val bookmarks: StateFlow<List<CraigslistGig>> = repository.allBookmarks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    // User profile for pitch auto-generation
    private val _userProfile = MutableStateFlow(UserProfile())
    val userProfile = _userProfile.asStateFlow()

    init {
        // Trigger initial fetch
        fetchListings()
    }

    /**
     * Re-fetch Listings when city or category changes
     */
    fun selectCity(city: String) {
        _selectedCity.value = city
        fetchListings()
    }

    fun selectCategory(category: String) {
        _selectedCategory.value = category
        fetchListings()
    }

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun updateProfile(profile: UserProfile) {
        _userProfile.value = profile
    }

    private fun getPostalForCity(city: String): String {
        return when (city.lowercase()) {
            "sfbay" -> "94102"
            "newyork" -> "10001"
            "losangeles" -> "90001"
            "seattle" -> "98101"
            "chicago" -> "60601"
            "austin" -> "78701"
            "boston" -> "02108"
            "denver" -> "80202"
            "portland" -> "97201"
            "miami" -> "33101"
            "fresno" -> "93728"
            "sandiego" -> "92101"
            "sacramento" -> "95814"
            "phoenix" -> "85001"
            "dallas" -> "75201"
            "atlanta" -> "30301"
            else -> "94102"
        }
    }

    fun fetchListings() {
        val city = _selectedCity.value
        val category = _selectedCategory.value
        
        viewModelScope.launch {
            _uiState.value = GigsUiState.Loading
            try {
                if (city == "all") {
                    val searchWord = _searchQuery.value.takeIf { it.isNotBlank() }
                    val targetCities = listOf(
                        "sfbay", "newyork", "losangeles", "seattle", "chicago",
                        "austin", "boston", "denver", "portland", "miami",
                        "fresno", "sandiego", "sacramento", "phoenix", "dallas", "atlanta"
                    )
                    val deferredList = targetCities.map { singleCity ->
                        async {
                            try {
                                repository.fetchGigs(
                                    cityPrefix = singleCity,
                                    category = category,
                                    query = searchWord,
                                    postal = getPostalForCity(singleCity),
                                    searchDistance = 1000
                                )
                            } catch (ex: Exception) {
                                emptyList<CraigslistGig>()
                            }
                        }
                    }
                    val allGigs = deferredList.awaitAll().flatten().distinctBy { it.id }
                    _uiState.value = GigsUiState.Success(allGigs)
                } else {
                    val searchWord = _searchQuery.value.takeIf { it.isNotBlank() }
                    val list = repository.fetchGigs(
                        cityPrefix = city,
                        category = category,
                        query = searchWord,
                        postal = getPostalForCity(city),
                        searchDistance = 1000
                    )
                    _uiState.value = GigsUiState.Success(list)
                }
            } catch (e: Exception) {
                val url = "https://$city.craigslist.org/search/$category"
                _uiState.value = GigsUiState.Error(
                    message = e.localizedMessage ?: "Failed to fetch Craigslist feed. Please check network connection.",
                    attemptedUrl = url
                )
            }
        }
    }

    // Toggle bookmark addition or subtraction
    fun toggleBookmark(gig: CraigslistGig, isBookmarked: Boolean) {
        viewModelScope.launch {
            if (isBookmarked) {
                repository.removeBookmarkById(gig.id)
            } else {
                repository.addBookmark(gig)
            }
        }
    }

    // Map of gig ID -> fetched description
    private val _fetchedDescriptions = MutableStateFlow<Map<String, String>>(emptyMap())
    val fetchedDescriptions = _fetchedDescriptions.asStateFlow()

    private val _isFetchingDescription = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val isFetchingDescription = _isFetchingDescription.asStateFlow()

    fun fetchFullDescriptionIfNeeded(gig: CraigslistGig) {
        val current = _fetchedDescriptions.value[gig.id]
        if (!current.isNullOrBlank()) return // Already fetched

        _isFetchingDescription.value = _isFetchingDescription.value + (gig.id to true)
        viewModelScope.launch {
            try {
                val realDesc = repository.fetchDescription(gig.link)
                if (realDesc.isNotBlank()) {
                    _fetchedDescriptions.value = _fetchedDescriptions.value + (gig.id to realDesc)
                }
            } catch (e: Exception) {
                // Ignore
            } finally {
                _isFetchingDescription.value = _isFetchingDescription.value + (gig.id to false)
            }
        }
    }

    /**
     * Reactive checking if a particular listing is bookmarked
     */
    fun observeIsBookmarked(id: String): Flow<Boolean> {
        return repository.observeIsBookmarked(id)
    }

    /**
     * Synthesizes a beautiful customized pitch letter draft
     */
    fun generateProposalText(gig: CraigslistGig): String {
        val profile = _userProfile.value
        val titleClean = gig.displayTitle
        val isShopify = gig.title.lowercase().contains("shopify") || gig.description.lowercase().contains("shopify")
        val isWordpress = gig.title.lowercase().contains("wordpress") || gig.description.lowercase().contains("wordpress") || gig.title.lowercase().contains("wp")
        
        val specificAngle = when {
            isShopify -> "I noticed you're looking for Shopify development. I specialize in designing premium, conversion-optimized Shopify storefronts with tailored layouts and fast, mobile-friendly landing pages."
            isWordpress -> "I noticed you're seeking WordPress assistance. I have extensive experience building robust, custom WordPress templates, implementing Elementor/Gutenberg layouts, and optimizing site speeds."
            else -> "I saw your posting for '${titleClean}' and would love to assist you. My focus is on crafting clean, responsive, and performance-oriented layouts that represent brands flawlessly."
        }

        return """
Hello,

I saw your posting on Craigslist for "${gig.title}" and would love to discuss how I can help bring your vision to life.

$specificAngle

My Background:
- ${profile.experience}
- Core Skills: ${profile.skills}

You can view my recent web designs and client success stories here:
${profile.portfolioUrl}

I am highly responsive, pay close attention to design details, and can deliver this project on-time and within your budget. Let me know if you would like to jump on a quick call to map out the scope!

Best regards,
${profile.fullName}
        """.trimIndent()
    }
}
