package com.example.wasaver

import android.app.Notification
import android.os.Bundle
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification

/**
 * WhatsApp ki har notification aate hi uska text save kar leta hai.
 * Sender baad mein delete kare tab bhi copy tumhare paas rehti hai.
 */
class MessageListener : NotificationListenerService() {

    private val targets = setOf("com.whatsapp", "com.whatsapp.w4b")

    private val deletedMarkers = listOf(
        "this message was deleted",
        "you deleted this message",
        "message deleted"
    )

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName !in targets) return
        val n = sbn.notification ?: return
        if (n.flags and Notification.FLAG_GROUP_SUMMARY != 0) return

        val extras = n.extras ?: return
        val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString()?.trim().orEmpty()
        if (title.isEmpty() || title.equals("WhatsApp", ignoreCase = true)) return

        val db = Db(this)

        @Suppress("DEPRECATION")
        val bundles = extras.getParcelableArray(Notification.EXTRA_MESSAGES)

        if (bundles != null && bundles.isNotEmpty()) {
            for (p in bundles) {
                val b = p as? Bundle ?: continue
                val text = b.getCharSequence("text")?.toString()?.trim().orEmpty()
                if (text.isEmpty()) continue
                val time = b.getLong("time", sbn.postTime)
                val s = b.getCharSequence("sender")?.toString()?.trim()
                val who = if (!s.isNullOrEmpty() && s != title) "$title • $s" else title
                handle(db, who, text, time)
            }
        } else {
            val text = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString()?.trim().orEmpty()
            if (text.isNotEmpty()) handle(db, title, text, sbn.postTime)
        }
    }

    private fun handle(db: Db, who: String, text: String, time: Long) {
        val low = text.lowercase()
        if (deletedMarkers.any { low.contains(it) }) {
            db.markLastDeleted(who)
        } else {
            db.addMessage(who, text, time)
        }
    }
}
