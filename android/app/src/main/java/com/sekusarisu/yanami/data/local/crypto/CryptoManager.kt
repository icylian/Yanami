package com.sekusarisu.yanami.data.local.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import android.util.Base64
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

/**
 * API Key 加密管理器
 *
 * 使用 Android KeyStore + AES/GCM 对 API Key 进行加密存储。
 * - 密钥永驻 KeyStore 中，应用卸载后密钥自动销毁
 * - GCM 模式提供认证加密，防止篡改
 * - IV 与密文一起存储（Base64 编码）
 */
class CryptoManager {

    companion object {
        private const val KEY_ALIAS = "yanami_api_key_alias"
        private const val ANDROID_KEY_STORE = "AndroidKeyStore"
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
        private const val GCM_TAG_LENGTH = 128
        private const val IV_SEPARATOR = ":"
    }

    /**
     * 加密明文 API Key
     *
     * @param plainText 明文 API Key
     * @return Base64 编码的 "IV:密文" 字符串
     */
    fun encrypt(plainText: String): String {
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())

        val iv = cipher.iv
        val encryptedBytes = cipher.doFinal(plainText.toByteArray(Charsets.UTF_8))

        val ivBase64 = Base64.encodeToString(iv, Base64.NO_WRAP)
        val cipherBase64 = Base64.encodeToString(encryptedBytes, Base64.NO_WRAP)

        return "$ivBase64$IV_SEPARATOR$cipherBase64"
    }

    /**
     * 解密已加密的 API Key
     *
     * @param cipherText Base64 编码的 "IV:密文" 字符串
     * @return 明文 API Key
     */
    fun decrypt(cipherText: String): String {
        val parts = cipherText.split(IV_SEPARATOR, limit = 2)
        require(parts.size == 2) { "Invalid encrypted data format" }

        val iv = Base64.decode(parts[0], Base64.NO_WRAP)
        val encryptedBytes = Base64.decode(parts[1], Base64.NO_WRAP)

        val cipher = Cipher.getInstance(TRANSFORMATION)
        val spec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
        cipher.init(Cipher.DECRYPT_MODE, getOrCreateKey(), spec)

        val decryptedBytes = cipher.doFinal(encryptedBytes)
        return String(decryptedBytes, Charsets.UTF_8)
    }

    /** 获取或创建 KeyStore 中的 AES 密钥 */
    private fun getOrCreateKey(): SecretKey {
        val keyStore = KeyStore.getInstance(ANDROID_KEY_STORE)
        keyStore.load(null)

        // 如果密钥已存在，直接返回
        keyStore.getEntry(KEY_ALIAS, null)?.let { entry ->
            return (entry as KeyStore.SecretKeyEntry).secretKey
        }

        // 生成新密钥
        val keyGenerator =
                KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE)
        val keySpec =
                KeyGenParameterSpec.Builder(
                                KEY_ALIAS,
                                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
                        )
                        .setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                        .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                        .setKeySize(256)
                        .build()

        keyGenerator.init(keySpec)
        return keyGenerator.generateKey()
    }
}
