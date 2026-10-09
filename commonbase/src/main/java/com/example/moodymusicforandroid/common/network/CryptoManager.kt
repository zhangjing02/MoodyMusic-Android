package com.example.moodymusicforandroid.common.network

import android.util.Base64
import java.security.KeyFactory
import java.security.PublicKey
import java.security.SecureRandom
import java.security.spec.MGF1ParameterSpec
import java.security.spec.X509EncodedKeySpec
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.OAEPParameterSpec
import javax.crypto.spec.PSource
import javax.crypto.spec.SecretKeySpec

object CryptoManager {

    private const val SERVER_RSA_PUBLIC_KEY_PEM = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAo+PGbCPY6AkZmGp0qn3I
5H52lgi9pIscVRsqtu/SukEpSM7OatPiPCBVg/Zk86+6PpgSNR8rmeWUaP6jDCRP
37S1hWcuD52TSpfVCyqgJ0E897aObaVSBjJAS2jJnrClwk4rJ7xzVgKhOwo6h4Lb
25GJcJ8ZnpiESBf8EXaZskuBuiFGQ1GDwXHipdlh8bkdpxKOv99KY89eS2vc+OqQ
LwHYliAz6fYSgtl4hb5+nIsQuxTrPtqjp97dF1XIXnltY3YkCf4LybuAn7NFfU99
X8Px27lCepYwJJ8/WRVZw/LRkV5gU4AYGAYwhvrGvrMn0PSqJN5/24WTlNTyXqNI
MQIDAQAB
-----END PUBLIC KEY-----"""

    @Volatile
    private var cachedPublicKey: PublicKey? = null

    fun getPublicKey(): PublicKey {
        cachedPublicKey?.let { return it }
        synchronized(this) {
            cachedPublicKey?.let { return it }
            val cleanKey = SERVER_RSA_PUBLIC_KEY_PEM
                .replace("-----BEGIN PUBLIC KEY-----", "")
                .replace("-----END PUBLIC KEY-----", "")
                .replace("\\s+".toRegex(), "")
            val keyBytes = Base64.decode(cleanKey, Base64.NO_WRAP)
            val spec = X509EncodedKeySpec(keyBytes)
            val key = KeyFactory.getInstance("RSA").generatePublic(spec)
            cachedPublicKey = key
            return key
        }
    }

    /**
     * 生成随机 32 字节 (256-bit) AES 对称会话密钥
     */
    fun generateAesKey(): ByteArray {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return key
    }

    /**
     * 使用服务端 RSA-2048 公钥 (RSA-OAEP-SHA256) 加密一次性 AES 密钥
     */
    fun encryptRsaKey(rawAesKey: ByteArray): String {
        val publicKey = getPublicKey()
        val cipher = Cipher.getInstance("RSA/ECB/OAEPPadding")
        val oaepParams = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams)
        val encrypted = cipher.doFinal(rawAesKey)
        return Base64.encodeToString(encrypted, Base64.NO_WRAP)
    }

    /**
     * 使用 AES-256-GCM 解密服务端响应 Payload
     */
    fun decryptPayload(ciphertextB64: String, ivB64: String, aesKey: ByteArray): String {
        val ciphertext = Base64.decode(ciphertextB64.trim(), Base64.NO_WRAP)
        val iv = Base64.decode(ivB64.trim(), Base64.NO_WRAP)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(aesKey, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val decryptedBytes = cipher.doFinal(ciphertext)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /**
     * 使用 AES-256-GCM 加密请求内容
     * @return Pair(payloadBase64, ivBase64)
     */
    fun encryptPayload(plaintext: String, aesKey: ByteArray): Pair<String, String> {
        val iv = ByteArray(12).apply { SecureRandom().nextBytes(this) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val keySpec = SecretKeySpec(aesKey, "AES")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val encryptedBytes = cipher.doFinal(plaintext.toByteArray(Charsets.UTF_8))
        return Pair(
            Base64.encodeToString(encryptedBytes, Base64.NO_WRAP),
            Base64.encodeToString(iv, Base64.NO_WRAP)
        )
    }
}
