package com.serwylo.babybook.editbook

import androidx.lifecycle.*
import com.serwylo.babybook.db.entities.Book
import com.serwylo.babybook.db.entities.PageEditingData
import com.serwylo.babybook.db.repositories.BookRepository
import com.serwylo.babybook.utils.debounce
import kotlinx.coroutines.launch

class EditBookViewModel(
    private val repository: BookRepository,
    initialBookId: Long,
) : ViewModel() {

    private val _bookTitle: MutableLiveData<String> = MutableLiveData("")
    val bookTitle: LiveData<String> = _bookTitle

    lateinit var pages: LiveData<List<PageEditingData>>

    private val _isLoading: MutableLiveData<Boolean> = MutableLiveData(true)
    val isLoading: LiveData<Boolean> = _isLoading

    private lateinit var book: Book

    fun getBookId(): Long = if (this::book.isInitialized) book.id else 0L

    init {
        viewModelScope.launch {
            book = if (initialBookId > 0L) {
                repository.getBook(initialBookId)
            } else {
                repository.addNewBook()
            }

            pages = repository.getFullPages(book.id)
            _bookTitle.value = book.title
            _isLoading.value = false
        }
    }

    fun updateTitle(title: String) {
        _bookTitle.value = title
        debouncedSaveTitle(title)
    }

    suspend fun deleteBook() {
        repository.removeBook(book)
    }

    private val debouncedSaveTitle = debounce<String>(300, viewModelScope) { title ->
        viewModelScope.launch {
            repository.updateTitle(book, title)
        }
    }

}

class EditBookViewModelFactory(private val repository: BookRepository, private val bookId: Long) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditBookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditBookViewModel(repository, bookId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
