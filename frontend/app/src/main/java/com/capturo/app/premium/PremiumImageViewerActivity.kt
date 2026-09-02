package com.capturo.app.premium

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import coil.load
import com.capturo.app.R
import com.capturo.app.databinding.ActivityPremiumImageViewerBinding

class PremiumImageViewerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityPremiumImageViewerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityPremiumImageViewerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val images = intent.getStringArrayListExtra(EXTRA_IMAGES) ?: arrayListOf()
        val start = intent.getIntExtra(EXTRA_START, 0)

        binding.viewerPager.adapter = ViewerAdapter(images)
        binding.viewerPager.setCurrentItem(start, false)
        binding.btnClose.setOnClickListener { finish() }
    }

    private class ViewerAdapter(val items: List<String>) :
        RecyclerView.Adapter<ViewerAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val image: ImageView = v.findViewById(R.id.imageFull)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_premium_viewer, parent, false)
            return VH(v)
        }

        override fun getItemCount() = items.size

        override fun onBindViewHolder(holder: VH, position: Int) {
            holder.image.load(items[position]) {
                placeholder(R.drawable.bg_image_placeholder)
                error(R.drawable.bg_image_placeholder)
            }
        }
    }

    companion object {
        const val EXTRA_IMAGES = "extra_images"
        const val EXTRA_START = "extra_start"
    }
}
