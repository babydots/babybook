package com.serwylo.babybook.onboarding

import android.os.Bundle
import android.view.View
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.serwylo.babybook.databinding.ActivityOnboardingBinding

/**
 * The number of pages (wizard steps) to show in this demo.
 */
private const val NUM_PAGES = 3

class OnboardingActivity : FragmentActivity() {

    private lateinit var binding: ActivityOnboardingBinding

    private val onPageChange = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            binding.apply {
                if (position == (pager.adapter?.itemCount ?: 0) - 1) {
                    next.visibility = View.GONE
                    skip.visibility = View.INVISIBLE
                    start.visibility = View.VISIBLE
                } else {
                    next.visibility = View.VISIBLE
                    skip.visibility = View.VISIBLE
                    start.visibility = View.GONE
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityOnboardingBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val pagerAdapter = ScreenSlidePagerAdapter(this)
        binding.pager.adapter = pagerAdapter
        binding.pager.registerOnPageChangeCallback(onPageChange)
        binding.pager.setPageTransformer(ZoomOutPageTransformer())

        binding.next.setOnClickListener {
            binding.pager.currentItem ++
        }

        binding.skip.setOnClickListener {
            finish()
        }

        binding.start.setOnClickListener {
            finish()
        }

        binding.dots.setViewPager2(binding.pager)
    }

    override fun onDestroy() {
        super.onDestroy()

        binding.pager.unregisterOnPageChangeCallback(onPageChange)
    }

    override fun onBackPressed() {
        if (binding.pager.currentItem == 0) {
            super.onBackPressed()
        } else {
            binding.pager.currentItem --
        }
    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = NUM_PAGES

        override fun createFragment(position: Int): Fragment = createOnboardingFragment(position)
    }
}