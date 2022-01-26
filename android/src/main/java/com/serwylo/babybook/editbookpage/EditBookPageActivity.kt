package com.serwylo.babybook.editbookpage

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.core.widget.doAfterTextChanged
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.serwylo.babybook.R
import com.serwylo.babybook.databinding.ActivityEditBookPageBinding
import com.serwylo.babybook.databinding.DialogPageTextInputBinding
import com.serwylo.babybook.databinding.DialogPageTitleInputBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.mediawiki.WikiSearchResults
import com.serwylo.babybook.mediawiki.processTitle
import com.serwylo.babybook.mediawiki.searchWikiTitles
import com.serwylo.babybook.utils.debounce
import kotlinx.coroutines.runBlocking
import java.lang.IllegalStateException
import kotlin.math.min
import com.squareup.picasso.Picasso


class EditBookPageActivity : AppCompatActivity() {

    private lateinit var viewModel: EditBookPageViewModel

    private lateinit var binding: ActivityEditBookPageBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditBookPageBinding.inflate(layoutInflater)

        setContentView(binding.root)

        val bookId = intent.extras?.getLong(EXTRA_BOOK_ID, 0L) ?: 0L
        if (bookId <= 0) {
            throw IllegalStateException("Expected $EXTRA_BOOK_ID to be > 0, but got 0")
        }

