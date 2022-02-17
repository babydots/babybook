package com.serwylo.babybook.attribution

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.serwylo.babybook.db.repositories.BookRepository

class AttributionViewModel(
    repository: BookRepository,
    bookId: Long
) : ViewModel() {

    val book = repository.getBookLive(bookId)
    val pages = repository.getFullPages(bookId)

}

class AttributionViewModelFactory(private val repository: BookRepository, private val bookId: Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AttributionViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return AttributionViewModel(repository, bookId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
