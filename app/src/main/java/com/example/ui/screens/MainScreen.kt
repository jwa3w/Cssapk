package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.model.CraigslistGig
import com.example.ui.viewmodel.GigViewModel
import com.example.ui.viewmodel.GigsUiState
import com.example.ui.viewmodel.UserProfile

// Simple navigation states
sealed class ActiveTab {
    object Feed : ActiveTab()
    object Bookmarks : ActiveTab()
    object Profile : ActiveTab()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(viewModel: GigViewModel) {
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val selectedCategory by viewModel.selectedCategory.collectAsStateWithLifecycle()
    val searchQuery by viewModel.searchQuery.collectAsStateWithLifecycle()
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val bookmarks by viewModel.bookmarks.collectAsStateWithLifecycle()
    
    // UI Local state tracking
    var currentTab by remember { mutableStateOf<ActiveTab>(ActiveTab.Feed) }
    var selectedGigForDetail by remember { mutableStateOf<CraigslistGig?>(null) }
    
    // Screen classification: Responsive layout check (width class)
    val context = LocalContext.current
    val configuration = androidx.compose.ui.platform.LocalConfiguration.current
    val isTablet = configuration.screenWidthDp >= 640

    // Warm Retro Craigslist Newspaper Palette Colors
    val isDark = isSystemInDarkTheme()
    val PaperBackground = if (isDark) Color(0xFF14171A) else Color(0xFFFAF7F0)
    val OnPaperText = if (isDark) Color(0xFFECE5D8) else Color(0xFF2C1E18)
    val InkPrimary = if (isDark) Color(0xFFB19DDF) else Color(0xFF4F359B) // Premium Slate Purple
    val InkAccent = if (isDark) Color(0xFF7CB8A0) else Color(0xFF1C6348) // Retro ink-green
    val PaperCard = if (isDark) Color(0xFF1F2228) else Color(0xFFFFFFFF)
    val HighContrastBorder = if (isDark) Color(0xFF333A42) else Color(0xFFE2DCCE)

