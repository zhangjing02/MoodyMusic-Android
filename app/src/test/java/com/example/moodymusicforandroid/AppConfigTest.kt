package com.example.moodymusicforandroid

import com.example.moodymusicforandroid.common.config.AppConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AppConfigTest {

    @Test
    fun testR2DomainCanonicalization() {
        val rawR2Url = "https://pub-147987db1e7b419cb6ea49acd48d0d25.r2.dev/music/孙燕姿/孙燕姿同名专辑/s_23832.mp3"
        val canonical = AppConfig.canonicalizeUrl(rawR2Url)
        assertTrue(canonical.startsWith("https://moody-music-gateway.netlify.app/r2-proxy/pub-147987db1e7b419cb6ea49acd48d0d25/"))
        assertTrue(canonical.endsWith("s_23832.mp3"))

        val rawLrcUrl = "https://pub-147987db1e7b419cb6ea49acd48d0d25.r2.dev/lyrics/孙燕姿/孙燕姿同名专辑/s_23832.lrc"
        val canonicalLrc = AppConfig.canonicalizeUrl(rawLrcUrl)
        assertTrue(canonicalLrc.startsWith("https://moody-music-gateway.netlify.app/r2-proxy/pub-147987db1e7b419cb6ea49acd48d0d25/lyrics/"))
    }

    @Test
    fun testVinylDefaultFallback() {
        val resolved = AppConfig.resolveUrl("/src/assets/images/vinyl_default.png")
        assertEquals("", resolved)

        val resolved2 = AppConfig.resolveUrl("vinyl_default.png")
        assertEquals("", resolved2)
    }
}
