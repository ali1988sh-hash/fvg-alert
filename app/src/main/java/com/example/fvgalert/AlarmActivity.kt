package com.example.fvgalert

import android.app.Activity
import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.*

/** صفحه تمام‌صفحه هشدار (روی قفل صفحه هم نمایش داده می‌شود) با دکمه بزرگ قطع */
class AlarmActivity : Activity() {
    private lateinit var title: TextView
    private lateinit var icon: ImageView
    private lateinit var stop: Button

    private fun dp(n: Int) = (n * resources.displayMetrics.density).toInt()

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        @Suppress("DEPRECATION")
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON or
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        )

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            layoutDirection = View.LAYOUT_DIRECTION_RTL
            setBackgroundColor(Color.parseColor("#0D1117"))
            setPadding(dp(28), dp(28), dp(28), dp(28))
        }
        icon = ImageView(this).also { root.addView(it, LinearLayout.LayoutParams(dp(120), dp(120))) }
        title = TextView(this).apply {
            textSize = 24f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            gravity = Gravity.CENTER; setPadding(0, dp(28), 0, dp(8))
        }.also { root.addView(it) }
        root.addView(TextView(this).apply {
            text = "XAU/USD"; textSize = 14f; setTextColor(Color.parseColor("#8B949E")); gravity = Gravity.CENTER
        })
        stop = Button(this).apply {
            text = "⏹  قطع هشدار"; textSize = 20f; setTextColor(Color.WHITE); typeface = Typeface.DEFAULT_BOLD
            isAllCaps = false
            background = GradientDrawable().apply { cornerRadius = dp(18).toFloat(); setColor(Color.parseColor("#EF4444")) }
            setOnClickListener { AlarmCenter.stopAlarm(this@AlarmActivity); finish() }
        }
        root.addView(stop, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(72)).apply { topMargin = dp(48) })
        setContentView(root)
        show(intent)
    }

    override fun onNewIntent(i: Intent) {
        super.onNewIntent(i)
        setIntent(i); show(i)
    }

    private fun show(i: Intent) {
        val bull = i.getBooleanExtra("bull", true)
        title.text = i.getStringExtra("msg") ?: "هشدار FVG"
        icon.setImageBitmap(Notif.dirIcon(bull))
    }

    override fun onStart() {
        super.onStart()
        AlarmCenter.onStopped = { finish() }
        if (!AlarmCenter.ringing) finish()
    }

    override fun onStop() {
        if (AlarmCenter.onStopped != null) AlarmCenter.onStopped = null
        super.onStop()
    }
}
