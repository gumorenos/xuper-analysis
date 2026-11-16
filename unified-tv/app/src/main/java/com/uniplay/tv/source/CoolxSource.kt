package com.uniplay.tv.source

import com.uniplay.tv.model.Channel
import com.uniplay.tv.util.CoolxCrypto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Fuente coolx: cliente del protocolo portalCore.
 * Cuerpo = JSON(bean + identidad) -> 3DES/ECB/PKCS5 -> hex (zb/c.java + zb/b.java + fc/b.java).
 * Respuesta = {"returnCode","errorMessage","data":"<hex>"}.
 * El portalCode/userToken llegan tras el login (m0.i0()).
 */
class CoolxSource(
    private val portalHost: String = "fuxok.nguvmqhpk.com",
    private val buildUuid: String,          // PORTAL_KEY descifrado (por build)
    private val sn: String,                 // SN del dispositivo registrado
    private val brandKey: String = "combrasiltvaslgklxckbcombrasiltv",
) : Source {

    override val id = "coolx"
    override val displayName = "Panel operador (coolx)"

    private var userToken: String = ""
    private var userId: String = ""
    private var portalCode: String = ""

    private val identity: JSONObject
        get() = JSONObject().apply {
            put("loginType", "2")
            put("appLanguage", "es")
            put("apkVersion", "43476")
            put("sysVersion", "2026-10-18 10:23:33_34_14_unknown")
            put("appId", "com.android.msandroid")
            put("hardwareInfo", "amlogic")
            put("model", "B866VH")
            put("product", "B866VH")
            put("cpu", "armeabi-v7a")
            put("B29", brandEncrypt("ZTEATV45503609797"))
            put("reserve1", brandEncrypt("d0ea11a7e816"))
            put("portalCode", portalCode)
            put("deviceToken", "")
            put("sn", sn)
            put("drmId", "")
            put("sdkVer", 34)
        }

    private fun brandEncrypt(plain: String): String {
        val ct = CoolxCrypto.des3EcbEncrypt(plain.toByteArray(), CoolxCrypto.dcBase64(brandKey).copyOf(24))
        return CoolxCrypto.hexEncode(java.util.Base64.getEncoder().encode(ct))
    }

    suspend fun call(endpoint: String, payload: JSONObject = JSONObject()): JSONObject =
        withContext(Dispatchers.IO) {
            val body = payload
            for (k in identity.keys()) body.put(k, identity.get(k))
            val hex = CoolxCrypto.encryptBody(body.toString(), buildUuid)
            val conn = URL("https://$portalHost$endpoint").openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.doOutput = true
            conn.connectTimeout = 15000
            conn.readTimeout = 20000
            conn.setRequestProperty("Content-Type", "application/json;charset=utf-8")
            conn.setRequestProperty("apk", "com.android.msandroid")
            conn.setRequestProperty("apkVer", "43404")
            conn.setRequestProperty("spkgVer", "2026-10-18 10:23:33_34_14_unknown")
            conn.setRequestProperty("User-Agent", "okhttp/4.9.3")
            conn.outputStream.use { it.write(hex.toByteArray()) }
            val text = conn.inputStream.bufferedReader().readText()
            conn.disconnect()
            val resp = JSONObject(text)
            val data = resp.optString("data", "")
            if (data.isNotEmpty()) {
                try { resp.put("data_decrypted", JSONObject(CoolxCrypto.decryptData(data, buildUuid))) }
                catch (_: Exception) { }
            }
            resp
        }

    suspend fun login(user: String, passwordMd5: String): Boolean = withContext(Dispatchers.IO) {
        val resp = call("/api/portalCore/v8/login", JSONObject().apply {
            put("userName", user); put("password", passwordMd5)
        })
        val data = resp.optJSONObject("data_decrypted")
        userId = data?.optString("userId", "") ?: ""
        userToken = data?.optString("userToken", "") ?: ""
        portalCode = data?.optString("portalCode", "") ?: ""
        userId.isNotEmpty()
    }

    override suspend fun channels(): List<Channel> = withContext(Dispatchers.IO) {
        // El catálogo live llega de getHome/v3-getShelveData; requiere sesión válida.
        // Mientras el gate de versión no esté resuelto, esta fuente puede no devolver canales.
        emptyList()
    }
}
