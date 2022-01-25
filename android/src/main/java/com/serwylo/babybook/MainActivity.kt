package com.serwylo.babybook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.viewModels
import com.serwylo.babybook.booklist.BookListViewModel
import com.serwylo.babybook.databinding.ActivityMainBinding
import com.serwylo.babybook.editbook.EditBookActivity

class MainActivity : AppCompatActivity() {

    private val viewModel: BookListViewModel by viewModels()

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)

        setContentView(binding.root)

        viewModel.isInEditMode.observe(this) { isInEditMode ->
            invalidateOptionsMenu()
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add -> {
                onAddBook()
                return true
            }
            R.id.edit -> {
                viewModel.toggleEditMode()
                return true
            }
            R.id.cancel_edit -> {
                viewModel.toggleEditMode()
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

    private fun onAddBook() {
        startActivity(Intent(this, EditBookActivity::class.java))
    }

}