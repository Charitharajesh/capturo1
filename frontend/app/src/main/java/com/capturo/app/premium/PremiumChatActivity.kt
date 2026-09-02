package com.capturo.app.premium

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import coil.load
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumChatBinding
import com.capturo.app.premium.ui.ChatAdapter

class PremiumChatActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumChatBinding
    private lateinit var photographer: Photographer
    private lateinit var adapter: ChatAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumChatBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val id = intent.getStringExtra(EXTRA_ID) ?: DemoData.photographers.first().id
        photographer = DemoData.byId(id)

        binding.textName.text = photographer.name
        binding.imageAvatar.load(photographer.avatarUrl) { placeholder(R.drawable.bg_image_placeholder) }
        binding.btnBack.setOnClickListener { finish() }

        val history = PremiumStore.messages(this, id)
        if (history.isEmpty()) {
            val greeting = PremiumStore.ChatMessage(
                false,
                "Hi! Thanks for reaching out to ${photographer.name}. How can I help with your shoot?",
                System.currentTimeMillis()
            )
            PremiumStore.addMessage(this, id, greeting)
            history.add(greeting)
        }

        adapter = ChatAdapter(history)
        binding.recyclerMessages.layoutManager = LinearLayoutManager(this).apply { stackFromEnd = true }
        binding.recyclerMessages.adapter = adapter
        scrollToEnd()

        binding.btnSend.setOnClickListener { send() }
    }

    private fun send() {
        val text = binding.inputMessage.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        binding.inputMessage.setText("")

        val mine = PremiumStore.ChatMessage(true, text, System.currentTimeMillis())
        PremiumStore.addMessage(this, photographer.id, mine)
        adapter.add(mine)
        scrollToEnd()

        // Simulated photographer reply so the conversation feels alive.
        binding.recyclerMessages.postDelayed({
            val reply = PremiumStore.ChatMessage(
                false, autoReply(text), System.currentTimeMillis()
            )
            PremiumStore.addMessage(this, photographer.id, reply)
            adapter.add(reply)
            scrollToEnd()
        }, 900)
    }

    private fun autoReply(userText: String): String {
        val t = userText.lowercase()
        return when {
            t.contains("available") || t.contains("date") ->
                "Yes, I have slots open that week. Share your event date and I'll confirm."
            t.contains("price") || t.contains("cost") || t.contains("package") ->
                "My packages start at ${photographer.startingPrice}. Happy to tailor one for you!"
            t.contains("hi") || t.contains("hello") ->
                "Hello! 👋 Great to connect. What are you planning to shoot?"
            else ->
                "Got it! I'll get back to you shortly with the details. 📸"
        }
    }

    private fun scrollToEnd() {
        binding.recyclerMessages.post {
            binding.recyclerMessages.scrollToPosition(adapter.itemCount - 1)
        }
    }

    companion object {
        const val EXTRA_ID = "extra_photographer_id"
    }
}
