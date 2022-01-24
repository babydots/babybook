package com.serwylo.babybook.bookviewer

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import androidx.lifecycle.ViewModelProvider
import com.serwylo.babybook.R
import com.serwylo.babybook.databinding.ActivityBookViewerBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.editbook.EditBookViewModel
import com.serwylo.babybook.editbook.EditBookViewModelFactory
import com.serwylo.babybook.mediawiki.processTitle
import com.squareup.picasso.Picasso

class BookViewerActivity : AppCompatActivity() {

    private lateinit var viewModel: BookViewerViewModel

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        val binding = ActivityBookViewerBinding.inflate(layoutInflater)

        setContentView(binding.root)

        AppDatabase.executor.execute {

            val dao = AppDatabase.getInstance(this).bookDao()

            val bookId = intent.extras?.getLong(EXTRA_BOOK_ID) ?: error("Can't view book, expected to find the ID of the book in the Intent $EXTRA_BOOK_ID but not found.")
            val book = dao.getBook(bookId)
            val pages = dao.getBookPages(bookId)

            viewModel = ViewModelProvider(this, BookViewerModelFactory(book, pages)).get(BookViewerViewModel::class.java)

            runOnUiThread {
                setup(binding)
            }

        }

    }

    private fun setup(binding: ActivityBookViewerBinding) {

        supportActionBar?.title = viewModel.book.title

        binding.previous.setOnClickListener { viewModel.previousPage() }
        binding.next.setOnClickListener { viewModel.nextPage() }

        viewModel.currentPage.observe(this) {
            it?.also { page ->
                if (page.imagePath == null) {
                    binding.image.visibility = View.GONE
                } else {
                    binding.image.visibility = View.VISIBLE
                    Picasso.get()
                        .load(page.imagePath)
                        .fit()
                        .centerCrop()
                        .into(binding.image)
                }

                binding.title.text = processTitle(page.title)
                binding.text.text = page.text
            }
        }

        viewModel.pages.observe(this) { pages ->
            val current = viewModel.currentPage.value

            if (pages.none { it.id == current?.id }) {
                viewModel.currentPage.value = pages.firstOrNull()
            }
        }
    }

    companion object {
        const val EXTRA_BOOK_ID = "bookId"
    }

}