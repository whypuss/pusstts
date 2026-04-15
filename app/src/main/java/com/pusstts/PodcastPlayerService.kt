package com.pusstts

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.*

/**
 * Podcast 背景播放服務
 * - Foreground Service（保活）
 * - AudioTrack 流式播放
 * - 通知欄控制（暫停/停止）
 */
class PodcastPlayerService : Service() {

    companion object {
        private const val CHANNEL_ID = "kokoro_podcast_channel"
        private const val NOTIFICATION_ID = 1001
    }

    inner class LocalBinder : Binder() {
        fun getService(): PodcastPlayerService = this@PodcastPlayerService
    }

    private val binder = LocalBinder()
    private var audioTrack: AudioTrack? = null
    private var playbackJob: Job? = null
    private val serviceScope = CoroutineScope(Dispatchers.Main + SupervisorJob())
    private var isPaused = false

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "PAUSE" -> pause()
            "STOP"  -> stop()
        }
        return START_NOT_STICKY
    }

    /**
     * 播放音訊片段佇列
     * @param chunks  分好段的文本塊（含角色資訊）
     * @param tts     sherpa-onnx OfflineTts 實例
     * @param speed   播放速度
     */
    fun playChunks(chunks: List<TextChunker.ScriptChunk>, tts: OfflineTts, speed: Float = 1.0f) {
        stopPlayback()

        startForeground(NOTIFICATION_ID, buildNotification("準備播放...", chunks[0].speakerName))

        playbackJob = serviceScope.launch {
            initAudioTrack(tts.sampleRate())

            for ((index, chunk) in chunks.withIndex()) {
                if (!isActive || isPaused) break

                updateNotification("${index + 1}/${chunks.size}", chunk.speakerName)

                try {
                    val audio = tts.generate(
                        text = chunk.text,
                        sid = chunk.speakerId,
                        speed = speed,
                    )
                    playPcm(audio.samples)
                } catch (e: Exception) {
                    Log.e("KokoroPodcast", "Chunk $index error: ${e.message}")
                }
            }

            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
        }
    }

    private suspend fun initAudioTrack(sampleRate: Int) = withContext(Dispatchers.Main) {
        val minBuffer = AudioTrack.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_OUT_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        audioTrack = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .setUsage(AudioAttributes.USAGE_MEDIA).build())
            .setAudioFormat(
                AudioFormat.Builder()
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setSampleRate(sampleRate)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
            .setBufferSizeInBytes(minBuffer * 4)
            .setTransferMode(AudioTrack.MODE_STREAM).build()
        audioTrack?.play()
    }

    private fun playPcm(samples: FloatArray) {
        val track = audioTrack ?: return
        val pcm = ShortArray(samples.size)
        for (i in samples.indices) {
            pcm[i] = (samples[i].coerceIn(-1f, 1f) * Short.MAX_VALUE).toInt().toShort()
        }
        track.write(pcm, 0, pcm.size)
    }

    fun pause() {
        isPaused = true
        audioTrack?.pause()
        updateNotification("已暫停", "")
    }

    fun stop() {
        stopPlayback()
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun stopPlayback() {
        isPaused = false
        playbackJob?.cancel()
        playbackJob = null
        audioTrack?.stop()
        audioTrack?.release()
        audioTrack = null
    }

    // ─── 通知 ───

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Podcast TTS", NotificationManager.IMPORTANCE_LOW
            ).apply { description = "Kokoro Podcast 播放控制" }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, speaker: String): Notification {
        val launchIntent = packageManager.getLaunchIntentForPackage(packageName)
        val pendingIntent = PendingIntent.getActivity(
            this, 0, launchIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText("說話者：$speaker")
            .setSmallIcon(android.R.drawable.ic_media_play)
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_media_pause, "暫停", makePendingIntent("PAUSE"))
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "停止", makePendingIntent("STOP"))
            .setOngoing(true)
            .build()
    }

    private fun updateNotification(title: String, speaker: String) {
        val manager = getSystemService(NotificationManager::class.java)
        manager.notify(NOTIFICATION_ID, buildNotification(title, speaker))
    }

    private fun makePendingIntent(action: String): PendingIntent {
        return PendingIntent.getService(
            this, action.hashCode(),
            Intent(this, PodcastPlayerService::class.java).apply { this.action = action },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    override fun onDestroy() {
        super.onDestroy()
        stopPlayback()
        serviceScope.cancel()
    }
}
