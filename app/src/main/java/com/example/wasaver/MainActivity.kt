package com.example.wasaver

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.webkit.MimeTypeMap
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.FileProvider
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.wasaver.databinding.ActivityMainBinding
import com.google.android.material.tabs.TabLayout
import java.io.File
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var b: ActivityMainBinding
    private lateinit var db: Db
    private val msgAdapter = MessageAdapter()
    private val fileAdapter = FileAdapter { openFile(it) }
    private val bg = Executors.newSingleThreadExecutor()
    private val ui = Handler(Looper.getMainLooper())

    private var allMessages: List<Msg> = emptyList()
    private var allFiles: List<SavedFile> = emptyList()
    private var showingMessages = true
    private var resumed = false

    private val ticker = object : Runnable {
        override fun run() {
            if (!resumed) return
            refresh()
            ui.postDelayed(this, 3000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        b = ActivityMainBinding.inflate(layoutInflater)
        setContentView(b.root)
        db = Db(this)

        b.rv.layoutManager = LinearLayoutManager(this)
        b.rv.adapter = msgAdapter

        b.tabs.addTab(b.tabs.newTab().setText("Messages"))
        b.tabs.addTab(b.tabs.newTab().setText("Files"))
        b.tabs.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab) {
                showingMessages = tab.position == 0
                b.rv.adapter = if (showingMessages) msgAdapter else fileAdapter
                b.searchLayout.visibility = if (showingMessages) View.VISIBLE else View.GONE
                render()
            }

            override fun onTabUnselected(tab: TabLayout.Tab) {}
            override fun onTabReselected(tab: TabLayout.Tab) {}
        })

        b.etSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, st: Int, c: Int, a: Int) {}
            override fun onTextChanged(s: CharSequence?, st: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) = render()
        })

        b.btnNotif.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }
        b.btnFiles.setOnClickListener { askAllFiles() }
        b.btnStart.setOnClickListener {
            prefs().edit().putBoolean("started", true).apply()
            startWatcher()
            updateSetup()
        }

        if (Build.VERSION.SDK_INT >= 33 &&
            checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 1)
        }
    }

    override fun onResume() {
        super.onResume()
        resumed = true
        // reboot ke baad service dobara chala do
        if (prefs().getBoolean("started", false) && hasAllFiles()) startWatcher()
        updateSetup()
        ui.removeCallbacks(ticker)
        ui.post(ticker)
    }

    override fun onPause() {
        resumed = false
        ui.removeCallbacks(ticker)
        super.onPause()
    }

    private fun prefs() = getSharedPreferences("wasaver", Context.MODE_PRIVATE)

    private fun startWatcher() {
        try {
            startForegroundService(Intent(this, MediaWatcherService::class.java))
        } catch (e: Exception) {
            Toast.makeText(this, "Service start nahi hui: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }

    private fun askAllFiles() {
        if (Build.VERSION.SDK_INT >= 30) {
            startActivity(
                Intent(
                    Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                    Uri.parse("package:$packageName")
                )
            )
        } else {
            requestPermissions(
                arrayOf(
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                ),
                2
            )
        }
    }

    private fun hasNotificationAccess(): Boolean {
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return enabled?.contains(packageName) == true
    }

    private fun hasAllFiles(): Boolean =
        if (Build.VERSION.SDK_INT >= 30) {
            Environment.isExternalStorageManager()
        } else {
            checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }

    private fun updateSetup() {
        val notif = hasNotificationAccess()
        val files = hasAllFiles()
        val started = prefs().getBoolean("started", false)

        setDone(b.btnNotif, notif)
        setDone(b.btnFiles, files)
        setDone(b.btnStart, started)

        val all = notif && files && started
        b.setupCard.visibility = if (all) View.GONE else View.VISIBLE
        b.tvStatus.text = if (all) "● Active" else "Setup baqi"
    }

    private fun setDone(btn: com.google.android.material.button.MaterialButton, done: Boolean) {
        btn.isEnabled = !done
        btn.text = if (done) "Done ✓" else if (btn.id == R.id.btnStart) "Start" else "Allow"
    }

    private fun refresh() {
        bg.execute {
            val msgs = db.messages(2000)
            val files = loadFiles()
            ui.post {
                allMessages = msgs
                allFiles = files
                render()
            }
        }
    }

    private fun loadFiles(): List<SavedFile> {
        val root = File(Environment.getExternalStorageDirectory(), "WASaver")
        if (!root.exists()) return emptyList()
        return root.walkTopDown()
            .filter { it.isFile }
            .map {
                SavedFile(
                    path = it.absolutePath,
                    name = it.name,
                    category = it.parentFile?.name ?: "Other",
                    size = it.length(),
                    time = it.lastModified()
                )
            }
            .sortedByDescending { it.time }
            .toList()
    }

    private fun render() {
        b.tvMsgCount.text = allMessages.size.toString()
        b.tvFileCount.text = allFiles.size.toString()
        b.tvDeletedCount.text = allMessages.count { it.deleted }.toString()

        if (showingMessages) {
            val q = b.etSearch.text?.toString()?.trim().orEmpty()
            val list = if (q.isEmpty()) allMessages else allMessages.filter {
                it.text.contains(q, ignoreCase = true) || it.sender.contains(q, ignoreCase = true)
            }
            msgAdapter.submitList(list)
            showEmpty(
                list.isEmpty(),
                if (q.isEmpty()) "Abhi koi message save nahi hua.\nJab WhatsApp par message aayega yahan nazar aayega."
                else "Koi match nahi mila."
            )
        } else {
            fileAdapter.submitList(allFiles)
            showEmpty(
                allFiles.isEmpty(),
                if (hasAllFiles()) "Abhi koi file save nahi hui.\nWhatsApp mein media auto-download ON rakho."
                else "Pehle \"All files access\" do."
            )
        }
    }

    private fun showEmpty(empty: Boolean, text: String) {
        b.tvEmpty.text = text
        b.tvEmpty.visibility = if (empty) View.VISIBLE else View.GONE
    }

    private fun openFile(f: SavedFile) {
        try {
            val uri = FileProvider.getUriForFile(this, "$packageName.fileprovider", f.file)
            val ext = f.name.substringAfterLast('.', "").lowercase()
            val mime = when (ext) {
                "opus" -> "audio/ogg"
                else -> MimeTypeMap.getSingleton().getMimeTypeFromExtension(ext) ?: "*/*"
            }
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, mime)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open with"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(this, "Is file ko kholne wali app nahi mili", Toast.LENGTH_LONG).show()
        } catch (e: Exception) {
            Toast.makeText(this, "File nahi khul saki: ${e.message}", Toast.LENGTH_LONG).show()
        }
    }
}
