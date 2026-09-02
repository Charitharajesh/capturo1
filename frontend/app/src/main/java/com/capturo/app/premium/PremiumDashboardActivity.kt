package com.capturo.app.premium

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import com.capturo.app.databinding.ActivityPremiumDashboardBinding
import com.capturo.app.premium.ui.PortfolioAdapter

class PremiumDashboardActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumDashboardBinding
    private lateinit var adapter: PortfolioAdapter

    // ACTION_OPEN_DOCUMENT so we can persist read access to the picked image.
    private val pickImage = registerForActivityResult(
        ActivityResultContracts.OpenDocument()
    ) { uri: Uri? -> uri?.let { onImagePicked(it) } }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumDashboardBinding.inflate(layoutInflater)
        setContentView(binding.root)

        PremiumStore.setPhotographerMode(this, true)

        adapter = PortfolioAdapter(emptyList()) { index -> openViewer(index) }
        binding.recyclerPosts.layoutManager = GridLayoutManager(this, 3)
        binding.recyclerPosts.adapter = adapter

        binding.btnCreatePost.setOnClickListener {
            pickImage.launch(arrayOf("image/*"))
        }
        binding.btnSwitchCustomer.setOnClickListener {
            PremiumStore.setPhotographerMode(this, false)
            finish()
        }

        refresh()
    }

    private fun onImagePicked(uri: Uri) {
        try {
            contentResolver.takePersistableUriPermission(
                uri, Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
        } catch (_: SecurityException) { /* some providers don't grant persistable access */ }

        val input = EditText(this).apply {
            hint = "Write a caption…"
            setText("")
            setPadding(48, 32, 48, 32)
        }
        AlertDialog.Builder(this)
            .setTitle("New Post")
            .setView(input)
            .setPositiveButton("Publish") { _, _ ->
                val caption = input.text?.toString()?.trim().ifNullOrBlank("New shoot 📸")
                PremiumStore.addPost(
                    this,
                    PremiumStore.CreatedPost(uri.toString(), caption, "My Work", System.currentTimeMillis())
                )
                Toast.makeText(this, "Post published", Toast.LENGTH_SHORT).show()
                refresh()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    private fun refresh() {
        val posts = PremiumStore.createdPosts(this)
        adapter.submit(posts.map { it.uri })
        binding.textEmpty.visibility = if (posts.isEmpty()) View.VISIBLE else View.GONE
    }

    private fun openViewer(index: Int) {
        val uris = PremiumStore.createdPosts(this).map { it.uri }
        startActivity(
            Intent(this, PremiumImageViewerActivity::class.java)
                .putStringArrayListExtra(PremiumImageViewerActivity.EXTRA_IMAGES, ArrayList(uris))
                .putExtra(PremiumImageViewerActivity.EXTRA_START, index)
        )
    }

    private fun String?.ifNullOrBlank(fallback: String) =
        if (this.isNullOrBlank()) fallback else this
}
