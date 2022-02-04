package com.serwylo.babybook.db.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.serwylo.babybook.db.entities.Book
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.db.entities.BookWithCoverPage
import com.serwylo.babybook.db.entities.BookWithPages

@Dao
interface BookDao {

    @Query("SELECT Book.*, BookPage.imagePath as coverPageImagePath FROM Book LEFT JOIN BookPage ON BookPage.id = (SELECT bp.id FROM BookPage as bp WHERE bp.imagePath IS NOT NULL AND bp.bookId = Book.id ORDER BY bp.pageNumber ASC LIMIT 0, 1)")
    fun findAllWithCoverPage(): LiveData<List<BookWithCoverPage>>

    @Transaction
    @Query("SELECT * FROM Book WHERE id = :bookId")
    fun getBookWithPages(bookId: Long): LiveData<BookWithPages>

    @Query("SELECT * FROM Book")
    fun findAll(): LiveData<List<Book>>

    @Query("SELECT * FROM BookPage WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getBookPages(bookId: Long): LiveData<List<BookPage>>

    @Query("SELECT imagePath FROM BookPage WHERE bookId = :bookId AND imagePath IS NOT NULL ORDER BY pageNumber ASC LIMIT 0, 1")
    suspend fun getBookCoverImage(bookId: Long): String?

    @Insert
    suspend fun insert(book: Book): Long

    @Insert
    suspend fun insert(page: BookPage): Long

    @Query("SELECT * FROM Book WHERE id = :id")
    suspend fun getBook(id: Long): Book

    @Query("SELECT * FROM Book WHERE id = :id")
    fun getBookLive(id: Long): LiveData<Book>

    @Query("SELECT * FROM BookPage WHERE id = :id")
    suspend fun getBookPage(id: Long): BookPage

    @Query("SELECT COUNT(*) FROM BookPage WHERE bookId = :bookId")
    suspend fun countPages(bookId: Long): Int

    @Update
    suspend fun update(book: Book)

    @Update
    suspend fun update(bookPage: BookPage)

    @Delete(entity = Book::class)
    suspend fun delete(book: Book)

    @Delete(entity = BookPage::class)
    suspend fun delete(bookPage: BookPage)

    @Query("UPDATE BookPage SET pageNumber = :pageNumber WHERE pageNumber = :pageId")
    suspend fun setPageNumber(pageId: Long, pageNumber: Int)

    @Query("SELECT * FROM BookPage WHERE pageNumber = :pageNumber")
    suspend fun getPageNumber(pageNumber: Int): BookPage?

}