        val existingId = intent.extras?.getLong(EXTRA_BOOK_PAGE_ID, 0L) ?: 0L
        if (existingId > 0) {
            AppDatabase.executor.execute {
                Log.d(TAG, "onCreate: Loading page from DB")
                val page = AppDatabase.getInstance(this).bookDao().getBookPage(existingId)
                runOnUiThread {
                    viewModel = ViewModelProvider(this, EditBookPageViewModelFactory(application, bookId, page)).get(
                        EditBookPageViewModel::class.java)
                    Log.d(TAG, "onCreate: Page ${page.id} loaded, values assigned to ViewModel. Will call setup()")
                    setup()
                }
            }
        } else {
            Log.d(TAG, "onCreate: New page, creating empty viewModel.")
            viewModel = ViewModelProvider(this, EditBookPageViewModelFactory(application, bookId)).get(
                EditBookPageViewModel::class.java)
            setup()
        }
    }

    private fun setup() {

        invalidateOptionsMenu()

        viewModel.isSearchingPages.observe(this) { isSearching ->
            if (isSearching) {
                Log.d(TAG, "setup: Updating view in response to vm.isSearchingPages")
                binding.bookPageText.text = ""
                binding.loadingSpinner.visibility = View.VISIBLE
                binding.loadingText.visibility = View.VISIBLE
                binding.loadingText.text = "Searching Wikipedia..."
            } else {
                Log.d(TAG, "setup: Updating view in response to !vm.isSearchingPages")
                binding.loadingSpinner.visibility = View.GONE
                binding.loadingText.visibility = View.GONE
            }
        }

        viewModel.isLoadingPage.observe(this) { isLoading ->
            if (isLoading) {
                Log.d(TAG, "setup: Updating view in response to vm.isLoadingPage")
                binding.loadingSpinner.visibility = View.VISIBLE
                binding.loadingText.visibility = View.VISIBLE
                binding.loadingText.text = "Loading details..."

                binding.image.visibility = View.GONE
                binding.bookPageTitleText.visibility = View.GONE
            } else {
                Log.d(TAG, "setup: Updating view in response to !vm.isLoadingPage")
                binding.loadingSpinner.visibility = View.GONE
                binding.loadingText.visibility = View.GONE

                binding.image.visibility = View.VISIBLE
                binding.bookPageTitleText.visibility = View.VISIBLE
            }
        }

        viewModel.pageText.observe(this) { binding.bookPageText.text = viewModel.text() }
        viewModel.wikiPageText.observe(this) { binding.bookPageText.text = viewModel.text() }

        viewModel.pageTitle.observe(this) { binding.bookPageTitleText.text = viewModel.title() }
        viewModel.wikiPageTitle.observe(this) {
            binding.bookPageTitleText.text = viewModel.title()

            // The first time we add a new page, there is no "view in wiki" link in the menu.
            // Once we get a page title sorted out, we can then show this menu option.
            invalidateOptionsMenu()
        }

        viewModel.mainImage.observe(this) { image ->
            if (image != null) {
                Picasso.get()
                    .load(image)
                    .fit()
                    .centerCrop()
                    .into(binding.image)
                binding.image.visibility = View.VISIBLE
            } else {
                binding.image.visibility = View.GONE
            }
        }

        binding.bookPageTitle.also {
            it.setText(viewModel.wikiPageTitle.value)
            it.setAdapter(AutocompleteAdapter())

            it.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
                val newTitle = binding.bookPageTitle.text.toString()
                viewModel.wikiPageTitle.value = newTitle
                if (newTitle.isNotEmpty()) {
                    viewModel.preparePage(newTitle)
                } else {
                    viewModel.clearPage()
                }
            }
        }

        binding.bookPageTitleText.setOnClickListener {
            val view = DialogPageTitleInputBinding.inflate(layoutInflater, null, false)
            view.titleInput.setText(viewModel.title())
            if (viewModel.wikiPageTitle.value?.isNotEmpty() == true) {
                view.originalTitle.setText(viewModel.wikiPageTitle.value ?: "")
                view.originalTitle.setOnLongClickListener {
                    view.titleInput.setText(viewModel.wikiPageTitle.value ?: "")
                    true
                }
            } else {
                view.originalTitle.visibility = View.GONE
                view.originalTitleLabel.visibility = View.GONE
            }
            AlertDialog.Builder(this)
                .setTitle("Edit page title")
                .setView(view.root)
                .setPositiveButton("Save") { _, _ ->
                    viewModel.manuallyUpdateTitle(view.titleInput.text?.toString() ?: "")
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
        }

        binding.bookPageText.setOnClickListener {
            val view = DialogPageTextInputBinding.inflate(layoutInflater, null, false)
            view.textInput.setText(viewModel.text())
            if (viewModel.wikiPageTitle.value?.isNotEmpty() == true) {
                view.originalText.setText(viewModel.wikiPageText.value ?: "")
                view.originalText.setOnLongClickListener {
                    view.textInput.setText(viewModel.wikiPageText.value ?: "")
                    true
                }
            } else {
                view.originalText.visibility = View.GONE
                view.originalTextLabel.visibility = View.GONE
            }
            AlertDialog.Builder(this)
                // .setTitle("Edit page text")
                .setView(view.root)
                .setPositiveButton("Save") { _, _ ->
                    viewModel.manuallyUpdateText(view.textInput.text?.toString() ?: "")
                }
                .setNegativeButton("Cancel") { _, _ -> }
                .show()
        }

        viewModel.mainImage.value?.also { image ->
            Picasso.get()
                .load(image)
                .fit()
                .centerCrop()
                .into(binding.image)
            binding.image.visibility = View.VISIBLE
        }

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.view_in_wikipedia -> {
                onViewInWikipedia()
                return true
            }

            R.id.delete_page -> {
                // viewModel.delete()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_book_page_menu, menu)

        val exists = viewModel.wikiPageTitle.value?.isNotEmpty() == true
        menu.findItem(R.id.view_in_wikipedia).isVisible = exists
        menu.findItem(R.id.delete_page).isVisible = exists

        return true
    }

    private fun onViewInWikipedia() {
        viewModel.wikiPageTitle.value?.also { title ->
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://simple.wikipedia.org/wiki/$title"));
            startActivity(intent)
        }
    }

    companion object {
        const val TAG = "EditBookPageActivity"
        const val EXTRA_BOOK_PAGE_ID = "bookPageId"
        const val EXTRA_BOOK_ID = "bookId"
    }

    inner class AutocompleteAdapter() : BaseAdapter(), Filterable {
        private val searchResults = mutableListOf<WikiSearchResults.SearchResult>()

        private var latestSearchTerms = ""

        override fun getCount() = searchResults.size
        override fun getItem(position: Int) = searchResults[position]
        override fun getItemId(position: Int) = searchResults[position].pageid.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: layoutInflater.inflate(android.R.layout.simple_list_item_2, parent, false)

            val text1: TextView = view.findViewById(android.R.id.text1)
            val text2: TextView = view.findViewById(android.R.id.text2)

            val item = getItem(position)
            text1.text = item.title
            text2.text = snippetToHtml(item.snippet)

            return view
        }

        private fun snippetToHtml(snippet: String): Spanned {
            val html = snippet.replace("<span class=\"searchmatch\">", "<strong>").replace("</span>", "</strong>")
            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT)
            } else {
                Html.fromHtml(html)
            }
        }

        override fun getFilter() = object: Filter() {

            override fun performFiltering(constraint: CharSequence?): FilterResults {
                latestSearchTerms = constraint?.toString() ?: ""
                Log.i("WikiSearch", "Recording that our last search was for: $latestSearchTerms")
                val results = FilterResults()
                if (constraint != null && constraint.isNotEmpty()) {
                    runBlocking {
                        viewModel.isSearchingPages.postValue(true)

                        val searchResults = searchWikiTitles(constraint.toString())

                        if (latestSearchTerms == constraint.toString()) {
                            Log.i("WikiSearch", "Found ${searchResults.results.size} results for $constraint which matches the latest search we performed: $latestSearchTerms")
                            results.values = searchResults.results
                            results.count = searchResults.results.size
                        }

                        viewModel.isSearchingPages.postValue(false)
                    }
                }
                return results
            }

            override fun convertResultToString(resultValue: Any?): CharSequence {
                return if (resultValue is WikiSearchResults.SearchResult) {
                    resultValue.title
                } else {
                    ""
                }
            }

            override fun publishResults(constraint: CharSequence?, results: FilterResults?) {
                if (results != null && results.count > 0) {
                    searchResults.clear()
                    searchResults.addAll(results.values as List<WikiSearchResults.SearchResult>)
                }

                notifyDataSetChanged()
            }

        }
    }

}