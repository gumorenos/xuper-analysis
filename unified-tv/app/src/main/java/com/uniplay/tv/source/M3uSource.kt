package com.uniplay.tv.source

import com.uniplay.tv.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URL

/** Parser M3U/M3U8 con metadatos #EXTINF. */
class M3uSource(private val url: String, override val displayName: String = "Lista M3U") : Source {

    override val id = "m3u"

    override suspend fun channels(): List<Channel> = withContext(Dispatchers.IO) {
        val text = URL(url).openStream().bufferedReader(Charsets.UTF_8).use { it.readText() }
        parse(text)
    }

    fun parse(text: String): List<Channel> {
        val out = mutableListOf<Channel>()
        var name = ""; var logo = ""; var group = ""
        for (raw in text.lineSequence()) {
            val line = raw.trim()
            when {
                line.startsWith("#EXTINF:") -> {
                    name = line.substringAfter(',', "").trim()
                    logo = Regex("tvg-logo=\"([^\"]*)\"").find(line)?.groupValues?.get(1) ?: ""
                    group = Regex("group-title=\"([^\"]*)\"").find(line)?.groupValues?.get(1) ?: ""
                }
                line.isNotEmpty() && !line.startsWith("#") -> {
                    out.add(Channel(name.ifEmpty { line.substringAfterLast('/') }, line, "m3u", logoUrl = logo, group = group))
                    name = ""; logo = ""; group = ""
                }
            }
        }
        return out
    }
}
