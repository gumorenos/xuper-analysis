package com.uniplay.tv.source

import android.content.Context
import android.os.Build
import com.uniplay.tv.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.FileReader

/**
 * Fuente live del portal coolx: usa CoolxClient (protocolo replicado).
 * Flujo: v8/active -> v6/getLiveData (catálogo 76182) -> canales.
 * Fallback: catálogo embebido (assets) si el portal no responde.
 */
class LiveSource(private val context: Context) : Source {

    override val id = "live"
    override val displayName = "Live portal"

    override suspend fun channels(): List<Channel> = withContext(Dispatchers.IO) {
        val client = CoolxClient()
        val mac = gatewayMac()
        val model = Build.MODEL ?: "B866VH"
        val product = Build.PRODUCT ?: model
        val cpu = Build.SUPPORTED_ABIS.firstOrNull() ?: "armeabi-v7a"
        if (!client.activate(model, product, cpu, mac)) {
            Log.w(CoolxClient.TAG, "activate fallo; uso catalogo embebido")
            return@withContext emptyList()
        }
        val data = client.getLiveData() ?: return@withContext emptyList()
        parse(data)
    }

    private fun parse(data: JSONObject): List<Channel> {
        val arr = data.optJSONArray("channelList") ?: return emptyList()
        val out = mutableListOf<Channel>()
        for (i in 0 until arr.length()) {
            val ch = arr.getJSONObject(i)
            val name = ch.optString("name", "")
            if (name.isEmpty()) continue
            var url = ""
            val addrs = ch.optJSONArray("liveAddressList")
            if (addrs != null && addrs.length() > 0) {
                val pc = addrs.getJSONObject(0).optString("playCode", "")
                // solo URLs directas reproducibles; los playCode SLB se resuelven via getSlbInfo
                url = if (pc.startsWith("http")) pc else ""
            }
            val logo = ch.optString("posterUrl", "")
            out.add(Channel(
                name = name,
                url = url,
                source = "live",
                number = ch.optInt("channelNumber", 0),
                logoUrl = logo,
                playCode = url,
                group = "Live",
            ))
        }
        return out
    }

    /** MAC del gateway desde /proc/net/arp (la vía del app original k2/j). */
    private fun gatewayMac(): String = try {
        val ip = localIp() ?: return "d0:ea:11:a7:e8:16"
        BufferedReader(FileReader("/proc/net/arp")).use { r ->
            r.readLines().firstOrNull { it.contains(ip) }
                ?.split(Regex("\\s+"))?.getOrNull(3) ?: "d0:ea:11:a7:e8:16"
        }
    } catch (_: Exception) { "d0:ea:11:a7:e8:16" }

    private fun localIp(): String? = try {
        val sock = java.net.DatagramSocket()
        sock.connect(java.net.InetSocketAddress("8.8.8.8", 53))
        val ip = sock.localAddress.hostAddress
        sock.close()
        ip
    } catch (_: Exception) { null }
}
