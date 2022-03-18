package com.serwylo.babybook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.lifecycle.ViewModelProvider
import com.serwylo.babybook.booklist.BookListViewModel
import com.serwylo.babybook.booklist.BookListViewModelFactory
import com.serwylo.babybook.databinding.ActivityMainBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.repositories.BookRepository
import com.serwylo.babybook.onboarding.OnboardingActivity

class MainActivity : AppCompatActivity() {

    private lateinit var viewModel: BookListViewModel

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        viewModel = ViewModelProvider(this, BookListViewModelFactory(BookRepository(AppDatabase.getInstance(this).bookDao())))
            .get(BookListViewModel::class.java)

        if (!Preferences.isOnboardingComplete(this)) {
            Preferences.setOnboardingComplete(this)
            startActivity(Intent(this, OnboardingActivity::class.java))
        } else {
            Changelog.show(this)
        }

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        viewModel.isInEditMode.observe(this) { isInEditMode ->
            invalidateOptionsMenu()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.edit -> {
                viewModel.toggleEditMode()
                return true
            }
            R.id.cancel_edit -> {
                viewModel.toggleEditMode()
                return true
            }
            R.id.settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)

        if (viewModel.isInEditMode.value == true) {
            menu.findItem(R.id.cancel_edit).isVisible = true
            menu.findItem(R.id.edit).isVisible = false
        } else {
            menu.findItem(R.id.cancel_edit).isVisible = false
            menu.findItem(R.id.edit).isVisible = true
        }

        return true
    }

}