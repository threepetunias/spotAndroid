package com.spotapp.personal

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

// Keeps the home screen widgets and the "workout in progress" notice current.
// Everything here is a bonus: if anything fails the app carries on and nothing is shown or lost.
// The snapshot holds only short display text (no ids that matter, no personal notes).

object PhoneSync {
    // A plain sentence about the notice from the latest sync. Shown in Settings > Phone widgets.
    var status by mutableStateOf("not checked yet")
        private set

    private var job: Job? = null

    // Set by the main screen: asks Android for permission to show the notice, the first time it is wanted.
    var requestPermission: (() -> Unit)? = null

    // Safe to call from anywhere, as often as you like: calls close together are folded into one run.
    fun update(app: AppState) {
        job?.cancel()
        job = app.scope.launch {
            delay(300)
            try {
                run(app)
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                // a bonus feature: never break the app
            }
        }
    }

    private suspend fun run(app: AppState) {
        val ctx = app.appContext
        val data = withContext(app.dispatcher) {
            val today = todayStr()
            val settings = getSettingsFor(app)
            val todays = app.store.listWorkouts().filter { it.day == today }.sortedBy { if (it.done) 1 else 0 }.firstOrNull()
            val full = todays?.let { app.store.getWorkout(it.id) }
            val week = periodRange(Period.WEEK, 0, today)
            val period = listOf("week", "month", "year").firstOrNull { it == settings["pFun"] } ?: "month"
            val unit = if (settings["unit"] == "kg") "kg" else "lb"
            val trained = app.store.trainedDays(week.from, week.to)
            val streak = app.store.streakNow(today)
            val prs = app.store.listPrs(null)
            val total = app.store.funTotal(Period.of(period), today)
            val like = full?.asLike()
            Triple(
                Snapshot(
                    day = today, today = todayInfo(like), progress = progressInfo(like), week = weekDays(week.from, trained),
                    weekCount = trained.map { it.day }.toSet().size, streak = streak, record = recordInfo(prs.latest.firstOrNull()),
                    fun_ = funInfo(funCompare(total, unit), unit, period),
                ),
                (settings["lockCard"] ?: "on") == "on",
                like,
            )
        }
        val (snap, noticeOn, like) = data
        PhoneStore.save(ctx, snap)
        PhoneWidgets.refreshAll(ctx)
        val progress = snap.progress
        status = when {
            !noticeOn -> { Notice.cancel(ctx); "the notice is turned off above" }
            snap.today == null -> { Notice.cancel(ctx); "no workout today" }
            progress == null || !hasStarted(like) -> { Notice.cancel(ctx); "waiting for you to log a set" }
            else -> {
                if (Notice.shouldAsk(ctx)) { Notice.markAsked(ctx); requestPermission?.invoke() }
                Notice.show(ctx, progress)
            }
        }
    }

    private fun getSettingsFor(app: AppState): Map<String, String> = app.store.getSettings()
}

// ---------- the ongoing notice ----------

object Notice {
    private const val CHANNEL = "workout_in_progress"
    private const val ID = 1001

