package pt.isel.leic.multicloudguardian.service.security

import jakarta.inject.Named
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.slf4j.LoggerFactory
import pt.isel.leic.multicloudguardian.domain.user.UsersDomain
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.Security
import java.util.Base64
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Contains the logic for encrypting and decrypting data.
 * The encryption algorithm used is AES-GCM.
 * The key size is 256 bits.
 * The IV size is 12 bytes.
 * The tag length is 128 bits.
 * The padding used is NoPadding.
 * The security provider used is Bouncy Castle.
 */

private const val ALGORITHM = "AES/GCM/NoPadding"
private const val GCM_TAG_LENGTH = 128 // 128 bits (16 bytes)
private const val IV_SIZE = 12 // 12 bytes is the recommended size for AES-GCM
private const val KEY_SIZE = 256
private const val PROVIDER = "BC"
private const val AES_ALGORITHM = "AES"
private const val SHA_ALGORITHM = "SHA-256"

sealed class TypeCrypto {
    data object Encryption : TypeCrypto()

    data object Decryption : TypeCrypto()
}

@Named
class SecurityService(
    private val usersDomain: UsersDomain,
) {
    init {
        // Registers Bouncy Castle as a security provider
        Security.addProvider(BouncyCastleProvider())
    }

    // Generates a 256-bit AES key
    fun generationKeyAES(): SecretKey {
        val keyGen = KeyGenerator.getInstance(AES_ALGORITHM, PROVIDER)
        keyGen.init(KEY_SIZE)
        return keyGen.generateKey()
    }

    // Generates a random IV of 12 bytes
    // Generate a different IV for each encryption
    fun generationIV(): ByteArray {
        val iv = ByteArray(IV_SIZE)
        SecureRandom().nextBytes(iv)
        return iv
    }

    // Encrypt a data using AES-GCM
    fun encrypt(
        data: ByteArray,
        key: SecretKey,
        iv: ByteArray,
    ): ByteArray? {
        try {
            return fileCryptoHandler(data, key, iv, TypeCrypto.Encryption)
        } catch (error: Exception) {
            logger.info("Failed to encrypt data", error)
            return null
        }
    }

    // Decrypt an encrypted data
    fun decrypt(
        data: ByteArray,
        key: SecretKey,
        iv: ByteArray,
    ): ByteArray? {
        try {
            return fileCryptoHandler(data, key, iv, TypeCrypto.Decryption)
        } catch (error: Exception) {
            logger.info("Failed to decrypt data", error)
            return null
        }
    }

    fun calculateChecksum(fileBytes: ByteArray): Long {
        val digest = MessageDigest.getInstance(SHA_ALGORITHM)
        val hash = digest.digest(fileBytes)
        return hash.take(8).fold(0L) { acc, byte ->
            acc * 31 + byte.toLong()
        } and 0x7FFFFFFFFFFFFFFFL
    }

    fun secretKeyToString(secretKey: SecretKey): String = Base64.getEncoder().encodeToString(secretKey.encoded)

    fun convertStringToSecretKey(encodedKey: String): SecretKey? {
        try {
            val decodedKey = Base64.getDecoder().decode(encodedKey)
            return SecretKeySpec(decodedKey, 0, decodedKey.size, AES_ALGORITHM)
        } catch (error: IllegalArgumentException) {
            logger.info("Failed to convert string to secret key", error)
            return null
        }
    }

    private fun fileCryptoHandler(
        data: ByteArray,
        key: SecretKey,
        iv: ByteArray,
        type: TypeCrypto,
    ): ByteArray {
        try {
            val cipher = Cipher.getInstance(ALGORITHM, PROVIDER)
            val gcmSpec = GCMParameterSpec(GCM_TAG_LENGTH, iv)
            val cipherType =
                when (type) {
                    is TypeCrypto.Encryption -> Cipher.ENCRYPT_MODE
                    is TypeCrypto.Decryption -> Cipher.DECRYPT_MODE
                }
            cipher.init(cipherType, key, gcmSpec)
            return cipher.doFinal(data)
        } catch (error: Exception) {
            logger.info("Failed to handle file crypto", error)
            throw error
        }
    }

    private val logger = LoggerFactory.getLogger(SecurityService::class.java)
}
