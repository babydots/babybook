package com.serwylo.babybook.editbookpage

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.db.repositories.BookRepository
import com.serwylo.babybook.mediawiki.downloadImages
import com.serwylo.babybook.mediawiki.loadWikiPage
import com.serwylo.babybook.mediawiki.processTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditBookPageViewModel(private val repository: BookRepository, private val filesDir: File, val bookId: Long, private val existingBookPageId: Long): ViewModel() {

    private var bookPageId = existingBookPageId

    val wikiPageTitle = MutableLiveData("")
    val pageTitle = MutableLiveData<String?>(null)
    val wikiPageText = MutableLiveData("")
    val pageText = MutableLiveData<String?>(null)
    val pageNumber = MutableLiveData(0)
    val mainImage = MutableLiveData<String?>(null)
    val allImages = MutableLiveData(listOf<File>())

    val isLoading = MutableLiveData(false)
    val isSearchingPages = MutableLiveData(false)
    val isPreparingPage = MutableLiveData(false)

    init {
        viewModelScope.launch {
            if (existingBookPageId > 0) {
                val page = repository.getBookPage(existingBookPageId)
                wikiPageTitle.value = page.wikiPageTitle
                pageTitle.value = page.pageTitle
                wikiPageText.value = page.wikiPageText
                pageText.value = page.pageText
                pageNumber.value = page.pageNumber
                mainImage.value = page.imagePath
            }

            isLoading.value = false
        }
    }

    fun title() = pageTitle.value ?: wikiPageTitle.value ?: ""
    fun text() = pageText.value ?: wikiPageText.value ?: ""

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


    suspend fun preparePage(title: String): Boolean = withContext(Dispatchers.Main) {
        Log.d(TAG, "preparePage: Getting ready to fetch wiki data")
        isPreparingPage.value = true

        try {
            pageTitle.value = processTitle(title)

            val dir = File(filesDir, title)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val details = loadWikiPage(title, dir)
            wikiPageText.value = details.parseParagraphs().firstOrNull() ?: ""
            val initialSaveJob = save()

            val imageNames = details.getImageNamesOfInterest()
            if (imageNames.isNotEmpty()) {
                val images = downloadImages(details.getImageNamesOfInterest(), dir)
                allImages.value = images
                images.firstOrNull()?.also { image ->
                    mainImage.value = "file://${image.absolutePath}"
                }
            } else {
                allImages.value = listOf()
                mainImage.value = null
            }

            // If we already have cached images, then the above fetching will be faster than saving
            // the record to the database. Hence, we wait for the save to comlete.
            initialSaveJob.join()
            save()

            return@withContext true
        } catch (e: Throwable) {
            return@withContext false
        } finally {
            isPreparingPage.value = false
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
                wikiPageTitle = wikiPageTitle.value ?: "",
                wikiPageText = wikiPageText.value ?: "",
                pageTitle = pageTitle.value,
                pageText = pageText.value,
                bookId = bookId,
                imagePath = mainImage.value
            ).apply { id = bookPageId }

            repository.updateBookPage(page)
        } else {
            Log.d(TAG, "save: Adding new book page")
            val newPageNumber = repository.getNextPageNumber(bookId)
            val page = BookPage(
                pageNumber = repository.getNextPageNumber(bookId),
                wikiPageTitle = wikiPageTitle.value ?: "",
                wikiPageText = wikiPageText.value ?: "",
                pageTitle = pageTitle.value,
                pageText = pageText.value,
                bookId = bookId,
                imagePath = mainImage.value
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