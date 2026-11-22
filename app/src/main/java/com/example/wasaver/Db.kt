package com.example.wasaver

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class Msg(val sender: String, val text: String, val time: Long, val deleted: Boolean)

class Db(ctx: Context) : SQLiteOpenHelper(ctx.applicationContext, "wasaver.db", null, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE messages(" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "sender TEXT NOT NULL, " +
                "text TEXT NOT NULL, " +
                "time INTEGER NOT NULL, " +
                "deleted INTEGER NOT NULL DEFAULT 0, " +
                "UNIQUE(sender, text, time))"
        )
        db.execSQL("CREATE TABLE files(key TEXT PRIMARY KEY, saved_at INTEGER NOT NULL)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {}

    @Synchronized
    fun addMessage(sender: String, text: String, time: Long) {
        val v = ContentValues().apply {
            put("sender", sender)
            put("text", text)
            put("time", time)
        }
        writableDatabase.insertWithOnConflict("messages", null, v, SQLiteDatabase.CONFLICT_IGNORE)
    }

    /** Is sender ka sab se naya saved message "deleted" mark karta hai. */
    @Synchronized
    fun markLastDeleted(sender: String) {
        writableDatabase.execSQL(
            "UPDATE messages SET deleted=1 WHERE id=(" +
                "SELECT id FROM messages WHERE sender=? AND deleted=0 ORDER BY time DESC LIMIT 1)",
            arrayOf<Any>(sender)
        )
    }

    @Synchronized
    fun messages(limit: Int = 500): List<Msg> {
        val out = ArrayList<Msg>()
        readableDatabase.rawQuery(
            "SELECT sender, text, time, deleted FROM messages ORDER BY time DESC LIMIT ?",
            arrayOf(limit.toString())
        ).use { c ->
            while (c.moveToNext()) {
                out.add(Msg(c.getString(0), c.getString(1), c.getLong(2), c.getInt(3) == 1))
            }
        }
        return out
    }

    @Synchronized
    fun isFileSaved(key: String): Boolean =
        readableDatabase.rawQuery("SELECT 1 FROM files WHERE key=?", arrayOf(key)).use { it.moveToFirst() }

    @Synchronized
    fun markFileSaved(key: String) {
        val v = ContentValues().apply {
            put("key", key)
            put("saved_at", System.currentTimeMillis())
        }
        writableDatabase.insertWithOnConflict("files", null, v, SQLiteDatabase.CONFLICT_IGNORE)
    }

    @Synchronized
    fun fileCount(): Int =
        readableDatabase.rawQuery("SELECT COUNT(*) FROM files", null).use {
            if (it.moveToFirst()) it.getInt(0) else 0
        }
}
