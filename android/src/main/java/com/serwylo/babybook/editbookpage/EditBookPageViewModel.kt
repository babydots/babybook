package com.serwylo.babybook.editbookpage

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.mediawiki.downloadImages
import com.serwylo.babybook.mediawiki.loadWikiPage
import kotlinx.coroutines.launch
import java.io.File

class EditBookPageViewModel(private val application: Application, val bookId: Long, private val existingBookPage: BookPage? = null): ViewModel() {

    val pageTitle = MutableLiveData(existingBookPage?.title ?: "")
    val pageText = MutableLiveData(existingBookPage?.text ?: "")
    val mainImage = MutableLiveData<String?>(existingBookPage?.imagePath)
    val allImages = MutableLiveData(listOf<File>())

    val isSearchingPages = MutableLiveData(false)
    val isLoadingPage = MutableLiveData(false)

    fun preparePage(title: String) {
        viewModelScope.launch {
            isLoadingPage.value = true

            val details = loadWikiPage(title, application.cacheDir)
            val images = downloadImages(details.getImageNamesOfInterest(), application.cacheDir)

            pageText.value = details.parseParagraphs().firstOrNull() ?: ""
            allImages.value = images

            images.firstOrNull()?.also { image ->
                mainImage.value = "file://${image.absolutePath}"
            }

            isLoadingPage.value = false
        }
    }

    fun clearPage() {
        isLoadingPage.value = false
        isSearchingPages.value = false
        pageText.value = ""
    }

    fun save(callback: () -> Unit) {
        val dao = AppDatabase.getInstance(application).bookDao()

        AppDatabase.executor.execute {

            if (existingBookPage != null) {
                val page = BookPage(
                    pageNumber = existingBookPage.pageNumber,
                    title = pageTitle.value ?: "",
                    text = pageText.value,
                    bookId = bookId,
                    imagePath = mainImage.value
                ).apply { id = existingBookPage.id }

                dao.update(page)
            } else {
                val page = BookPage(
                    pageNumber = dao.countPages(bookId) + 1,
                    title = pageTitle.value ?: "",
                    text = pageText.value,
                    bookId = bookId,
                    imagePath = mainImage.value
                )

                dao.insert(page)
            }

            callback()

        }
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