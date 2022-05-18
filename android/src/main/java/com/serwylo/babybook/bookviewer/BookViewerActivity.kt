package com.serwylo.babybook.bookviewer

import android.content.Intent
import android.os.Bundle
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.NavUtils
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.serwylo.babybook.Preferences
import com.serwylo.babybook.R
import com.serwylo.babybook.attribution.AttributionActivity
import com.serwylo.babybook.book.BookConfig
import com.serwylo.babybook.book.Page
import com.serwylo.babybook.databinding.ActivityBookViewerBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.entities.imagePathToFile
import com.serwylo.babybook.db.repositories.BookRepository
import com.serwylo.babybook.pdf.generatePdf
import com.serwylo.babybook.utils.viewInWikipedia
import com.serwylo.immersivelock.ImmersiveLock
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.*

class BookViewerActivity : AppCompatActivity() {

    private lateinit var pageTurnType: String
    private lateinit var viewModel: BookViewerViewModel
    private lateinit var binding: ActivityBookViewerBinding
    private lateinit var immersiveLock: ImmersiveLock

    private val onPageChange = object: ViewPager2.OnPageChangeCallback() {
        override fun onPageSelected(position: Int) {
            binding.apply {
                val count = (pager.adapter?.itemCount ?: 0)
                nextOverlay.visibility = if (pageTurnType == Preferences.PAGE_TURN_TYPE_BUTTONS_OVERLAYED && position < count - 1) {
                    View.VISIBLE
                } else {
                    View.GONE
                }

                previousOverlay.visibility = if (pageTurnType == Preferences.PAGE_TURN_TYPE_BUTTONS_OVERLAYED && position > 0) {
                    View.VISIBLE
                } else {
                    View.GONE
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        pageTurnType = Preferences.pageTurnType(this)
    }

    override fun onCreate(savedInstanceState: Bundle?) {

        super.onCreate(savedInstanceState)

        binding = ActivityBookViewerBinding.inflate(layoutInflater)

        setContentView(binding.root)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        immersiveLock = ImmersiveLock.Builder(binding.tapToUnlock)
            .onStopImmersiveMode {
                lifecycleScope.launch {
                    // Without this delay, the showing of the action bar trips over the showing of
                    // the status bar, and results in it showing behind the status bar. I discovered
                    // this while reading through the show() code for the support action bar which
                    // had a hard coded delay of 250ms before starting an animation for showing the
                    // action bar, specifically due to the fact it wants to wait for the status bar
                    // to do its thing first.
                    delay(300)
                    supportActionBar?.show()
                }
            }
            .build()

        val dao = AppDatabase.getInstance(this).bookDao()
        val bookId = intent.extras?.getLong(EXTRA_BOOK_ID) ?: error("Can't view book, expected to find the ID of the book in the Intent $EXTRA_BOOK_ID but not found.")

        viewModel = ViewModelProvider(this@BookViewerActivity, BookViewerModelFactory(BookRepository(dao), bookId)).get(BookViewerViewModel::class.java)
        setup(binding)

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.view_book_menu, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.lock -> {
                supportActionBar?.hide()
                immersiveLock.startImmersiveMode(this)
                return true
            }

            R.id.view_in_wikipedia -> {
                viewModel.currentPage()?.wikiPage?.title?.also { title ->
                    lifecycleScope.launch(Dispatchers.IO) {
                        val wikiSite = viewModel.getWikiSite()
                        viewInWikipedia(this@BookViewerActivity, wikiSite, title)
                    }
                }
                return true
            }
            R.id.pdf -> {
                startPdfExport()
                return true
            }
            R.id.attribution -> {
                viewModel.book.value?.id?.also { bookId ->
                    startActivity(Intent(this, AttributionActivity::class.java).apply {
                        putExtra(AttributionActivity.EXTRA_BOOK_ID, bookId)
                    })
                }
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_VOLUME_DOWN -> viewModel.turnToNextPage()
            KeyEvent.KEYCODE_VOLUME_UP -> viewModel.turnToPreviousPage()
        }
        return true
    }

    private fun startPdfExport() {
        val intent = Intent(Intent.ACTION_CREATE_DOCUMENT).apply {
            addCategory(Intent.CATEGORY_OPENABLE)
            type = "application/pdf"
            putExtra(Intent.EXTRA_TITLE, "${viewModel.book.value?.title}.pdf")
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
        viewModel.pages.value?.also { pages ->
            val pdfPages = pages.map {
                val file = imagePathToFile(this, it.image?.filename)

                Page(
                    it.title(),
                    file,
                    it.text(),
                )
            }

            val title = viewModel.book.value?.title ?: "Book"

            val pdfFile = File(cacheDir, "${title}.pdf")

            generatePdf(title, pdfPages, pdfFile, BookConfig.Default)

            pdfFile.inputStream().use { it.copyTo(outputStream) }
        }
    }

    private fun setup(binding: ActivityBookViewerBinding) {

        viewModel.book.observe(this) { book ->
            supportActionBar?.title = book.title
        }

        viewModel.pages.observe(this) { pages ->
            val firstPage = pages.firstOrNull()
            if (firstPage == null) {
                Toast.makeText(this, "This book does not have any pages yet.", Toast.LENGTH_SHORT).show()
                finish()
            } else {
                val pagerAdapter = ScreenSlidePagerAdapter(this)
                binding.pager.adapter = pagerAdapter
                binding.pager.offscreenPageLimit = 3
                binding.pager.registerOnPageChangeCallback(onPageChange)
            }
        }

        binding.previousOverlay.setOnClickListener { viewModel.turnToPreviousPage() }
        binding.nextOverlay.setOnClickListener { viewModel.turnToNextPage() }

        viewModel.currentPageIndex.observe(this) { currentPage ->
            // Be defensive here, because it is almost always guaranteed that we receive a
            // currentPageIndex value of 0 in this observer, prior to loading the page data from
            // the database. If so, just ignore it (the actual first page render will be triggered
            // by the observer on viewModel.bookWithPages).
            val pages = viewModel.pages.value
            if (pages != null && pages.size > currentPage) {
                binding.pager.currentItem = currentPage
            }
        }
    }

    companion object {

        private const val TAG = "BookViewerActivity"

        const val EXTRA_BOOK_ID = "bookId"

        private const val RESULT_CREATE_FILE = 1

    }

    private inner class ScreenSlidePagerAdapter(fa: FragmentActivity) : FragmentStateAdapter(fa) {
        override fun getItemCount(): Int = viewModel.pages.value!!.size

        override fun createFragment(position: Int): Fragment = BookPageFragment(viewModel.pages.value!![position])
    }

}