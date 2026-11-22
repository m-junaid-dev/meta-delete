package com.example.wasaver

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Environment
import android.os.IBinder
import java.io.File

/**
 * WhatsApp ke media folders ko har 2 second mein scan karta hai aur
 * nayi files (video, voice note, document, photo) turant
 * /sdcard/WASaver/<category>/ mein copy kar deta hai.
 */
class MediaWatcherService : Service() {

    @Volatile
    private var running = false
    private var worker: Thread? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        startAsForeground()
        if (!running) {
            running = true
            worker = Thread {
                val db = Db(this)
                while (running) {
                    try {
                        scan(db)
                    } catch (_: Exception) {
                    }
                    try {
                        Thread.sleep(2000)
                    } catch (_: InterruptedException) {
                        break
                    }
                }
            }.also { it.start() }
        }
        return START_STICKY
    }

    override fun onDestroy() {
        running = false
        worker?.interrupt()
        super.onDestroy()
    }

    private fun startAsForeground() {
        val nm = getSystemService(NotificationManager::class.java)
        nm.createNotificationChannel(
            NotificationChannel("watcher", "WA Saver", NotificationManager.IMPORTANCE_LOW)
        )
        val n = Notification.Builder(this, "watcher")
            .setContentTitle("WA Saver chal raha hai")
            .setContentText("WhatsApp media save ho rahi hai")
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setOngoing(true)
            .build()
        if (Build.VERSION.SDK_INT >= 29) {
            startForeground(1, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            startForeground(1, n)
        }
    }

    private fun scan(db: Db) {
        val base = Environment.getExternalStorageDirectory()
        val roots = listOf(
            File(base, "Android/media/com.whatsapp/WhatsApp/Media"),
            File(base, "WhatsApp/Media"),
            File(base, "Android/media/com.whatsapp.w4b/WhatsApp Business/Media")
        )
        val now = System.currentTimeMillis()

        for (root in roots) {
            if (!root.exists()) continue
            root.walkTopDown()
                .onEnter { d -> !d.name.startsWith(".") && d.name != "Sent" }
                .filter { it.isFile && !it.name.startsWith(".") }
                .forEach { f ->
                    // abhi likhi ja rahi file skip karo, agle scan mein aayegi
                    if (f.length() == 0L || now - f.lastModified() < 1500) return@forEach

                    val key = "${f.absolutePath}:${f.length()}"
                    if (db.isFileSaved(key)) return@forEach

                    val rel = f.relativeTo(root).path
                    val category = if (rel.contains('/')) rel.substringBefore('/') else "Other"
                    val dir = File(base, "WASaver/$category").apply { mkdirs() }

                    var dest = File(dir, f.name)
                    if (dest.exists()) dest = File(dir, "${System.currentTimeMillis()}_${f.name}")

                    try {
                        f.copyTo(dest, overwrite = false)
                        db.markFileSaved(key)
                    } catch (_: Exception) {
                        // file beech mein delete ho gayi ya copy fail, agle scan mein dobara try
                    }
                }
        }
    }
}
