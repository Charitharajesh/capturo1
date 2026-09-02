package com.capturo.app.premium

import android.content.Intent
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.capturo.app.databinding.ActivityPremiumSearchBinding
import com.capturo.app.premium.ui.PhotographerAdapter

class PremiumSearchActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumSearchBinding
    private lateinit var adapter: PhotographerAdapter

    private var category: String? = null
    private var results: List<Photographer> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumSearchBinding.inflate(layoutInflater)
        setContentView(binding.root)

        category = intent.getStringExtra(EXTRA_CATEGORY)

        adapter = PhotographerAdapter(emptyList()) { p -> openProfile(p.id) }
        binding.recyclerResults.layoutManager = LinearLayoutManager(this)
        binding.recyclerResults.adapter = adapter

        binding.btnBack.setOnClickListener { finish() }

        category?.let { binding.inputSearch.hint = "Search in $it" }
        binding.inputSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) = filter(s?.toString().orEmpty())
            override fun afterTextChanged(s: Editable?) {}
        })

        intent.getStringExtra(EXTRA_QUERY)?.let { binding.inputSearch.setText(it) }
        filter(binding.inputSearch.text?.toString().orEmpty())

        if (category == null && intent.getStringExtra(EXTRA_QUERY) == null) {
            binding.inputSearch.requestFocus()
        }
    }

    private fun filter(query: String) {
        var list = DemoData.search(query)
        val cat = category
        if (cat != null) {
            val byCat = list.filter { it.specialties.lowercase().contains(cat.lowercase()) }
            // Fall back to all matches so a category tap never shows an empty screen.
            list = if (byCat.isNotEmpty()) byCat else list
        }
        results = list
        adapter.submit(list)

        val title = when {
            query.isNotBlank() -> "${list.size} result${if (list.size == 1) "" else "s"} for \"$query\""
            cat != null -> "${cat} photographers • ${list.size}"
            else -> "${list.size} photographers near you"
        }
        binding.textResultCount.text = title
        binding.emptyState.visibility = if (list.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openProfile(id: String) {
        startActivity(
            Intent(this, PremiumProfileActivity::class.java)
                .putExtra(PremiumProfileActivity.EXTRA_ID, id)
        )
    }

    companion object {
        const val EXTRA_CATEGORY = "extra_category"
        const val EXTRA_QUERY = "extra_query"
    }
}
