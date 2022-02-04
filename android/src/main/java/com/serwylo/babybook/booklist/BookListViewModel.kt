package com.serwylo.babybook.booklist

import androidx.lifecycle.*
import com.serwylo.babybook.db.entities.BookWithCoverPage
import com.serwylo.babybook.db.repositories.BookRepository


class BookListViewModel(repository: BookRepository) : ViewModel() {

    val isInEditMode = MutableLiveData(false)

    val allBooks: LiveData<List<BookWithCoverPage>> = repository.getAllBooksWithCoverPage()

    fun toggleEditMode() {
        isInEditMode.value = !(isInEditMode.value ?: false)
    }

}

class BookListViewModelFactory(private val repository: BookRepository) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(BookListViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return BookListViewModel(repository) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}