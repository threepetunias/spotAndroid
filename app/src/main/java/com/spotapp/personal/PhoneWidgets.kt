package com.spotapp.personal

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.ComponentName
import android.content.Context
import android.widget.RemoteViews

// Home screen widgets. Each one is a small card of text that opens Spot at the right screen.
// They read the box PhoneStore keeps, never the database.

class WidgetText(val eyebrow: String, val title: String, val sub: String, val link: String)

private const val OPEN_SPOT = "Open Spot to set this up."

private fun plural(n: Int, one: String, many: String) = if (n == 1) one else many

abstract class SpotWidget : AppWidgetProvider() {
    abstract fun text(ctx: Context, v: PhoneStore.View?): WidgetText

    override fun onUpdate(context: Context, manager: AppWidgetManager, ids: IntArray) {
        PhoneWidgets.render(context, manager, ids, this)
    }
}

class TodayWidgetProvider : SpotWidget() {
    override fun text(ctx: Context, v: PhoneStore.View?): WidgetText {
        if (v == null) return WidgetText("Today", "Spot", OPEN_SPOT, "spot://")
        val t = v.today ?: return WidgetText("Today", "No workout planned", "Tap to start one", "spot://workout/new")
        val sub = if (t.finished) "Finished" else "${t.done} of ${t.total} ${plural(t.total, "exercise", "exercises")}" + (if (t.place.isEmpty()) "" else " · ${t.place}")
        return WidgetText("Today", t.title, sub, "spot://workout/${t.id}")
    }
}

class WeekWidgetProvider : SpotWidget() {
    override fun text(ctx: Context, v: PhoneStore.View?): WidgetText {
        if (v == null) return WidgetText("This week", "Spot", OPEN_SPOT, "spot://")
        return WidgetText("This week", "${v.weekCount} ${plural(v.weekCount, "day", "days")} trained", "${v.streak} ${plural(v.streak, "week", "weeks")} in a row", "spot://log")
    }
}

class RecordWidgetProvider : SpotWidget() {
    override fun text(ctx: Context, v: PhoneStore.View?): WidgetText {
        if (v == null) return WidgetText("Newest record", "Spot", OPEN_SPOT, "spot://")
        val r = v.record ?: return WidgetText("Newest record", "No records yet", "Log a set to start", "spot://prs")
        return WidgetText("Newest record", r.weight, "${r.exercise} · ${r.dateLabel}", "spot://prs")
    }
}

class FunWidgetProvider : SpotWidget() {
    override fun text(ctx: Context, v: PhoneStore.View?): WidgetText {
        if (v == null) return WidgetText("Fun total", "Spot", OPEN_SPOT, "spot://")
        val f = v.fun_ ?: return WidgetText("Fun total", "A good start", "", "spot://")
        return WidgetText(f.eyebrow, f.headline, f.caption, "spot://")
    }
}

// "Add workout", or "Back to workout" once a set has been logged.
class ActionWidgetProvider : SpotWidget() {
    override fun text(ctx: Context, v: PhoneStore.View?): WidgetText {
        val p = v?.progress ?: return WidgetText("Spot", "Add workout", "Start today's workout", "spot://workout/new")
        return WidgetText("Workout in progress", "Back to workout", if (p.setText.isEmpty()) p.exercise else "${p.exercise} · ${p.setText}", "spot://workout/${p.id}")
    }
}

object PhoneWidgets {
    private val PROVIDERS: List<Pair<Class<out SpotWidget>, () -> SpotWidget>> = listOf(
        TodayWidgetProvider::class.java to { TodayWidgetProvider() },
        WeekWidgetProvider::class.java to { WeekWidgetProvider() },
        RecordWidgetProvider::class.java to { RecordWidgetProvider() },
        FunWidgetProvider::class.java to { FunWidgetProvider() },
        ActionWidgetProvider::class.java to { ActionWidgetProvider() },
    )

    fun refreshAll(ctx: Context) {
        val mgr = AppWidgetManager.getInstance(ctx)
        for ((cls, make) in PROVIDERS) {
            val ids = mgr.getAppWidgetIds(ComponentName(ctx, cls))
            if (ids.isEmpty()) continue
            render(ctx, mgr, ids, make())
        }
    }

    fun render(ctx: Context, mgr: AppWidgetManager, ids: IntArray, w: SpotWidget) {
        val t = w.text(ctx, PhoneStore.load(ctx))
        val open = PendingIntent.getActivity(ctx, t.link.hashCode(), deepLinkIntent(ctx, t.link), PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        for (id in ids) {
            val views = RemoteViews(ctx.packageName, R.layout.widget_card)
            views.setTextViewText(R.id.w_eyebrow, t.eyebrow.uppercase())
            views.setTextViewText(R.id.w_title, t.title)
            views.setTextViewText(R.id.w_sub, t.sub)
            views.setOnClickPendingIntent(R.id.w_root, open)
            mgr.updateAppWidget(id, views)
        }
    }
}
