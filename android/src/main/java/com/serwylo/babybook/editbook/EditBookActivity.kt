package com.serwylo.babybook.editbook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.widget.addTextChangedListener
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.GridLayoutManager
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.serwylo.babybook.R
import com.serwylo.babybook.databinding.ActivityEditBookBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.entities.Book
import com.serwylo.babybook.editbookpage.EditBookPageActivity
import com.serwylo.babybook.utils.debounce


class EditBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBookBinding
    private lateinit var viewModel: EditBookViewModel

    private val dao = AppDatabase.getInstance(this).bookDao()

    private var bookId: Long = 0L

    private val addPage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        bookId = intent.extras?.getLong(EXTRA_BOOK_ID) ?: 0L
        if (bookId == 0L) {
            AppDatabase.executor.execute {
                bookId = dao.insert(Book("New Book"))
                val pages = dao.getBookPages(bookId)

                runOnUiThread {
                    viewModel = ViewModelProvider(this, EditBookViewModelFactory(application, bookId, "New Book", pages)).get(EditBookViewModel::class.java)
                    setup(bookId)
                }
            }
        } else {
            AppDatabase.executor.execute {
                val book = dao.getBook(bookId)
                val pages = dao.getBookPages(bookId)

                runOnUiThread {
                    viewModel = ViewModelProvider(this, EditBookViewModelFactory(application, bookId, book.title, pages)).get(EditBookViewModel::class.java)
                    setup(bookId)
                }
            }
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.add_page -> {
                onAddPage()
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
        menuInflater.inflate(R.menu.edit_book_menu, menu)
        return true
    }

    private fun setup(bookId: Long) {
        viewModel.bookTitle.observe(this, { title ->
            if (binding.bookTitle.text.toString() != title) {
                binding.bookTitle.setText(title)
            }
        })

        binding.pages.layoutManager = GridLayoutManager(this, 2)
        binding.pages.adapter = EditBookPagesAdapter().also { adapter ->

            adapter.setPageSelectedListener { page ->
                startActivity(Intent(this, EditBookPageActivity::class.java).apply {
                    putExtra(EditBookPageActivity.EXTRA_BOOK_ID, bookId)
                    putExtra(EditBookPageActivity.EXTRA_BOOK_PAGE_ID, page.id)
                })
            }

            viewModel.pages.observe(this) { pages ->
                adapter.setData(pages)
                adapter.notifyDataSetChanged()
            }
        }

        binding.bookTitle.addTextChangedListener { viewModel.bookTitle.value = it.toString() }
        binding.bookTitle.doAfterTextChanged(debounce(300L, lifecycleScope) {
            onSave(bookId, it?.toString() ?: "")
        })
    }

    private fun onDeleteBook() {
        MaterialAlertDialogBuilder(this)
            .setTitle("Delete book?")
            .setMessage("Are you sure you want to remove this book? This action cannot be undone.")
            .setPositiveButton("Delete") { _, _ ->
                viewModel.deleteBook {
                    finish()
                }
            }
            .show()
    }

    private fun onAddPage() {
        addPage.launch(
            Intent(this, EditBookPageActivity::class.java).apply {
                putExtra(EditBookPageActivity.EXTRA_BOOK_ID, bookId)
            }
        )
    }

    private fun onSave(bookId: Long, title: String) {
        AppDatabase.executor.execute {
            val newBook = Book(if (title.isNotEmpty()) title else "New Book").apply { id = bookId }

            dao.update(newBook)
        }
    }

    companion object {
        const val EXTRA_BOOK_ID = "bookId"
    }

}