package com.example.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.GigViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreatePostMainContent(
    viewModel: GigViewModel,
    onTabSelected: (ActiveTab) -> Unit
) {
    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val selectedCity by viewModel.selectedCity.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val keyboardController = LocalSoftwareKeyboardController.current

    val initialCity = if (selectedCity == "all" || selectedCity.isBlank()) "sfbay" else selectedCity

    // Posting form states from persistent ViewModel
    val title by viewModel.resumeTitle.collectAsStateWithLifecycle()
    val resumeCityCode by viewModel.resumeCityCode.collectAsStateWithLifecycle()
    val cityCode = resumeCityCode.ifBlank { initialCity }
    val neighborhood by viewModel.resumeNeighborhood.collectAsStateWithLifecycle()
    val postalCode by viewModel.resumePostalCode.collectAsStateWithLifecycle()
    val postingBody by viewModel.resumeBody.collectAsStateWithLifecycle()

    var contactName by remember { mutableStateOf(userProfile.fullName) }
    var contactEmail by remember { mutableStateOf("abazhgin1@gmail.com") }
    var contactPhone by remember { mutableStateOf("") }
    val postType = "resume"

    // Dropdown visibility states
    var showCityDropdown by remember { mutableStateOf(false) }

    // Initialize posting body from user profile
    LaunchedEffect(userProfile) {
        if (contactName.isEmpty()) {
            contactName = userProfile.fullName
        }
    }

    // Auto-initialize if empty
    LaunchedEffect(cityCode) {
        if (resumeCityCode.isBlank()) {
            viewModel.updateResumeCityCode(cityCode)
        }
        if (postalCode.isBlank()) {
            viewModel.updateResumePostalCode(getPostalForCityCode(cityCode))
        }
        if (neighborhood.isBlank()) {
            viewModel.updateResumeNeighborhood(getNeighborhoodForCityCode(cityCode))
        }
    }

    // WebView active state
    var isWebViewOpen by remember { mutableStateOf(false) }
    var currentWebUrl by remember { mutableStateOf("") }
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var autoNavigateAndClick by remember { mutableStateOf(true) }
    var currentWebTitle by remember { mutableStateOf("Loading Craigslist...") }
    var webPageLoading by remember { mutableStateOf(false) }
    var showDetailsTray by remember { mutableStateOf(false) }
    var shouldRedirectToPostAfterAuth by remember { mutableStateOf(false) }

    val cityOptions = listOf(
        "sfbay" to "San Francisco Bay Area",
        "newyork" to "New York City",
        "losangeles" to "Los Angeles",
        "seattle" to "Seattle",
        "chicago" to "Chicago",
        "austin" to "Austin",
        "boston" to "Boston",
        "denver" to "Denver",
        "portland" to "Portland",
        "miami" to "Miami",
        "fresno" to "Fresno",
        "sandiego" to "San Diego",
        "sacramento" to "Sacramento",
        "phoenix" to "Phoenix",
        "dallas" to "Dallas",
        "atlanta" to "Atlanta"
    )

    if (isWebViewOpen) {
        // --- WEBVIEW POSTING VIEW ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Automation Header Control Panel
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Build,
                                contentDescription = "Automation Status",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Column {
                                Text(
                                    text = "🤖 Auto-Poster Active",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    fontSize = 15.sp
                                )
                                Text(
                                    text = if (webPageLoading) "Loading next page..." else "Interactive Craigslist flow",
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        IconButton(
                            onClick = { isWebViewOpen = false },
                            modifier = Modifier.testTag("post_webview_close_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Close poster",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Toggle for auto-navigation clicking
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.clickable { autoNavigateAndClick = !autoNavigateAndClick }
                        ) {
                            Checkbox(
                                checked = autoNavigateAndClick,
                                onCheckedChange = { autoNavigateAndClick = it },
                                modifier = Modifier.testTag("post_webview_autoclick_checkbox")
                            )
                            Text(
                                text = "Auto-Navigate/Fill",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }

                        // Manual Control Button Row
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            // Run script manually
                            Button(
                                onClick = {
                                    webViewInstance?.let { webView ->
                                        val js = getAutomationJs(
                                            type = postType,
                                            title = title,
                                            body = postingBody,
                                            postal = postalCode,
                                            location = neighborhood,
                                            email = contactEmail,
                                            phone = contactPhone,
                                            name = contactName,
                                            autoClick = autoNavigateAndClick,
                                            clEmail = userProfile.craigslistEmail,
                                            clPassword = userProfile.craigslistPassword
                                        )
                                        webView.evaluateJavascript(js, null)
                                        Toast.makeText(context, "Autofill triggered manually", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier
                                    .height(36.dp)
                                    .testTag("post_webview_reinject_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Autofill", fontSize = 11.sp)
                            }

                            // Expandable Details Tray button
                            Button(
                                onClick = { showDetailsTray = !showDetailsTray },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 4.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                                shape = RoundedCornerShape(4.dp),
                                modifier = Modifier.height(36.dp)
                            ) {
                                Icon(
                                    imageVector = if (showDetailsTray) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("View Fields", fontSize = 11.sp)
                            }
                        }
                    }

                    // Expandable fields drawer
                    if (showDetailsTray) {
                        Spacer(modifier = Modifier.height(8.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.2f))
                        Spacer(modifier = Modifier.height(4.dp))
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 150.dp)
                                .verticalScroll(rememberScrollState())
                                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f))
                                .padding(8.dp)
                        ) {
                            Text("Title: $title", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            Text("Postal Code: $postalCode", fontSize = 11.sp)
                            Text("Location: $neighborhood", fontSize = 11.sp)
                            Text("Email: $contactEmail", fontSize = 11.sp)
                            Text("Phone: $contactPhone", fontSize = 11.sp)
                            Text("Type: ${postType.uppercase()}", fontSize = 11.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text("Body Template:", fontWeight = FontWeight.SemiBold, fontSize = 11.sp)
                            Text(postingBody, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }

            // WebView implementation
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                AndroidView(
                    factory = { ctx ->
                        WebView(ctx).apply {
                            webViewInstance = this
                            @SuppressLint("SetJavaScriptEnabled")
                            settings.javaScriptEnabled = true
                            settings.domStorageEnabled = true
                            settings.cacheMode = WebSettings.LOAD_DEFAULT
                            settings.userAgentString = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                            
                            webViewClient = object : WebViewClient() {
                                override fun onPageStarted(view: WebView?, url: String?, favicon: Bitmap?) {
                                    super.onPageStarted(view, url, favicon)
                                    webPageLoading = true
                                    url?.let { currentWebUrl = it }
                                    
                                    // Automatic redirect to posting URL after successful account authentication
                                    if (url != null) {
                                        val lowerUrl = url.lowercase()
                                        if (shouldRedirectToPostAfterAuth && lowerUrl.contains("accounts.craigslist.org") && 
                                            (!lowerUrl.contains("/login") || lowerUrl.contains("/login/home") || lowerUrl.contains("/home"))) {
                                            shouldRedirectToPostAfterAuth = false
                                            val postingCityCode = getCraigslistPostingCityCode(cityCode)
                                            view?.loadUrl("https://post.craigslist.org/c/$postingCityCode")
                                            Toast.makeText(context, "Authentication successful! Redirecting to Resume Creation...", Toast.LENGTH_LONG).show()
                                        }
                                    }
                                }

                                override fun onPageFinished(view: WebView?, url: String?) {
                                    super.onPageFinished(view, url)
                                    webPageLoading = false
                                    url?.let { currentWebUrl = it }
                                    currentWebTitle = view?.title ?: "Craigslist"
                                    
                                    // Automatic redirect to posting URL after successful account authentication (backup check)
                                    if (url != null) {
                                        val lowerUrl = url.lowercase()
                                        if (shouldRedirectToPostAfterAuth && lowerUrl.contains("accounts.craigslist.org") && 
                                            (!lowerUrl.contains("/login") || lowerUrl.contains("/login/home") || lowerUrl.contains("/home"))) {
                                            shouldRedirectToPostAfterAuth = false
                                            val postingCityCode = getCraigslistPostingCityCode(cityCode)
                                            view?.loadUrl("https://post.craigslist.org/c/$postingCityCode")
                                            Toast.makeText(context, "Authentication successful! Redirecting to Resume Creation...", Toast.LENGTH_LONG).show()
                                            return
                                        }
                                    }
                                    
                                    // Inject automation script
                                    val js = getAutomationJs(
                                        type = postType,
                                        title = title,
                                        body = postingBody,
                                        postal = postalCode,
                                        location = neighborhood,
                                        email = contactEmail,
                                        phone = contactPhone,
                                        name = contactName,
                                        autoClick = autoNavigateAndClick,
                                        clEmail = userProfile.craigslistEmail,
                                        clPassword = userProfile.craigslistPassword
                                    )
                                    view?.evaluateJavascript(js, null)
                                }
                            }
                            loadUrl(currentWebUrl)
                        }
                    },
                    modifier = Modifier.fillMaxSize(),
                    update = { webView ->
                        // If url or config changed, we can load it here
                    }
                )

                if (webPageLoading) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
        }
    } else {
        // --- RESUME POSTING CONFIGURATION FORM ---
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
                .testTag("create_post_section"),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            TabbedHeader(
                currentTab = ActiveTab.CreatePost,
                onTabSelected = onTabSelected
            )

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "Automated Craigslist Poster",
                            fontWeight = FontWeight.Bold,
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = "Fill in your resume, choose a region, and launch. The embedded browser will auto-fill Craigslist forms and skip tedious selection screens!",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Title Field
            OutlinedTextField(
                value = title,
                onValueChange = { viewModel.updateResumeTitle(it) },
                label = { Text("Posting Title") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_title_input"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // City and Location row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // City Select Box (Dropdown representation)
                Box(modifier = Modifier.weight(1f)) {
                    OutlinedTextField(
                        value = cityOptions.find { it.first == cityCode }?.second ?: cityCode,
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Craigslist Region") },
                        trailingIcon = {
                            IconButton(onClick = { showCityDropdown = true }) {
                                Icon(Icons.Default.ArrowDropDown, contentDescription = "Select region")
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showCityDropdown = true }
                            .testTag("post_city_select_box"),
                        colors = TextFieldDefaults.colors(
                            focusedContainerColor = Color.Transparent,
                            unfocusedContainerColor = Color.Transparent
                        )
                    )
                    DropdownMenu(
                        expanded = showCityDropdown,
                        onDismissRequest = { showCityDropdown = false }
                    ) {
                        cityOptions.forEach { (code, name) ->
                            DropdownMenuItem(
                                text = { Text(name) },
                                onClick = {
                                    viewModel.updateResumeCityCode(code)
                                    viewModel.updateResumePostalCode(getPostalForCityCode(code))
                                    viewModel.updateResumeNeighborhood(getNeighborhoodForCityCode(code))
                                    showCityDropdown = false
                                }
                            )
                        }
                    }
                }

                // Postal code input
                OutlinedTextField(
                    value = postalCode,
                    onValueChange = { viewModel.updateResumePostalCode(it) },
                    label = { Text("Postal Code") },
                    singleLine = true,
                    modifier = Modifier
                        .weight(1f)
                        .testTag("post_postal_input"),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number, imeAction = ImeAction.Next),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            // Neighborhood / Location input
            OutlinedTextField(
                value = neighborhood,
                onValueChange = { viewModel.updateResumeNeighborhood(it) },
                label = { Text("Specific Location / Neighborhood") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_location_input"),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // Posting Body / Resume text field
            OutlinedTextField(
                value = postingBody,
                onValueChange = { viewModel.updateResumeBody(it) },
                label = { Text("Posting Description (Resume / Services / Pitch Body)") },
                minLines = 8,
                maxLines = 16,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("post_body_input"),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent
                )
            )

            // Credentials card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.2f)),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.3f))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Craigslist Account Authentication",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    if (userProfile.craigslistEmail.isNotBlank()) {
                        Text(
                            text = "Auto-login active for account: ${userProfile.craigslistEmail}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "No account configured. Tap the Settings tab to configure your credentials.",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedButton(
                        onClick = {
                            keyboardController?.hide()
                            currentWebUrl = "https://accounts.craigslist.org/login"
                            isWebViewOpen = true
                            Toast.makeText(context, "Opening Craigslist Authentication Page...", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(38.dp)
                            .testTag("post_auth_btn"),
                        shape = RoundedCornerShape(8.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.secondary)
                    ) {
                        Icon(Icons.Default.Lock, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Show Craigslist user", fontSize = 12.sp)
                    }
                }
            }

            // Launch button
            Button(
                onClick = {
                    if (title.isBlank()) {
                        Toast.makeText(context, "Please enter a posting title", Toast.LENGTH_SHORT).show()
                    } else if (postingBody.isBlank()) {
                        Toast.makeText(context, "Please enter posting description details", Toast.LENGTH_SHORT).show()
                    } else {
                        keyboardController?.hide()
                        currentWebUrl = "https://accounts.craigslist.org/login"
                        shouldRedirectToPostAfterAuth = true
                        isWebViewOpen = true
                        Toast.makeText(context, "Authenticating first... Then automatic redirect to Posting Flow", Toast.LENGTH_LONG).show()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp)
                    .testTag("post_launch_btn"),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Icon(Icons.Default.Send, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Launch Automated Posting Browser",
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }

            Spacer(modifier = Modifier.height(40.dp))
        }
    }
}

// Map standard city ids to craigslist posting code (e.g. "sfo" for "sfbay", etc.)
private fun getCraigslistPostingCityCode(city: String): String {
    return when (city.lowercase()) {
        "sfbay" -> "sfo"
        "newyork" -> "nyc"
        "losangeles" -> "lax"
        "seattle" -> "sea"
        "chicago" -> "chi"
        "austin" -> "aus"
        "boston" -> "bos"
        "denver" -> "den"
        "portland" -> "pdx"
        "miami" -> "mia"
        "fresno" -> "fre"
        "sandiego" -> "sdo"
        "sacramento" -> "sac"
        "phoenix" -> "phx"
        "dallas" -> "dal"
        "atlanta" -> "atl"
        else -> "sfo"
    }
}

// Map cities to standard zip codes
private fun getPostalForCityCode(city: String): String {
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

// Map cities to default neighborhood strings
private fun getNeighborhoodForCityCode(city: String): String {
    return when (city.lowercase()) {
        "sfbay" -> "San Francisco"
        "newyork" -> "Manhattan"
        "losangeles" -> "Los Angeles"
        "seattle" -> "Downtown"
        "chicago" -> "Loop"
        "austin" -> "Downtown Austin"
        "boston" -> "Boston Common"
        "denver" -> "Denver"
        "portland" -> "Portland"
        "miami" -> "Miami Beach"
        "fresno" -> "Fresno"
        "sandiego" -> "San Diego"
        "sacramento" -> "Sacramento"
        "phoenix" -> "Phoenix"
        "dallas" -> "Dallas"
        "atlanta" -> "Atlanta"
        else -> ""
    }
}

// Javascript automation generation
private fun getAutomationJs(
    type: String,
    title: String,
    body: String,
    postal: String,
    location: String,
    email: String,
    phone: String,
    name: String,
    autoClick: Boolean,
    clEmail: String,
    clPassword: String
): String {
    val escapedTitle = title.replace("'", "\\'").replace("\n", " ").replace("\r", " ")
    val escapedBody = body.replace("'", "\\'").replace("\n", "\\n").replace("\r", "\\r")
    val escapedPostal = postal.replace("'", "\\'").replace("\n", "")
    val escapedLocation = location.replace("'", "\\'").replace("\n", "")
    val escapedEmail = email.replace("'", "\\'").replace("\n", "")
    val escapedPhone = phone.replace("'", "\\'").replace("\n", "")
    val escapedName = name.replace("'", "\\'").replace("\n", "")
    val escapedClEmail = clEmail.replace("'", "\\'").replace("\n", "")
    val escapedClPassword = clPassword.replace("'", "\\'")

    return """
        (function() {
            console.log("Craigslist Automation Running...");
            
            // Suppress alerts/confirms
            window.alert = function() { console.log("Suppressed alert:", arguments); return true; };
            window.confirm = function() { console.log("Suppressed confirm:", arguments); return true; };
            window.onbeforeunload = null;

            // 1. Inject Visual Banner at top
            function updateBanner(text, bgColor) {
                var banner = document.getElementById('cl-automation-banner');
                if (!banner) {
                    banner = document.createElement('div');
                    banner.id = 'cl-automation-banner';
                    banner.style.fontFamily = 'system-ui, -apple-system, sans-serif';
                    banner.style.textAlign = 'center';
                    banner.style.fontWeight = 'bold';
                    banner.style.fontSize = '14px';
                    banner.style.zIndex = '999999';
                    banner.style.position = 'fixed';
                    banner.style.top = '0';
                    banner.style.left = '0';
                    banner.style.right = '0';
                    banner.style.boxShadow = '0 2px 8px rgba(0,0,0,0.3)';
                    banner.style.padding = '12px';
                    document.body.appendChild(banner);
                    document.body.style.paddingTop = '45px';
                }
                banner.style.background = bgColor || '#4F359B';
                banner.style.color = 'white';
                banner.innerHTML = '🤖 ' + text;
            }

            updateBanner('Craigslist Post Automator Active &bull; Scanning page...', '#4F359B');

            // Helper to style highlighted elements
            function highlight(el) {
                if (!el) return;
                el.style.border = '2px solid #4F359B';
                el.style.backgroundColor = '#EDE6FA';
                el.style.transition = 'all 0.3s ease';
            }

            var autoClickEnabled = $autoClick;
            
            // Robust Continue Button finder
            function findContinueButton() {
                // 1. Standard Craigslist class names and IDs
                var btn = document.querySelector('button.continue:not([data-cl-clicked])') || 
                          document.querySelector('button.done:not([data-cl-clicked])') ||
                          document.querySelector('button[value="continue"]:not([data-cl-clicked])') ||
                          document.querySelector('input[type="submit"].continue:not([data-cl-clicked])') ||
                          document.querySelector('form[id="leaflet"] button[type="submit"]:not([data-cl-clicked])') ||
                          document.querySelector('form[id="publish_form"] button[type="submit"]:not([data-cl-clicked])') ||
                          document.querySelector('.page-container button[type="submit"]:not([data-cl-clicked])');
                if (btn) return btn;

                // 2. Search by text content inside all buttons and inputs
                var buttons = document.querySelectorAll('button:not([data-cl-clicked]), input[type="submit"]:not([data-cl-clicked]), input[type="button"]:not([data-cl-clicked])');
                for (var i = 0; i < buttons.length; i++) {
                    var b = buttons[i];
                    var text = (b.textContent || b.value || "").toLowerCase().trim();
                    if (text.includes("continue") || 
                        text.includes("done with images") || 
                        text.includes("publish") || 
                        text.includes("submit") || 
                        text.includes("next") || 
                        text.includes("accept") || 
                        text.includes("agree") || 
                        text.includes("go to")) {
                        return b;
                    }
                }

                // 3. Fallback: If there is a form, use its primary submit button
                var forms = document.querySelectorAll('form');
                for (var j = 0; j < forms.length; j++) {
                    var f = forms[j];
                    var submitBtn = f.querySelector('button[type="submit"]:not([data-cl-clicked])') || f.querySelector('input[type="submit"]:not([data-cl-clicked])');
                    if (submitBtn) {
                        return submitBtn;
                    }
                }

                return null;
            }

            function runAutomationCycle() {
                var form = document.querySelector('form');
                var actionTaken = false;

                // --- CRAIGSLIST LOGIN AUTO-FILL ---
                var loginEmailInput = document.getElementById('inputEmailHandle') || 
                                      document.querySelector('input[name="inputEmailHandle"]') ||
                                      document.querySelector('input[type="email"]') ||
                                      document.querySelector('input[name="email"]');
                var loginPasswordInput = document.getElementById('inputPassword') || 
                                         document.querySelector('input[name="inputPassword"]') ||
                                         document.querySelector('input[type="password"]') ||
                                         document.querySelector('input[name="password"]');

                if (loginEmailInput && loginPasswordInput) {
                    var clEmail = '$escapedClEmail';
                    var clPassword = '$escapedClPassword';
                    
                    if (clEmail && clPassword) {
                        var changed = false;
                        if (loginEmailInput.value !== clEmail) {
                            loginEmailInput.value = clEmail;
                            highlight(loginEmailInput);
                            changed = true;
                        }
                        if (loginPasswordInput.value !== clPassword) {
                            loginPasswordInput.value = clPassword;
                            highlight(loginPasswordInput);
                            changed = true;
                        }
                        
                        if (changed) {
                            updateBanner('Auto-filling Craigslist login credentials...', '#2E7D32');
                            actionTaken = true;
                            
                            if (autoClickEnabled) {
                                setTimeout(function() {
                                    var loginBtn = document.getElementById('login') || 
                                                   document.querySelector('button[type="submit"]') || 
                                                   document.querySelector('input[type="submit"]');
                                    if (loginBtn) {
                                        updateBanner('Logging in...', '#1B5E20');
                                        loginBtn.setAttribute('data-cl-clicked', 'true');
                                        loginBtn.click();
                                    }
                                }, 1500);
                            }
                            return;
                        }
                    } else {
                        updateBanner('Craigslist login detected. Enter email & password in app settings to auto-login.', '#D32F2F');
                    }
                }

                // --- CRAIGSLIST VERIFICATION DETECTION ---
                var verificationInput = document.querySelector('input[name="code"]') || 
                                        document.getElementById('code') || 
                                        document.querySelector('input[name="verification_code"]');
                if (verificationInput) {
                    updateBanner('Verification Code Required. Please check your email or phone and enter the code below.', '#EF6C00');
                    highlight(verificationInput);
                    return;
                }

                // --- EXPLICIT USER PREFERENCE: SELECT "individual seeking employment" ---
                var allRadiosForPref = document.querySelectorAll('input[type="radio"]');
                for (var r = 0; r < allRadiosForPref.length; r++) {
                    var radio = allRadiosForPref[r];
                    var label = document.querySelector('label[for="' + radio.id + '"]') || radio.closest('label');
                    if (label) {
                        var text = label.textContent.toLowerCase();
                        if (text.includes("individual seeking employment") || text.includes("seeking employment")) {
                            if (!radio.checked) {
                                radio.checked = true;
                                highlight(label);
                                actionTaken = true;
                                updateBanner('Found and selected: "Individual seeking employment"', '#2E7D32');
                                if (autoClickEnabled && form) {
                                    setTimeout(function() {
                                        var submitBtn = form.querySelector('button[type="submit"]') || form.querySelector('input[type="submit"]') || form.querySelector('.go');
                                        if (submitBtn) {
                                            submitBtn.setAttribute('data-cl-clicked', 'true');
                                            submitBtn.click();
                                        } else {
                                            form.submit();
                                        }
                                    }, 1000);
                                }
                                return;
                            }
                        }
                    }
                }

                var allInteractiveForPref = document.querySelectorAll('a, button, input[type="button"], input[type="submit"]');
                for (var i = 0; i < allInteractiveForPref.length; i++) {
                    var element = allInteractiveForPref[i];
                    var text = (element.textContent || element.value || "").toLowerCase();
                    if (text.includes("individual seeking employment") || text.includes("seeking employment")) {
                        if (!element.getAttribute('data-cl-clicked')) {
                            actionTaken = true;
                            highlight(element);
                            updateBanner('Found and clicking option: "' + element.textContent.trim() + '"', '#2E7D32');
                            setTimeout(function() {
                                element.setAttribute('data-cl-clicked', 'true');
                                element.click();
                            }, 1000);
                            return;
                        }
                    }
                }

                // --- STEP 1: CHOOSE POSTING TYPE PAGE ---
                var typeRadios = document.querySelectorAll('input[type="radio"][name="id"]');
                if (typeRadios.length > 0) {
                    var targetType = '$type';
                    for (var i = 0; i < typeRadios.length; i++) {
                        var radio = typeRadios[i];
                        var label = document.querySelector('label[for="' + radio.id + '"]') || radio.closest('label');
                        var labelText = label ? label.textContent.toLowerCase() : "";
                        var value = radio.value;

                        var match = false;
                        if (targetType === 'resume' && (value === 'wa' || labelText.includes('resume') || labelText.includes('job wanted'))) {
                            match = true;
                        } else if (targetType === 'service' && (value === 'so' || labelText.includes('service offered') || labelText.includes('services offered'))) {
                            match = true;
                        } else if (targetType === 'gig' && (value === 'g' || value === 'go' || labelText.includes('gig offered') || labelText.includes('gigs offered'))) {
                            match = true;
                        }

                        if (match && !radio.checked) {
                            radio.checked = true;
                            highlight(label);
                            actionTaken = true;
                            
                            updateBanner('Selected posting type: ' + targetType.toUpperCase() + '. Advancing...', '#4F359B');
                            if (autoClickEnabled && form) {
                                setTimeout(function() {
                                    var submitBtn = form.querySelector('button[type="submit"]') || form.querySelector('input[type="submit"]') || form.querySelector('.go');
                                    if (submitBtn) {
                                        submitBtn.setAttribute('data-cl-clicked', 'true');
                                        submitBtn.click();
                                    } else {
                                        form.submit();
                                    }
                                }, 1000);
                            }
                            return;
                        }
                    }

                    // Fallback for Step 1: Select 3rd option (or any that fits)
                    if (!actionTaken) {
                        var targetIndex = Math.min(2, typeRadios.length - 1);
                        if (targetIndex >= 0) {
                            var radio = typeRadios[targetIndex];
                            if (!radio.checked) {
                                radio.checked = true;
                                var label = document.querySelector('label[for="' + radio.id + '"]') || radio.closest('label');
                                highlight(label || radio);
                                actionTaken = true;
                                updateBanner('Selected 3rd/available option: ' + (label ? label.textContent.trim() : radio.value), '#4F359B');
                                if (autoClickEnabled && form) {
                                    setTimeout(function() {
                                        var submitBtn = form.querySelector('button[type="submit"]') || form.querySelector('input[type="submit"]') || form.querySelector('.go');
                                        if (submitBtn) {
                                            submitBtn.setAttribute('data-cl-clicked', 'true');
                                            submitBtn.click();
                                        } else {
                                            form.submit();
                                        }
                                    }, 1000);
                                }
                                return;
                            }
                        }
                    }
                }

                // --- STEP 2: CHOOSE CATEGORY PAGE ---
                var catRadios = document.querySelectorAll('input[type="radio"][name="id"]');
                if (!actionTaken && catRadios.length > 0) {
                    var targetType = '$type';
                    for (var i = 0; i < catRadios.length; i++) {
                        var radio = catRadios[i];
                        var label = document.querySelector('label[for="' + radio.id + '"]') || radio.closest('label');
                        var labelText = label ? label.textContent.toLowerCase() : "";
                        var value = radio.value;

                        var match = false;
                        if (targetType === 'resume' && (value === '145' || labelText.includes('resumes'))) {
                            match = true;
                        } else if (targetType === 'service' && (labelText.includes('computer') || labelText.includes('creative') || labelText.includes('web'))) {
                            match = true;
                        } else if (targetType === 'gig' && (labelText.includes('computer') || labelText.includes('creative') || labelText.includes('labor'))) {
                            match = true;
                        }

                        if (match && !radio.checked) {
                            radio.checked = true;
                            highlight(label);
                            actionTaken = true;
                            
                            updateBanner('Selected category. Advancing...', '#4F359B');
                            if (autoClickEnabled && form) {
                                setTimeout(function() {
                                    var submitBtn = form.querySelector('button[type="submit"]') || form.querySelector('input[type="submit"]') || form.querySelector('.go');
                                    if (submitBtn) {
                                        submitBtn.setAttribute('data-cl-clicked', 'true');
                                        submitBtn.click();
                                    } else {
                                        form.submit();
                                    }
                                }, 1000);
                            }
                            return;
                        }
                    }

                    // Fallback for Step 2: Select 3rd option (or any that fits)
                    if (!actionTaken) {
                        var targetIndex = Math.min(2, catRadios.length - 1);
                        if (targetIndex >= 0) {
                            var radio = catRadios[targetIndex];
                            if (!radio.checked) {
                                radio.checked = true;
                                var label = document.querySelector('label[for="' + radio.id + '"]') || radio.closest('label');
                                highlight(label || radio);
                                actionTaken = true;
                                updateBanner('Selected 3rd/available option: ' + (label ? label.textContent.trim() : radio.value), '#4F359B');
                                if (autoClickEnabled && form) {
                                    setTimeout(function() {
                                        var submitBtn = form.querySelector('button[type="submit"]') || form.querySelector('input[type="submit"]') || form.querySelector('.go');
                                        if (submitBtn) {
                                            submitBtn.setAttribute('data-cl-clicked', 'true');
                                            submitBtn.click();
                                        } else {
                                            form.submit();
                                        }
                                    }, 1000);
                                }
                                return;
                            }
                        }
                    }
                }

                // --- STEP 3: SUB-AREA/LOCATION SELECTION ---
                var subAreaRadios = document.querySelectorAll('input[type="radio"]');
                if (!actionTaken && subAreaRadios.length > 0) {
                    var h1 = document.querySelector('h1');
                    var pageText = document.body.textContent.toLowerCase();
                    if ((h1 && (h1.textContent.toLowerCase().includes("location") || h1.textContent.toLowerCase().includes("area") || h1.textContent.toLowerCase().includes("neighborhood") || h1.textContent.toLowerCase().includes("choose"))) || pageText.includes("choose") || pageText.includes("select")) {
                        var targetIndex = Math.min(2, subAreaRadios.length - 1);
                        if (targetIndex >= 0 && !subAreaRadios[targetIndex].checked) {
                            var radio = subAreaRadios[targetIndex];
                            var label = document.querySelector('label[for="' + radio.id + '"]') || radio.closest('label');
                            radio.checked = true;
                            highlight(label || radio);
                            actionTaken = true;
                            
                            updateBanner('Selected 3rd/available area option: ' + (label ? label.textContent.trim() : radio.value), '#4F359B');
                            if (autoClickEnabled && form) {
                                setTimeout(function() {
                                    var submitBtn = form.querySelector('button[type="submit"]') || form.querySelector('input[type="submit"]') || form.querySelector('.go');
                                    if (submitBtn) {
                                        submitBtn.setAttribute('data-cl-clicked', 'true');
                                        submitBtn.click();
                                    } else {
                                        form.submit();
                                    }
                                }, 1000);
                            }
                            return;
                        }
                    }
                }


                // --- STEP 4: FILL POST DETAILS FORM PAGE ---
                var isFormFilled = false;
                
                // Title
                var titleField = document.getElementById('PostingTitle') || document.querySelector('input[name="PostingTitle"]');
                if (titleField && titleField.value !== '$escapedTitle') {
                    titleField.value = '$escapedTitle';
                    highlight(titleField);
                    isFormFilled = true;
                }

                // Postal Code
                var postalField = document.getElementById('postal_code') || document.querySelector('input[name="postal_code"]');
                if (postalField && postalField.value !== '$escapedPostal') {
                    postalField.value = '$escapedPostal';
                    highlight(postalField);
                    isFormFilled = true;
                }

                // Location
                var locationField = document.getElementById('gmap_address') || document.querySelector('input[name="gmap_address"]') || document.querySelector('input[name="xstreet0"]');
                if (locationField && locationField.value !== '$escapedLocation') {
                    locationField.value = '$escapedLocation';
                    highlight(locationField);
                    isFormFilled = true;
                }

                // Email
                var emailField = document.getElementById('FromEMail') || document.querySelector('input[name="FromEMail"]');
                if (emailField && emailField.value !== '$escapedEmail') {
                    emailField.value = '$escapedEmail';
                    highlight(emailField);
                    isFormFilled = true;
                }
                
                var emailConfirmField = document.getElementById('ConfirmEMail') || document.querySelector('input[name="ConfirmEMail"]');
                if (emailConfirmField && emailConfirmField.value !== '$escapedEmail') {
                    emailConfirmField.value = '$escapedEmail';
                    highlight(emailConfirmField);
                    isFormFilled = true;
                }

                // Phone
                var phoneField = document.getElementById('contact_phone') || document.querySelector('input[name="contact_phone"]');
                if (phoneField && '$escapedPhone' && phoneField.value !== '$escapedPhone') {
                    phoneField.value = '$escapedPhone';
                    highlight(phoneField);
                    isFormFilled = true;
                    
                    var phoneOk = document.getElementById('contact_phone_ok') || document.querySelector('input[name="contact_phone_ok"]') || document.querySelector('input[name="phone_ok"]');
                    if (phoneOk) { phoneOk.checked = true; highlight(phoneOk.closest('label') || phoneOk); }
                    var textOk = document.getElementById('contact_text_ok') || document.querySelector('input[name="contact_text_ok"]') || document.querySelector('input[name="text_ok"]');
                    if (textOk) { textOk.checked = true; highlight(textOk.closest('label') || textOk); }
                }

                // Contact Name
                var nameField = document.getElementById('contact_name') || document.querySelector('input[name="contact_name"]');
                if (nameField && nameField.value !== '$escapedName') {
                    nameField.value = '$escapedName';
                    highlight(nameField);
                    isFormFilled = true;
                }

                // Body Description
                var bodyField = document.getElementById('PostingBody') || document.querySelector('textarea[name="PostingBody"]');
                if (bodyField && bodyField.value !== '$escapedBody') {
                    bodyField.value = '$escapedBody';
                    highlight(bodyField);
                    isFormFilled = true;
                }

                if (isFormFilled) {
                    actionTaken = true;
                    updateBanner('Autofilling forms! Auto-submitting details...', '#2E7D32');
                    if (autoClickEnabled && form) {
                        setTimeout(function() {
                            var submitBtn = form.querySelector('button[type="submit"]') || form.querySelector('input[type="submit"]') || form.querySelector('.go');
                            if (submitBtn) {
                                submitBtn.setAttribute('data-cl-clicked', 'true');
                                submitBtn.click();
                            } else {
                                form.submit();
                            }
                        }, 1500);
                    }
                    return;
                }

                // --- STEP 5: AUTOMATED CLICK/CONTINUE ON ALL TRANSITION PAGES ---
                if (autoClickEnabled) {
                    var continueBtn = findContinueButton();
                    if (continueBtn) {
                        var btnText = (continueBtn.textContent || continueBtn.value || "Continue").trim();
                        updateBanner('Auto-advancing: clicking "' + btnText + '"...', '#1565C0');
                        highlight(continueBtn);
                        
                        setTimeout(function() {
                            continueBtn.setAttribute('data-cl-clicked', 'true');
                            continueBtn.click();
                        }, 1000);
                    }
                }
            }

            // Run the automation cycle immediately
            runAutomationCycle();

            // Run a recurrent checker every 1200ms to handle any slow/delayed dynamic DOM rendering or transition
            if (!window.clAutomationInterval) {
                window.clAutomationInterval = setInterval(function() {
                    runAutomationCycle();
                }, 1200);
            }
        })();
    """.trimIndent()
}
