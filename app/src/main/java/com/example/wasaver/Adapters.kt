package com.example.wasaver

import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.example.wasaver.databinding.ItemFileBinding
import com.example.wasaver.databinding.ItemMessageBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private val timeFmt = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault())

class MessageAdapter : ListAdapter<Msg, MessageAdapter.VH>(Diff) {

    class VH(val b: ItemMessageBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemMessageBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = getItem(position)
        holder.b.sender.text = m.sender
        holder.b.time.text = timeFmt.format(Date(m.time))
        holder.b.body.text = m.text
        holder.b.deletedChip.visibility = if (m.deleted) View.VISIBLE else View.GONE
        holder.b.card.strokeColor =
            if (m.deleted) Color.parseColor("#F3B7B1") else Color.parseColor("#E1E8E6")
    }

    private object Diff : DiffUtil.ItemCallback<Msg>() {
        override fun areItemsTheSame(a: Msg, b: Msg) =
            a.sender == b.sender && a.time == b.time && a.text == b.text

        override fun areContentsTheSame(a: Msg, b: Msg) = a == b
    }
}

data class SavedFile(
    val path: String,
    val name: String,
    val category: String,
    val size: Long,
    val time: Long
) {
    val file: File get() = File(path)
}

class FileAdapter(private val onClick: (SavedFile) -> Unit) :
    ListAdapter<SavedFile, FileAdapter.VH>(Diff) {

    class VH(val b: ItemFileBinding) : RecyclerView.ViewHolder(b.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
        VH(ItemFileBinding.inflate(LayoutInflater.from(parent.context), parent, false))

    override fun onBindViewHolder(holder: VH, position: Int) {
        val f = getItem(position)
        holder.b.icon.text = iconFor(f.category)
        holder.b.name.text = f.name
        holder.b.meta.text = "${prettyCategory(f.category)}  •  ${formatSize(f.size)}  •  ${timeFmt.format(Date(f.time))}"
        holder.b.card.setOnClickListener { onClick(f) }
    }

    private fun iconFor(category: String): String {
        val c = category.lowercase()
        return when {
            "video" in c -> "🎬"
            "voice" in c -> "🎙"
            "audio" in c -> "🎵"
            "document" in c -> "📄"
            "image" in c || "gif" in c || "sticker" in c -> "🖼"
            else -> "📁"
        }
    }

    private fun prettyCategory(category: String) = category.removePrefix("WhatsApp ").trim()

    private fun formatSize(bytes: Long): String = when {
        bytes >= 1_048_576 -> String.format(Locale.US, "%.1f MB", bytes / 1_048_576.0)
        bytes >= 1024 -> String.format(Locale.US, "%d KB", bytes / 1024)
        else -> "$bytes B"
    }

    private object Diff : DiffUtil.ItemCallback<SavedFile>() {
        override fun areItemsTheSame(a: SavedFile, b: SavedFile) = a.path == b.path
        override fun areContentsTheSame(a: SavedFile, b: SavedFile) = a == b
    }
}
