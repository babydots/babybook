package com.serwylo.babybook.editbook

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.serwylo.babybook.db.entities.BookPage

class EditBookViewModel(
    private val application: Application,
    bookId: Long,
    title: String,
    val pages: LiveData<List<BookPage>>,
) : ViewModel() {

    var bookTitle: MutableLiveData<String> = MutableLiveData(title)

}

class EditBookViewModelFactory(private val application: Application, private val bookId: Long, private val title: String, private val pages: LiveData<List<BookPage>>) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditBookViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditBookViewModel(application, bookId, title, pages) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
