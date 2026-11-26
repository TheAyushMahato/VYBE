package com.vybe.music.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Person
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.vybe.music.data.repository.ArtistRepository
import com.vybe.music.data.repository.MusicRepository
import com.vybe.music.model.ArtistSeed
import com.vybe.music.model.CuratedArtist
import com.vybe.music.ui.components.bouncyClickable
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private val CURATED_ARTISTS = listOf(
    // ── Global Icons ──
    CuratedArtist("The Weeknd", "https://c.saavncdn.com/artists/The_Weeknd_002_20241003071400_500x500.jpg", listOf("English"), listOf("R&B", "Pop")),
    CuratedArtist("Arctic Monkeys", "https://cdn-images.dzcdn.net/images/artist/6c03e4c7c36800897fd468633286db24/500x500-000000-80-0-0.jpg", listOf("English"), listOf("Rock", "Indie")),
    CuratedArtist("Lana Del Rey", "https://cdn-images.dzcdn.net/images/artist/8994d3be1a59a72f887f1f8afd2d4c6c/500x500-000000-80-0-0.jpg", listOf("English"), listOf("Pop", "Indie")),
    CuratedArtist("Britney Spears", "https://cdn-images.dzcdn.net/images/artist/caea45732bb52679494602c60430435a/500x500-000000-80-0-0.jpg", listOf("English"), listOf("Pop", "EDM")),
    CuratedArtist("Taylor Swift", "https://c.saavncdn.com/artists/Taylor_Swift_500x500.jpg", listOf("English"), listOf("Pop", "Indie")),
    CuratedArtist("Billie Eilish", "https://c.saavncdn.com/artists/Billie_Eilish_004_20240520063200_500x500.jpg", listOf("English"), listOf("Indie", "Pop")),
    CuratedArtist("Frank Ocean", "https://cdn-images.dzcdn.net/images/artist/d57b0431d7d794302e1782dfd7a3800d/500x500-000000-80-0-0.jpg", listOf("English"), listOf("R&B", "Indie")),
    CuratedArtist("Drake", "https://cdn-images.dzcdn.net/images/artist/70223888f501f4b843142e071abda364/500x500-000000-80-0-0.jpg", listOf("English"), listOf("Hip-Hop & Rap", "R&B")),
    CuratedArtist("Tame Impala", "https://cdn-images.dzcdn.net/images/artist/879015e713cc6ad6ffaeec154c027505/500x500-000000-80-0-0.jpg", listOf("English"), listOf("Indie", "Rock")),
    CuratedArtist("SZA", "https://cdn-images.dzcdn.net/images/artist/c1291c474f833ba1d02cc3bc3e0940eb/500x500-000000-80-0-0.jpg", listOf("English"), listOf("R&B", "Pop")),
    CuratedArtist("Dua Lipa", "https://c.saavncdn.com/artists/Dua_Lipa_004_20231120090922_500x500.jpg", listOf("English"), listOf("Pop", "EDM")),
    CuratedArtist("Coldplay", "https://cdn-images.dzcdn.net/images/artist/3087954bca22f306324912e5ac8375c3/500x500-000000-80-0-0.jpg", listOf("English"), listOf("Rock", "Pop")),
    CuratedArtist("Bruno Mars", "https://cdn-images.dzcdn.net/images/artist/90f0b5b11df4f87ee878f38569b5995b/500x500-000000-80-0-0.jpg", listOf("English"), listOf("Pop", "R&B")),
    CuratedArtist("Sabrina Carpenter", "https://c.saavncdn.com/artists/Sabrina_Carpenter_002_20240902091400_500x500.jpg", listOf("English"), listOf("Pop")),
    CuratedArtist("Kendrick Lamar", "https://cdn-images.dzcdn.net/images/artist/be0a7c550567f4af0ed202d7235b74d6/500x500-000000-80-0-0.jpg", listOf("English"), listOf("Hip-Hop & Rap")),
    CuratedArtist("Post Malone", "https://c.saavncdn.com/artists/Post_Malone_500x500.jpg", listOf("English"), listOf("Hip-Hop & Rap", "Pop")),
    CuratedArtist("Ed Sheeran", "https://c.saavncdn.com/artists/Ed_Sheeran_500x500.jpg", listOf("English"), listOf("Pop", "Acoustic")),
    CuratedArtist("Alan Walker", "https://c.saavncdn.com/artists/Alan_Walker_003_20231116095443_500x500.jpg", listOf("English"), listOf("EDM")),
    CuratedArtist("Justin Bieber", "https://c.saavncdn.com/artists/Justin_Bieber_500x500.jpg", listOf("English"), listOf("Pop", "R&B")),
    CuratedArtist("Eminem", "https://c.saavncdn.com/artists/Eminem_500x500.jpg", listOf("English"), listOf("Hip-Hop & Rap", "Workout")),
    CuratedArtist("Travis Scott", "https://c.saavncdn.com/artists/Travis_Scott_002_20230728124933_500x500.jpg", listOf("English"), listOf("Hip-Hop & Rap")),

    // ── Indian & Regional Icons ──
    CuratedArtist("Arijit Singh", "https://c.saavncdn.com/artists/Arijit_Singh_500x500.jpg", listOf("Hindi"), listOf("Bollywood", "Romance")),
    CuratedArtist("Pritam", "https://c.saavncdn.com/artists/Pritam_Chakraborty-20170711073326_500x500.jpg", listOf("Hindi"), listOf("Bollywood", "Pop")),
    CuratedArtist("Shreya Ghoshal", "https://c.saavncdn.com/artists/Shreya_Ghoshal_500x500.jpg", listOf("Hindi"), listOf("Bollywood", "Romance")),
    CuratedArtist("Atif Aslam", "https://c.saavncdn.com/artists/Atif_Aslam_500x500.jpg", listOf("Hindi", "Punjabi"), listOf("Bollywood", "Romance")),
    CuratedArtist("Diljit Dosanjh", "https://c.saavncdn.com/artists/Diljit_Dosanjh_500x500.jpg", listOf("Punjabi"), listOf("Pop", "Hip-Hop & Rap")),
    CuratedArtist("Anuv Jain", "https://c.saavncdn.com/artists/Anuv_Jain_002_20230713063539_500x500.jpg", listOf("Hindi"), listOf("Indie", "Acoustic")),
    CuratedArtist("AP Dhillon", "https://c.saavncdn.com/artists/AP_Dhillon_004_20251023102150_500x500.jpg", listOf("Punjabi"), listOf("Hip-Hop & Rap", "Indie")),
    CuratedArtist("KK", "https://c.saavncdn.com/artists/K_K__500x500.jpg", listOf("Hindi"), listOf("Bollywood", "Rock", "Retro Classics")),
    CuratedArtist("Sonu Nigam", "https://c.saavncdn.com/artists/Sonu_Nigam_500x500.jpg", listOf("Hindi"), listOf("Bollywood", "Retro Classics")),
    CuratedArtist("A.R. Rahman", "https://c.saavncdn.com/artists/A_R__Rahman_500x500.jpg", listOf("Tamil", "Hindi", "Telugu"), listOf("Bollywood", "Indie")),
    CuratedArtist("Seedhe Maut", "https://c.saavncdn.com/artists/Seedhe_Maut_002_20240322102100_500x500.jpg", listOf("Hindi"), listOf("Hip-Hop & Rap")),
    CuratedArtist("Shubh", "https://c.saavncdn.com/artists/Shubh_002_20231117094056_500x500.jpg", listOf("Punjabi"), listOf("Hip-Hop & Rap")),
    CuratedArtist("Karan Aujla", "https://c.saavncdn.com/artists/Karan_Aujla_005_20260925061936_500x500.jpg", listOf("Punjabi"), listOf("Hip-Hop & Rap")),
    CuratedArtist("Jubin Nautiyal", "https://c.saavncdn.com/artists/Jubin_Nautiyal_500x500.jpg", listOf("Hindi"), listOf("Bollywood", "Romance")),
    CuratedArtist("Vishal Mishra", "https://c.saavncdn.com/artists/Vishal_Mishra_500x500.jpg", listOf("Hindi"), listOf("Bollywood", "Indie")),
    CuratedArtist("B Praak", "https://c.saavncdn.com/artists/B_Praak_500x500.jpg", listOf("Punjabi", "Hindi"), listOf("Romance", "Bollywood")),
    CuratedArtist("Badshah", "https://c.saavncdn.com/artists/Badshah_500x500.jpg", listOf("Hindi", "Punjabi"), listOf("Hip-Hop & Rap", "Bollywood")),
    CuratedArtist("Neha Kakkar", "https://c.saavncdn.com/artists/Neha_Kakkar_500x500.jpg", listOf("Hindi", "Punjabi"), listOf("Bollywood", "Pop")),
    CuratedArtist("Armaan Malik", "https://c.saavncdn.com/artists/Armaan_Malik_500x500.jpg", listOf("Hindi", "English"), listOf("Pop", "Romance")),
    CuratedArtist("Sunidhi Chauhan", "https://c.saavncdn.com/artists/Sunidhi_Chauhan_500x500.jpg", listOf("Hindi"), listOf("Bollywood", "Pop")),
    CuratedArtist("Sidhu Moose Wala", "https://c.saavncdn.com/artists/Sidhu_Moose_Wala_500x500.jpg", listOf("Punjabi"), listOf("Hip-Hop & Rap")),
    CuratedArtist("Guru Randhawa", "https://c.saavncdn.com/artists/Guru_Randhawa_500x500.jpg", listOf("Punjabi", "Hindi"), listOf("Pop")),
    CuratedArtist("Anirudh Ravichander", "https://c.saavncdn.com/artists/Anirudh_Ravichander_500x500.jpg", listOf("Tamil", "Telugu"), listOf("Pop", "EDM")),
    CuratedArtist("Sid Sriram", "https://c.saavncdn.com/artists/Sid_Sriram_500x500.jpg", listOf("Tamil", "Telugu"), listOf("Romance", "Indie")),
    CuratedArtist("Devi Sri Prasad", "https://c.saavncdn.com/artists/Devi_Sri_Prasad_500x500.jpg", listOf("Telugu", "Tamil"), listOf("Pop")),
    CuratedArtist("Thaman S", "https://c.saavncdn.com/artists/Thaman_S_500x500.jpg", listOf("Telugu", "Tamil"), listOf("Pop", "EDM")),

    // ── K-Pop & Japanese / Anime ──
    CuratedArtist("BTS", "https://c.saavncdn.com/artists/BTS_500x500.jpg", listOf("K-Pop"), listOf("Pop", "Hip-Hop & Rap")),
    CuratedArtist("BLACKPINK", "https://c.saavncdn.com/artists/BLACKPINK_500x500.jpg", listOf("K-Pop"), listOf("Pop", "EDM")),
    CuratedArtist("Jung Kook", "https://c.saavncdn.com/artists/Jung_Kook_500x500.jpg", listOf("K-Pop", "English"), listOf("Pop", "R&B")),
    CuratedArtist("NewJeans", "https://c.saavncdn.com/artists/NewJeans_500x500.jpg", listOf("K-Pop"), listOf("Pop", "R&B")),
    CuratedArtist("YOASOBI", "https://c.saavncdn.com/artists/YOASOBI_500x500.jpg", listOf("Japanese / Anime"), listOf("Pop", "Rock")),

    // ── Regional Folk & Spanish ──
    CuratedArtist("Pawan Singh", "https://c.saavncdn.com/artists/Pawan_Singh_500x500.jpg", listOf("Bhojpuri"), listOf("Pop")),
    CuratedArtist("Khesari Lal Yadav", "https://c.saavncdn.com/artists/Khesari_Lal_Yadav_500x500.jpg", listOf("Bhojpuri"), listOf("Pop")),
    CuratedArtist("Masoom Sharma", "https://c.saavncdn.com/artists/Masoom_Sharma_500x500.jpg", listOf("Haryanvi"), listOf("Hip-Hop & Rap")),
    CuratedArtist("Renuka Panwar", "https://c.saavncdn.com/artists/Renuka_Panwar_500x500.jpg", listOf("Haryanvi"), listOf("Pop")),
    CuratedArtist("Ajay-Atul", "https://c.saavncdn.com/artists/Ajay-Atul_500x500.jpg", listOf("Marathi", "Hindi"), listOf("Bollywood")),
    CuratedArtist("Anupam Roy", "https://c.saavncdn.com/artists/Anupam_Roy_500x500.jpg", listOf("Bengali", "Hindi"), listOf("Indie")),
    CuratedArtist("Bad Bunny", "https://c.saavncdn.com/artists/Bad_Bunny_500x500.jpg", listOf("Spanish"), listOf("Hip-Hop & Rap", "Pop"))
)

