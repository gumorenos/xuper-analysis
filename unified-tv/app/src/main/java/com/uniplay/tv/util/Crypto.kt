package com.uniplay.tv.util

import java.io.ByteArrayOutputStream
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESedeKeySpec

/**
 * Réplica exacta del esquema criptográfico del framework coolx:
 *  - decodificador base64 propio (caracteres inválidos -> -1, átomos de 4, '=' encoge)
 *  - 3DES/ECB/PKCS5
 *  - fc.b.b: 3DES -> base64 -> hex ; fc.b.a: hex -> base64 -> 3DES
 * Referencia Python: tools/portal_client.py
 */
object CoolxCrypto {

    private val TABLE = ByteArray(256) { -1 }.also { t ->
        val alphabet = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
        alphabet.forEachIndexed { i, c -> t[c.code] = i.toByte() }
    }

    /** dc.a / ec.c — decoder del APK. */
    fun dcBase64(s: String): ByteArray {
        val out = ByteArrayOutputStream()
        var i = 0
        while (i < s.length) {
            val atomEnd = minOf(i + 4, s.length)
            val atom = s.substring(i, atomEnd); i = atomEnd
            if (atom.length < 2) break
            var size = atom.length
            if (size > 3 && atom[3] == '=') size = 3
            if (size > 2 && atom[2] == '=') size = 2
            fun v(c: Char): Int = TABLE[c.code].toInt()
            val v0 = v(atom[0]); val v1 = v(atom[1])
            val v2 = if (atom.length > 2) v(atom[2]) else -1
            val v3 = if (atom.length > 3) v(atom[3]) else -1
            fun sh(x: Int, k: Int) = (x shl k) and 0xFF
            fun m1(x: Int) = if (x < 0) 0xFF else x
            out.write((sh(m1(v0), 2) and 0xFC) or (3.takeIf { v1 < 0 } ?: (v1 shr 4 and 3)))
            if (size >= 3) out.write((sh(m1(v1), 4) and 0xF0) or (15.takeIf { v2 < 0 } ?: (v2 shr 2 and 15)))
            if (size >= 4) out.write((sh(m1(v2), 6) and 0xC0) or (63.takeIf { v3 < 0 } ?: (v3 and 63)))
        }
        return out.toByteArray()
    }

    private fun desedeKey(keyMaterial: ByteArray): ByteArray = keyMaterial.copyOf(24)

    fun des3EcbDecrypt(data: ByteArray, key24: ByteArray): ByteArray {
        val spec = DESedeKeySpec(desedeKey(key24))
        val key = SecretKeyFactory.getInstance("DESede").generateSecret(spec)
        val cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding")
        cipher.init(Cipher.DECRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    fun des3EcbEncrypt(data: ByteArray, key24: ByteArray): ByteArray {
        val spec = DESedeKeySpec(desedeKey(key24))
        val key = SecretKeyFactory.getInstance("DESede").generateSecret(spec)
        val cipher = Cipher.getInstance("DESede/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, key)
        return cipher.doFinal(data)
    }

    fun hexEncode(b: ByteArray): String = buildString(b.size * 2) {
        for (x in b) append(((x.toInt() shr 4) and 0xF).toString(16)).append((x.toInt() and 0xF).toString(16))
    }

    fun hexDecode(s: String): ByteArray {
        val str = if (s.length % 2 == 1) "0$s" else s
        return ByteArray(str.length / 2) { i ->
            ((Character.digit(str[2 * i], 16) shl 4) or Character.digit(str[2 * i + 1], 16)).toByte()
        }
    }

    /** fc.b.b: cifra JSON y devuelve el body hex. */
    fun encryptBody(json: String, keyString: String): String =
        hexEncode(des3EcbEncrypt(json.toByteArray(Charsets.UTF_8), dcBase64(keyString).copyOf(24)))

    /** fc.b.a: descifra el campo data hex de la respuesta. */
    fun decryptData(hex: String, keyString: String): String =
        String(des3EcbDecrypt(java.util.Base64.getDecoder().decode(hexDecode(hex)), dcBase64(keyString).copyOf(24)), Charsets.UTF_8)
}