    val customColors = if (isDark) {
        darkColorScheme(
            primary = InkPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFF2E243A),
            onPrimaryContainer = InkPrimary,
            secondary = InkAccent,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFF192A23),
            onSecondaryContainer = InkAccent,
            tertiary = InkAccent,
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFF2F291B),
            onTertiaryContainer = Color(0xFFE4A100),
            background = PaperBackground,
            onBackground = OnPaperText,
            surface = PaperCard,
            onSurface = OnPaperText,
            surfaceVariant = Color(0xFF262A30),
            onSurfaceVariant = Color(0xFFC7BEB2),
            surfaceTint = InkPrimary,
            error = Color(0xFFD32F2F),
            onError = Color.White,
            errorContainer = Color(0xFFFFCDD2),
            onErrorContainer = Color(0xFFB71C1C),
            outline = HighContrastBorder,
            outlineVariant = HighContrastBorder,
            scrim = Color.Black
        )
    } else {
        lightColorScheme(
            primary = InkPrimary,
            onPrimary = Color.White,
            primaryContainer = Color(0xFFEDE6FA),
            onPrimaryContainer = InkPrimary,
            secondary = InkAccent,
            onSecondary = Color.White,
            secondaryContainer = Color(0xFFE2F0EA),
            onSecondaryContainer = InkAccent,
            tertiary = InkAccent,
            onTertiary = Color.White,
            tertiaryContainer = Color(0xFFFFF9E6),
            onTertiaryContainer = Color(0xFFE4A100),
            background = PaperBackground,
            onBackground = OnPaperText,
            surface = PaperCard,
            onSurface = OnPaperText,
            surfaceVariant = Color(0xFFF2ECE0),
            onSurfaceVariant = Color(0xFF5E544A),
            surfaceTint = InkPrimary,
            error = Color(0xFFD32F2F),
            onError = Color.White,
            errorContainer = Color(0xFFFFCDD2),
            onErrorContainer = Color(0xFFB71C1C),
            outline = HighContrastBorder,
            outlineVariant = HighContrastBorder,
            scrim = Color.Black
        )
    }

    MaterialTheme(colorScheme = customColors) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = MaterialTheme.colorScheme.background
        ) {
            // Check if tablet-based list-detail is appropriate
            if (isTablet) {
                // Tablet Landscape: Left menu/list and right detail view pane
                Row(modifier = Modifier.fillMaxSize()) {
                    Column(
                        modifier = Modifier
                            .weight(1.2f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        TabbedHeader(
                            currentTab = currentTab,
                            onTabSelected = { 
                                currentTab = it
                                // Reset detail if switching tabs
                                if (it != ActiveTab.Feed) {
                                    selectedGigForDetail = null
                                }
                            }
                        )
                        
                        when (currentTab) {
                            ActiveTab.Feed -> {
                                FeedMainContent(
                                    viewModel = viewModel,
                                    uiState = uiState,
                                    searchQuery = searchQuery,
                                    selectedCity = selectedCity,
                                    selectedCategory = selectedCategory,
                                    bookmarks = bookmarks,
                                    selectedGigForDetail = selectedGigForDetail,
                                    onGigClick = { selectedGigForDetail = it }
                                )
                            }
                            ActiveTab.Bookmarks -> {
                                BookmarksMainContent(
                                    bookmarks = bookmarks,
                                    selectedGigForDetail = selectedGigForDetail,
                                    onGigClick = { selectedGigForDetail = it }
                                )
                            }
                            ActiveTab.Profile -> {
                                ProfileMainContent(viewModel = viewModel)
                            }
                        }
                    }

                    Box(
                        modifier = Modifier
                            .weight(1.8f)
                            .fillMaxHeight()
                            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f))
                    ) {
                        val currentDetail = selectedGigForDetail
                        if (currentDetail != null) {
                            key(currentDetail.id) {
                                val isBookmarkedFlow = viewModel.observeIsBookmarked(currentDetail.id).collectAsState(initial = false)
                                DetailPane(
                                    gig = currentDetail,
                                    isBookmarked = isBookmarkedFlow.value,
                                    viewModel = viewModel,
                                    onCloseClicked = { selectedGigForDetail = null }
                                )
                            }
                        } else {
                            Box(
                                modifier = Modifier.fillMaxSize(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f),
                                        modifier = Modifier.size(64.dp)
                                    )
                                    Spacer(modifier = Modifier.height(16.dp))
                                    Text(
                                        text = "Select a listing to view descriptions & draft proposal pitch letters.",
                                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        fontSize = 15.sp,
                                        modifier = Modifier.padding(horizontal = 32.dp),
                                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                // Mobile: Normal Stack transitions
                Scaffold(
                    bottomBar = {
                        NavigationBar(
                            containerColor = MaterialTheme.colorScheme.surface,
                            tonalElevation = 4.dp
                        ) {
                            NavigationBarItem(
                                selected = currentTab == ActiveTab.Feed,
                                onClick = { 
                                    currentTab = ActiveTab.Feed
                                    selectedGigForDetail = null
                                },
                                label = { Text("Search Gigs") },
                                icon = { Icon(Icons.Default.Search, contentDescription = "Browse") },
                                modifier = Modifier.testTag("nav_tab_feed")
                            )
                            NavigationBarItem(
                                selected = currentTab == ActiveTab.Bookmarks,
                                onClick = { 
                                    currentTab = ActiveTab.Bookmarks
                                    selectedGigForDetail = null
                                },
                                label = { Text("Bookmarks") },
                                icon = { Icon(Icons.Default.Favorite, contentDescription = "Bookmarks") },
                                modifier = Modifier.testTag("nav_tab_bookmarks")
                            )
                            NavigationBarItem(
                                selected = currentTab == ActiveTab.Profile,
                                onClick = { 
                                    currentTab = ActiveTab.Profile
                                    selectedGigForDetail = null
                                },
                                label = { Text("My Pitch Profile") },
                                icon = { Icon(Icons.Default.AccountBox, contentDescription = "Profile") },
                                modifier = Modifier.testTag("nav_tab_profile")
                            )
                        }
                    }
                ) { innerPadding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        AnimatedContent(
                            targetState = Pair(currentTab, selectedGigForDetail),
                            transitionSpec = {
                                slideInHorizontally { width -> width } + fadeIn() togetherWith
                                        slideOutHorizontally { width -> -width } + fadeOut()
                            },
                            label = "screen_transitions"
                        ) { (tab, detailGig) ->
                            if (detailGig != null) {
                                key(detailGig.id) {
                                    val isBookmarkedFlow = viewModel.observeIsBookmarked(detailGig.id).collectAsState(initial = false)
                                    DetailPane(
                                        gig = detailGig,
                                        isBookmarked = isBookmarkedFlow.value,
                                        viewModel = viewModel,
                                        onCloseClicked = { selectedGigForDetail = null }
                                    )
                                }
                            } else {
                                when (tab) {
                                    ActiveTab.Feed -> {
                                        FeedMainContent(
                                            viewModel = viewModel,
                                            uiState = uiState,
                                            searchQuery = searchQuery,
                                            selectedCity = selectedCity,
                                            selectedCategory = selectedCategory,
                                            bookmarks = bookmarks,
                                            selectedGigForDetail = null,
                                            onGigClick = { selectedGigForDetail = it }
                                        )
                                    }
                                    ActiveTab.Bookmarks -> {
                                        BookmarksMainContent(
                                            bookmarks = bookmarks,
                                            selectedGigForDetail = null,
                                            onGigClick = { selectedGigForDetail = it }
                                        )
                                    }
                                    ActiveTab.Profile -> {
                                        ProfileMainContent(viewModel = viewModel)
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabbedHeader(
    currentTab: ActiveTab,
    onTabSelected: (ActiveTab) -> Unit
) {
    TabRow(
        selectedTabIndex = when (currentTab) {
            ActiveTab.Feed -> 0
            ActiveTab.Bookmarks -> 1
            ActiveTab.Profile -> 2
        },
        containerColor = MaterialTheme.colorScheme.surface,
        contentColor = MaterialTheme.colorScheme.primary
    ) {
        Tab(
            selected = currentTab == ActiveTab.Feed,
            onClick = { onTabSelected(ActiveTab.Feed) },
            text = { Text("Search Gigs", fontWeight = FontWeight.Bold) },
            icon = { Icon(Icons.Default.Search, contentDescription = null) }
        )
        Tab(
            selected = currentTab == ActiveTab.Bookmarks,
            onClick = { onTabSelected(ActiveTab.Bookmarks) },
            text = { Text("Bookmarks", fontWeight = FontWeight.Bold) },
            icon = { Icon(Icons.Default.Favorite, contentDescription = null) }
        )
        Tab(
            selected = currentTab == ActiveTab.Profile,
            onClick = { onTabSelected(ActiveTab.Profile) },
            text = { Text("Pitch Profile", fontWeight = FontWeight.Bold) },
            icon = { Icon(Icons.Default.AccountBox, contentDescription = null) }
        )
    }
}

@Composable
fun FeedMainContent(
    viewModel: GigViewModel,
    uiState: GigsUiState,
    searchQuery: String,
    selectedCity: String,
    selectedCategory: String,
    bookmarks: List<CraigslistGig>,
    selectedGigForDetail: CraigslistGig?,
    onGigClick: (CraigslistGig) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    Column(modifier = Modifier.fillMaxSize()) {
        // Search & Category Segment
        SearchAndDomainBar(
            searchQuery = searchQuery,
            onQueryChanged = { viewModel.updateSearchQuery(it) },
            selectedCategory = selectedCategory,
            onCategorySelected = { viewModel.selectCategory(it) }
        )

        // Locations Carousel Row
        LocationsHorizontalSelector(
            selectedCity = selectedCity,
            searchQuery = searchQuery,
            selectedCategory = selectedCategory,
            onCitySelected = { viewModel.selectCity(it) }
        )

        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))

        // Listing Feed lists
        Box(modifier = Modifier.weight(1f)) {
            when (uiState) {
                is GigsUiState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                is GigsUiState.Error -> {
                    FeedErrorPane(
                        message = uiState.message,
                        attemptedUrl = uiState.attemptedUrl,
                        searchQuery = searchQuery,
                        selectedCategory = selectedCategory,
                        onRetryClick = { viewModel.fetchListings() }
                    )
                }
                is GigsUiState.Success -> {
                    // Filter list dynamically based on search bar
                    val filteredGigs = remember(uiState.list, searchQuery) {
                        if (searchQuery.trim().isEmpty()) {
                            uiState.list
                        } else {
                            val lower = searchQuery.trim().lowercase()
                            uiState.list.filter { gig ->
                                gig.title.lowercase().contains(lower) ||
                                gig.description.lowercase().contains(lower) ||
                                gig.displayLocation.lowercase().contains(lower) ||
                                gig.tags.any { it.lowercase().contains(lower) }
                            }
                        }
                    }

                    if (filteredGigs.isEmpty()) {
                        EmptyListCard(
                            title = "No Gigs Found",
                            subtitle = "Try searching for a different keyword, category, or clear city filters.",
                            onSearchTempestClick = {
                                val q = if (searchQuery.trim().isEmpty()) {
                                    if (selectedCategory == "wdg") "web design" else "web developer"
                                } else {
                                    searchQuery.trim()
                                }
                                val subcat = if (selectedCategory == "wdg") "wdg" else "web"
                                val url = "https://www.searchtempest.com/search?search_string=${Uri.encode(q)}&category=8&subcat=$subcat"
                                try {
                                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                                } catch (e: Exception) {
                                    clipboardManager.setText(AnnotatedString(url))
                                    Toast.makeText(context, "SearchTempest URL copied", Toast.LENGTH_SHORT).show()
                                }
                            }
                        )
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .testTag("gigs_list"),
                            contentPadding = PaddingValues(12.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            items(filteredGigs, key = { it.id }) { gig ->
                                val isBookmarked = bookmarks.any { b -> b.id == gig.id }
                                GigListingItemCard(
                                    gig = gig,
                                    isBookmarked = isBookmarked,
                                    isSelected = selectedGigForDetail?.id == gig.id,
                                    onBookmarkToggle = { viewModel.toggleBookmark(gig, isBookmarked) },
                                    onItemClick = { onGigClick(gig) }
                                )
                            }
                        }
                    }
                }
                GigsUiState.Idle -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text("Search for design gigs to begin.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }
}

@Composable
fun BookmarksMainContent(
    bookmarks: List<CraigslistGig>,
    selectedGigForDetail: CraigslistGig?,
    onGigClick: (CraigslistGig) -> Unit
) {
    if (bookmarks.isEmpty()) {
        EmptyListCard(
            title = "Bookmark Gigs",
            subtitle = "Tap the stars on listings to save them. View and draft custom gig proposals offline!"
        )
    } else {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.5f))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Favorite,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Saved Bookmarks (${bookmarks.size})",
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                        fontSize = 14.sp
                    )
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("bookmarks_list"),
                contentPadding = PaddingValues(12.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(bookmarks, key = { it.id }) { gig ->
                    GigListingItemCard(
                        gig = gig,
                        isBookmarked = true,
                        isSelected = selectedGigForDetail?.id == gig.id,
                        onBookmarkToggle = { onGigClick(gig) /* trigger and edit */ },
                        onItemClick = { onGigClick(gig) }
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileMainContent(viewModel: GigViewModel) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    var name by remember { mutableStateOf(userProfile.fullName) }
    var portfolio by remember { mutableStateOf(userProfile.portfolioUrl) }
    var skills by remember { mutableStateOf(userProfile.skills) }
    var experience by remember { mutableStateOf(userProfile.experience) }
    val keyboardController = LocalSoftwareKeyboardController.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
            .testTag("profile_section"),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "💼 Smart Cover Letter Settings",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Configure your professional coordinates to automatically generated customized job pitch templates below.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Your Full Name") },
            leadingIcon = { Icon(Icons.Default.Person, null) },
            modifier = Modifier.fillMaxWidth().testTag("profile_name_input"),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        OutlinedTextField(
            value = portfolio,
            onValueChange = { portfolio = it },
            label = { Text("Portfolio Website URL") },
            leadingIcon = { Icon(Icons.Default.Send, null) },
            placeholder = { Text("https://behance.net/username") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            modifier = Modifier.fillMaxWidth().testTag("profile_portfolio_input"),
            singleLine = true,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        OutlinedTextField(
            value = skills,
            onValueChange = { skills = it },
            label = { Text("Key Technical Talents (Separated by commas)") },
            leadingIcon = { Icon(Icons.Default.Star, null) },
            placeholder = { Text("UI/UX Design, Figma, React, WordPress") },
            modifier = Modifier.fillMaxWidth().testTag("profile_skills_input"),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        OutlinedTextField(
            value = experience,
            onValueChange = { experience = it },
            label = { Text("Professional Experience Paragraph") },
            leadingIcon = { Icon(Icons.Default.Info, null) },
            placeholder = { Text("3+ years creating premium business websites") },
            modifier = Modifier.fillMaxWidth().testTag("profile_exp_input"),
            maxLines = 3,
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent
            )
        )

        Button(
            onClick = {
                viewModel.updateProfile(
                    UserProfile(
                        fullName = name,
                        portfolioUrl = portfolio,
                        skills = skills,
                        experience = experience
                    )
                )
                keyboardController?.hide()
            },
            modifier = Modifier
                .align(Alignment.End)
                .testTag("profile_save_btn")
        ) {
            Icon(Icons.Default.Check, contentDescription = null)
            Spacer(modifier = Modifier.width(8.dp))
            Text("Update Profile Info")
        }
        
        Spacer(modifier = Modifier.height(20.dp))
    }
}

@Composable
fun SearchAndDomainBar(
    searchQuery: String,
    onQueryChanged: (String) -> Unit,
    selectedCategory: String,
    onCategorySelected: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Search Input text field
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onQueryChanged,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("feed_search_input"),
                placeholder = { Text("Search title or keywords (e.g. Figma, WordPress)") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onQueryChanged("") }) {
                            Icon(Icons.Default.Clear, contentDescription = "Clear")
                        }
                    }
                },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Sub category domain selection tabs
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val searchGigs = selectedCategory == "wdg"
                FilterChip(
                    selected = searchGigs,
                    onClick = { onCategorySelected("wdg") },
                    label = { Text("💻 Web Design Gigs") },
                    modifier = Modifier.weight(1f).testTag("select_category_wdg")
                )
                FilterChip(
                    selected = !searchGigs,
                    onClick = { onCategorySelected("web") },
                    label = { Text("👔 Web Master Jobs") },
                    modifier = Modifier.weight(1f).testTag("select_category_web")
                )
            }
        }
    }
}

@Composable
fun LocationsHorizontalSelector(
    selectedCity: String,
    searchQuery: String,
    selectedCategory: String,
    onCitySelected: (String) -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current

    val cities = listOf(
        Pair("all", "🌍 All Areas")
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 12.dp, vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        cities.forEach { (cityCode, cityName) ->
            val isSelected = selectedCity == cityCode
            InputChip(
                selected = isSelected,
                onClick = { onCitySelected(cityCode) },
                label = { Text(cityName, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium) },
                colors = InputChipDefaults.inputChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                    selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                ),
                modifier = Modifier.testTag("city_chip_$cityCode")
            )
        }

        // SearchTempest External Multi-Area search chip
        InputChip(
            selected = false,
            onClick = {
                val q = if (searchQuery.trim().isEmpty()) {
                    if (selectedCategory == "wdg") "web design" else "web developer"
                } else {
                    searchQuery.trim()
                }
                val subcat = if (selectedCategory == "wdg") "wdg" else "web"
                val url = "https://www.searchtempest.com/search?search_string=${Uri.encode(q)}&category=8&subcat=$subcat"
                try {
                    context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    clipboardManager.setText(AnnotatedString(url))
                    Toast.makeText(context, "SearchTempest URL copied to clipboard", Toast.LENGTH_SHORT).show()
                }
            },
            label = { 
                Text("🌪️ SearchTempest (All Areas)", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.secondary) 
            },
            colors = InputChipDefaults.inputChipColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.4f),
                labelColor = MaterialTheme.colorScheme.onSecondaryContainer
            ),
            modifier = Modifier.testTag("city_chip_searchtempest")
        )
    }
}

@Composable
fun GigListingItemCard(
    gig: CraigslistGig,
    isBookmarked: Boolean,
    isSelected: Boolean,
    onBookmarkToggle: () -> Unit,
    onItemClick: () -> Unit
) {
    val dateText = remember(gig.date) {
        try {
            // E.g. "2026-06-14T10:00:00-07:00" -> extract month/day
            val parts = gig.date.split("T")[0].split("-")
            if (parts.size >= 3) {
                val monthNames = listOf("Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec")
                val monthIdx = parts[1].toIntOrNull()?.minus(1) ?: 0
                val day = parts[2].trim().toIntOrNull() ?: ""
                "${monthNames.getOrElse(monthIdx) { "" }} $day"
            } else {
                gig.date
            }
        } catch (e: Exception) {
            "Active"
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onItemClick)
            .testTag("gig_card_${gig.id}"),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f) else MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 4.dp else 1.dp),
        border = BorderStroke(
            1.dp, 
            if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Card top info header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.secondaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = dateText,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                    
                    if (gig.displayLocation.isNotEmpty()) {
                        Text(
                            text = "📍 ${gig.displayLocation}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                IconButton(
                    onClick = onBookmarkToggle,
                    modifier = Modifier.size(24.dp).testTag("bookmark_toggle_${gig.id}")
                ) {
                    Icon(
                        imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Save Bookmarks",
                        tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Main Listing Title
            Text(
                text = gig.displayTitle,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Bottom pills, dynamic pricing, and descriptors
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    gig.tags.forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Pricing highlight badge if detected
                if (gig.estimatedPay.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(MaterialTheme.colorScheme.tertiaryContainer)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = gig.estimatedPay,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onTertiaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun DetailPane(
    gig: CraigslistGig,
    isBookmarked: Boolean,
    viewModel: GigViewModel,
    onCloseClicked: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val customProposal = remember(gig, viewModel.userProfile.collectAsStateWithLifecycle().value) {
        viewModel.generateProposalText(gig)
    }

    val fetchedDescs by viewModel.fetchedDescriptions.collectAsStateWithLifecycle()
    val isFetchingMap by viewModel.isFetchingDescription.collectAsStateWithLifecycle()
    
    val realDesc = fetchedDescs[gig.id]
    val isFetching = isFetchingMap[gig.id] ?: false

    LaunchedEffect(gig.id) {
        viewModel.fetchFullDescriptionIfNeeded(gig)
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
            .verticalScroll(rememberScrollState())
            .testTag("detail_pane")
    ) {
        // Detailed Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f))
                .padding(16.dp)
        ) {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    IconButton(
                        onClick = onCloseClicked,
                        modifier = Modifier.testTag("detail_back_btn")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Go Back",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }

                    IconButton(
                        onClick = { viewModel.toggleBookmark(gig, isBookmarked) },
                        modifier = Modifier.testTag("detail_bookmark_toggle")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Save Bookmarks",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))
                
                Text(
                    text = gig.displayTitle,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (gig.displayLocation.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "📍 location: ${gig.displayLocation}",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    val browserIntent = remember(gig.link) {
                        Intent(Intent.ACTION_VIEW, Uri.parse(gig.link))
                    }
                    Button(
                        onClick = {
                            try {
                                context.startActivity(browserIntent)
                            } catch (e: Exception) {
                                Toast.makeText(context, "Could not open browser. Link copied instead.", Toast.LENGTH_SHORT).show()
                                clipboardManager.setText(AnnotatedString(gig.link))
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("open_craigslist_btn"),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) {
                        Icon(Icons.Default.ExitToApp, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Apply on Craigslist")
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Body Description
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            Text(
                text = "📰 Position Detail Specification",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(10.dp))
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (isFetching && realDesc == null) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.padding(vertical = 12.dp)
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "Fetching original description from Craigslist...",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val displayText = realDesc ?: gig.description
                        Text(
                            text = displayText,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 22.sp
                        )

                        if (realDesc != null) {
                            Spacer(modifier = Modifier.height(14.dp))
                            Text(
                                text = "✔️ Verified original posting details fetched from Craigslist",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        } else if (!isFetching) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier
                                    .clickable { viewModel.fetchFullDescriptionIfNeeded(gig) }
                                    .padding(vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Refresh,
                                    contentDescription = "Retry fetch",
                                    tint = MaterialTheme.colorScheme.secondary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "Tap to fetch full description directly from the original listing page",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            Spacer(modifier = Modifier.height(16.dp))

            // Cover letter pitcher
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✍️ Customized Proposal Draft Picker",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            IconButton(
                                onClick = {
                                    clipboardManager.setText(AnnotatedString(customProposal))
                                    Toast.makeText(context, "Copied Pitch letter to clipboard!", Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp).testTag("copy_pitch_btn")
                            ) {
                                Icon(Icons.Default.CheckCircle, contentDescription = "Copy text", tint = MaterialTheme.colorScheme.primary)
                            }
                            IconButton(
                                onClick = {
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "text/plain"
                                        putExtra(Intent.EXTRA_SUBJECT, "Application: ${gig.displayTitle}")
                                        putExtra(Intent.EXTRA_TEXT, customProposal)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Apply for Gig"))
                                },
                                modifier = Modifier.size(32.dp).testTag("share_pitch_btn")
                            ) {
                                Icon(Icons.Default.Share, contentDescription = "Share text", tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Customize your developer name & experiences inside 'My Pitch Profile' tab to instantly update details.",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Surface(
                        color = MaterialTheme.colorScheme.surface,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth(),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.3f))
                    ) {
                        Text(
                            text = customProposal,
                            fontSize = 12.sp,
                            fontFamily = FontFamily.Monospace,
                            color = MaterialTheme.colorScheme.onSurface,
                            lineHeight = 16.sp,
                            modifier = Modifier
                                .padding(12.dp)
                                .testTag("proposal_draft_text")
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

@Composable
fun FeedErrorPane(
    message: String,
    attemptedUrl: String,
    searchQuery: String,
    selectedCategory: String,
    onRetryClick: () -> Unit
) {
    val clipboardManager = LocalClipboardManager.current
    val context = LocalContext.current

    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.verticalScroll(rememberScrollState())
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Communication Error",
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        text = "Attempted Feed Target URL:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = attemptedUrl,
                        fontSize = 10.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "🌪️ SearchTempest Multi-Region Alternative:",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Search all Craigslist areas simultaneously on SearchTempest matching your parameters.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    val q = if (searchQuery.trim().isEmpty()) {
                        if (selectedCategory == "wdg") "web design" else "web developer"
                    } else {
                        searchQuery.trim()
                    }
                    val subcat = if (selectedCategory == "wdg") "wdg" else "web"
                    val searchTempestUrl = "https://www.searchtempest.com/search?search_string=${Uri.encode(q)}&category=8&subcat=$subcat"

                    Button(
                        onClick = {
                            try {
                                context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(searchTempestUrl)))
                            } catch (e: Exception) {
                                clipboardManager.setText(AnnotatedString(searchTempestUrl))
                                Toast.makeText(context, "SearchTempest link copied to clipboard", Toast.LENGTH_SHORT).show()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(14.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open SearchTempest Multi-Area Query", fontSize = 11.sp)
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Tip: Craigslist can occasionally throttle repetitive network queries. Feel free to use the SearchTempest alternative button above to bypass restrictions and inspect multiple client locations at once.",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onRetryClick,
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Retry Load Feed")
            }
        }
    }
}

@Composable
fun EmptyListCard(
    title: String, 
    subtitle: String,
    onSearchTempestClick: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                modifier = Modifier.size(48.dp)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = subtitle,
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )

            if (onSearchTempestClick != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onSearchTempestClick,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("🌪️ Search Nationwide on SearchTempest", fontSize = 12.sp)
                }
            }
        }
    }
}
