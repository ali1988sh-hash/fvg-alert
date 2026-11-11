package com.example.fvgalert

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.TimeZone

data class Candle(val t: Long, val o: Double, val h: Double, val l: Double, val c: Double)

enum class AlertKind { TOUCH, NEW }

class Fvg(val key: Long, val bull: Boolean, val top: Double, val bot: Double, val at: Long) {
    /** آیا قیمت الان داخل ناحیه است؟ هر بار که از بیرون وارد شود یک هشدار جدید می‌دهیم */
    var inside = false
    var active = true
}

/** همون منطق اندیکاتور FVG MTF */
class Engine(
    private val apiKey: String,
    private val tfMin: Int,
    private val minGap: Double,
    private val alertTouch: Boolean,
    private val alertNew: Boolean,
    private val onAlert: (AlertKind, Boolean) -> Unit
) {
    val fvgs = LinkedHashMap<Long, Fvg>()
    var price = Double.NaN
    private var lastTs = -1L

    private fun interval(m: Int) = when {
        m <= 1 -> "1min"
        m >= 60 -> "${m / 60}h"
        else -> "${m}min"
    }

    private fun fetch(minutes: Int, n: Int): List<Candle> {
        val u = "https://api.twelvedata.com/time_series?symbol=XAU/USD&interval=${interval(minutes)}" +
                "&outputsize=$n&timezone=UTC&apikey=$apiKey"
        val conn = URL(u).openConnection() as HttpURLConnection
        conn.connectTimeout = 15000
        conn.readTimeout = 15000
        val body = try {
            (if (conn.responseCode < 400) conn.inputStream else conn.errorStream).bufferedReader().readText()
        } finally {
            conn.disconnect()
        }
        val j = JSONObject(body)
        val arr = j.optJSONArray("values") ?: throw RuntimeException(j.optString("message", "no data"))
        val fmt = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).apply { timeZone = TimeZone.getTimeZone("UTC") }
        val out = ArrayList<Candle>()
        for (i in 0 until arr.length()) {
            val v = arr.getJSONObject(i)
            val dt = v.getString("datetime").let { if (it.length <= 10) "$it 00:00:00" else it }
            out.add(Candle(fmt.parse(dt)!!.time, v.getDouble("open"), v.getDouble("high"), v.getDouble("low"), v.getDouble("close")))
        }
        out.sortBy { it.t }
        return out
    }

    private fun replay(f: Fvg, d: List<Candle>, i: Int) {
        for (j in i + 1 until d.size) {
            val c = d[j]
            if ((f.bull && c.c < f.bot) || (!f.bull && c.c > f.top)) { f.active = false; return }
        }
    }

    fun refresh(first: Boolean) {
        val tf = tfMin * 60000L
        val now = System.currentTimeMillis()
        val d = fetch(tfMin, 300).filter { it.t + tf <= now }.takeLast(200)
        if (d.size < 3) return
        for (i in 2 until d.size) {
            val a = d[i - 2]; val m = d[i - 1]; val c = d[i]
            val f = when {
                c.l > a.h && c.l - a.h >= minGap -> Fvg(m.t, true, c.l, a.h, c.t + tf)
                c.h < a.l && a.l - c.h >= minGap -> Fvg(m.t, false, a.l, c.h, c.t + tf)
                else -> null
            } ?: continue
            if (fvgs.containsKey(f.key)) continue
            replay(f, d, i)
            fvgs[f.key] = f
            if (!first && f.active && i == d.size - 1 && alertNew) onAlert(AlertKind.NEW, f.bull)
        }
        val oldest = d[0].t
        fvgs.keys.removeAll { it < oldest }
    }

    fun check(first: Boolean) {
        val d = fetch(1, 120)
        val w = if (lastTs < 0 || first) d.takeLast(2) else d.filter { it.t >= lastTs }
        for (x in w) for (f in fvgs.values) {
            if (!f.active || x.t < f.at) continue
            if ((f.bull && x.c < f.bot) || (!f.bull && x.c > f.top)) { f.active = false; f.inside = false; continue }
            val now = x.l <= f.top && x.h >= f.bot
            // هر ورود تازه به ناحیه = یک هشدار (نه فقط اولین برخورد)
            if (now && !f.inside && !first && alertTouch) onAlert(AlertKind.TOUCH, f.bull)
            f.inside = now
        }
        if (d.isNotEmpty()) { lastTs = d.last().t; price = d.last().c }
    }

    fun activeCount() = fvgs.values.count { it.active }
}
