package com.serwylo.babybook.editbook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.serwylo.babybook.Preferences
import com.serwylo.babybook.R
import com.serwylo.babybook.bookviewer.BookViewerActivity
import com.serwylo.babybook.contentwarning.ContentWarningActivity
import com.serwylo.babybook.databinding.ActivityEditBookBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.repositories.BookRepository
import com.serwylo.babybook.editbookpage.EditBookPageActivity
import kotlinx.coroutines.launch


class EditBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBookBinding
    private lateinit var viewModel: EditBookViewModel

    private val addPage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (!Preferences.hasShownContentWarning(this)) {
            Preferences.setHasShownContentWarning(this)
            startActivity(Intent(this, ContentWarningActivity::class.java))
        }

        binding = ActivityEditBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val bookId = intent.extras?.getLong(EXTRA_BOOK_ID) ?: 0L
        val dao = AppDatabase.getInstance(this).bookDao()
        viewModel = ViewModelProvider(this@EditBookActivity, EditBookViewModelFactory(BookRepository(dao), bookId)).get(EditBookViewModel::class.java)

        viewModel.isLoading.observe(this) { isLoading ->
            if (!isLoading) {
                setup()
            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.view_book -> {
                startActivity(Intent(this, BookViewerActivity::class.java).apply {
                    putExtra(BookViewerActivity.EXTRA_BOOK_ID, viewModel.getBookId())
                })
                return true
            }

            R.id.delete_book -> {
                onDeleteBook()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        if (viewModel.getBookId() > 0) {
            menuInflater.inflate(R.menu.edit_book_menu, menu)

            val isLoading = viewModel.isLoading.value!!
            val pageCount = viewModel.pages.value?.size ?: 0
            menu.findItem(R.id.view_book).isVisible = !isLoading && pageCount > 0
            menu.findItem(R.id.delete_book).isVisible = !isLoading
        }
        return true
    }

    private fun setup() {
        invalidateOptionsMenu()

        binding.addBookButton.isEnabled = true
        binding.addBookButton.setOnClickListener { onAddPage() }

        binding.bookTitle.isEnabled = true
        viewModel.bookTitle.observe(this) { title ->
            if (binding.bookTitle.text.toString() != title) {
                binding.bookTitle.setText(title)
            }
        }

        binding.pages.layoutManager = GridLayoutManager(this, 2)
        binding.pages.adapter = EditBookPagesAdapter().also { adapter ->

            adapter.setPageSelectedListener { page ->
                startActivity(Intent(this, EditBookPageActivity::class.java).apply {
                    putExtra(EditBookPageActivity.EXTRA_BOOK_ID, viewModel.getBookId())
                    putExtra(EditBookPageActivity.EXTRA_BOOK_PAGE_ID, page.bookPage.id)
                })
            }

            viewModel.pages.observe(this) { pages ->
                adapter.setData(pages)
                adapter.notifyDataSetChanged()

                // Initially we don't have a "view book" item, but once we have pages added, we
                // then show it.
                invalidateOptionsMenu()
            }
        }

        binding.bookTitle.addTextChangedListener { viewModel.updateTitle(it.toString()) }
    }

    private fun onDeleteBook() {
        if (viewModel.pages.value.isNullOrEmpty()) {
            lifecycleScope.launch {
                viewModel.deleteBook()
                finish()
            }
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete book?")
                .setMessage("Are you sure you want to remove this book? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        viewModel.deleteBook()
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    private fun onAddPage() {
        addPage.launch(
            Intent(this, EditBookPageActivity::class.java).apply {
                putExtra(EditBookPageActivity.EXTRA_BOOK_ID, viewModel.getBookId())
            }
        )
    }

    companion object {
        const val EXTRA_BOOK_ID = "bookId"
    }

}