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
import androidx.recyclerview.widget.GridLayoutManager
import com.alexvasilkov.gestures.Settings
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.serwylo.babybook.R
import com.serwylo.babybook.databinding.*
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
                binding.bodyText.text = ""
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

                hideTextAndIcons()
            } else {
                Log.d(TAG, "setup: Updating view in response to !vm.isLoadingPage")
                binding.loadingSpinner.visibility = View.GONE
                binding.loadingText.visibility = View.GONE

                showTextAndIcons()
            }
        }

        viewModel.pageText.observe(this) { binding.bodyText.text = viewModel.text() }

        viewModel.wikiPage.observe(this) { wikiPage ->
            binding.bodyText.text = viewModel.text()

            if (wikiPage == null) {
                hideTextAndIcons()
            } else {
                showTextAndIcons()
            }

            // Defensive so as to stop the auto complete prompt firing after the wiki page is
            // fetched for the first time.
            if (binding.titleInput.text.toString() != wikiPage?.title ?: "") {
                binding.titleInput.setText(wikiPage?.title ?: "")
            }
            binding.titleText.text = viewModel.title()

            // The first time we add a new page, there is no "view in wiki" link in the menu.
            // Once we get a page title sorted out, we can then show this menu option.
            invalidateOptionsMenu()
        }

        viewModel.pageTitle.observe(this) {
            binding.titleText.text = viewModel.title()
            binding.titleTextIcon.visibility = if (it.isNullOrEmpty()) View.GONE else View.VISIBLE
        }

        viewModel.mainImage.observe(this) { image ->
            if (image != null) {
                Picasso.get()
                    .load(image.filename)
                    .fit()
                    .centerCrop()
                    .into(binding.mainImage)
                binding.mainImage.visibility = View.VISIBLE
            } else {
                binding.mainImage.visibility = View.GONE
            }
        }

        binding.titleInput.also {
            it.setText(viewModel.wikiPage.value?.title ?: "")
            it.setAdapter(AutocompleteAdapter())

            it.onItemClickListener = AdapterView.OnItemClickListener { _, _, _, _ ->
                val newTitle = it.text.toString()
                // viewModel.wikiPageTitle.value = newTitle // TODO: Will the autocomplete automatically populate the GUI widget? Or do we need to do that ourselves? If automatic, can probably just leave this as is.
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

        binding.imageConfig.setOnClickListener {

            val images = viewModel.allImages.value
            val wikiPage = viewModel.wikiPage.value

            if (wikiPage != null) {
                // val view = DialogPageImageSelectorBinding.inflate(layoutInflater, null, false)
                // view.images.layoutManager = GridLayoutManager(this, 3)

                val view = DialogPageImageSettingsBinding.inflate(layoutInflater, null, false)

                view.previewImageSource.controller.settings
                    .setPanEnabled(true)
                    .setZoomEnabled(true)
                    .setRotationEnabled(false)
                    .setBoundsType(Settings.Bounds.NONE)
                    .setFitMethod(Settings.Fit.OUTSIDE)
                    .setMovementArea(1000, 1000)
                    .setFillViewport(true)

                // view.previewImageSource.controller.state
                //     .set(50f, 100f, 2f, 2f)

                viewModel.mainImage.observe(this) { image ->
                    if (image != null) {
                        Picasso.get()
                            .load(image.filename)
                            .into(view.previewImageSource)
                        view.previewImageSource.visibility = View.VISIBLE
                    } else {
                        view.previewImageSource.visibility = View.GONE
                    }
                }

                val alert = AlertDialog.Builder(this)
                    .setView(view.root)
                    .setPositiveButton("Close") { _, _ -> }
                    .show()

                /*val adapter = SelectImageAdapter(images ?: listOf()).also { adapter ->
                    adapter.setImageSelectedListener { wikiImage ->
                        viewModel.mainImage.value = wikiImage
                        alert.dismiss()
                        viewModel.save()
                    }
                }

                view.images.adapter = adapter

                viewModel.allImages.observe(this) { adapter.setImages(it) }

                viewModel.isLoadingImages.observe(this) { isLoading ->
                    if (isLoading) {
                        view.images.visibility = View.GONE
                        view.loadingSpinner.visibility = View.VISIBLE
                        view.loadingText.visibility = View.VISIBLE
                    } else {
                        view.images.visibility = View.VISIBLE
                        view.loadingSpinner.visibility = View.GONE
                        view.loadingText.visibility = View.GONE
                    }
                }

                viewModel.ensureImagesDownloaded(wikiPage)*/
            }
        }

        binding.titleText.setOnClickListener {
            val view = DialogPageTitleInputBinding.inflate(layoutInflater, null, false)
            view.titleInput.setText(viewModel.title())
            val wikiPageTitle = viewModel.wikiPage.value?.title
            if (!wikiPageTitle.isNullOrEmpty()) {
                view.originalTitle.text = "Original title on Wikipedia: $wikiPageTitle"
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

        binding.bodyText.setOnClickListener {
            val view = DialogPageTextInputBinding.inflate(layoutInflater, null, false)
            view.textInput.setText(viewModel.text())
            val wikiPageText = viewModel.wikiPage.value?.text
            if (!wikiPageText.isNullOrEmpty()) {
                view.originalText.text = wikiPageText
                view.originalText.setOnLongClickListener {
                    view.textInput.setText(wikiPageText)
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
                .load(image.filename)
                .fit()
                .centerCrop()
                .into(binding.mainImage)
            binding.mainImage.visibility = View.VISIBLE
        }

    }

    private fun showTextAndIcons() {
        binding.titleText.visibility = View.VISIBLE
        binding.titleTextIcon.visibility = View.VISIBLE

        binding.imageConfig.visibility = View.VISIBLE
        binding.imageChooseIcon.visibility = View.VISIBLE
        binding.imageSettingsIcon.visibility = View.VISIBLE

        binding.bodyText.visibility = View.VISIBLE
        binding.bodyTextIcon.visibility = View.VISIBLE
    }

    private fun hideTextAndIcons() {
        binding.titleText.visibility = View.GONE
        binding.titleTextIcon.visibility = View.GONE

        binding.imageConfig.visibility = View.GONE
        binding.imageChooseIcon.visibility = View.GONE
        binding.imageSettingsIcon.visibility = View.GONE

        binding.bodyText.visibility = View.GONE
        binding.bodyTextIcon.visibility = View.GONE
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.view_in_wikipedia -> {
                viewModel.wikiPage.value?.title?.also { title ->
                    viewInWikipedia(this, viewModel.wikiSite, title)
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

        val exists = viewModel.wikiPage.value != null
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
                if (constraint?.toString() ?: "" == viewModel.wikiPage.value?.title) {
                    return FilterResults()
                }

                latestSearchTerms = constraint?.toString() ?: ""
                Log.i("WikiSearch", "Recording that our last search was for: $latestSearchTerms")
                val results = FilterResults()
                if (constraint != null && constraint.isNotEmpty()) {
                    runBlocking {
                        viewModel.isSearchingPages.postValue(true)

                        try {
                            val searchResults = searchWikiTitles(viewModel.wikiSite.url(), constraint.toString())

                            if (latestSearchTerms == constraint.toString()) {
                                Log.i("WikiSearch", "Found ${searchResults.results.size} results for $constraint which matches the latest search we performed: $latestSearchTerms")
                                results.values = searchResults.results
                                results.count = searchResults.results.size
                            }
                        } catch (e: Throwable) {
                            withContext(Dispatchers.Main) {
                                Log.e("WikiSearch", "Error searching wiki pages: ${e.message}", e)
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