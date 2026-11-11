package com.example.fvgalert

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.OpenableColumns
import org.json.JSONArray
import org.json.JSONObject

data class SoundItem(val id: String, val name: String, val custom: Boolean = false)

/** فهرست آهنگ‌های هشدار: داخلی + آهنگ پیش‌فرض گوشی + آهنگ‌هایی که کاربر اضافه کرده */
object Sounds {
    const val DEFAULT_ID = "raw:alarm_bell"

    private val builtin = listOf(
        SoundItem("raw:alarm_bell", "🔔 زنگ کلاسیک"),
        SoundItem("raw:alarm_siren", "🚨 آژیر"),
        SoundItem("raw:alarm_beep", "📟 بوق سه‌تایی"),
        SoundItem("raw:alarm_digital", "⏰ ساعت دیجیتال"),
        SoundItem("raw:alarm_chime", "🎐 زنگوله"),
        SoundItem("raw:alarm_horn", "📯 بوق ممتد"),
        SoundItem("system", "📱 آهنگ هشدار پیش‌فرض گوشی")
    )

    private fun prefs(c: Context) = c.getSharedPreferences("sounds", Context.MODE_PRIVATE)

    fun custom(c: Context): List<SoundItem> {
        val out = ArrayList<SoundItem>()
        try {
            val arr = JSONArray(prefs(c).getString("list", "[]"))
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                out.add(SoundItem(o.getString("id"), "🎵 " + o.getString("name"), true))
            }
        } catch (_: Exception) {
        }
        return out
    }

    fun all(c: Context): List<SoundItem> = builtin + custom(c)

    fun addCustom(c: Context, uri: Uri, flags: Int): SoundItem? {
        try {
            c.contentResolver.takePersistableUriPermission(uri, flags and Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
        var name = "آهنگ من"
        try {
            c.contentResolver.query(uri, null, null, null, null)?.use {
                if (it.moveToFirst()) {
                    val idx = it.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (idx >= 0) name = it.getString(idx).substringBeforeLast('.')
                }
            }
        } catch (_: Exception) {
        }
        val id = "custom:$uri"
        if (custom(c).any { it.id == id }) return SoundItem(id, "🎵 $name", true)
        val arr = JSONArray()
        custom(c).forEach { arr.put(JSONObject().put("id", it.id).put("name", it.name.removePrefix("🎵 "))) }
        arr.put(JSONObject().put("id", id).put("name", name))
        prefs(c).edit().putString("list", arr.toString()).apply()
        return SoundItem(id, "🎵 $name", true)
    }

    fun removeCustom(c: Context, id: String) {
        val arr = JSONArray()
        custom(c).filter { it.id != id }.forEach {
            arr.put(JSONObject().put("id", it.id).put("name", it.name.removePrefix("🎵 ")))
        }
        prefs(c).edit().putString("list", arr.toString()).apply()
        try {
            c.contentResolver.releasePersistableUriPermission(Uri.parse(id.removePrefix("custom:")), Intent.FLAG_GRANT_READ_URI_PERMISSION)
        } catch (_: Exception) {
        }
    }
}

/** پخش صدا + لرزش؛ در یک پروسه است پس هم سرویس و هم صفحه‌ها ازش استفاده می‌کنند */
object AlarmCenter {
    private var mp: MediaPlayer? = null
    private var vib: Vibrator? = null
    private val ids = ArrayList<Int>()

    @Volatile var ringing = false
        private set
    @Volatile var onStopped: (() -> Unit)? = null

    private fun newPlayer(c: Context, id: String, loop: Boolean): MediaPlayer {
        val m = MediaPlayer()
        m.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ALARM)
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                .build()
        )
        when {
            id.startsWith("raw:") -> {
                val res = c.resources.getIdentifier(id.removePrefix("raw:"), "raw", c.packageName)
                c.resources.openRawResourceFd(res).use { m.setDataSource(it.fileDescriptor, it.startOffset, it.length) }
            }
            id.startsWith("custom:") -> m.setDataSource(c, Uri.parse(id.removePrefix("custom:")))
            else -> m.setDataSource(c, systemUri())
        }
        m.isLooping = loop
        m.prepare()
        return m
    }

    private fun systemUri(): Uri =
        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)

    @Synchronized
    fun playSound(c: Context, id: String, loop: Boolean) {
        stopSound()
        val m = try {
            newPlayer(c, id, loop)
        } catch (e: Exception) {
            try { newPlayer(c, "system", loop) } catch (e2: Exception) { return }
        }
        m.setOnCompletionListener { if (!it.isLooping) stopSound() }
        m.start()
        mp = m
    }

    @Synchronized
    fun stopSound() {
        mp?.let {
            try { if (it.isPlaying) it.stop() } catch (_: Exception) {}
            it.release()
        }
        mp = null
    }

    @Synchronized
    fun isPlaying(): Boolean = try { mp?.isPlaying == true } catch (_: Exception) { false }

    @Suppress("DEPRECATION")
    private fun vibrator(c: Context): Vibrator =
        if (Build.VERSION.SDK_INT >= 31) c.getSystemService(VibratorManager::class.java).defaultVibrator
        else c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator

    @Synchronized
    fun startAlarm(c: Context, soundId: String, loop: Boolean, vibrate: Boolean) {
        ringing = true
        if (!isPlaying() || !loop) playSound(c, soundId, loop)
        if (vibrate) {
            val v = vibrator(c)
            v.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 700, 500), if (loop) 0 else -1))
            vib = v
        }
    }

    @Synchronized
    fun track(id: Int) { ids.add(id) }

    /** قطع هشدار: صدا، لرزش و اعلان‌های هشدار */
    @Synchronized
    fun stopAlarm(c: Context) {
        stopSound()
        vib?.cancel(); vib = null
        ringing = false
        val nm = c.getSystemService(android.app.NotificationManager::class.java)
        ids.forEach { nm.cancel(it) }
        ids.clear()
        onStopped?.let { cb -> android.os.Handler(android.os.Looper.getMainLooper()).post { cb() } }
    }
}
