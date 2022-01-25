package com.serwylo.babybook.booklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.daos.BookDao
import com.serwylo.babybook.db.entities.Book


class BookListViewModel(application: Application) : AndroidViewModel(application) {

    private val bookDao: BookDao = AppDatabase.getInstance(application).bookDao()

    val isInEditMode = MutableLiveData(false)

    val allBooks: LiveData<List<Book>>
        get() = bookDao.findAll()

    fun toggleEditMode() {
        isInEditMode.value = !(isInEditMode.value ?: false)
    }

}
