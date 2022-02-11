package com.serwylo.babybook.db.repositories

import androidx.lifecycle.LiveData
import com.serwylo.babybook.db.daos.BookDao
import com.serwylo.babybook.db.entities.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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

    fun getBookLive(bookId: Long): LiveData<Book> = dao.getBookLive(bookId)

    fun getFullPages(bookId: Long): LiveData<List<PageEditingData>> = dao.getPageEditingData(bookId)

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

        dao.update(currentPage.copy(id = currentPage.id, pageNumber = currentPage.pageNumber + 1))
        dao.update(nextPage.copy(id = nextPage.id, pageNumber = nextPage.pageNumber - 1))

        currentPage.pageNumber + 1
    }

    suspend fun movePageDown(bookPageId: Long, bookId: Long): Int = withContext(Dispatchers.IO) {
        val currentPage = dao.getBookPage(bookPageId)
        if (currentPage.pageNumber <= 1) {
            return@withContext currentPage.pageNumber
        }

        val previousPage = dao.getPageNumber(currentPage.pageNumber - 1) ?: return@withContext currentPage.pageNumber

        dao.update(currentPage.copy(id = currentPage.id, pageNumber = currentPage.pageNumber - 1))
        dao.update(previousPage.copy(id = previousPage.id, pageNumber = previousPage.pageNumber + 1))

        currentPage.pageNumber - 1
    }

    suspend fun getNextPageNumber(bookId: Long): Int = withContext(Dispatchers.IO) {
        dao.countPages(bookId) + 1
    }

    fun findWikiPageByTitle(title: String): WikiPage? = dao.findWikiPageByTitle(title)

    fun findWikiImageByName(filename: String): WikiImage? = dao.findWikiImageByName(filename)

    suspend fun addNewWikiPage(title: String, text: String): WikiPage = withContext(Dispatchers.IO) {
        val page = WikiPage(title, text)
        val id = dao.insert(page)
        page.copy(id = id)
    }

    suspend fun addNewWikiImage(parentPage: WikiPage, filename: String, file: File): WikiImage = withContext(Dispatchers.IO) {
        val image = WikiImage(name = filename, filename = "file://${file.absolutePath}", wikiPageId = parentPage.id) // TODO: Use name properly
        val id = dao.insert(image)
        image.copy(id = id)
    }

    suspend fun getWikiPage(wikiPageId: Long): WikiPage {
        return dao.getWikiPage(wikiPageId)
    }

    suspend fun getWikiImage(wikiImageId: Long): WikiImage {
        return dao.getWikiImage(wikiImageId)
    }

    suspend fun getWikiImages(wikiPage: WikiPage): List<WikiImage> {
        return dao.getWikiImages(wikiPage.id)
    }

    suspend fun recordImagesAsDownloaded(wikiPage: WikiPage) {
        dao.update(wikiPage.copy(imagesFetched = true))
    }

}