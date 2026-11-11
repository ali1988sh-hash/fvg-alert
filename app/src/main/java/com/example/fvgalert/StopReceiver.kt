package com.example.fvgalert

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

/** دکمه «قطع هشدار» داخل اعلان */
class StopReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent?) = AlarmCenter.stopAlarm(c)
}
