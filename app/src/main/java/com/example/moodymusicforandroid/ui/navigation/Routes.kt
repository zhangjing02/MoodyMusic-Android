package com.example.moodymusicforandroid.ui.navigation

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

@Serializable
data object RouteHome : NavKey

@Serializable
data object RouteDiscover : NavKey

@Serializable
data object RouteLibrary : NavKey

@Serializable
data class RouteArtistDetail(
    val artistId: String = "abigail_chen",
    val artistName: String = "阿比盖尔·陈"
) : NavKey

@Serializable
data class RouteAlbumDetail(
    val albumId: String = "forest_echo",
    val albumTitle: String = "林间碎影",
    val artistId: String = "",
    val artistName: String = ""
) : NavKey

@Serializable
data class RouteMusicDetail(
    val songId: String
) : NavKey

@Serializable
data class RouteThemeDetail(
    val themeId: String = "snow_cafe_theme",
    val title: String = "雪天咖啡館的閱讀鋼琴",
    val audioUrl: String = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/music/theme/snow_cafe_piano.mp3",
    val coverUrl: String = "https://pub-9ea7ff16135d47238c0229f1aa54ecc4.r2.dev/covers/home/snow_cafe_static.jpg",
    val artistName: String = "放鬆鋼琴 · 慢時光"
) : NavKey

@Serializable
data object RouteVersion : NavKey

@Serializable
data object RouteSettings : NavKey

@Serializable
data class RouteCollectionManager(
    val initialTab: Int = 0 // 0: 歌曲, 1: 专辑, 2: 歌手
) : NavKey



