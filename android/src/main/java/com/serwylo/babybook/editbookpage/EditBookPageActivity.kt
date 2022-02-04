package com.serwylo.babybook.editbookpage

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
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.serwylo.babybook.R
import com.serwylo.babybook.databinding.ActivityEditBookPageBinding
import com.serwylo.babybook.databinding.DialogPageTextInputBinding
import com.serwylo.babybook.databinding.DialogPageTitleInputBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.repositories.BookRepository
import com.serwylo.babybook.mediawiki.WikiSearchResults
import com.serwylo.babybook.mediawiki.searchWikiTitles
import com.serwylo.babybook.utils.viewInWikipedia
import kotlinx.coroutines.runBlocking
import java.lang.IllegalStateException
import com.squareup.picasso.Picasso
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


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
        val repository = BookRepository(AppDatabase.getInstance(this).bookDao())
        viewModel = ViewModelProvider(this@EditBookPageActivity, EditBookPageViewModelFactory(repository, filesDir, bookId, existingId)).get(EditBookPageViewModel::class.java)
        setup()
    }

    private fun setup() {

        invalidateOptionsMenu()

        binding.pageUp.setOnClickListener { viewModel.movePageUp() }
        binding.pageDown.setOnClickListener { viewModel.movePageDown() }

        viewModel.pageNumber.observe(this) { pageNumber ->
            if (pageNumber > 0) {
                binding.pageNoLabel.text = "Page $pageNumber"

                binding.pageUp.visibility = View.VISIBLE
                binding.pageDown.visibility = View.VISIBLE
                binding.pageNoLabel.visibility = View.VISIBLE
            } else {
                binding.pageUp.visibility = View.GONE
                binding.pageDown.visibility = View.GONE
                binding.pageNoLabel.visibility = View.GONE
            }
        }

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

        viewModel.isPreparingPage.observe(this) { isLoading ->
            if (isLoading) {
                Log.d(TAG, "setup: Updating view in response to vm.isLoadingPage")
                binding.loadingSpinner.visibility = View.VISIBLE
                binding.loadingText.visibility = View.VISIBLE
                binding.loadingText.text = "Loading details..."

                binding.image.visibility = View.GONE
                binding.bookPageTitleText.visibility = View.GONE
                binding.bookPageText.visibility = View.GONE
            } else {
                Log.d(TAG, "setup: Updating view in response to !vm.isLoadingPage")
                binding.loadingSpinner.visibility = View.GONE
                binding.loadingText.visibility = View.GONE

                binding.image.visibility = View.VISIBLE
                binding.bookPageTitleText.visibility = View.VISIBLE
                binding.bookPageText.visibility = View.VISIBLE
            }
        }

        viewModel.pageText.observe(this) { binding.bookPageText.text = viewModel.text() }

        viewModel.wikiPageText.observe(this) {
            binding.bookPageText.text = viewModel.text()

            // No need to also do this for viewModel.pageText, because that can only happen if
            // we were already displaying the editText view.
            binding.editText.visibility = if (it.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.pageTitle.observe(this) {
            binding.bookPageTitleText.text = viewModel.title()
            binding.editTitle.visibility = if (it.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

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
                    lifecycleScope.launch {
                        if (!viewModel.preparePage(newTitle)) {
                            Toast.makeText(applicationContext, "An error occurred fetching the \"$newTitle\" page details from wikipedia. Are you connected to the internet?", Toast.LENGTH_LONG).show()
                        }
                    }
                } else {
                    viewModel.clearPage()
                }
            }
        }

        binding.bookPageTitleText.setOnClickListener {
            val view = DialogPageTitleInputBinding.inflate(layoutInflater, null, false)
            view.titleInput.setText(viewModel.title())
            if (viewModel.wikiPageTitle.value?.isNotEmpty() == true) {
                view.originalTitle.text = "Original title on Wikipedia: ${viewModel.wikiPageTitle.value ?: "Unknown"}"
            } else {
                view.originalTitle.visibility = View.GONE
            }
            AlertDialog.Builder(this)
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
                viewModel.wikiPageTitle.value?.also { title ->
                    viewInWikipedia(this, title)
                }
                return true
            }

            R.id.delete_page -> {
                onDeletePage()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    private fun onDeletePage() {
        if (viewModel.pageTitle.value.isNullOrEmpty()) {
            lifecycleScope.launch {
                viewModel.deletePage()
                finish()
            }
        } else {
            MaterialAlertDialogBuilder(this)
                .setTitle("Delete page?")
                .setMessage("Are you sure you want to remove this page? This action cannot be undone.")
                .setPositiveButton("Delete") { _, _ ->
                    lifecycleScope.launch {
                        viewModel.deletePage()
                        finish()
                    }
                }
                .setNegativeButton("Cancel", null)
                .show()
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.edit_book_page_menu, menu)

        val exists = viewModel.wikiPageTitle.value?.isNotEmpty() == true
        menu.findItem(R.id.view_in_wikipedia).isVisible = exists
        menu.findItem(R.id.delete_page).isVisible = exists

        return true
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

                        try {
                            val searchResults = searchWikiTitles(constraint.toString())

                            if (latestSearchTerms == constraint.toString()) {
                                Log.i("WikiSearch", "Found ${searchResults.results.size} results for $constraint which matches the latest search we performed: $latestSearchTerms")
                                results.values = searchResults.results
                                results.count = searchResults.results.size
                            }
                        } catch (e: Throwable) {
                            withContext(Dispatchers.Main) {
                                Toast.makeText(applicationContext, "An error occurred searching wikipedia. Are you connected to the internet?", Toast.LENGTH_LONG).show()
                            }
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