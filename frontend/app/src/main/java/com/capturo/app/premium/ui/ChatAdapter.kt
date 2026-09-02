package com.capturo.app.premium.ui

import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.capturo.app.R
import com.capturo.app.premium.PremiumStore

class ChatAdapter(
    private val items: MutableList<PremiumStore.ChatMessage>
) : RecyclerView.Adapter<ChatAdapter.VH>() {

    fun add(msg: PremiumStore.ChatMessage) {
        items.add(msg)
        notifyItemInserted(items.size - 1)
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val row: LinearLayout = v.findViewById(R.id.bubbleRow)
        val bubble: TextView = v.findViewById(R.id.textBubble)
        val attachment: LinearLayout = v.findViewById(R.id.attachmentCard)
        val attachmentName: TextView = v.findViewById(R.id.textAttachmentName)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_premium_chat, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val m = items[position]
        val ctx = holder.itemView.context

        holder.bubble.visibility = if (m.text.isBlank()) View.GONE else View.VISIBLE
        holder.bubble.text = m.text
        if (m.fromMe) {
            holder.row.gravity = Gravity.END
            holder.bubble.setBackgroundResource(R.drawable.bg_gold_button)
            holder.bubble.setTextColor(ContextCompat.getColor(ctx, R.color.colorTextOnAccent))
        } else {
            holder.row.gravity = Gravity.START
            holder.bubble.setBackgroundResource(R.drawable.bg_chip_dark)
            holder.bubble.setTextColor(ContextCompat.getColor(ctx, R.color.colorTextPrimary))
        }

        val uri = m.attachmentUri
        if (uri.isNullOrBlank()) {
            holder.attachment.visibility = View.GONE
        } else {
            holder.attachment.visibility = View.VISIBLE
            holder.attachmentName.text = m.attachmentName ?: "Receipt.pdf"
            holder.attachment.setOnClickListener { openPdf(ctx, uri) }
        }
    }

    private fun openPdf(ctx: android.content.Context, uriString: String) {
        val uri = Uri.parse(uriString)
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(uri, "application/pdf")
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            ctx.startActivity(Intent.createChooser(intent, "Open receipt"))
        } catch (e: ActivityNotFoundException) {
            Toast.makeText(ctx, "No PDF viewer installed", Toast.LENGTH_SHORT).show()
        }
    }
}
