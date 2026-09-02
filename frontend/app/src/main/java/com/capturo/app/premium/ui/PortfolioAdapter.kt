package com.capturo.app.premium.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.capturo.app.R

class PortfolioAdapter(
    private var items: List<String>,
    private val onClick: (Int) -> Unit
) : RecyclerView.Adapter<PortfolioAdapter.VH>() {

    fun submit(list: List<String>) {
        items = list
        notifyDataSetChanged()
    }

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val image: ImageView = v.findViewById(R.id.imagePortfolio)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_premium_portfolio, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        holder.image.load(items[position]) {
            placeholder(R.drawable.bg_image_placeholder)
            error(R.drawable.bg_image_placeholder)
        }
        holder.image.setOnClickListener { onClick(position) }
    }
}
