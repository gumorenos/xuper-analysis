package com.uniplay.tv.model

/** Canal o ítem de contenido, común a todas las fuentes. */
data class Channel(
    val name: String,
    val url: String,                 // URL directa (m3u/direct); vacío si coolx resuelve por playCode
    val source: String,              // "m3u" | "direct" | "coolx"
    val number: Int = 0,
    val logoUrl: String = "",
    val playCode: String = "",       // coolx: código para startPlayLive/getSlbInfo
    val group: String = "",          // país/categoría
)
