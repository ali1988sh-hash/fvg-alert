package com.example.fvgalert

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path

object Notif {
    val GREEN = Color.parseColor("#22C55E")
    val RED = Color.parseColor("#EF4444")

    /** آیکن دایره‌ای سبز (صعودی) یا قرمز (نزولی) با فلش سفید */
    fun dirIcon(bull: Boolean, size: Int = 192): Bitmap {
        val bmp = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(bmp)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.color = if (bull) GREEN else RED
        c.drawCircle(size / 2f, size / 2f, size / 2f, p)
        p.color = Color.WHITE
        val s = size.toFloat()
        val path = Path()
        if (bull) {
            path.moveTo(s * .5f, s * .22f); path.lineTo(s * .78f, s * .55f); path.lineTo(s * .60f, s * .55f)
            path.lineTo(s * .60f, s * .78f); path.lineTo(s * .40f, s * .78f); path.lineTo(s * .40f, s * .55f)
            path.lineTo(s * .22f, s * .55f)
        } else {
            path.moveTo(s * .5f, s * .78f); path.lineTo(s * .78f, s * .45f); path.lineTo(s * .60f, s * .45f)
            path.lineTo(s * .60f, s * .22f); path.lineTo(s * .40f, s * .22f); path.lineTo(s * .40f, s * .45f)
            path.lineTo(s * .22f, s * .45f)
        }
        path.close()
        c.drawPath(path, p)
        return bmp
    }
}