private val ALL_LANGUAGES = listOf(
    "Hindi", "English", "Punjabi", "Tamil", "Telugu", "Bhojpuri",
    "Haryanvi", "K-Pop", "Marathi", "Bengali", "Spanish", "Japanese / Anime"
)

private val ALL_GENRES = listOf(
    "Pop", "Hip-Hop & Rap", "Bollywood", "Indie", "R&B", "Lo-Fi & Chill",
    "Rock", "Romance", "EDM", "Phonk", "Workout", "Retro Classics"
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OnboardingTasteScreen(
    initialLanguages: Set<String> = emptySet(),
    initialGenres: Set<String> = emptySet(),
    initialArtists: Set<String> = emptySet(),
    canDismiss: Boolean = false,
    musicRepository: MusicRepository,
    onFinish: (languages: Set<String>, genres: Set<String>, artists: Set<String>, artistSeeds: List<ArtistSeed>) -> Unit,
    onDismiss: () -> Unit = {}
) {
    var currentStep by remember { mutableIntStateOf(1) }

    var selectedLanguages by remember {
        mutableStateOf(if (initialLanguages.isNotEmpty()) initialLanguages else setOf("Hindi", "English"))
    }
    var selectedGenres by remember {
        mutableStateOf(if (initialGenres.isNotEmpty()) initialGenres else setOf("Bollywood", "Pop"))
    }
    var selectedArtists by remember {
        mutableStateOf(initialArtists)
    }
    var selectedArtistSeeds by remember {
        val initialMap = mutableMapOf<String, ArtistSeed>()
        for (name in initialArtists) {
            val found = CURATED_ARTISTS.find { it.name.equals(name, ignoreCase = true) }
            val img = found?.imageUrl ?: ""
            initialMap[name] = ArtistSeed(id = name.lowercase().replace(Regex("[^a-z0-9]"), "_"), name = name, imageUrl = img)
        }
        mutableStateOf<Map<String, ArtistSeed>>(initialMap)
    }

    // Search state for Step 3
    var artistSearchQuery by remember { mutableStateOf("") }
    var searchedArtists by remember { mutableStateOf<List<CuratedArtist>>(emptyList()) }
    var isSearching by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var searchJob by remember { mutableStateOf<Job?>(null) }
    val focusManager = LocalFocusManager.current

    // Dynamically prioritize artists based on Step 1 & Step 2 selections, interleaving Global & Indian icons
    val displayedArtists = remember(selectedLanguages, selectedGenres, searchedArtists, artistSearchQuery, selectedArtists, selectedArtistSeeds) {
        if (artistSearchQuery.isNotBlank() && searchedArtists.isNotEmpty()) {
            val list = mutableListOf<CuratedArtist>()
            // Always keep currently selected artists visible at the top, preserving their imageUrl
            for (artistName in selectedArtists) {
                val seed = selectedArtistSeeds[artistName]
                val existing = if (seed != null && seed.imageUrl.isNotBlank()) {
                    CuratedArtist(name = seed.name, imageUrl = seed.imageUrl)
                } else {
                    CURATED_ARTISTS.find { it.name.equals(artistName, ignoreCase = true) }
                        ?: searchedArtists.find { it.name.equals(artistName, ignoreCase = true) }
                        ?: CuratedArtist(name = artistName, imageUrl = "")
                }
                if (list.none { it.name.equals(existing.name, ignoreCase = true) }) {
                    list.add(existing)
                }
            }
            for (artist in searchedArtists) {
                if (list.none { it.name.equals(artist.name, ignoreCase = true) }) {
                    list.add(artist)
                }
            }
            list
        } else {
            val globalPool = CURATED_ARTISTS.filter { it.languages.contains("English") || it.languages.contains("Spanish") || it.languages.contains("K-Pop") }
            val indianPool = CURATED_ARTISTS.filter { !globalPool.contains(it) }

            val scoredGlobal = globalPool.sortedByDescending { artist ->
                var s = 0
                if (selectedArtists.contains(artist.name)) s += 100
                if (artist.languages.any { selectedLanguages.contains(it) }) s += 10
                if (artist.genres.any { selectedGenres.contains(it) }) s += 5
                s
            }
            val scoredIndian = indianPool.sortedByDescending { artist ->
                var s = 0
                if (selectedArtists.contains(artist.name)) s += 100
                if (artist.languages.any { selectedLanguages.contains(it) }) s += 10
                if (artist.genres.any { selectedGenres.contains(it) }) s += 5
                s
            }

            // Interleave: 1 Global, 1 Indian, 1 Global, 1 Indian...
            val interleaved = mutableListOf<CuratedArtist>()
            val maxLen = maxOf(scoredGlobal.size, scoredIndian.size)
            for (idx in 0 until maxLen) {
                if (idx < scoredGlobal.size) interleaved.add(scoredGlobal[idx])
                if (idx < scoredIndian.size) interleaved.add(scoredIndian[idx])
            }

            // Ensure all selected artists are included at the top, with imageUrl preserved
            val finalList = mutableListOf<CuratedArtist>()
            for (artistName in selectedArtists) {
                val seed = selectedArtistSeeds[artistName]
                val found = if (seed != null && seed.imageUrl.isNotBlank()) {
                    CuratedArtist(name = seed.name, imageUrl = seed.imageUrl)
                } else {
                    CURATED_ARTISTS.find { it.name.equals(artistName, ignoreCase = true) }
                        ?: CuratedArtist(name = artistName, imageUrl = "")
                }
                finalList.add(found)
            }
            for (artist in interleaved) {
                if (finalList.none { it.name.equals(artist.name, ignoreCase = true) }) {
                    finalList.add(artist)
                }
            }
            finalList
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF000000))
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // ── Top Bar (Back / Close & Step Progress) ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (currentStep > 1) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141414))
                        .bouncyClickable { currentStep-- },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFFE0E0E0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else if (canDismiss) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF141414))
                        .bouncyClickable { onDismiss() },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = "Close",
                        tint = Color(0xFFE0E0E0),
                        modifier = Modifier.size(20.dp)
                    )
                }
            } else {
                Spacer(modifier = Modifier.width(38.dp))
            }

            Spacer(modifier = Modifier.weight(1f))

            Text(
                text = "$currentStep of 3",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF888888)
            )

            Spacer(modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(38.dp))
        }

        // ── Step Progress Indicator ──
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            for (i in 1..3) {
                val isActive = currentStep >= i
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.5.dp)
                        .clip(CircleShape)
                        .background(if (isActive) Color(0xFFE6E6E6) else Color(0xFF222222))
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // ── Step Content ──
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = currentStep,
                transitionSpec = {
                    if (targetState > initialState) {
                        (slideInHorizontally { width -> width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> -width } + fadeOut()
                        )
                    } else {
                        (slideInHorizontally { width -> -width } + fadeIn()).togetherWith(
                            slideOutHorizontally { width -> width } + fadeOut()
                        )
                    }
                },
                label = "onboarding_step"
            ) { step ->
                when (step) {
                    1 -> StepLanguages(
                        selectedLanguages = selectedLanguages,
                        onToggle = { lang ->
                            selectedLanguages = if (selectedLanguages.contains(lang)) {
                                selectedLanguages - lang
                            } else {
                                selectedLanguages + lang
                            }
                        }
                    )
                    2 -> StepGenres(
                        selectedGenres = selectedGenres,
                        onToggle = { genre ->
                            selectedGenres = if (selectedGenres.contains(genre)) {
                                selectedGenres - genre
                            } else {
                                selectedGenres + genre
                            }
                        }
                    )
                    3 -> StepArtists(
                        searchQuery = artistSearchQuery,
                        onSearchChange = { query ->
                            artistSearchQuery = query
                            searchJob?.cancel()
                            if (query.trim().length >= 2) {
                                isSearching = true
                                searchJob = scope.launch(Dispatchers.IO) {
                                    delay(300)
                                    try {
                                        val artists = ArtistRepository.searchArtists(query)
                                        withContext(Dispatchers.Main) {
                                            searchedArtists = artists
                                            isSearching = false
                                        }
                                    } catch (e: Exception) {
                                        withContext(Dispatchers.Main) {
                                            isSearching = false
                                        }
                                    }
                                }
                            } else {
                                searchedArtists = emptyList()
                                isSearching = false
                            }
                        },
                        isSearching = isSearching,
                        artists = displayedArtists,
                        selectedArtists = selectedArtists,
                        onToggleArtist = { artist ->
                            val name = artist.name
                            val newSet = selectedArtists.toMutableSet()
                            val newSeeds = selectedArtistSeeds.toMutableMap()
                            if (newSet.contains(name)) {
                                newSet.remove(name)
                                newSeeds.remove(name)
                            } else {
                                newSet.add(name)
                                newSeeds[name] = ArtistSeed(id = artist.id, name = name, imageUrl = artist.imageUrl)
                            }
                            selectedArtists = newSet
                            selectedArtistSeeds = newSeeds
                        },
                        onClearSearch = {
                            artistSearchQuery = ""
                            searchedArtists = emptyList()
                            focusManager.clearFocus()
                        }
                    )
                }
            }
        }

        // ── Bottom Fixed CTA Capsule ──
        val isStepValid = when (currentStep) {
            1 -> selectedLanguages.isNotEmpty()
            2 -> selectedGenres.size >= 2
            3 -> selectedArtists.size >= 3
            else -> false
        }

        val buttonText = when (currentStep) {
            1 -> if (selectedLanguages.isNotEmpty()) "Continue" else "Select at least 1 language"
            2 -> if (selectedGenres.size >= 2) "Continue" else "Select at least 2 vibes"
            3 -> if (selectedArtists.size >= 3) "Build my VYBE" else "Choose ${3 - selectedArtists.size} more"
            else -> "Continue"
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(CircleShape)
                    .background(
                        if (isStepValid) Color(0xFFE6E6E6) else Color(0xFF181818)
                    )
                    .then(
                        if (isStepValid) {
                            Modifier.bouncyClickable {
                                when (currentStep) {
                                    1 -> currentStep = 2
                                    2 -> currentStep = 3
                                    3 -> onFinish(selectedLanguages, selectedGenres, selectedArtists, selectedArtistSeeds.values.toList())
                                }
                            }
                        } else Modifier
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = buttonText,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isStepValid) Color(0xFF121212) else Color(0xFF666666)
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepLanguages(
    selectedLanguages: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Which languages\ndo you listen to?",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Select all that apply to shape your feed",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )
        Spacer(modifier = Modifier.height(28.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (language in ALL_LANGUAGES) {
                val isSelected = selectedLanguages.contains(language)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFE6E6E6) else Color(0xFF141414))
                        .bouncyClickable { onToggle(language) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = language,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF121212) else Color(0xFFE0E0E0)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun StepGenres(
    selectedGenres: Set<String>,
    onToggle: (String) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "What's your vibe?",
            fontSize = 30.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            lineHeight = 36.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose 2 or more vibes to personalize your radio",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )
        Spacer(modifier = Modifier.height(28.dp))

        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            for (genre in ALL_GENRES) {
                val isSelected = selectedGenres.contains(genre)
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(if (isSelected) Color(0xFFE6E6E6) else Color(0xFF141414))
                        .bouncyClickable { onToggle(genre) }
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = genre,
                        fontSize = 14.sp,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSelected) Color(0xFF121212) else Color(0xFFE0E0E0)
                    )
                }
            }
        }
    }
}

