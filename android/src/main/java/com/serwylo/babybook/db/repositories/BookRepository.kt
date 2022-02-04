package com.serwylo.babybook.db.repositories

import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.serwylo.babybook.db.daos.BookDao
import com.serwylo.babybook.db.entities.Book
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.db.entities.BookWithCoverPage
import com.serwylo.babybook.db.entities.BookWithPages
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class BookRepository(private val dao: BookDao) {

    fun getAllBooks(): LiveData<List<Book>> = dao.findAll()

    fun getAllBooksWithCoverPage(): LiveData<List<BookWithCoverPage>> = dao.findAllWithCoverPage()

    fun getBookWithPages(bookId: Long): LiveData<BookWithPages> = dao.getBookWithPages(bookId)

    fun getBookPages(book: Book): LiveData<List<BookPage>> = dao.getBookPages(book.id)

    suspend fun removeBook(book: Book) = withContext(Dispatchers.IO) {
        dao.delete(book)
    }

    suspend fun removeBookPage(bookPageId: Long) = withContext(Dispatchers.IO) {
        if (bookPageId > 0) {
            val page = dao.getBookPage(bookPageId)
            dao.delete(page)
        }
    }

    suspend fun updateTitle(book: Book, newTitle: String) = withContext(Dispatchers.IO) {
        val newBook = book.copy(title = if (newTitle.isNotEmpty()) newTitle else "New Book")
        dao.update(newBook)
    }

    suspend fun addNewBook(): Book = withContext(Dispatchers.IO) {
        val id = dao.insert(Book(title = "New Book"))
        dao.getBook(id)
    }

    suspend fun addNewBookPage(page: BookPage): Long = withContext(Dispatchers.IO) {
        dao.insert(page)
    }

    suspend fun updateBookPage(page: BookPage) = withContext(Dispatchers.IO) {
        dao.update(page)
    }

    suspend fun getBook(bookId: Long): Book = withContext(Dispatchers.IO) {
        dao.getBook(bookId)
    }

    suspend fun getBookPage(bookPageId: Long): BookPage = withContext(Dispatchers.IO) {
        dao.getBookPage(bookPageId)
    }

    suspend fun movePageUp(bookPageId: Long, bookId: Long): Int = withContext(Dispatchers.IO) {
        val currentPage = dao.getBookPage(bookPageId)
        val maxPage = dao.countPages(bookId)
        if (currentPage.pageNumber >= maxPage) {
            return@withContext currentPage.pageNumber
        }

        val nextPage = dao.getPageNumber(currentPage.pageNumber + 1) ?: return@withContext currentPage.pageNumber

        dao.update(currentPage.copy(pageNumber = currentPage.pageNumber + 1).apply { id = currentPage.id })
        dao.update(nextPage.copy(pageNumber = nextPage.pageNumber - 1).apply { id = nextPage.id })

        currentPage.pageNumber + 1
    }

    suspend fun movePageDown(bookPageId: Long, bookId: Long): Int = withContext(Dispatchers.IO) {
        val currentPage = dao.getBookPage(bookPageId)
        if (currentPage.pageNumber <= 1) {
            return@withContext currentPage.pageNumber
        }

        val previousPage = dao.getPageNumber(currentPage.pageNumber - 1) ?: return@withContext currentPage.pageNumber

        dao.update(currentPage.copy(pageNumber = currentPage.pageNumber - 1).apply { id = currentPage.id })
        dao.update(previousPage.copy(pageNumber = previousPage.pageNumber + 1).apply { id = previousPage.id })

        currentPage.pageNumber - 1
    }

    suspend fun getNextPageNumber(bookId: Long): Int = withContext(Dispatchers.IO) {
        dao.countPages(bookId) + 1
    }

}