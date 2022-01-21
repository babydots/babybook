package com.serwylo.babybook.editbook

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.mediawiki.loadWikiPage
import kotlinx.coroutines.launch

class EditBookPageViewModel(private val application: Application, val bookId: Long, private val existingBookPage: BookPage? = null): ViewModel() {

    val pageTitle = MutableLiveData(existingBookPage?.title ?: "")
    val pageText = MutableLiveData(existingBookPage?.text ?: "")

    val isSearchingPages = MutableLiveData(false)
    val isLoadingPage = MutableLiveData(false)

    fun preparePage(title: String) {
        viewModelScope.launch {
            isLoadingPage.value = true

            val details = loadWikiPage(title, application.cacheDir)
            pageText.value = details.parseParagraphs()[0]

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
                    imagePath = existingBookPage.imagePath,
                ).apply { id = existingBookPage.id }

                dao.update(page)
            } else {
                val page = BookPage(
                    pageNumber = dao.countPages(bookId) + 1,
                    title = pageTitle.value ?: "",
                    text = pageText.value,
                    bookId = bookId,
                    imagePath = null,
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