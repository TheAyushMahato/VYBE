package com.vybe.music.data.repository

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class GlobalChartRepositoryTest {

    @Test
    fun `parseDeezerChartJson filters out remix, karaoke, compilation, duplicate entries and returns exact top 5 with 1000x1000 HD artwork`() {
        val mockDeezerJson = """
        {
          "data": [
            {
              "id": 101,
              "title": "Choosin' Texas",
              "title_short": "Choosin' Texas",
              "duration": 232,
              "artist": { "id": 1, "name": "Ella Langley" },
              "album": {
                "id": 11,
                "title": "Hungover",
                "cover_xl": "https://cdn-images.dzcdn.net/images/cover/749ad2ad7609ec768e342bec41078095/1000x1000-000000-80-0-0.jpg"
              }
            },
            {
              "id": 102,
              "title": "Choosin' Texas (Club Remix)",
              "title_short": "Choosin' Texas (Club Remix)",
              "duration": 210,
              "artist": { "id": 1, "name": "Ella Langley" },
              "album": {
                "id": 12,
                "title": "Remixes EP",
                "cover_xl": "https://cdn-images.dzcdn.net/images/cover/remix/1000x1000.jpg"
              }
            },
            {
              "id": 103,
              "title": "Choosin' Texas - Karaoke Version",
              "title_short": "Choosin' Texas (Karaoke)",
              "duration": 232,
              "artist": { "id": 99, "name": "Karaoke All Stars" },
              "album": {
                "id": 13,
                "title": "Country Karaoke",
                "cover_xl": "https://cdn-images.dzcdn.net/images/cover/karaoke/1000x1000.jpg"
              }
            },
            {
              "id": 104,
              "title": "Choosin' Texas",
              "title_short": "Choosin' Texas",
              "duration": 232,
              "artist": { "id": 1, "name": "Ella Langley" },
              "album": {
                "id": 11,
                "title": "Hungover Deluxe",
                "cover_xl": "https://cdn-images.dzcdn.net/images/cover/duplicate/1000x1000.jpg"
              }
            },
            {
              "id": 105,
              "title": "Boston",
              "title_short": "Boston",
              "duration": 185,
              "artist": { "id": 2, "name": "STELLA LEFTY" },
              "album": {
                "id": 21,
                "title": "Boston Single",
                "cover_big": "https://cdn-images.dzcdn.net/images/cover/66d6baca/500x500-000000-80-0-0.jpg"
              }
            },
            {
              "id": 106,
              "title": "YRF Hits (Compilation Album)",
              "title_short": "YRF Hits",
              "duration": 300,
              "artist": { "id": 3, "name": "Various Artists" },
              "album": {
                "id": 31,
                "title": "Bollywood Blockbusters Compilation",
                "cover_xl": "https://cdn-images.dzcdn.net/images/cover/comp/1000x1000.jpg"
              }
            },
            {
              "id": 107,
              "title": "BbY WOW",
              "title_short": "BbY WOW",
              "duration": 195,
              "artist": { "id": 4, "name": "KAROL G" },
              "album": {
                "id": 41,
                "title": "BbY WOW",
                "cover_xl": "https://cdn-images.dzcdn.net/images/cover/f794dd/1000x1000-000000-80-0-0.jpg"
              }
            },
            {
              "id": 108,
              "title": "Nicole Kidman (Instrumental)",
              "title_short": "Nicole Kidman",
              "duration": 160,
              "artist": { "id": 5, "name": "ADÉLA" },
              "album": {
                "id": 51,
                "title": "Instrumentals",
                "cover_xl": "https://cdn-images.dzcdn.net/images/cover/inst/1000x1000.jpg"
              }
            },
            {
              "id": 109,
              "title": "Nicole Kidman",
              "title_short": "Nicole Kidman",
              "duration": 160,
              "artist": { "id": 5, "name": "ADÉLA" },
              "album": {
                "id": 52,
                "title": "Nicole Kidman Single",
                "cover_xl": "https://cdn-images.dzcdn.net/images/cover/68502d87/1000x1000-000000-80-0-0.jpg"
              }
            },
            {
              "id": 110,
              "title": "Dracula (with JENNIE)",
              "title_short": "Dracula",
              "duration": 210,
              "artist": { "id": 6, "name": "Tame Impala" },
              "album": {
                "id": 61,
                "title": "Dracula",
                "cover_xl": "https://cdn-images.dzcdn.net/images/cover/b868399d/1000x1000-000000-80-0-0.jpg"
              }
            },
            {
              "id": 111,
              "title": "Housewife",
              "title_short": "Housewife",
              "duration": 175,
              "artist": { "id": 7, "name": "Tate McRae" },
              "album": {
                "id": 71,
                "title": "Housewife Single",
                "cover_xl": "https://cdn-images.dzcdn.net/images/cover/0717657/1000x1000-000000-80-0-0.jpg"
              }
            }
          ]
        }
        """.trimIndent()

        val parsed = GlobalChartRepository.parseDeezerChartJson(mockDeezerJson)

        // 1. Must return exact top 5 ranked tracks
        assertEquals(5, parsed.size)

        // 2. Rank order & titles
        assertEquals("Choosin' Texas", parsed[0].title)
        assertEquals("Ella Langley", parsed[0].artist)

        assertEquals("Boston", parsed[1].title)
        assertEquals("STELLA LEFTY", parsed[1].artist)

        assertEquals("BbY WOW", parsed[2].title)
        assertEquals("KAROL G", parsed[2].artist)

        assertEquals("Nicole Kidman", parsed[3].title)
        assertEquals("ADÉLA", parsed[3].artist)

        assertEquals("Dracula", parsed[4].title)
        assertEquals("Tame Impala", parsed[4].artist)

        // 3. Verify no filtered keywords exist in titles or albums
        for (track in parsed) {
            val lowerTitle = track.title.lowercase()
            val lowerAlbum = track.album.lowercase()
            assertFalse(lowerTitle.contains("remix"))
            assertFalse(lowerTitle.contains("karaoke"))
            assertFalse(lowerTitle.contains("compilation"))
            assertFalse(lowerTitle.contains("instrumental"))
            assertFalse(lowerAlbum.contains("remix"))
            assertFalse(lowerAlbum.contains("karaoke"))
            assertFalse(lowerAlbum.contains("compilation"))
            assertFalse(lowerAlbum.contains("instrumental"))

            // 4. Verify 1000x1000 HD artwork
            assertTrue(
                "Artwork must be 1000x1000 HD, but got: ${track.artworkUrl}",
                track.artworkUrl.contains("1000x1000")
            )
        }
    }
}