@Composable
private fun StepArtists(
    searchQuery: String,
    onSearchChange: (String) -> Unit,
    isSearching: Boolean,
    artists: List<CuratedArtist>,
    selectedArtists: Set<String>,
    onToggleArtist: (CuratedArtist) -> Unit,
    onClearSearch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 20.dp)
    ) {
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Choose 3 or more artists",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Tap favorite artists or search for any creator",
            fontSize = 14.sp,
            color = Color(0xFF888888)
        )
        Spacer(modifier = Modifier.height(16.dp))

        // ── Search Pill ──
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(46.dp)
                .clip(CircleShape)
                .background(Color(0xFF141414))
                .padding(horizontal = 14.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Rounded.Search,
                    contentDescription = "Search",
                    tint = Color(0xFF888888),
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(modifier = Modifier.weight(1f)) {
                    if (searchQuery.isEmpty()) {
                        Text(
                            text = "Search artists...",
                            fontSize = 14.sp,
                            color = Color(0xFF666666)
                        )
                    }
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchChange,
                        singleLine = true,
                        textStyle = TextStyle(
                            fontSize = 14.sp,
                            color = Color.White,
                            fontWeight = FontWeight.Medium
                        ),
                        cursorBrush = SolidColor(Color.White),
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search)
                    )
                }
                if (isSearching) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color(0xFF888888),
                        strokeWidth = 2.dp
                    )
                } else if (searchQuery.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .clip(CircleShape)
                            .bouncyClickable { onClearSearch() },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Close,
                            contentDescription = "Clear",
                            tint = Color(0xFFAAAAAA),
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // ── 3-Column Circular Artist Grid ──
        LazyVerticalGrid(
            columns = GridCells.Fixed(3),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(bottom = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(artists, key = { it.id }, contentType = { "artist_cell" }) { artist ->
                val isSelected = selectedArtists.contains(artist.name)
                ArtistGridItem(
                    artist = artist,
                    isSelected = isSelected,
                    onToggle = { onToggleArtist(artist) }
                )
            }
        }
    }
}

@Composable
private fun ArtistGridItem(
    artist: CuratedArtist,
    isSelected: Boolean,
    onToggle: () -> Unit
) {
    val context = LocalContext.current
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .bouncyClickable { onToggle() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(92.dp),
            contentAlignment = Alignment.Center
        ) {
            // Circular portrait
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(Color(0xFF141414))
                    .then(
                        if (isSelected) Modifier.border(2.5.dp, Color.White, CircleShape)
                        else Modifier
                    )
            ) {
                if (artist.imageUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artist.imageUrl)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = artist.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Person,
                            contentDescription = artist.name,
                            tint = Color(0xFF666666),
                            modifier = Modifier.size(44.dp)
                        )
                    }
                }
            }

            // Selection Checkmark Badge
            if (isSelected) {
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .align(Alignment.BottomEnd)
                        .clip(CircleShape)
                        .background(Color.White),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Check,
                        contentDescription = "Selected",
                        tint = Color(0xFF121212),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = artist.name,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            color = if (isSelected) Color.White else Color(0xFFD0D0D0),
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.fillMaxWidth()
        )
    }
}
