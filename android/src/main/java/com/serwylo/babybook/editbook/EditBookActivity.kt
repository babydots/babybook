package com.serwylo.babybook.editbook

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.GridLayoutManager
import com.serwylo.babybook.databinding.ActivityEditBookBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.entities.Book
import com.serwylo.babybook.editbookpage.EditBookPageActivity

class EditBookActivity : AppCompatActivity() {

    private lateinit var binding: ActivityEditBookBinding
    private lateinit var viewModel: EditBookViewModel
    private val dao = AppDatabase.getInstance(this).bookDao()

    private val addPage = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) {
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditBookBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val bookId: Long = intent.extras?.getLong(EXTRA_BOOK_ID) ?: 0L
        if (bookId == 0L) {
            AppDatabase.executor.execute {
                val newBookId = dao.insert(Book("New Book"))
                val pages = dao.getBookPages(newBookId)

                runOnUiThread {
                    viewModel = ViewModelProvider(this, EditBookViewModelFactory(application, newBookId, "New Book", pages)).get(EditBookViewModel::class.java)
                    setup(newBookId)
                }
            }
        } else {
            AppDatabase.executor.execute {
                val book = dao.getBook(bookId)

                runOnUiThread {
                    val pages = dao.getBookPages(bookId)
                    viewModel = ViewModelProvider(this, EditBookViewModelFactory(application, bookId, book.title, pages)).get(EditBookViewModel::class.java)
                    setup(bookId)
                }
            }
        }

    }

    private fun setup(bookId: Long) {
        viewModel.bookTitle.observe(this, { title ->
            binding.save.isEnabled = title.isNotEmpty()
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
        binding.addPage.setOnClickListener { onAddPage(bookId) }
        binding.save.setOnClickListener { onSave(bookId) }
    }

    private fun onAddPage(bookId: Long) {
        addPage.launch(
            Intent(this, EditBookPageActivity::class.java).apply {
                putExtra(EditBookPageActivity.EXTRA_BOOK_ID, bookId)
            }
        )
    }

    private fun onSave(bookId: Long) {
        AppDatabase.executor.execute {
            val title = viewModel.bookTitle.value ?: ""
            val newBook = Book(if (title.isNotEmpty()) title else "New Book").apply { id = bookId }

            dao.update(newBook)
            finish()
        }
    }

    companion object {
        const val EXTRA_BOOK_ID = "bookId"
    }

}