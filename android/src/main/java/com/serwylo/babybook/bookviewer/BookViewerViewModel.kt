package com.serwylo.babybook.bookviewer

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.serwylo.babybook.db.entities.Book
import com.serwylo.babybook.db.entities.BookPage

class BookViewerViewModel(
    val book: Book,
    val pages: LiveData<List<BookPage>>,
) : ViewModel() {

    var currentPage: MutableLiveData<BookPage?> = MutableLiveData(pages.value?.firstOrNull())

    private fun currentPageIndex() = pages.value?.indexOfFirst { it.id == currentPage.value?.id } ?: -1

    fun nextPage(): BookPage? {
        val index = currentPageIndex()
        val pages = pages.value ?: return null

        return if (index < pages.size - 1) {
            pages[index + 1]
        } else {
            null
        }
    }

    fun turnToNextPage() {
        val next = nextPage() ?: return
        currentPage.value = next
    }

    fun previousPage(): BookPage? {
        val index = currentPageIndex()
        val pages = pages.value ?: return null

        return if (index > 0 && pages.isNotEmpty()) {
            pages[index - 1]
        } else {
            null
        }
    }

    fun turnToPreviousPage() {
        val previous = previousPage() ?: return
        currentPage.value = previous
    }

}

class BookViewerModelFactory(private val book: Book, private val pages: LiveData<List<BookPage>>) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookViewerViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookViewerViewModel(book, pages) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
