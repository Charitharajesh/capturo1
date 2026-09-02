package com.capturo.app.premium.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.capturo.app.R
import com.capturo.app.premium.Photographer

class PhotographerAdapter(
    private var items: List<Photographer>,
    private val onClick: (Photographer) -> Unit
) : RecyclerView.Adapter<PhotographerAdapter.VH>() {

    fun submit(list: List<Photographer>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cover: ImageView = v.findViewById(R.id.imageCover)
        val name: TextView = v.findViewById(R.id.textName)
        val verified: ImageView = v.findViewById(R.id.iconVerified)
        val rating: TextView = v.findViewById(R.id.textRating)
        val location: TextView = v.findViewById(R.id.textLocation)
        val specialties: TextView = v.findViewById(R.id.textSpecialties)
        val price: TextView = v.findViewById(R.id.textPrice)
        val badge: TextView = v.findViewById(R.id.badgeAvailable)
        val btn: TextView = v.findViewById(R.id.btnViewProfile)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_premium_photographer, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.cover.load(p.coverUrl) {
            placeholder(R.drawable.bg_image_placeholder)
            error(R.drawable.bg_image_placeholder)
        }
        holder.name.text = p.name
        holder.verified.visibility = if (p.verified) View.VISIBLE else View.GONE
        holder.rating.text = "${p.rating} (${p.reviews} reviews)"
        holder.location.text = p.location
        holder.specialties.text = p.specialties
        holder.price.text = "From ${p.startingPrice}"
        if (p.available) {
            holder.badge.text = "🟢 Available"
            holder.badge.setTextColor(holder.itemView.resources.getColor(R.color.colorSuccess, null))
        } else {
            holder.badge.text = "🔴 Booked"
            holder.badge.setTextColor(holder.itemView.resources.getColor(R.color.colorError, null))
        }
        val open = View.OnClickListener { onClick(p) }
        holder.itemView.setOnClickListener(open)
        holder.btn.setOnClickListener(open)
    }
}
