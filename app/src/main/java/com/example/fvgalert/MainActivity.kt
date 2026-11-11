package com.example.fvgalert

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.res.ColorStateList
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.InputType
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*

class MainActivity : Activity() {
    private companion object {
        const val REQ_SOUND = 11
        val BG = Color.parseColor("#0D1117")
        val CARD = Color.parseColor("#161B22")
        val LINE = Color.parseColor("#30363D")
        val FIELD = Color.parseColor("#0D1117")
        val GOLD = Color.parseColor("#F5B800")
        val TEXT = Color.parseColor("#E6EDF3")
        val MUTED = Color.parseColor("#8B949E")
    }

    private lateinit var key: EditText
    private lateinit var tf: EditText
    private lateinit var min: EditText
    private lateinit var poll: EditText
    private lateinit var touch: Switch
    private lateinit var nw: Switch
    private lateinit var loop: Switch
    private lateinit var vibe: Switch
    private lateinit var btn: Button
    private lateinit var chip: TextView
    private lateinit var spinner: Spinner
    private lateinit var preview: Button
    private lateinit var del: Button
    private lateinit var root: LinearLayout
    private var sounds: List<SoundItem> = emptyList()

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()
    private fun prefs() = getSharedPreferences("cfg", MODE_PRIVATE)

    private fun box(color: Int, radius: Int, stroke: Int = 0) = GradientDrawable().apply {
        setColor(color); cornerRadius = dp(radius).toFloat()
        if (stroke != 0) setStroke(dp(1), stroke)
    }

    private fun lp(top: Int = 0) = LinearLayout.LayoutParams(
        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
    ).apply { topMargin = dp(top) }

