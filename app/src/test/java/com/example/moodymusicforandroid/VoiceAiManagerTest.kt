package com.example.moodymusicforandroid

import com.example.moodymusicforandroid.ui.home.voice.HanziConverter
import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceAiManagerTest {

    @Test
    fun testHanziConverter() {
        assertEquals("周杰伦", HanziConverter.toSimplified("周杰倫"))
        assertEquals("陈奕迅", HanziConverter.toSimplified("陳奕迅"))
        assertEquals("刘德华", HanziConverter.toSimplified("劉德華"))
        assertEquals("张学友", HanziConverter.toSimplified("張學友"))
        assertEquals("孙燕姿", HanziConverter.toSimplified("孫燕姿"))
        assertEquals("请播放周杰伦的歌", HanziConverter.toSimplified("請播放周杰倫的歌"))
        assertEquals("我想听周华健的朋友", HanziConverter.toSimplified("我想聽周華健的朋友"))
        assertEquals("爱相随", HanziConverter.toSimplified("愛相隨"))
    }
}
