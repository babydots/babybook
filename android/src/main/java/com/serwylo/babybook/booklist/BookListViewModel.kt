package com.serwylo.babybook.booklist

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.daos.BookDao
import com.serwylo.babybook.db.entities.Book


class BookListViewModel(application: Application) : AndroidViewModel(application) {

    private val bookDao: BookDao = AppDatabase.getInstance(application).bookDao()

    val allBooks: LiveData<List<Book>>
        get() = bookDao.findAll()

    fun saveBook(book: Book) {
        AppDatabase.executor.execute { bookDao.insert(book) }
    }

}
