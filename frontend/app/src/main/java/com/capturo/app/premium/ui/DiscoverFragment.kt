package com.capturo.app.premium.ui

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.capturo.app.databinding.FragmentPremiumDiscoverBinding
import com.capturo.app.premium.DemoData
import com.capturo.app.premium.Post
import com.capturo.app.premium.PremiumProfileActivity
import com.capturo.app.premium.PremiumStore

class DiscoverFragment : Fragment() {

    private var _binding: FragmentPremiumDiscoverBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPremiumDiscoverBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        binding.recyclerFeed.layoutManager = LinearLayoutManager(requireContext())
    }

    override fun onResume() {
        super.onResume()
        binding.recyclerFeed.adapter = FeedAdapter(buildFeed()) { post ->
            val match = DemoData.photographers.firstOrNull { it.name == post.photographer }
                ?: DemoData.photographers.first()
            startActivity(
                Intent(requireContext(), PremiumProfileActivity::class.java)
                    .putExtra(PremiumProfileActivity.EXTRA_ID, match.id)
            )
        }
    }

    /** Your own uploaded posts appear at the top of the discover feed. */
    private fun buildFeed(): List<Post> {
        val mine = PremiumStore.createdPosts(requireContext()).map {
            Post(
                id = "own_${it.ts}",
                photographer = "You",
                avatarUrl = DemoData.photographers.first().avatarUrl,
                location = "Your upload",
                category = it.category,
                imageUrl = it.uri,
                caption = it.caption,
                likes = 0,
                comments = 0
            )
        }
        return mine + DemoData.feed
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
