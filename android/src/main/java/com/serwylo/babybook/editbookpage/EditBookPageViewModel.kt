package com.serwylo.babybook.editbookpage

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.db.entities.WikiImage
import com.serwylo.babybook.db.entities.WikiPage
import com.serwylo.babybook.db.repositories.BookRepository
import com.serwylo.babybook.mediawiki.downloadWikiImage
import com.serwylo.babybook.mediawiki.loadWikiPage
import kotlinx.coroutines.*
import java.io.File

class EditBookPageViewModel(private val repository: BookRepository, private val filesDir: File, val bookId: Long, private val existingBookPageId: Long): ViewModel() {

    private var bookPageId = existingBookPageId

    val pageTitle = MutableLiveData<String?>(null)
    val pageText = MutableLiveData<String?>(null)
    val pageNumber = MutableLiveData(0)

    val wikiPage = MutableLiveData<WikiPage?>(null)
    val mainImage = MutableLiveData<WikiImage?>(null)
    val allImages = MutableLiveData(listOf<WikiImage>())

    val isLoading = MutableLiveData(false)
    val isSearchingPages = MutableLiveData(false)
    val isPreparingPage = MutableLiveData(false)

    init {
        viewModelScope.launch {
            if (existingBookPageId > 0) {
                val bookPage = repository.getBookPage(existingBookPageId)

                pageTitle.value = bookPage.title
                pageText.value = bookPage.text
                pageNumber.value = bookPage.pageNumber

                bookPage.wikiPageId?.also { wikiPageId ->
                    val page = repository.getWikiPage(wikiPageId)
                    wikiPage.value = page
                    allImages.value = withContext(Dispatchers.IO) { repository.getWikiImages(page) }

                    bookPage.wikiImageId?.also { wikiImageId ->
                        mainImage.value = withContext(Dispatchers.IO) { repository.getWikiImage(wikiImageId) }
                    }
                }
            }

            isLoading.value = false
        }
    }

    fun title() = pageTitle.value ?: wikiPage.value?.title ?: ""
    fun text() = pageText.value ?: wikiPage.value?.text ?: ""

    fun movePageUp() {
        val currentId = bookPageId
        if (currentId < 0) {
            return
        }

        viewModelScope.launch {
            pageNumber.value = repository.movePageUp(bookPageId, bookId)
        }
    }

    fun movePageDown() {
        val currentId = bookPageId
        if (currentId < 0) {
            return
        }

        viewModelScope.launch {
            pageNumber.value = repository.movePageDown(bookPageId, bookId)
        }
    }


    suspend fun preparePage(title: String): Boolean = withContext(Dispatchers.IO) {
        Log.d(TAG, "preparePage: Getting ready to fetch wiki data")
        withContext(Dispatchers.Main) { isPreparingPage.value = true }

        try {
            val existingWikiPage = repository.findWikiPageByTitle(title)
            if (existingWikiPage != null) {
                withContext(Dispatchers.Main) {
                    wikiPage.value = existingWikiPage
                    pageTitle.value = null
                    pageText.value = null
                }

                val images = repository.getWikiImages(existingWikiPage)

                withContext(Dispatchers.Main) {
                    mainImage.value = images.firstOrNull()
                    allImages.value = images
                }
            } else {
                Log.d(TAG, "preparePage: Page does not yet exist in our local DB, will fetch it.")
                val dir = File(filesDir, title)
                if (!dir.exists()) {
                    dir.mkdirs()
                }

                val details = loadWikiPage(title, dir)

                Log.d(TAG, "preparePage: Wikipedia page details loaded, will save to DB.")
                val newWikiPage = repository.addNewWikiPage(title, details.parseParagraphs().firstOrNull() ?: "")

                withContext(Dispatchers.Main) {
                    wikiPage.value = newWikiPage
                    pageTitle.value = null
                    pageText.value = null
                }

                Log.d(TAG, "preparePage: Saving book page to DB now that we have wiki page details.")
                val initialSaveJob = save()

                val imageNames = details.getImageNamesOfInterest()

                Log.d(TAG, "preparePage: Ensuring all ${imageNames.size} images are available.")
                if (imageNames.isNotEmpty()) {
                    val images: List<WikiImage> = imageNames.map { filename ->
                        async(Dispatchers.IO) {
                            val existingWikiImage = repository.findWikiImageByName(filename)
                            if (existingWikiImage != null) {
                                Log.d(TAG, "preparePage: Ignoring $filename because we already have a local copy saved.")
                                existingWikiImage
                            } else {
                                Log.d(TAG, "preparePage: Downloading $filename and saving metadata to our local DB.")
                                val file = downloadWikiImage(filename, dir)
                                repository.addNewWikiImage(newWikiPage, filename, file)
                            }
                        }
                    }.awaitAll()

                    withContext(Dispatchers.Main) {
                        allImages.value = images
                        mainImage.value = images.firstOrNull()
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        allImages.value = listOf()
                        mainImage.value = null
                    }
                }

                // If we already have cached images, then the above fetching will be faster than saving
                // the record to the database. Hence, we wait for the save to comlete.
                initialSaveJob.join()

                Log.d(TAG, "preparePage: Saving book page again now that we know about its images.")
                save()
            }

            return@withContext true
        } catch (e: Throwable) {
            Log.e(TAG, "preparePage: Error fetching wiki data: $e", e)
            return@withContext false
        } finally {
            withContext(Dispatchers.Main) {
                isPreparingPage.value = false
            }
        }
    }

    suspend fun deletePage() {
        repository.removeBookPage(bookPageId)
    }

    fun clearPage() {
        isPreparingPage.value = false
        isSearchingPages.value = false
        pageText.value = ""
    }

    fun save() = viewModelScope.launch {
        if (bookPageId > 0) {
            Log.d(TAG, "save: Updating existing book page")
            val page = BookPage(
                pageNumber = pageNumber.value!!,
                text = pageText.value,
                title = pageTitle.value,
                bookId = bookId,
                wikiImageId = mainImage.value?.id,
                wikiPageId = wikiPage.value?.id,
                id = bookPageId,
            )

            repository.updateBookPage(page)
        } else {
            Log.d(TAG, "save: Adding new book page")
            val newPageNumber = repository.getNextPageNumber(bookId)
            val page = BookPage(
                pageNumber = repository.getNextPageNumber(bookId),
                text = pageText.value,
                title = pageTitle.value,
                bookId = bookId,
                wikiImageId = mainImage.value?.id,
                wikiPageId = wikiPage.value?.id,
            )

            bookPageId = repository.addNewBookPage(page)
            pageNumber.value = newPageNumber
        }

    }

    companion object {
        private const val TAG = "EditBookPageViewModel"
    }

    fun manuallyUpdateTitle(title: String) {
        pageTitle.value = title
        save()
    }

    fun manuallyUpdateText(title: String) {
        pageText.value = title
        save()
    }

}

class EditBookPageViewModelFactory(private val repository: BookRepository, private val filesDir: File, private val bookId: Long, private val existingPageId: Long = 0) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditBookPageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditBookPageViewModel(repository, filesDir, bookId, existingPageId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}