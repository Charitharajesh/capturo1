package com.capturo.app.premium

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capturo.app.databinding.ActivityPremiumSavedBinding
import com.capturo.app.premium.ui.PhotographerAdapter

class PremiumSavedActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumSavedBinding
    private lateinit var adapter: PhotographerAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumSavedBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = PhotographerAdapter(emptyList()) { p ->
            startActivity(
                Intent(this, PremiumProfileActivity::class.java)
                    .putExtra(PremiumProfileActivity.EXTRA_ID, p.id)
            )
        }
        binding.recyclerSaved.layoutManager = LinearLayoutManager(this)
        binding.recyclerSaved.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }
    }

    override fun onResume() {
        super.onResume()
        val saved = PremiumStore.savedPhotographers(this)
        adapter.submit(saved)
        binding.emptyState.visibility = if (saved.isEmpty()) View.VISIBLE else View.GONE
        binding.recyclerSaved.visibility = if (saved.isEmpty()) View.GONE else View.VISIBLE
    }
}
