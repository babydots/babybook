package com.serwylo.babybook.editbookpage

import android.app.Application
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.mediawiki.downloadImages
import com.serwylo.babybook.mediawiki.loadWikiPage
import com.serwylo.babybook.mediawiki.processTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class EditBookPageViewModel(private val application: Application, val bookId: Long, private val existingBookPage: BookPage? = null): ViewModel() {

    private val dao = AppDatabase.getInstance(application).bookDao()

    private var bookPageId = existingBookPage?.id ?: 0L

    val wikiPageTitle = MutableLiveData(existingBookPage?.wikiPageTitle ?: "")
    val pageTitle = MutableLiveData(existingBookPage?.pageTitle)
    val wikiPageText = MutableLiveData(existingBookPage?.wikiPageText ?: "")
    val pageText = MutableLiveData(existingBookPage?.pageText)
    val pageNumber = MutableLiveData(existingBookPage?.pageNumber ?: 0)
    val mainImage = MutableLiveData<String?>(existingBookPage?.imagePath)
    val allImages = MutableLiveData(listOf<File>())

    val isSearchingPages = MutableLiveData(false)
    val isLoadingPage = MutableLiveData(false)

    fun title() = pageTitle.value ?: wikiPageTitle.value ?: ""
    fun text() = pageText.value ?: wikiPageText.value ?: ""

    fun movePageUp() {
        AppDatabase.executor.execute {
            val currentId = bookPageId
            if (currentId < 0) {
                return@execute
            }

            val currentPage = dao.getBookPage(bookPageId)
            val maxPage = dao.countPages(bookId)
            if (currentPage.pageNumber >= maxPage) {
                return@execute
            }

            val nextPage = dao.getPageNumber(currentPage.pageNumber + 1) ?: return@execute

            dao.update(currentPage.copy(pageNumber = currentPage.pageNumber + 1).apply { id = currentPage.id })
            dao.update(nextPage.copy(pageNumber = nextPage.pageNumber - 1).apply { id = nextPage.id })

            pageNumber.postValue(currentPage.pageNumber + 1)
        }
    }

    fun movePageDown() {
        AppDatabase.executor.execute {
            val currentId = bookPageId
            if (currentId < 0) {
                return@execute
            }

            val currentPage = dao.getBookPage(bookPageId)
            if (currentPage.pageNumber <= 1) {
                return@execute
            }

            val previousPage = dao.getPageNumber(currentPage.pageNumber - 1) ?: return@execute

            dao.update(currentPage.copy(pageNumber = currentPage.pageNumber - 1).apply { id = currentPage.id })
            dao.update(previousPage.copy(pageNumber = previousPage.pageNumber + 1).apply { id = previousPage.id })

            pageNumber.postValue(currentPage.pageNumber - 1)
        }
    }

    suspend fun preparePage(title: String): Boolean = withContext(Dispatchers.Main) {
        Log.d(TAG, "preparePage: Getting ready to fetch wiki data")
        isLoadingPage.value = true

        try {
            pageTitle.value = processTitle(title)

            val dir = File(application.filesDir, title)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            val details = loadWikiPage(title, dir)
            wikiPageText.value = details.parseParagraphs().firstOrNull() ?: ""
            save()

            val imageNames = details.getImageNamesOfInterest()
            if (imageNames.isNotEmpty()) {
                val images = downloadImages(details.getImageNamesOfInterest(), dir)
                allImages.value = images
                images.firstOrNull()?.also { image ->
                    mainImage.value = "file://${image.absolutePath}"
                }
                save()
            }

            return@withContext true
        } catch (e: Throwable) {
            return@withContext false
        } finally {
            isLoadingPage.value = false
        }
    }

    fun deletePage(callback: () -> Unit) {
        bookPageId.also { id ->
            if (id > 0) {
                AppDatabase.executor.execute {
                    val page = dao.getBookPage(id)
                    dao.delete(page)
                    callback()
                }
            }
        }
    }

    fun clearPage() {
        isLoadingPage.value = false
        isSearchingPages.value = false
        pageText.value = ""
    }

    fun save() {
        AppDatabase.executor.execute {

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

                dao.update(page)
            } else {
                Log.d(TAG, "save: Adding new book page")
                val page = BookPage(
                    pageNumber = dao.countPages(bookId) + 1,
                    wikiPageTitle = wikiPageTitle.value ?: "",
                    wikiPageText = wikiPageText.value ?: "",
                    pageTitle = pageTitle.value,
                    pageText = pageText.value,
                    bookId = bookId,
                    imagePath = mainImage.value
                )

                bookPageId = dao.insert(page)
                pageNumber.postValue(page.pageNumber) // WARNING: This may not update in time for a subsequent call to 'save'!
            }

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

class EditBookPageViewModelFactory(private val application: Application, private val bookId: Long, private val existingPage: BookPage? = null) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditBookPageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditBookPageViewModel(application, bookId, existingPage) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}