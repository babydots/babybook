package com.serwylo.babybook.db.daos

import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.serwylo.babybook.db.entities.Book
import com.serwylo.babybook.db.entities.BookPage

@Dao
interface BookDao {

    @Query("SELECT * FROM Book")
    fun findAll(): LiveData<List<Book>>

    @Query("SELECT * FROM BookPage WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getBookPages(bookId: Long): LiveData<List<BookPage>>

    @Query("SELECT imagePath FROM BookPage WHERE bookId = :bookId AND imagePath IS NOT NULL ORDER BY pageNumber ASC LIMIT 0, 1")
    fun getBookCoverImage(bookId: Long): String?

    @Insert
    fun insert(book: Book): Long

    @Insert
    fun insert(page: BookPage): Long

    @Query("SELECT * FROM Book WHERE id = :id")
    fun getBook(id: Long): Book

    @Query("SELECT * FROM Book WHERE id = :id")
    fun getBookLive(id: Long): LiveData<Book>

    @Query("SELECT * FROM BookPage WHERE id = :id")
    fun getBookPage(id: Long): BookPage

    @Query("SELECT COUNT(*) FROM BookPage WHERE bookId = :bookId")
    fun countPages(bookId: Long): Int

    @Update
    fun update(book: Book)

    @Update
    fun update(bookPage: BookPage)

}