package com.serwylo.babybook.editbook

import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.Html
import android.text.Spanned
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.serwylo.babybook.databinding.ActivityEditBookPageBinding
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.mediawiki.WikiSearchResults
import com.serwylo.babybook.mediawiki.searchWikiTitles
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.lang.IllegalStateException
import kotlin.math.min


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
                    viewModel = ViewModelProvider(this, EditBookPageViewModelFactory(application, bookId, page)).get(EditBookPageViewModel::class.java)
                    Log.d(TAG, "onCreate: Page ${page.id} loaded, values assigned to ViewModel. Will call setup()")
                    setup()
                }
            }
        } else {
            Log.d(TAG, "onCreate: New page, creating empty viewModel.")
            viewModel = ViewModelProvider(this, EditBookPageViewModelFactory(application, bookId)).get(EditBookPageViewModel::class.java)
            setup()
        }
    }

    private fun setup() {

        viewModel.pageTitle.observe(this) { title ->
        }

        viewModel.isSearchingPages.observe(this) { isSearching ->
            if (isSearching) {
                Log.d(TAG, "setup: Updating view in response to vm.isSearchingPages")
                binding.bookPageText.text = ""
                binding.save.isEnabled = false
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
            } else {
                Log.d(TAG, "setup: Updating view in response to !vm.isLoadingPage")
                binding.loadingSpinner.visibility = View.GONE
                binding.loadingText.visibility = View.GONE

                binding.save.isEnabled = true
            }
        }

        viewModel.pageText.observe(this) { text ->
            Log.d(TAG, "setup: Updating view in response to pageText being updated: ${text.substring(0, min(text.length, 30))}")
            binding.bookPageText.text = text
        }

        binding.save.setOnClickListener { onSave() }

        Log.d(TAG, "setup: Setting initial text input to ${viewModel.pageTitle.value}")
        binding.bookPageTitle.setText(viewModel.pageTitle.value)

        binding.bookPageTitle.also {
            it.setAdapter(AutocompleteAdapter(this))

            it.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
                Log.d(TAG, "setup: Selected recommended value from wiki: ${binding.bookPageTitle.text}. Assigning to ViewModel.")
                val newTitle = binding.bookPageTitle.text.toString()
                viewModel.pageTitle.value = newTitle
                if (newTitle.isNotEmpty()) {
                    Log.d(TAG, "setup: asking vm to preparePage(...)")
                    viewModel.preparePage(newTitle)
                } else {
                    Log.d(TAG, "setup: asking vm to clearPage()")
                    viewModel.clearPage()
                }
            }
        }

    }

    private fun onSave() {
        viewModel.save {
            finish()
        }
    }

    companion object {
        const val TAG = "EditBookPageActivity"
        const val EXTRA_BOOK_PAGE_ID = "bookPageId"
        const val EXTRA_BOOK_ID = "bookId"
    }

    inner class AutocompleteAdapter(private val context: Context) : BaseAdapter(), Filterable {
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
                        lifecycleScope.launch(Dispatchers.Main) {
                            viewModel.isSearchingPages.value = true
                        }

                        val searchResults = searchWikiTitles(constraint.toString())

                        if (latestSearchTerms == constraint.toString()) {
                            Log.i("WikiSearch", "Found ${searchResults.results.size} results for $constraint which matches the latest search we performed: $latestSearchTerms")
                            results.values = searchResults.results
                            results.count = searchResults.results.size
                        }

                        lifecycleScope.launch(Dispatchers.Main) {
                            viewModel.isSearchingPages.value = false
                        }
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