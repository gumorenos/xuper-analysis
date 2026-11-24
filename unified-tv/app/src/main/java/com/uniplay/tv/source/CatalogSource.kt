package com.uniplay.tv.source

import android.content.Context
import com.uniplay.tv.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Catálogo completo extraído del app original (data/uniplay_catalog.json).
 * 1. Descarga la versión actualizada desde el repo (raw.githubusercontent).
 * 2. Si falla, usa la copia incluida en assets/ (offline).
 * Canales con URL directa se reproducen; los SLB quedan listados como pendientes.
 */
class CatalogSource(private val context: Context) : Source {

    override val id = "catalog"
    override val displayName = "Catálogo Xuper"

    private val rawUrl = "https://raw.githubusercontent.com/gumorenos/xuper-analysis/master/data/uniplay_catalog.json"

    override suspend fun channels(): List<Channel> = withContext(Dispatchers.IO) {
        val json = fetchRemote() ?: loadAsset()
        parse(json)
    }

    private fun fetchRemote(): String? = try {
        val conn = URL(rawUrl).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000; conn.readTimeout = 10000
        conn.requestMethod = "GET"
        val text = if (conn.responseCode == 200)
            conn.inputStream.bufferedReader().readText() else null
        conn.disconnect(); text
    } catch (_: Exception) { null }

    private fun loadAsset(): String =
        context.assets.open("uniplay_catalog.json").bufferedReader().readText()

    private fun parse(text: String): List<Channel> {
        val arr = JSONObject(text).optJSONArray("channels") ?: JSONArray()
        val out = mutableListOf<Channel>()
        for (i in 0 until arr.length()) {
            val ch = arr.getJSONObject(i)
            val name = ch.optString("name", "")
            val num = ch.optInt("num", 0)
            val logo = ch.optString("logo", "")
            val addrs = ch.optJSONArray("addrs") ?: continue
            var direct = ""
            var playCode = ""
            for (j in 0 until addrs.length()) {
                val a = addrs.getJSONObject(j)
                if (direct.isEmpty() && a.optString("kind") == "direct") {
                    direct = a.optString("url", "")
                }
                if (playCode.isEmpty()) playCode = a.optString("playCode", "")
            }
            out.add(Channel(
                name = name,
                url = direct,
                source = "catalog",
                number = num,
                logoUrl = logo,
                playCode = playCode,
                group = if (direct.isNotEmpty()) "En vivo" else "SLB (pendiente)",
            ))
        }
        return out
    }
}
