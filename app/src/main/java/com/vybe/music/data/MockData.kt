package com.vybe.music.data

import com.vybe.music.model.Track

object MockData {
    val sampleTracks = listOf(
        Track(
            id = "1693891139",
            title = "One Of The Girls",
            artist = "The Weeknd, JENNIE & Lily Rose Depp",
            album = "The Idol Episode 4",
            durationMs = 244685L,
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/db/28/e7/db28e7c3-f53a-76c8-eaa4-d92a22e4f160/23UMGIM72115.rgb.jpg/1200x1200bb.jpg",
            mediaUri = "https://aac.saavncdn.com/262/cfee281799a04f2be345a57bdeef26de_320.mp4",
            defaultDominantColorHex = 0xFFE50914
        ),
        Track(
            id = "1495799403",
            title = "Levitating",
            artist = "Dua Lipa",
            album = "Future Nostalgia",
            durationMs = 203807L,
            artworkUrl = "https://is1-ssl.mzstatic.com/image/thumb/Music116/v4/6c/11/d6/6c11d681-aa3a-d59e-4c2e-f77e181026ab/190295092665.jpg/1200x1200bb.jpg",
            mediaUri = "https://aac.saavncdn.com/117/190295092665_320.mp4",
            defaultDominantColorHex = 0xFF9C27B0
        ),
        Track(
            id = "rjkrTnma",
            title = "Kesariya",
            artist = "Pritam, Arijit Singh",
            album = "Brahmastra",
            durationMs = 268000L,
            artworkUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/871/c2febd353f3a076a406fa37510f31f9f_320.mp4",
            defaultDominantColorHex = 0xFFFF5722
        ),
        Track(
            id = "aAOXwvz-",
            title = "Born to Shine",
            artist = "Diljit Dosanjh",
            album = "G.O.A.T.",
            durationMs = 214000L,
            artworkUrl = "https://c.saavncdn.com/597/G-O-A-T-Punjabi-2020-20240708055140-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/597/f1efd650819d3f427bd10e8b9addcd40_320.mp4",
            defaultDominantColorHex = 0xFF00BCD4
        ),
        Track(
            id = "DF6eazs2",
            title = "Winning Speech",
            artist = "Karan Aujla",
            album = "Winning Speech",
            durationMs = 227000L,
            artworkUrl = "https://c.saavncdn.com/089/Winning-Speech-Punjabi-2024-20260626013220-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/089/64beffa430e4c948223ec6bfcc3a13f0_320.mp4",
            defaultDominantColorHex = 0xFFE91E63
        ),
        Track(
            id = "aRZbUYD7",
            title = "Tum Hi Ho",
            artist = "Mithoon, Arijit Singh",
            album = "Aashiqui 2",
            durationMs = 262000L,
            artworkUrl = "https://c.saavncdn.com/430/Aashiqui-2-Hindi-2013-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/430/5c5ea5cc00e3bff45616013226f376fe_320.mp4",
            defaultDominantColorHex = 0xFFD32F2F
        ),
        Track(
            id = "M7k5t7vw",
            title = "Lover",
            artist = "Diljit Dosanjh",
            album = "MoonChild Era",
            durationMs = 190000L,
            artworkUrl = "https://c.saavncdn.com/209/MoonChild-Era-Punjabi-2021-20240715073449-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/209/88cd9a1cc0af8768d67272876bb09851_320.mp4",
            defaultDominantColorHex = 0xFF9C27B0
        ),
        Track(
            id = "CVeqCCYc",
            title = "Tauba Tauba",
            artist = "Karan Aujla",
            album = "Bad Newz",
            durationMs = 207000L,
            artworkUrl = "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20250730113701-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/992/5d44da8bc1d78fb72d18b701d758fd1f_320.mp4",
            defaultDominantColorHex = 0xFFFF9800
        ),
        Track(
            id = "9oeSYVb7",
            title = "Story of a Bird",
            artist = "Alan Walker, King",
            album = "Story of a Bird",
            durationMs = 230000L,
            artworkUrl = "https://c.saavncdn.com/264/Story-of-a-Bird-Hindi-2025-20260715071550-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/264/6d18d4f877b222330db1f95b2d37a7b7_320.mp4",
            defaultDominantColorHex = 0xFF1976D2
        ),
        Track(
            id = "xzUVX40K",
            title = "Brown Munde",
            artist = "AP Dhillon",
            album = "Brown Munde",
            durationMs = 254000L,
            artworkUrl = "https://c.saavncdn.com/973/Brown-Munde-English-2020-20260520131422-500x500.jpg",
            mediaUri = "https://aac.saavncdn.com/973/76216adb3df5ef476f948891b40efb7a_320.mp4",
            defaultDominantColorHex = 0xFF4CAF50
        )
    )

    data class Playlist(
        val id: String,
        val title: String,
        val description: String,
        val artworkUrl: String,
        val trackCount: Int,
        val dominantColorHex: Long
    )

    val samplePlaylists = listOf(
        Playlist(
            id = "pl_1",
            title = "Midnight Melodies",
            description = "Arijit Singh & Pritam emotional late-night hits",
            artworkUrl = "https://c.saavncdn.com/871/Brahmastra-Original-Motion-Picture-Soundtrack-Hindi-2022-20221006155213-500x500.jpg",
            trackCount = 42,
            dominantColorHex = 0xFF8E24AA
        ),
        Playlist(
            id = "pl_2",
            title = "Punjabi Fire",
            description = "High BPM bangers from Diljit Dosanjh & Karan Aujla",
            artworkUrl = "https://c.saavncdn.com/597/G-O-A-T-Punjabi-2020-20240708055140-500x500.jpg",
            trackCount = 38,
            dominantColorHex = 0xFFD84315
        ),
        Playlist(
            id = "pl_3",
            title = "Electronic Horizons",
            description = "Alan Walker & global bass anthems",
            artworkUrl = "https://c.saavncdn.com/264/Story-of-a-Bird-Hindi-2025-20260715071550-500x500.jpg",
            trackCount = 65,
            dominantColorHex = 0xFF1565C0
        ),
        Playlist(
            id = "pl_4",
            title = "Pure Energy",
            description = "Top chart releases and official party anthems",
            artworkUrl = "https://c.saavncdn.com/992/Bad-Newz-Hindi-2024-20250730113701-500x500.jpg",
            trackCount = 50,
            dominantColorHex = 0xFF00897B
        )
    )

    data class GenreCategory(
        val name: String,
        val gradientStartHex: Long,
        val gradientEndHex: Long
    )

    val genreCategories = listOf(
        GenreCategory("Pop", 0xFFE91E63, 0xFF9C27B0),
        GenreCategory("Hip-Hop", 0xFFFF5722, 0xFFE91E63),
        GenreCategory("Electronic", 0xFF00BCD4, 0xFF3F51B5),
        GenreCategory("Bollywood", 0xFFF44336, 0xFF673AB7),
        GenreCategory("Punjabi", 0xFF9C27B0, 0xFF3F51B5),
        GenreCategory("Indie", 0xFF4CAF50, 0xFF009688),
        GenreCategory("Soul & Acoustic", 0xFF3F51B5, 0xFF2196F3),
        GenreCategory("Party", 0xFFFF9800, 0xFFF44336)
    )
}
