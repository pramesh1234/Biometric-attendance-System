package com.example.attendance.core.data

import com.example.attendance.core.domain.PasswordHasher
import com.example.attendance.core.domain.SessionStore
import com.example.attendance.core.model.Session
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.security.MessageDigest
import java.security.SecureRandom
import java.util.Base64
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.PBEKeySpec

class MemorySessionStore : SessionStore {
    private val value = MutableStateFlow<Session?>(null)
    override val session: StateFlow<Session?> = value.asStateFlow()
    override fun set(session: Session?) {
        value.value = session
    }
}

class Pbkdf2PasswordHasher : PasswordHasher {
    private fun derive(password: String, salt: ByteArray, iterations: Int): ByteArray {
        val spec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        return try {
            SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).encoded
        } finally {
            spec.clearPassword()
        }
    }

    override suspend fun hash(password: String): String = withContext(Dispatchers.Default) {
        val salt = ByteArray(16).also(SecureRandom()::nextBytes)
        val encoder = Base64.getEncoder()
        listOf(
            "pbkdf2-sha256",
            "600000",
            encoder.encodeToString(salt),
            encoder.encodeToString(derive(password, salt, 600000))
        ).joinToString(":")
    }

    override suspend fun matches(password: String, encoded: String): Boolean =
        withContext(Dispatchers.Default) {
            runCatching {
                val parts =
                    encoded.split(':'); if (parts.size != 4 || parts[0] != "pbkdf2-sha256") return@runCatching false
                val iterations =
                    parts[1].toInt(); if (iterations !in 100000..2000000) return@runCatching false
                MessageDigest.isEqual(
                    Base64.getDecoder().decode(parts[3]),
                    derive(password, Base64.getDecoder().decode(parts[2]), iterations)
                )
            }.getOrDefault(false)
        }
}
