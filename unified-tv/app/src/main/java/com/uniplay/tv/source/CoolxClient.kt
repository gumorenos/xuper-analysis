package com.uniplay.tv.source

import android.util.Log
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONObject
import java.util.concurrent.TimeUnit
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.DESedeKeySpec

/**
 * Cliente del protocolo portalCore (familia coolx/Xuper), replicado del app original.
 *
 * Claves verificadas por logging smali (2026-11-24):
 *  - Sobre: JSON plano -> 3DES/ECB/PKCS5 con key = b64Propio(PORTAL_KEY)[:24] -> hex como body
 *  - PORTAL_KEY (meta-data del manifest del app original): "568e194f-d170-4369-96e8-4a457f641880"
 *  - Headers: apk/apkVer/spkgVer/NoLog; UA = okhttp/3.12.12 (la huella TLS de okhttp Android
 *    es lo unico que pasa el gate de Cloudflare del operador)
 */
class CoolxClient {

    companion object {
        const val TAG = "UniPlayCoolx"
        const val PORTAL = "https://fuxok.nguvmqhpk.com"
        const val PORTAL_BACKUP = "https://brkhzs.udigkkrc.com"

        /** Clave del sobre (descifrada del meta-data PORTAL_KEY del app original). */
        const val KEY_STR = "568e194f-d170-4369-96e8-4a457f641880"

        /** Clave de marca para cifrar serial/MAC (B29/reserve1). */
        const val BRAND_KEY = "combrasiltvaslgklxckbcombrasiltv"

        const val SN = "c05fdf3595ba2ec9f49486f20a72ef06"
        const val PORTAL_CODE = "masnew"
        const val SPKG_VER = "2025-11-06 18:02:55_34_14_"

        /** Decoder base64 del app (dc.a): inválidos -> -1, '=' encoge el átomo. */
        private val B64 = ByteArray(256) { -1 }
        init {
            val alpha = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/"
            for (i in alpha.indices) B64[alpha[i].code] = i.toByte()
        }

        fun dcb64(s: String): ByteArray {
            val out = java.io.ByteArrayOutputStream()
            var i = 0
            while (i < s.length) {
                val end = minOf(i + 4, s.length)
                val atom = s.substring(i, end)
                i += 4
                if (atom.length < 2) break
                var size = atom.length
                if (size > 3 && atom[3] == '=') size = 3
                if (size > 2 && atom[2] == '=') size = 2
                fun v(k: Int): Int = if (k < atom.length) B64[atom[k].code].toInt() else -1
                val v0 = v(0); val v1 = v(1); val v2 = v(2); val v3 = v(3)
                fun sh(x: Int, k: Int) = (x shl k) and 0xFF
                fun m1(x: Int) = if (x < 0) 0xFF else x
                out.write(((sh(m1(v0), 2) and 0xFC) or (if (v1 < 0) 3 else (v1 shr 4) and 3)))
                if (size >= 3) out.write(((sh(m1(v1), 4) and 0xF0) or (if (v2 < 0) 15 else (v2 shr 2) and 15)))
                if (size >= 4) out.write(((sh(m1(v2), 6) and 0xC0) or (if (v3 < 0) 63 else v3 and 63)))
            }
            return out.toByteArray()
        }

        fun key24(keyStr: String): ByteArray = dcb64(keyStr).copyOf(24)
    }

    private val key = key24(KEY_STR)
    private val brandKey = key24(BRAND_KEY)

    private val http: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()

    private var userId: String = ""
    private var userToken: String = ""

    // ---------- crypto ----------

    private fun des3(data: ByteArray, key: ByteArray, encrypt: Boolean): ByteArray {
        val spec = DESedeKeySpec(key)
        val kf = SecretKeyFactory.getInstance("DESede")
        val sk = kf.generateSecret(spec)
        val c = Cipher.getInstance("DESede/ECB/PKCS5Padding")
        c.init(if (encrypt) Cipher.ENCRYPT_MODE else Cipher.DECRYPT_MODE, sk)
        return c.doFinal(data)
    }

    private fun hex(b: ByteArray): String = b.joinToString("") { "%02x".format(it) }

    private fun brandEncrypt(plain: String): String {
        val ct = des3(plain.toByteArray(), brandKey, true)
        return hex(android.util.Base64.getEncoder().encode(ct))
    }

    private fun encryptBody(json: String): String = hex(des3(json.toByteArray(), key, true))

    private fun decryptData(hexData: String): JSONObject {
        val b64 = StringBuilder()
        var i = 0
        while (i < hexData.length) { b64.append(hexData.substring(i, i + 2).toInt(16).toChar()); i += 2 }
        val pt = des3(android.util.Base64.decode(b64.toString(), android.util.Base64.DEFAULT), key, false)
        var s = String(pt)
        val n = s.last().code
        if (n in 1..8 && s.length >= n && s.substring(s.length - n).all { it.code == n }) s = s.substring(0, s.length - n)
        return JSONObject(s)
    }

