package com.example.moodymusicforandroid.common.crypto

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

/**
 * MOODY 端到端全链路混合信封加密引擎 (Android 原生纯 JDK 实现)
 * 对齐 MoodyMusic-Server/cloudflare-worker/src/crypto.ts
 *
 * 架构：
 * 1. RSA-2048 (RSA-OAEP-SHA256) 会话握手：客户端每次请求动态生成 32 字节 AES 会话密钥并由服务端公钥封装；
 * 2. AES-256-GCM 载荷加解密：请求 Body 与响应 Body 均由该一次性会话秘钥透明加解密；
 * 3. 零第三方依赖：原生基于 javax.crypto 标准库，硬件加速，延迟 < 1ms。
 */
object ApiCryptoManager {

    const val DEFAULT_RSA_PUBLIC_KEY = """-----BEGIN PUBLIC KEY-----
MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAo+PGbCPY6AkZmGp0qn3I
5H52lgi9pIscVRsqtu/SukEpSM7OatPiPCBVg/Zk86+6PpgSNR8rmeWUaP6jDCRP
37S1hWcuD52TSpfVCyqgJ0E897aObaVSBjJAS2jJnrClwk4rJ7xzVgKhOwo6h4Lb
25GJcJ8ZnpiESBf8EXaZskuBuiFGQ1GDwXHipdlh8bkdpxKOv99KY89eS2vc+OqQ
LwHYliAz6fYSgtl4hb5+nIsQuxTrPtqjp97dF1XIXnltY3YkCf4LybuAn7NFfU99
X8Px27lCepYwJJ8/WRVZw/LRkV5gU4AYGAYwhvrGvrMn0PSqJN5/24WTlNTyXqNI
MQIDAQAB
-----END PUBLIC KEY-----"""

    private val publicKey: PublicKey by lazy {
        val cleanPem = DEFAULT_RSA_PUBLIC_KEY
            .replace("-----BEGIN PUBLIC KEY-----", "")
            .replace("-----END PUBLIC KEY-----", "")
            .replace("\\s+".toRegex(), "")
        val keyBytes = base64Decode(cleanPem)
        val spec = X509EncodedKeySpec(keyBytes)
        KeyFactory.getInstance("RSA").generatePublic(spec)
    }

    data class EncryptedPayload(val payload: String, val iv: String)

    /**
     * 生成 32 字节 (256 位) 安全随机 AES 会话密钥
     */
    fun generateAesKey(): ByteArray {
        val key = ByteArray(32)
        SecureRandom().nextBytes(key)
        return key
    }

    /**
     * 使用服务端 RSA-2048 公钥 (RSA-OAEP-SHA256) 加密 AES 会话密钥
     * 输出 Base64 编码供 HTTP 标头 x-encrypted-key 使用
     */
    fun encryptAesKeyWithRsa(rawAesKey: ByteArray): String {
        val cipher = Cipher.getInstance("RSA/ECB/OAEPWithSHA-256AndMGF1Padding")
        val oaepParams = OAEPParameterSpec(
            "SHA-256",
            "MGF1",
            MGF1ParameterSpec.SHA256,
            PSource.PSpecified.DEFAULT
        )
        cipher.init(Cipher.ENCRYPT_MODE, publicKey, oaepParams)
        val encrypted = cipher.doFinal(rawAesKey)
        return base64Encode(encrypted)
    }

    /**
     * 使用 AES-256-GCM 加密请求明文 JSON
     */
    fun encryptPayload(plainJson: String, rawAesKey: ByteArray): EncryptedPayload {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        val keySpec = SecretKeySpec(rawAesKey, "AES")
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        val cipherBytes = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
        return EncryptedPayload(
            payload = base64Encode(cipherBytes),
            iv = base64Encode(iv)
        )
    }

    /**
     * 使用 AES-256-GCM 解密服务端响应密文
     */
    fun decryptPayload(payloadBase64: String, ivBase64: String, rawAesKey: ByteArray): String {
        val cipherBytes = base64Decode(payloadBase64)
        val iv = base64Decode(ivBase64)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        val keySpec = SecretKeySpec(rawAesKey, "AES")
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        val plainBytes = cipher.doFinal(cipherBytes)
        return String(plainBytes, Charsets.UTF_8)
    }

    fun base64Decode(str: String): ByteArray {
        val clean = str.trim()
        return try {
            java.util.Base64.getDecoder().decode(clean)
        } catch (_: Throwable) {
            android.util.Base64.decode(clean, android.util.Base64.DEFAULT)
        }
    }

    fun base64Encode(bytes: ByteArray): String {
        return try {
            java.util.Base64.getEncoder().encodeToString(bytes)
        } catch (_: Throwable) {
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        }
    }
}
