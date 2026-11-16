package com.uniplay.tv

import android.os.Bundle
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.media3.common.MediaItem
import androidx.media3.common.Player
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.exoplayer.hls.HlsMediaSource
import androidx.media3.ui.PlayerView

class PlayerActivity : AppCompatActivity() {

    private var player: ExoPlayer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val root = FrameLayout(this).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        }
        val playerView = PlayerView(this).apply {
            layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT, FrameLayout.LayoutParams.MATCH_PARENT)
        }
        root.addView(playerView)
        setContentView(root)

        val url = intent.getStringExtra("url") ?: return
        val name = intent.getStringExtra("name") ?: ""

        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(
                DefaultHttpDataSource.Factory()
                    .setUserAgent("okhttp/4.9.3")
                    .setAllowCrossProtocolRedirects(true)))
            .build().apply {
                playerView.player = this
                val isHls = url.contains(".m3u8") || url.contains("/hls/")
                val mediaItem = MediaItem.fromUri(url)
                val source = if (isHls)
                    HlsMediaSource.Factory(DefaultHttpDataSource.Factory()
                        .setUserAgent("okhttp/4.9.3")
                        .setAllowCrossProtocolRedirects(true))
                        .createMediaSource(mediaItem)
                else DefaultMediaSourceFactory(DefaultHttpDataSource.Factory())
                    .createMediaSource(mediaItem)
                setMediaSource(source)
                prepare()
                playWhenReady = true
                addListener(object : Player.Listener {
                    override fun onPlayerError(error: androidx.media3.common.PlaybackException) {
                        android.widget.Toast.makeText(
                            this@PlayerActivity, "Error: ${error.errorCode}", android.widget.Toast.LENGTH_SHORT).show()
                    }
                })
            }
        title = name
    }

    override fun onStop() {
        player?.release(); player = null
        super.onStop()
    }
}
