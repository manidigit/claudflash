package com.flashlearn.data.backup

import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.inject.Inject

/**
 * Backup file encryption (Descriptions §16.2, Phase 38 decision).
 *
 * The spec offers two key sources: "کلیدی از Android Keystore یا کلیدی
 * مشتق از ورودی کاربر (مثل PIN)". This deliberately uses the **PIN**
 * option, not Android Keystore, for a concrete reason: an
 * AndroidKeyStore-backed key is tied to one app install on one device
 * and is not guaranteed to survive an app uninstall — which would make
 * an "encrypted backup" silently undecryptable after the exact kind of
 * event (reinstall, new device) a backup exists to protect against. A
 * PIN-derived key has no such trap: the same PIN decrypts the file
 * anywhere, and losing the PIN is the same expected trade-off as losing
 * the passphrase on any encrypted archive.
 *
 * Pure `javax.crypto`/`java.security` (PBKDF2 + AES/GCM) — no Android
 * Keystore provider, no [android.content.Context] — so this class is
 * testable as a plain JVM unit test, unlike [SafetyBackupStore].
 *
 * File shape: `MAGIC (5 bytes) | salt (16 bytes) | ivSize (1 byte) | iv | ciphertext`.
 * [isEncrypted] reads only the magic header, so Restore can detect an
 * encrypted file regardless of the current Settings toggle state.
 */
class BackupEncryption @Inject constructor() {

    fun encrypt(plainBytes: ByteArray, pin: String): ByteArray {
        val salt = ByteArray(SALT_SIZE).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance(TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, deriveKey(pin, salt))
        val iv = cipher.iv
        val cipherText = cipher.doFinal(plainBytes)
        return MAGIC + salt + byteArrayOf(iv.size.toByte()) + iv + cipherText
    }

    /** @throws BackupDecryptionException on a wrong PIN or corrupted/truncated data. */
    fun decrypt(data: ByteArray, pin: String): ByteArray {
        require(isEncrypted(data)) { "Not an encrypted FlashLearn backup file" }
        try {
            var offset = MAGIC.size
            val salt = data.copyOfRange(offset, offset + SALT_SIZE)
            offset += SALT_SIZE
            val ivSize = data[offset].toInt()
            offset += 1
            val iv = data.copyOfRange(offset, offset + ivSize)
            offset += ivSize
            val cipherText = data.copyOfRange(offset, data.size)

            val cipher = Cipher.getInstance(TRANSFORMATION)
            cipher.init(Cipher.DECRYPT_MODE, deriveKey(pin, salt), GCMParameterSpec(GCM_TAG_BITS, iv))
            return cipher.doFinal(cipherText)
        } catch (e: Exception) {
            // AEADBadTagException (wrong PIN) and any array-bounds issue on truncated
            // data are both reported the same way — the user can't act differently on either.
            throw BackupDecryptionException("Wrong PIN or corrupted backup file", e)
        }
    }

    fun isEncrypted(data: ByteArray): Boolean =
        data.size > MAGIC.size + SALT_SIZE + 1 && data.copyOfRange(0, MAGIC.size).contentEquals(MAGIC)

    private fun deriveKey(pin: String, salt: ByteArray): SecretKeySpec {
        val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val spec = PBEKeySpec(pin.toCharArray(), salt, PBKDF2_ITERATIONS, KEY_BITS)
        val keyBytes = factory.generateSecret(spec).encoded
        return SecretKeySpec(keyBytes, "AES")
    }

    companion object {
        private val MAGIC = "FLBE1".toByteArray(Charsets.US_ASCII) // FlashLearn Backup Encrypted, v1
        private const val SALT_SIZE = 16
        private const val PBKDF2_ITERATIONS = 100_000
        private const val KEY_BITS = 256
        private const val GCM_TAG_BITS = 128
        private const val TRANSFORMATION = "AES/GCM/NoPadding"
    }
}

class BackupDecryptionException(message: String, cause: Throwable) : Exception(message, cause)
