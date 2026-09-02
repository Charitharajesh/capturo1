package com.capturo.app.premium

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumConversationsBinding

class PremiumConversationsActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumConversationsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumConversationsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        binding.recyclerConversations.layoutManager = LinearLayoutManager(this)
        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val ids = PremiumStore.conversationIds(this)
            .filter { id -> DemoData.photographers.any { it.id == id } }
        binding.recyclerConversations.adapter = ConversationAdapter(ids)
        binding.emptyState.visibility = if (ids.isEmpty()) View.VISIBLE else View.GONE
    }

    private inner class ConversationAdapter(val ids: List<String>) :
        RecyclerView.Adapter<ConversationAdapter.VH>() {

        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val avatar: ImageView = v.findViewById(R.id.imageAvatar)
            val name: TextView = v.findViewById(R.id.textName)
            val last: TextView = v.findViewById(R.id.textLast)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_premium_conversation, parent, false)
            return VH(v)
        }

        override fun getItemCount() = ids.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            val id = ids[position]
            val p = DemoData.byId(id)
            holder.avatar.load(p.avatarUrl) { placeholder(R.drawable.bg_image_placeholder) }
            holder.name.text = p.name
            holder.last.text = PremiumStore.messages(this@PremiumConversationsActivity, id)
                .lastOrNull()?.text ?: "Say hello 👋"
            holder.itemView.setOnClickListener {
                startActivity(
                    Intent(this@PremiumConversationsActivity, PremiumChatActivity::class.java)
                        .putExtra(PremiumChatActivity.EXTRA_ID, id)
                )
            }
        }
    }
}
