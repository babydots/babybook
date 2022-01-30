package com.serwylo.babybook.bookviewer

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.DocumentsContract
import android.util.Log
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.serwylo.babybook.R
import com.serwylo.babybook.book.BookConfig
import com.serwylo.babybook.book.Page
import com.serwylo.babybook.databinding.ActivityBookViewerBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.pdf.generatePdf
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*

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

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.view_book_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.share -> {
                share()
                return true
            }
            R.id.pdf -> {
                startPdfExport()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun share() {

    }

    private fun startPdfExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "${viewModel.book.title}.pdf")

            // Optionally, specify a URI for the directory that should be opened in
            // the system file picker before your app creates the document.
            // putExtra(DocumentsContract.EXTRA_INITIAL_URI, pickerInitialUri)
        }
        startActivityForResult(intent, RESULT_CREATE_FILE)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, resultData: Intent?) {
        super.onActivityResult(requestCode, resultCode, resultData)

        if (requestCode == RESULT_CREATE_FILE && resultCode == RESULT_OK) {
            val uri = resultData?.data ?: return
            try {
                lifecycleScope.launchWhenCreated {
                    withContext(Dispatchers.IO) {
                        contentResolver.openFileDescriptor(uri, "w")?.use {
                            FileOutputStream(it.fileDescriptor).use { outputStream ->
                                exportPdf(outputStream)
                            }
                        }
                    }

                    Toast.makeText(this@BookViewerActivity, "PDF successfully created.", Toast.LENGTH_LONG).show()
                }
            } catch (e: IOException) {
                Toast.makeText(this, "Uh oh. Something went wrong making your PDF. It may not have saved correctly.", Toast.LENGTH_LONG).show()
            }
        }
    }

    private suspend fun exportPdf(outputStream: OutputStream) {
        val pages = viewModel.pages.value?.map {
            val file = it.imageFile(this)

            if (file == null) null else Page(
                it.title(),
                file,
                it.text() ?: "",
            )
        } ?: listOf()

        val pdfFile = File(cacheDir, "${viewModel.book.title}.pdf")

        generatePdf(viewModel.book.title, pages.filterNotNull(), pdfFile, BookConfig.Default)

        pdfFile.inputStream().use { it.copyTo(outputStream) }
    }

    private fun setup(binding: ActivityBookViewerBinding) {

        supportActionBar?.title = viewModel.book.title

        binding.previous.setOnClickListener { viewModel.turnToPreviousPage() }
        binding.next.setOnClickListener { viewModel.turnToNextPage() }

        viewModel.currentPage.observe(this) {
            binding.previous.visibility = if (viewModel.previousPage() == null) View.GONE else View.VISIBLE
            binding.next.visibility = if (viewModel.nextPage() == null) View.GONE else View.VISIBLE

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

                binding.title.text = page.title()
                binding.text.text = page.text()
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

        private const val TAG = "BookViewerActivity"

        const val EXTRA_BOOK_ID = "bookId"

        private const val RESULT_CREATE_FILE = 1

    }

}