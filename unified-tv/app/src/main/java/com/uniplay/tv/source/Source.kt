package com.uniplay.tv.source

import com.uniplay.tv.model.Channel

/** Fuente de contenido intercambiable. */
interface Source {
    val id: String
    val displayName: String
    suspend fun channels(): List<Channel>
}
