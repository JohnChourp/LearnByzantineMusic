package com.johnchourp.learnbyzantinemusic.recordings.player

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.PlaybackParams
import android.net.Uri
import android.os.Build

/**
 * Android's own `MediaPlayer` as an [AudioPlayer] — no extra library (ClickUp `869f5x268`). Thin on
 * purpose: every decision is in [PlayerController].
 *
 * - Speed and pitch through `PlaybackParams`, which stretches time without moving the pitch.
 * - Seeks are frame-accurate (`SEEK_CLOSEST`) from Android 8; before that the platform lands on the
 *   nearest sync point, which is what plain `seekTo` does.
 * - A file the device cannot decode (e.g. Ogg-Opus on an older Android) reports an error instead of
 *   throwing: the screen then offers another app.
 *
 * Calls come on the main thread, and the player is created there, so its callbacks arrive there too.
 */
class MediaPlayerAudio(private val context: Context, private val uri: Uri) : AudioPlayer {
    private var player: MediaPlayer? = null

    override fun prepare(listener: AudioPlayer.Listener) {
        val created = MediaPlayer()
        player = created
        try {
            created.setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_MEDIA)
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .build()
            )
            created.setOnPreparedListener { listener.onPrepared(it.duration) }
            created.setOnErrorListener { _, _, _ ->
                listener.onError()
                true
            }
            created.setOnCompletionListener { listener.onCompletion() }
            created.setDataSource(context, uri)
            created.prepareAsync()
        } catch (failure: Exception) {
            listener.onError()
        }
    }

    override fun start() {
        player?.start()
    }

    override fun pause() {
        player?.let { if (it.isPlaying) it.pause() }
    }

    override fun seekTo(positionMs: Int) {
        val current = player ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            current.seekTo(positionMs.toLong(), MediaPlayer.SEEK_CLOSEST)
        } else {
            current.seekTo(positionMs)
        }
    }

    override fun setTuning(speed: Float, pitchRatio: Float): Boolean {
        val current = player ?: return false
        return runCatching {
            current.playbackParams = PlaybackParams().setSpeed(speed).setPitch(pitchRatio)
        }.isSuccess
    }

    override val positionMs: Int
        get() = runCatching { player?.currentPosition ?: 0 }.getOrDefault(0)

    override fun release() {
        val current = player ?: return
        player = null
        current.setOnPreparedListener(null)
        current.setOnErrorListener(null)
        current.setOnCompletionListener(null)
        runCatching { current.release() }
    }
}
