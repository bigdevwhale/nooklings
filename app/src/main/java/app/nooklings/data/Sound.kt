package app.nooklings.data

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaPlayer
import android.media.MediaRecorder
import android.media.MediaRecorder.AudioSource
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.sqrt

/** Ten seconds of the world into a file, with its loudness for the waveform while it records. */
class Recorder(private val c: Context) {
    private var rec: MediaRecorder? = null
    var file: File? = null
        private set

    /** Starts once; a second call while recording keeps the first take. */
    fun start(): Boolean = if (rec != null) true else runCatching {
        val f = Files.newSound(c)
        val r = if (Build.VERSION.SDK_INT >= 31) MediaRecorder(c) else @Suppress("DEPRECATION") MediaRecorder()
        r.setAudioSource(AudioSource.MIC)
        r.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
        r.setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
        r.setAudioEncodingBitRate(96_000)
        r.setAudioSamplingRate(44_100)
        r.setOutputFile(f.path)
        r.prepare()
        r.start()
        rec = r; file = f
        true
    }.getOrElse { release(); false }

    /** 0..1, how loud since the last ask. */
    fun level(): Float = runCatching { (rec?.maxAmplitude ?: 0) / 32767f }.getOrDefault(0f)

    /** Stops and keeps the file; null when nothing was recorded. */
    fun stop(): File? {
        val ok = runCatching { rec?.stop() }.isSuccess
        release()
        val f = file
        file = null
        return if (ok && f != null && f.length() > 0) f else { f?.delete(); null }
    }

    /** Drops a take still being recorded; a finished one belongs to its jar now. */
    fun cancel() {
        if (rec == null) return
        runCatching { rec?.stop() }; release(); file?.delete(); file = null
    }

    private fun release() { runCatching { rec?.release() }; rec = null }
}

/** Listens without recording: how loud the room is, for the quiet catch. Nothing is kept. */
class Meter {
    private var rec: AudioRecord? = null
    private val buf = ShortArray(1024)

    @SuppressLint("MissingPermission")
    fun start(): Boolean = runCatching {
        val size = maxOf(AudioRecord.getMinBufferSize(16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT), 4096)
        val r = AudioRecord(AudioSource.MIC, 16_000, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, size)
        if (r.state != AudioRecord.STATE_INITIALIZED) { r.release(); return false }
        r.startRecording(); rec = r
        true
    }.getOrDefault(false)

    /** The room's loudness now, 0..1 (RMS). */
    suspend fun level(): Float = withContext(Dispatchers.IO) {
        val r = rec ?: return@withContext 0f
        val n = r.read(buf, 0, buf.size)
        if (n <= 0) return@withContext 0f
        var s = 0.0
        for (i in 0 until n) { val v = buf[i] / 32768.0; s += v * v }
        sqrt(s / n).toFloat()
    }

    fun stop() { runCatching { rec?.stop() }; runCatching { rec?.release() }; rec = null }
}

/** Plays what's inside a jar: its own recording, or the synthesised sound of its kind. */
object Player {
    private var media: MediaPlayer? = null
    private var track: AudioTrack? = null

    fun play(c: Context, j: Jar) {
        stop()
        val f = j.sound?.let { Files.sound(c, it) }?.takeIf { it.exists() }
        if (f != null) {
            media = runCatching { MediaPlayer().apply { setDataSource(f.path); prepare(); start(); setOnCompletionListener { stop() } } }.getOrNull()
            if (media != null) return
        }
        val pcm = Synth.render(j.tags, seed = (j.id % 1000).toInt())
        track = runCatching {
            AudioTrack.Builder()
                .setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                .setAudioFormat(AudioFormat.Builder().setSampleRate(Synth.RATE).setEncoding(AudioFormat.ENCODING_PCM_16BIT).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                .setBufferSizeInBytes(pcm.size * 2).setTransferMode(AudioTrack.MODE_STATIC).build()
                .apply { write(pcm, 0, pcm.size); play() }
        }.getOrNull()
    }

    fun stop() {
        runCatching { media?.stop() }; runCatching { media?.release() }; media = null
        runCatching { track?.stop() }; runCatching { track?.release() }; track = null
    }
}
