package com.example.moodymusicforandroid

import com.example.moodymusicforandroid.common.crypto.ApiCryptoManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ApiCryptoTest {

    @Test
    fun testGenerateAesKey() {
        val key = ApiCryptoManager.generateAesKey()
        assertEquals(32, key.size)
    }

    @Test
    fun testRsaEncryptAesKey() {
        val rawKey = ApiCryptoManager.generateAesKey()
        val encryptedKey = ApiCryptoManager.encryptAesKeyWithRsa(rawKey)
        assertNotNull(encryptedKey)
        assertTrue(encryptedKey.isNotEmpty())

        val decodedBytes = ApiCryptoManager.base64Decode(encryptedKey)
        assertEquals(256, decodedBytes.size) // 2048 位 RSA 密文长度为 256 字节
    }

    @Test
    fun testAesGcmEncryptAndDecrypt() {
        val rawKey = ApiCryptoManager.generateAesKey()
        val plainJson = """{"username":"test_user","password":"test_password_123","timestamp":1728364800}"""

        val encrypted = ApiCryptoManager.encryptPayload(plainJson, rawKey)
        assertNotNull(encrypted.payload)
        assertNotNull(encrypted.iv)

        val decrypted = ApiCryptoManager.decryptPayload(encrypted.payload, encrypted.iv, rawKey)
        assertEquals(plainJson, decrypted)
    }
}
