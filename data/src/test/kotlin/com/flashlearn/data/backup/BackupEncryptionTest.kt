package com.flashlearn.data.backup

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BackupEncryptionTest {

    private val encryption = BackupEncryption()

    @Test
    fun `encrypt then decrypt with the correct pin reproduces the original bytes`() {
        val plain = """{"hello":"world"}""".toByteArray()

        val cipherBytes = encryption.encrypt(plain, pin = "1234")
        val decrypted = encryption.decrypt(cipherBytes, pin = "1234")

        assertArrayEquals(plain, decrypted)
    }

    @Test
    fun `decrypting with the wrong pin throws BackupDecryptionException, not a raw crypto exception`() {
        val cipherBytes = encryption.encrypt("secret".toByteArray(), pin = "1234")

        assertThrows(BackupDecryptionException::class.java) {
            encryption.decrypt(cipherBytes, pin = "0000")
        }
    }

    @Test
    fun `isEncrypted is true for an encrypted payload`() {
        val cipherBytes = encryption.encrypt("secret".toByteArray(), pin = "1234")

        assertTrue(encryption.isEncrypted(cipherBytes))
    }

    @Test
    fun `isEncrypted is false for plain JSON text`() {
        val plainJson = """{"schemaVersion":1}""".toByteArray()

        assertFalse(encryption.isEncrypted(plainJson))
    }

    @Test
    fun `two encryptions of the same plaintext with the same pin produce different ciphertext`() {
        val plain = "same text".toByteArray()

        val first = encryption.encrypt(plain, "1234")
        val second = encryption.encrypt(plain, "1234")

        // Random salt + random IV each time — ciphertext must never repeat even for identical input.
        assertFalse(first.contentEquals(second))
    }

    @Test
    fun `truncated or corrupted data is reported as BackupDecryptionException`() {
        val cipherBytes = encryption.encrypt("secret".toByteArray(), pin = "1234")
        val corrupted = cipherBytes.copyOf(cipherBytes.size - 5)

        assertThrows(BackupDecryptionException::class.java) {
            encryption.decrypt(corrupted, pin = "1234")
        }
    }
}
