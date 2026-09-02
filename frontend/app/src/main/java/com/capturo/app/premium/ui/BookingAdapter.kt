package com.capturo.app.premium.ui

import android.content.Intent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.capturo.app.R
import com.capturo.app.premium.Booking
import com.capturo.app.premium.BookingStatus
import com.capturo.app.premium.DemoData
import com.capturo.app.premium.PremiumChatActivity

class BookingAdapter(
    private var items: List<Booking>
) : RecyclerView.Adapter<BookingAdapter.VH>() {

    fun submit(list: List<Booking>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.imageAvatar)
        val photographer: TextView = v.findViewById(R.id.textPhotographer)
        val event: TextView = v.findViewById(R.id.textEvent)
        val status: TextView = v.findViewById(R.id.textStatus)
        val date: TextView = v.findViewById(R.id.textDate)
        val time: TextView = v.findViewById(R.id.textTime)
        val price: TextView = v.findViewById(R.id.textPrice)
        val btnMessage: TextView = v.findViewById(R.id.btnMessage)
        val btnDetails: TextView = v.findViewById(R.id.btnDetails)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_premium_booking, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val b = items[position]
        holder.avatar.load(b.avatarUrl) { placeholder(R.drawable.bg_image_placeholder) }
        holder.photographer.text = b.photographer
        holder.event.text = b.eventType
        holder.date.text = "📅 ${b.date}"
        holder.time.text = "🕒 ${b.time}"
        holder.price.text = b.price

        val res = holder.itemView.resources
        val (label, colorRes) = when (b.status) {
            BookingStatus.CONFIRMED -> "Confirmed" to R.color.colorSuccess
            BookingStatus.COMPLETED -> "Completed" to R.color.colorInfo
            BookingStatus.CANCELLED -> "Cancelled" to R.color.colorError
        }
        holder.status.text = label
        holder.status.setTextColor(res.getColor(colorRes, null))

        holder.btnMessage.setOnClickListener {
            val match = DemoData.photographers.firstOrNull { it.name == b.photographer }
                ?: DemoData.photographers.first()
            holder.itemView.context.startActivity(
                Intent(holder.itemView.context, PremiumChatActivity::class.java)
                    .putExtra(PremiumChatActivity.EXTRA_ID, match.id)
            )
        }
        holder.btnDetails.setOnClickListener {
            Toast.makeText(holder.itemView.context, "Booking ${b.id}", Toast.LENGTH_SHORT).show()
        }
    }
}
