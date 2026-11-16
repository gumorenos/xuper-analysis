package com.uniplay.tv.source

import com.uniplay.tv.model.Channel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Streams fijos cosechados del catálogo del app original (estilo XuperAmigOnly). */
class DirectSource : Source {

    override val id = "direct"
    override val displayName = "Canales directos"

    override suspend fun channels(): List<Channel> = withContext(Dispatchers.IO) { STATIC }

    companion object {
        /** HLS estáticos encontrados en el catálogo descifrado (76182.json). */
        val STATIC: List<Channel> = listOf(
            "ESPN", "ESPN_2", "ESPN_3", "ESPN_4", "ESPN_5", "ESPN_6", "ESPN_7",
        ).map { slug ->
            Channel(
                name = slug.replace('_', ' '),
                url = "https://barinaslive1.boldmss.com/$slug/$slug.isml/$slug.m3u8",
                source = "direct",
                group = "Deportes",
            )
        }
    }
}
