package com.spotapp.personal

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// The rest timer lives here, outside any screen, so it keeps running when you go to another screen or another app.
// It stores the end time (not a count). While it runs, an ongoing notification shows the time left (Android counts it down by itself,
// so Spot does not wake up every second). When the end comes, Spot buzzes and the notification changes to "Rest is over".
// A backup alarm does the same if Spot was closed by the phone in the meantime. No exact-alarm permission is needed: the backup may be a little late.

object RestTimer {
    var total by mutableStateOf(60)
        private set
    var endAt by mutableStateOf<Long?>(null)
        private set
    var finished by mutableStateOf(false)
        private set

    private var job: Job? = null

    fun start(app: AppState, secs: Int) {
        val ctx = app.appContext
        job?.cancel()
        stopBuzz(ctx)
        val end = System.currentTimeMillis() + secs * 1000L
        total = secs; finished = false; endAt = end
        RestNotice.begin(ctx, end)
        job = app.scope.launch {
            delay(secs * 1000L)
            finish(ctx, buzz = true)
        }
    }

    fun stop(ctx: Context) {
        job?.cancel()
        stopBuzz(ctx)
        endAt = null; finished = false
        RestNotice.clear(ctx)
    }

    // Coming back to Spot: if the end passed while the screen was off, show it finished.
    fun settle(ctx: Context) {
        val end = endAt ?: return
        if (System.currentTimeMillis() >= end) finish(ctx, buzz = false)
    }

    private fun finish(ctx: Context, buzz: Boolean) {
        if (endAt == null) return
        job?.cancel()
        endAt = null; finished = true
        RestNotice.over(ctx)
        if (buzz) buzz(ctx)
    }
}

object RestNotice {
    private const val CHANNEL_RUN = "rest_timer"
    private const val CHANNEL_OVER = "rest_over"
    private const val ID = 1002
    const val ACTION_OVER = "com.spotapp.personal.REST_OVER"
    private const val KEY_END = "rest_end"

    private fun flags(ctx: Context) = ctx.getSharedPreferences("spot_flags", Context.MODE_PRIVATE)

    private fun openTimer(ctx: Context) = PendingIntent.getActivity(
        ctx, 2000, deepLinkIntent(ctx, "spot://timer"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun alarmIntent(ctx: Context) = PendingIntent.getBroadcast(
        ctx, 2001, Intent(ctx, RestTimerReceiver::class.java).setAction(ACTION_OVER), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun channels(ctx: Context, mgr: NotificationManager) {
        mgr.createNotificationChannel(NotificationChannel(CHANNEL_RUN, "Rest timer", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows the time left while a rest is running."
            setShowBadge(false)
        })
        mgr.createNotificationChannel(NotificationChannel(CHANNEL_OVER, "Rest is over", NotificationManager.IMPORTANCE_HIGH).apply {
            description = "Tells you when a rest ends."
            setShowBadge(false)
        })
    }

    fun begin(ctx: Context, end: Long) {
        flags(ctx).edit().putLong(KEY_END, end).apply()
        try {
            (ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, end, alarmIntent(ctx))
        } catch (e: Exception) { /* the in-app timer still runs */ }
        if (!Notice.allowed(ctx)) return
        try {
            val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
            channels(ctx, mgr)
            val n = NotificationCompat.Builder(ctx, CHANNEL_RUN)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Resting")
                .setContentText("Time left until your next set")
                .setContentIntent(openTimer(ctx))
                .setOngoing(true)
                .setOnlyAlertOnce(true)
                .setWhen(end)
                .setShowWhen(true)
                .setUsesChronometer(true)
                .setChronometerCountDown(true)
                .setCategory(NotificationCompat.CATEGORY_PROGRESS)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            NotificationManagerCompat.from(ctx).notify(ID, n)
        } catch (e: SecurityException) { /* notifications are off: the timer still works in the app */ }
    }

    // The end has come: replace the countdown with a short "Rest is over" alert.
    fun over(ctx: Context) {
        flags(ctx).edit().remove(KEY_END).apply()
        try { (ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(alarmIntent(ctx)) } catch (e: Exception) { /* nothing to cancel */ }
        if (!Notice.allowed(ctx)) return
        try {
            val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return
            channels(ctx, mgr)
            val n = NotificationCompat.Builder(ctx, CHANNEL_OVER)
                .setSmallIcon(R.drawable.ic_notification)
                .setContentTitle("Rest is over")
                .setContentText("Time for your next set.")
                .setContentIntent(openTimer(ctx))
                .setAutoCancel(true)
                .setTimeoutAfter(60_000)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build()
            NotificationManagerCompat.from(ctx).notify(ID, n)
        } catch (e: SecurityException) { /* notifications are off */ }
    }

    fun clear(ctx: Context) {
        flags(ctx).edit().remove(KEY_END).apply()
        try { (ctx.getSystemService(Context.ALARM_SERVICE) as? AlarmManager)?.cancel(alarmIntent(ctx)) } catch (e: Exception) { /* nothing to cancel */ }
        try { NotificationManagerCompat.from(ctx).cancel(ID) } catch (e: Exception) { /* nothing to remove */ }
    }

    // True when a rest was started and has not been finished or stopped (used by the backup alarm).
    fun pendingEnd(ctx: Context): Long = flags(ctx).getLong(KEY_END, 0L)

    // The permission is asked for once, the first time a rest is started.
    fun shouldAsk(ctx: Context): Boolean =
        Build.VERSION.SDK_INT >= 33 && !Notice.allowed(ctx) && !flags(ctx).getBoolean("asked_rest_notifications", false)

    fun markAsked(ctx: Context) {
        flags(ctx).edit().putBoolean("asked_rest_notifications", true).apply()
    }
}

// The backup alarm: if Spot was closed by the phone before the rest ended, this shows "Rest is over" and buzzes.
// It is not exported, so only Spot can send it, and it does nothing unless a rest is really pending.
class RestTimerReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != RestNotice.ACTION_OVER) return
        val ctx = context.applicationContext
        if (RestNotice.pendingEnd(ctx) == 0L) return // already finished or stopped inside the app
        RestNotice.over(ctx)
        buzz(ctx)
    }
}

fun buzz(context: Context) {
    try {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        v?.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 250, 500, 250, 500), -1))
    } catch (e: Exception) { /* no vibration on this phone */ }
}

fun stopBuzz(context: Context) {
    try {
        val v: Vibrator? = if (Build.VERSION.SDK_INT >= 31) (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)?.defaultVibrator
        else @Suppress("DEPRECATION") (context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        v?.cancel()
    } catch (e: Exception) { /* nothing to stop on this phone */ }
}
