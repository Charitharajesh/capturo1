package com.capturo.app.premium.ui

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.capturo.app.R
import com.capturo.app.premium.Post

class FeedAdapter(
    private val items: List<Post>,
    private val onOpenAuthor: (Post) -> Unit = {}
) : RecyclerView.Adapter<FeedAdapter.VH>() {

    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        val avatar: ImageView = v.findViewById(R.id.imageAvatar)
        val author: TextView = v.findViewById(R.id.textAuthor)
        val location: TextView = v.findViewById(R.id.textLocation)
        val category: TextView = v.findViewById(R.id.textCategory)
        val image: ImageView = v.findViewById(R.id.imagePost)
        val likes: TextView = v.findViewById(R.id.textLikes)
        val comments: TextView = v.findViewById(R.id.textComments)
        val caption: TextView = v.findViewById(R.id.textCaption)
        val iconLike: ImageView = v.findViewById(R.id.iconLike)
        val iconSave: ImageView = v.findViewById(R.id.iconSave)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_premium_post, parent, false)
        return VH(v)
    }

    override fun getItemCount() = items.size

    override fun onBindViewHolder(holder: VH, position: Int) {
        val p = items[position]
        holder.avatar.load(p.avatarUrl) { placeholder(R.drawable.bg_image_placeholder) }
        holder.author.text = p.photographer
        holder.location.text = p.location
        holder.category.text = p.category
        holder.image.load(p.imageUrl) {
            placeholder(R.drawable.bg_image_placeholder)
            error(R.drawable.bg_image_placeholder)
        }
        holder.likes.text = p.likes.toString()
        holder.comments.text = p.comments.toString()
        holder.caption.text = p.caption

        val open = View.OnClickListener { onOpenAuthor(p) }
        holder.avatar.setOnClickListener(open)
        holder.author.setOnClickListener(open)
        holder.image.setOnClickListener(open)

        var liked = false
        var saved = false
        holder.iconLike.setImageResource(R.drawable.ic_heart_outline)
        holder.iconSave.setImageResource(R.drawable.ic_bookmark)
        holder.iconLike.setOnClickListener {
            liked = !liked
            holder.iconLike.setImageResource(
                if (liked) R.drawable.ic_heart_filled else R.drawable.ic_heart_outline
            )
            holder.likes.text = (p.likes + if (liked) 1 else 0).toString()
        }
        holder.iconSave.setOnClickListener {
            saved = !saved
            holder.iconSave.setColorFilter(
                holder.itemView.resources.getColor(
                    if (saved) R.color.colorPrimary else R.color.colorTextPrimary, null
                )
            )
        }
    }
}
