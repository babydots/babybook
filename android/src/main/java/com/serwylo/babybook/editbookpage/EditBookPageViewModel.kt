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
import com.serwylo.babybook.mediawiki.processTitle
import kotlinx.coroutines.launch
import java.io.File

class EditBookPageViewModel(private val application: Application, val bookId: Long, private val existingBookPage: BookPage? = null): ViewModel() {

    val wikiPageTitle = MutableLiveData(existingBookPage?.wikiPageTitle ?: "")
    val pageTitle = MutableLiveData(existingBookPage?.pageTitle)
    val wikiPageText = MutableLiveData(existingBookPage?.wikiPageText ?: "")
    val pageText = MutableLiveData(existingBookPage?.pageText)
    val mainImage = MutableLiveData<String?>(existingBookPage?.imagePath)
    val allImages = MutableLiveData(listOf<File>())

    val isSearchingPages = MutableLiveData(false)
    val isLoadingPage = MutableLiveData(false)

    fun title() = pageTitle.value ?: wikiPageTitle.value ?: ""
    fun text() = pageText.value ?: wikiPageText.value ?: ""

    fun preparePage(title: String) {
        viewModelScope.launch {
            isLoadingPage.value = true

            pageTitle.value = processTitle(title)

            val details = loadWikiPage(title, application.cacheDir)
            val images = downloadImages(details.getImageNamesOfInterest(), application.cacheDir)

            pageText.value = details.parseParagraphs().firstOrNull() ?: ""
            allImages.value = images

            images.firstOrNull()?.also { image ->
                mainImage.value = "file://${image.absolutePath}"
            }

            save()

            isLoadingPage.value = false
        }
    }

    fun clearPage() {
        isLoadingPage.value = false
        isSearchingPages.value = false
        pageText.value = ""
    }

    fun save() {
        val dao = AppDatabase.getInstance(application).bookDao()

        AppDatabase.executor.execute {

            if (existingBookPage != null) {
                val page = BookPage(
                    pageNumber = existingBookPage.pageNumber,
                    wikiPageTitle = wikiPageTitle.value ?: "",
                    wikiPageText = wikiPageText.value ?: "",
                    pageTitle = pageTitle.value,
                    pageText = pageText.value,
                    bookId = bookId,
                    imagePath = mainImage.value
                ).apply { id = existingBookPage.id }

                dao.update(page)
            } else {
                val page = BookPage(
                    pageNumber = dao.countPages(bookId) + 1,
                    wikiPageTitle = wikiPageTitle.value ?: "",
                    wikiPageText = wikiPageText.value ?: "",
                    pageTitle = pageTitle.value,
                    pageText = pageText.value,
                    bookId = bookId,
                    imagePath = mainImage.value
                )

                dao.insert(page)
            }

        }
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