    fun allowed(ctx: Context): Boolean =
        (Build.VERSION.SDK_INT < 33 || ContextCompat.checkSelfPermission(ctx, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) &&
            NotificationManagerCompat.from(ctx).areNotificationsEnabled()

    private fun flags(ctx: Context) = ctx.getSharedPreferences("spot_flags", Context.MODE_PRIVATE)

    // The permission is asked for once, when the notice is first wanted (after the first logged set), never at launch.
    fun shouldAsk(ctx: Context): Boolean =
        Build.VERSION.SDK_INT >= 33 && !allowed(ctx) && !flags(ctx).getBoolean("asked_notifications", false)

    fun markAsked(ctx: Context) {
        flags(ctx).edit().putBoolean("asked_notifications", true).apply()
    }

    // Returns a plain sentence saying what happened.
    fun show(ctx: Context, p: ProgressInfo): String {
        if (!allowed(ctx)) return "notifications are not allowed for Spot in the Android settings"
        val mgr = ctx.getSystemService(NotificationManager::class.java) ?: return "the phone did not give Spot a notification service"
        mgr.createNotificationChannel(NotificationChannel(CHANNEL, "Workout in progress", NotificationManager.IMPORTANCE_LOW).apply {
            description = "Shows your exercise and set while a workout is open. Tap it to return."
            setShowBadge(false)
        })
        val open = PendingIntent.getActivity(
            ctx, 0, deepLinkIntent(ctx, "spot://workout/${p.id}"), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val n = NotificationCompat.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(p.title)
            .setContentText(if (p.setText.isEmpty()) p.exercise else "${p.exercise} · ${p.setText}")
            .setContentIntent(open)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setProgress(Math.max(p.total, 1), p.done, false)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
        return try {
            NotificationManagerCompat.from(ctx).notify(ID, n)
            "notice shown"
        } catch (e: SecurityException) {
            "notifications are not allowed for Spot in the Android settings"
        }
    }

    fun cancel(ctx: Context) {
        try { NotificationManagerCompat.from(ctx).cancel(ID) } catch (e: Exception) { /* nothing to remove */ }
    }
}

// Opens Spot at a spot:// address. The address is checked again by the app before anything opens.
fun deepLinkIntent(ctx: Context, link: String): Intent =
    Intent(Intent.ACTION_VIEW, Uri.parse(link)).setClass(ctx, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)

// ---------- the small box of text the widgets read ----------

object PhoneStore {
    private fun p(c: Context) = c.getSharedPreferences("spot_phone", Context.MODE_PRIVATE)

    fun save(c: Context, s: Snapshot) {
        p(c).edit().clear().apply {
            putString("day", s.day)
            s.today?.let { putLong("tId", it.id); putString("tTitle", it.title); putString("tPlace", it.place); putInt("tDone", it.done); putInt("tTotal", it.total); putBoolean("tFin", it.finished) }
            s.progress?.let { putLong("pId", it.id); putString("pTitle", it.title); putString("pExercise", it.exercise); putString("pSet", it.setText) }
            putInt("weekCount", s.weekCount); putInt("streak", s.streak)
            s.record?.let { putString("rWeight", it.weight); putString("rExercise", it.exercise); putString("rDate", it.dateLabel) }
            s.fun_?.let { putString("fEyebrow", it.eyebrow); putString("fHeadline", it.headline); putString("fCaption", it.caption) }
        }.apply()
    }

    class View(val day: String, val today: TodayInfo?, val progress: ProgressInfo?, val weekCount: Int, val streak: Int, val record: RecordInfo?, val fun_: FunInfo?)

    // Null when nothing was saved yet or the saved day is not today (the widget then asks to open Spot).
    fun load(c: Context): View? {
        val q = p(c)
        val day = q.getString("day", null) ?: return null
        if (day != todayStr()) return null
        val today = if (q.contains("tId")) TodayInfo(q.getLong("tId", 0), q.getString("tTitle", "") ?: "", q.getString("tPlace", "") ?: "", q.getInt("tDone", 0), q.getInt("tTotal", 0), q.getBoolean("tFin", false)) else null
        val prog = if (q.contains("pId")) ProgressInfo(q.getLong("pId", 0), q.getString("pTitle", "") ?: "", q.getString("pExercise", "") ?: "", q.getString("pSet", "") ?: "", "", 0, 0) else null
        val rec = if (q.contains("rWeight")) RecordInfo(q.getString("rWeight", "") ?: "", q.getString("rExercise", "") ?: "", q.getString("rDate", "") ?: "") else null
        val fn = if (q.contains("fHeadline")) FunInfo(q.getString("fEyebrow", "") ?: "", q.getString("fHeadline", "") ?: "", q.getString("fCaption", "") ?: "") else null
        return View(day, today, prog, q.getInt("weekCount", 0), q.getInt("streak", 0), rec, fn)
    }
}
