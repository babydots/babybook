package com.serwylo.babybook.bookviewer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.serwylo.babybook.db.entities.WikiSite
import com.serwylo.babybook.db.repositories.BookRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BookViewerViewModel(
    private val repository: BookRepository,
    private val bookId: Long
) : ViewModel() {

    val book = repository.getBookLive(bookId)
    val pages = repository.getFullPages(bookId)

    var currentPageIndex: MutableLiveData<Int> = MutableLiveData(0)

    fun currentPage() = pages.value?.get(currentPageIndex.value!!)

    fun hasNextPage() = currentPageIndex.value!! < (pages.value?.size ?: 0) - 1
    fun hasPreviousPage() = currentPageIndex.value!! > 0

    fun turnToNextPage() {
        currentPageIndex.value?.also { index ->
            pages.value?.size?.also { pagesSize ->
                if (index < pagesSize - 1) {
                    currentPageIndex.value = index + 1
                }
            }
        }
    }

    fun turnToPreviousPage() {
        currentPageIndex.value?.also { index ->
            if (index > 0) {
                currentPageIndex.value = index - 1
            }
        }
    }

    fun pageCount(): Int {
        return pages.value?.size ?: 0
    }

    suspend fun getWikiSite(): WikiSite = withContext(Dispatchers.IO) {
        repository.getWikiSite(bookId)
    }

}

class BookViewerModelFactory(private val repository: BookRepository, private val bookId: Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewerViewModel(repository, bookId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