    // ---------- identidad ----------

    private fun identity(model: String, product: String, cpu: String, gatewayMac: String): JSONObject =
        JSONObject().apply {
            put("loginType", "2")
            put("appLanguage", "es")
            put("apkVersion", "49902")
            put("sysVersion", SPKG_VER)
            put("appId", "com.android.msandroid")
            put("hardwareInfo", "amlogic")
            put("model", model)
            put("product", product)
            put("cpu", cpu)
            put("B29", brandEncrypt(android.os.Build.getRadioVersion().ifEmpty { "ZTEATV45503609797" }))
            put("reserve1", brandEncrypt(gatewayMac))
            put("portalCode", PORTAL_CODE)
            put("deviceToken", "")
            put("sn", SN)
            put("drmId", "")
            put("sdkVer", 34)
        }

    // ---------- llamadas ----------

    fun call(endpoint: String, payload: JSONObject, host: String = PORTAL): JSONObject {
        val body = encryptBody(payload.toString())
        val req = Request.Builder()
            .url("$host$endpoint")
            .post(RequestBody.create(MediaType.parse("application/json;charset=utf-8"), body))
            .addHeader("apk", "com.android.msandroid")
            .addHeader("apkVer", "43404")
            .addHeader("spkgVer", SPKG_VER)
            .addHeader("NoLog", "true")
            .build()
        http.newCall(req).execute().use { resp ->
            val text = resp.body()?.string() ?: "{}"
            val out = JSONObject(text)
            val data = out.optString("data", "")
            if (data.isNotEmpty()) {
                try { out.put("data_decrypted", decryptData(data)) } catch (_: Exception) { }
            }
            return out
        }
    }

    /** Registro/sesión: v8/active con identidad completa. Actualiza userId/userToken. */
    fun activate(model: String, product: String, cpu: String, gatewayMac: String): Boolean {
        val payload = JSONObject().apply {
            put("authCode", ""); put("authVersion", ""); put("channel", "default")
            put("macAddr", "02:00:00:00:00:00"); put("matadata", ""); put("openNum", 0)
            put("reserve1", brandEncrypt(gatewayMac)); put("signdata", ""); put("snToken", "")
            for (k in identity(model, product, cpu, gatewayMac).keys()) put(k, identity(model, product, cpu, gatewayMac).get(k))
        }
        val resp = call("/api/portalCore/v8/active", payload)
        val d = resp.optJSONObject("data_decrypted") ?: return false
        userId = d.optString("userId", "")
        userToken = d.optString("userToken", "")
        Log.i(TAG, "active: rc=${resp.optString("returnCode")} userId=$userId")
        return userId.isNotEmpty() && userToken.isNotEmpty()
    }

    /** Catálogo live (v6/getLiveData). Devuelve el JSON descifrado o null. */
    fun getLiveData(columnId: Int = 76182, dataVersion: String = "", expireTimeStr: String = ""): JSONObject? {
        if (userId.isEmpty() || userToken.isEmpty()) return null
        val payload = JSONObject().apply {
            put("columnId", columnId); put("dataVersion", dataVersion); put("expireTimeStr", expireTimeStr)
            put("pageNum", 1); put("pageSize", 3000)
            put("portalCode", PORTAL_CODE); put("userId", userId); put("userToken", userToken)
        }
        for (host in arrayOf(PORTAL, PORTAL_BACKUP)) {
            val resp = call("/api/portalCore/v6/getLiveData", payload, host)
            val d = resp.optJSONObject("data_decrypted")
            if (d != null && resp.optString("returnCode") == "0") return d
        }
        return null
    }

    /** Resolución de streams (v15/getSlbInfo). Devuelve el JSON descifrado o null. */
    fun getSlbInfo(liveCode: String = "${PORTAL_CODE}_live"): JSONObject? {
        if (userId.isEmpty() || userToken.isEmpty()) return null
        val payload = JSONObject().apply {
            put("appParams", ""); put("appVer", "49902"); put("encMediaSupported", 1)
            put("hasPay", "0"); put("lang", "es")
            put("liveCodeList", org.json.JSONArray().put(liveCode))
            put("pipFlag", "0"); put("portalCode", PORTAL_CODE)
            put("reserve1", brandEncrypt("d0:ea:11:a7:e8:16"))
            put("type", "merge"); put("userId", userId)
            put("userIdentity", "1"); put("userToken", userToken)
        }
        for (host in arrayOf(PORTAL, PORTAL_BACKUP)) {
            val resp = call("/api/portalCore/v15/getSlbInfo", payload, host)
            val d = resp.optJSONObject("data_decrypted")
            if (d != null && resp.optString("returnCode") == "0") return d
        }
        return null
    }

    fun currentToken(): String = userToken
    fun currentUserId(): String = userId
}