    private fun card(title: String, icon: String): LinearLayout {
        val c = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            background = box(CARD, 18, LINE)
            setPadding(dp(18), dp(16), dp(18), dp(18))
        }
        c.addView(TextView(this).apply {
            text = "$icon  $title"; textSize = 15f; setTextColor(GOLD); typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(c, lp(14))
        return c
    }

    private fun label(parent: LinearLayout, t: String) = TextView(this).apply {
        text = t; textSize = 12.5f; setTextColor(MUTED); setPadding(dp(2), dp(14), 0, dp(6))
    }.also { parent.addView(it) }

    private fun field(v: String, type: Int) = EditText(this).apply {
        setText(v); inputType = type; setTextColor(TEXT); textSize = 15f
        setHintTextColor(MUTED)
        background = box(FIELD, 12, LINE)
        setPadding(dp(14), dp(12), dp(14), dp(12))
        textDirection = View.TEXT_DIRECTION_LTR
        gravity = Gravity.START or Gravity.CENTER_VERTICAL
        isSingleLine = true
    }

    private fun switchRow(parent: LinearLayout, text: String, sub: String, on: Boolean): Switch {
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL; gravity = Gravity.CENTER_VERTICAL }
        val col = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL }
        col.addView(TextView(this).apply { this.text = text; textSize = 15f; setTextColor(TEXT) })
        col.addView(TextView(this).apply { this.text = sub; textSize = 11.5f; setTextColor(MUTED) })
        row.addView(col, LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f))
        val sw = Switch(this).apply {
            isChecked = on
            val states = arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf())
            thumbTintList = ColorStateList(states, intArrayOf(GOLD, Color.parseColor("#6E7681")))
            trackTintList = ColorStateList(states, intArrayOf(Color.parseColor("#66F5B800"), LINE))
        }
        row.addView(sw)
        parent.addView(row, lp(14))
        return sw
    }

    private fun smallBtn(t: String, bg: Int, fg: Int = Color.WHITE) = Button(this).apply {
        text = t; textSize = 13f; isAllCaps = false; setTextColor(fg)
        background = box(bg, 12); stateListAnimator = null
        minHeight = 0; minimumHeight = 0; setPadding(dp(8), dp(10), dp(8), dp(10))
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        window.statusBarColor = BG
        window.navigationBarColor = BG
        val p = prefs()

        root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setPadding(dp(16), dp(28), dp(16), dp(32))
        }

        // ---------- هدر ----------
        val head = LinearLayout(this).apply { orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_HORIZONTAL }
        head.addView(ImageView(this).apply {
            setImageResource(R.drawable.app_logo)
            scaleType = ImageView.ScaleType.FIT_CENTER
        }, LinearLayout.LayoutParams(dp(104), dp(104)))
        head.addView(TextView(this).apply {
            text = "FVG Alert"; textSize = 26f; setTextColor(TEXT); typeface = Typeface.DEFAULT_BOLD
            setPadding(0, dp(12), 0, 0)
        })
        head.addView(TextView(this).apply {
            text = "هشدار شکاف ارزش منصفانه · XAU/USD"; textSize = 13f; setTextColor(MUTED)
        })
        chip = TextView(this).apply {
            textSize = 12.5f; typeface = Typeface.DEFAULT_BOLD; setPadding(dp(14), dp(6), dp(14), dp(6))
        }
        head.addView(chip, LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            .apply { topMargin = dp(12) })
        root.addView(head, lp())

        btn = Button(this).apply {
            textSize = 17f; typeface = Typeface.DEFAULT_BOLD; isAllCaps = false; stateListAnimator = null
            setOnClickListener { if (FvgService.running) stopSvc() else startSvc() }
        }
        root.addView(btn, LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(58)).apply { topMargin = dp(20) })

        // ---------- اتصال ----------
        val c1 = card("اتصال", "🔑")
        label(c1, "Twelve Data API key")
        key = field(p.getString("key", "") ?: "", InputType.TYPE_CLASS_TEXT).also { c1.addView(it, lp()) }

        // ---------- تنظیمات ----------
        val c2 = card("تنظیمات FVG", "⚙️")
        label(c2, "تایم‌فریم FVG به دقیقه (5 / 15 / 30 / 60)")
        tf = field(p.getString("tf", "15") ?: "15", InputType.TYPE_CLASS_NUMBER).also { c2.addView(it, lp()) }
        label(c2, "حداقل اندازه گپ (دلار)")
        min = field(p.getString("min", "0") ?: "0", InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL)
            .also { c2.addView(it, lp()) }
        label(c2, "فاصله چک به ثانیه (حداقل ۶۰)")
        poll = field(p.getString("poll", "150") ?: "150", InputType.TYPE_CLASS_NUMBER).also { c2.addView(it, lp()) }

        // ---------- هشدارها ----------
        val c3 = card("هشدارها", "🔔")
        touch = switchRow(c3, "رسیدن قیمت به FVG", "هر بار که قیمت وارد ناحیه شود هشدار می‌دهد", p.getBoolean("touch", true))
        nw = switchRow(c3, "تشکیل FVG جدید", "وقتی یک FVG تازه ساخته شود", p.getBoolean("new", true))
        loop = switchRow(c3, "زنگ پی‌درپی", "تا وقتی خودت قطع نکنی ادامه دارد", p.getBoolean("loop", true))
        vibe = switchRow(c3, "لرزش", "همراه با زنگ", p.getBoolean("vibe", true))

        // ---------- آهنگ ----------
        val c4 = card("آهنگ هشدار", "🎵")
        spinner = Spinner(this).apply { background = box(FIELD, 12, LINE); setPadding(dp(10), dp(4), dp(10), dp(4)) }
        c4.addView(spinner, lp(12))
        val row = LinearLayout(this).apply { orientation = LinearLayout.HORIZONTAL }
        preview = smallBtn("▶  پخش", Color.parseColor("#238636"))
        val add = smallBtn("＋  افزودن آهنگ", Color.parseColor("#1F6FEB"))
        del = smallBtn("🗑  حذف", Color.parseColor("#6E1B1B"))
        val w = { m: Int -> LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f).apply { marginStart = dp(m) } }
        row.addView(preview, w(0)); row.addView(add, w(8)); row.addView(del, w(8))
        c4.addView(row, lp(12))
        c4.addView(TextView(this).apply {
            text = "با «افزودن آهنگ» هر فایل صوتی گوشی (mp3، wav، ogg …) رو می‌تونی به لیست اضافه کنی."
            textSize = 11.5f; setTextColor(MUTED); setPadding(dp(2), dp(10), 0, 0)
        })
        preview.setOnClickListener {
            if (AlarmCenter.isPlaying()) AlarmCenter.stopSound()
            else AlarmCenter.playSound(this, selectedSound().id, false)
            refreshPreviewBtn()
        }
        add.setOnClickListener {
            startActivityForResult(Intent(Intent.ACTION_OPEN_DOCUMENT).apply {
                type = "audio/*"; addCategory(Intent.CATEGORY_OPENABLE)
                putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION)
            }, REQ_SOUND)
        }
        del.setOnClickListener {
            val s = selectedSound()
            if (s.custom) {
                AlarmCenter.stopSound()
                Sounds.removeCustom(this, s.id)
                prefs().edit().putString("sound", Sounds.DEFAULT_ID).apply()
                loadSounds(Sounds.DEFAULT_ID)
            }
        }
        loadSounds(p.getString("sound", Sounds.DEFAULT_ID) ?: Sounds.DEFAULT_ID)

        // ---------- پایین ----------
        root.addView(Button(this).apply {
            text = "🔋  غیرفعال کردن بهینه‌سازی باتری (پیشنهادی)"
            textSize = 13f; isAllCaps = false; setTextColor(TEXT); stateListAnimator = null
            background = box(Color.TRANSPARENT, 14, LINE)
            setOnClickListener { startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS)) }
        }, lp(18))
        root.addView(TextView(this).apply {
            text = "بعد از شروع، سرویس تو پس‌زمینه با یه اعلان دائمی کار می‌کنه."
            textSize = 11.5f; setTextColor(MUTED); gravity = Gravity.CENTER; setPadding(0, dp(12), 0, 0)
        })

        setContentView(ScrollView(this).apply { setBackgroundColor(BG); addView(root) })
    }

    private fun selectedSound(): SoundItem =
        sounds.getOrNull(spinner.selectedItemPosition) ?: sounds.first()

    private fun loadSounds(selectId: String) {
        sounds = Sounds.all(this)
        spinner.adapter = object : ArrayAdapter<SoundItem>(this, android.R.layout.simple_spinner_item, sounds) {
            override fun getView(pos: Int, cv: View?, parent: ViewGroup): View = bind(pos, super.getView(pos, cv, parent), false)
            override fun getDropDownView(pos: Int, cv: View?, parent: ViewGroup): View = bind(pos, super.getDropDownView(pos, cv, parent), true)
            private fun bind(pos: Int, v: View, dropdown: Boolean): View {
                (v as TextView).apply {
                    text = sounds[pos].name; setTextColor(TEXT); textSize = 15f
                    layoutDirection = View.LAYOUT_DIRECTION_RTL
                    setPadding(dp(12), dp(12), dp(12), dp(12))
                    if (dropdown) setBackgroundColor(CARD)
                }
                return v
            }
        }
        val idx = sounds.indexOfFirst { it.id == selectId }.let { if (it < 0) 0 else it }
        spinner.setSelection(idx)
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(a: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                prefs().edit().putString("sound", sounds[pos].id).apply()
                del.alpha = if (sounds[pos].custom) 1f else 0.35f
                del.isEnabled = sounds[pos].custom
            }
            override fun onNothingSelected(a: AdapterView<*>?) {}
        }
        del.alpha = if (sounds[idx].custom) 1f else 0.35f
        del.isEnabled = sounds[idx].custom
    }

    private fun refreshPreviewBtn() {
        preview.text = if (AlarmCenter.isPlaying()) "⏹  توقف" else "▶  پخش"
    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(req: Int, res: Int, data: Intent?) {
        super.onActivityResult(req, res, data)
        if (req != REQ_SOUND || res != RESULT_OK || data == null) return
        val flags = data.flags
        var last: SoundItem? = null
        val clip = data.clipData
        if (clip != null) {
            for (i in 0 until clip.itemCount) last = Sounds.addCustom(this, clip.getItemAt(i).uri, flags) ?: last
        } else data.data?.let { last = Sounds.addCustom(this, it, flags) }
        last?.let {
            prefs().edit().putString("sound", it.id).apply()
            loadSounds(it.id)
            Toast.makeText(this, "آهنگ اضافه شد", Toast.LENGTH_SHORT).show()
        }
    }

    private fun setRunning(on: Boolean) {
        btn.text = if (on) "⏹  توقف" else "▶  شروع"
        btn.setTextColor(if (on) Color.WHITE else Color.parseColor("#1A1300"))
        btn.background = box(if (on) Color.parseColor("#DA3633") else GOLD, 16)
        chip.text = if (on) "● فعال" else "● متوقف"
        chip.setTextColor(if (on) Notif.GREEN else Notif.RED)
        chip.background = box(if (on) Color.parseColor("#1A22C55E") else Color.parseColor("#1AEF4444"), 20)
    }

    override fun onResume() {
        super.onResume()
        setRunning(FvgService.running)
        refreshPreviewBtn()
    }

    override fun onPause() {
        saveSettings()
        if (AlarmCenter.isPlaying() && !AlarmCenter.ringing) AlarmCenter.stopSound()
        super.onPause()
    }

    private fun saveSettings() {
        prefs().edit()
            .putString("key", key.text.toString().trim())
            .putString("tf", tf.text.toString())
            .putString("min", min.text.toString())
            .putString("poll", poll.text.toString())
            .putBoolean("touch", touch.isChecked)
            .putBoolean("new", nw.isChecked)
            .putBoolean("loop", loop.isChecked)
            .putBoolean("vibe", vibe.isChecked)
            .apply()
    }

    private fun startSvc() {
        if (key.text.isBlank()) { Toast.makeText(this, "کلید API رو وارد کن", Toast.LENGTH_SHORT).show(); return }
        saveSettings()
        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        startForegroundService(Intent(this, FvgService::class.java))
        setRunning(true)
    }

    private fun stopSvc() {
        stopService(Intent(this, FvgService::class.java))
        setRunning(false)
    }
}
