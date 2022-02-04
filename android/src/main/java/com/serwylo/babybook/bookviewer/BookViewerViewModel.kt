package com.serwylo.babybook.bookviewer

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.serwylo.babybook.db.repositories.BookRepository

class BookViewerViewModel(
    repository: BookRepository,
    bookId: Long
) : ViewModel() {

    val bookWithPages = repository.getBookWithPages(bookId)

    var currentPageIndex: MutableLiveData<Int> = MutableLiveData(0)

    fun currentPage() = bookWithPages.value?.pages?.get(currentPageIndex.value!!)

    fun hasNextPage() = currentPageIndex.value!! < (bookWithPages.value?.pages?.size ?: 0) - 1
    fun hasPreviousPage() = currentPageIndex.value!! > 0

    fun turnToNextPage() {
        currentPageIndex.value?.also { index ->
            bookWithPages.value?.pages?.size?.also { pagesSize ->
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
        return bookWithPages.value?.pages?.size ?: 0
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
