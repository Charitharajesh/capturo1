package com.capturo.app.premium.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.capturo.app.R
import com.capturo.app.premium.DemoData
import com.capturo.app.premium.Photographer

class NearbyAdapter(
    private val items: List<Photographer>,
    private val onClick: (Photographer) -> Unit
) : RecyclerView.Adapter<NearbyAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val cover: ImageView = v.findViewById(R.id.imageCover)
        val name: TextView = v.findViewById(R.id.textName)
        val distance: TextView = v.findViewById(R.id.textDistance)
        val specialties: TextView = v.findViewById(R.id.textSpecialties)
        val rating: TextView = v.findViewById(R.id.textRating)
        val price: TextView = v.findViewById(R.id.textPrice)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_premium_nearby, parent, false)
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
        holder.distance.text = "📍 %.1f km".format(DemoData.distanceKm(p))
        holder.specialties.text = p.specialties
        holder.rating.text = "${p.rating} (${p.reviews})"
        holder.price.text = "From ${p.startingPrice}"
        holder.itemView.setOnClickListener { onClick(p) }
    }
}
