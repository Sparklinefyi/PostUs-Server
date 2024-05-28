package postus.utils;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import java.util.Base64
import javax.crypto.spec.SecretKeySpec

class EncryptionUtil {
    companion object {
        private val
                key = "1234567890123456".toByteArray()
        private val
                iv = "1234567890123456".toByteArray()

        fun encrypt(value: String): String {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val encrypted = cipher.doFinal(value.toByteArray())
            return Base64.getEncoder().encodeToString(encrypted)
        }

        fun decrypt(value: String): String {
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), IvParameterSpec(iv))
            val decrypted = cipher.doFinal(Base64.getDecoder().decode(value))
            return String(decrypted)
        }
    }
    }