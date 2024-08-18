package com.serwylo.babybook.onboarding

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.content.res.AppCompatResources
import androidx.fragment.app.Fragment
import com.serwylo.babybook.R
import com.serwylo.babybook.databinding.FragmentOnboardingBinding

fun createOnboardingFragment(position: Int): Fragment {

    val title: String
    val description: String
    val drawableRes: Int

    when (position) {
        0 -> {
            title = "Make your own books"
            description = "Create a book, search for wiki pages, watch images and text flow onto your pages."
            drawableRes = R.drawable.onboarding_book
        }

        1 -> {
            title = "Learn with your kids"
            description = "Interested in biology? " +
                    "Search for Organism, Animal, Plant, Fungi and watch your book come to life."
            drawableRes = R.drawable.onboarding_biology
        }

        else -> {
            title = "Share your creations"
            description = "Generate a PDF and email it to a friend, or print it for your own physical book."
            drawableRes = R.drawable.onboarding_tick
        }
    }

    return OnboardingFragment(title, description, drawableRes)

}

class OnboardingFragment(
    private val title: String,
    private val description: String,
    private val drawableRes: Int,
): Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val binding = FragmentOnboardingBinding.inflate(inflater, container, false)

        binding.title.text = title
        binding.description.text = description
        binding.image.setImageDrawable(AppCompatResources.getDrawable(requireContext(), drawableRes))

        return binding.root
    }

}