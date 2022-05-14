package com.serwylo.babybook.bookviewer

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import com.serwylo.babybook.databinding.FragmentBookViewerPageBinding
import com.serwylo.babybook.db.entities.PageEditingData
import com.squareup.picasso.Picasso
import kotlinx.coroutines.launch

class BookPageFragment(
    private val page: PageEditingData,
): Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentBookViewerPageBinding.inflate(inflater, container, false)

        binding.title.text = page.title()
        binding.text.text = page.text()

        if (page.image == null) {
            binding.image.visibility = View.GONE
        } else {
            binding.image.visibility = View.VISIBLE
            lifecycleScope.launch {
                val image = page.image(requireContext())
                if (image != null) {
                    Picasso.get()
                        .load(image)
                        .fit()
                        .centerCrop()
                        .into(binding.image)
                }
            }
        }

        return binding.root
    }

}