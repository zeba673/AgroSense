package com.example.data

import java.security.MessageDigest

/**
 * Utilidades locales de encriptación y hash para asegurar que la autenticación
 * se procese de manera local e inequívoca, protegiendo las credenciales en la base de datos.
 */
object CifradoUtil {

    /**
     * Genera un hash SHA-256 de la contraseña ingresada para su almacenamiento y verificación local.
     */
    fun hashSha256(texto: String): String {
        return try {
            val bytes = MessageDigest.getInstance("SHA-256").digest(texto.toByteArray(Charsets.UTF_8))
            bytes.joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            // Retorna un hash de respaldo o el código hash por defecto si ocurre un error
            texto.hashCode().toString()
        }
    }
}
