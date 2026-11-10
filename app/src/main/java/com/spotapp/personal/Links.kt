package com.spotapp.personal

import android.content.Context
import android.content.Intent
import android.net.Uri

// Opens a web address the person saved on an exercise. Only http and https are allowed (checked again here).
fun openLink(context: Context, raw: String, app: AppState) {
    try {
        val clean = cleanLink(raw)
        if (clean.isEmpty()) return
        val i = Intent(Intent.ACTION_VIEW, Uri.parse(clean)).addCategory(Intent.CATEGORY_BROWSABLE).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(i)
    } catch (e: InputError) {
        app.say("Link", e.message ?: "That link cannot be opened.")
    } catch (e: Exception) {
        app.say("Link", "That link could not be opened.")
    }
}
