package com.serwylo.babybook.db.daos

import androidx.lifecycle.LiveData
import androidx.room.*
import com.serwylo.babybook.db.entities.*

@Dao
interface BookDao {

    @Transaction
    @Query("SELECT BookPage.*, WikiPage.id as wikiPage_id, WikiPage.text as wikiPage_text, WikiPage.title as wikiPage_title, WikiPage.imagesFetched as wikiPage_imagesFetched, WikiImage.id as wikiImage_id, WikiImage.filename as wikiImage_filename, WikiImage.wikiPageId as wikiImage_wikiPageId  FROM BookPage LEFT JOIN WikiPage ON (WikiPage.id = BookPage.wikiPageId) LEFT JOIN WikiImage ON (WikiImage.id = BookPage.wikiImageId) WHERE BookPage.bookId = :bookId ORDER BY BookPage.pageNumber ASC")
    fun getPageEditingData(bookId: Long): LiveData<List<PageEditingData>>

    @Query("SELECT Book.*, WikiImage.filename as coverPageImagePath FROM Book LEFT JOIN BookPage ON BookPage.id = (SELECT bp.id FROM BookPage as bp WHERE bp.wikiImageId IS NOT NULL AND bp.bookId = Book.id ORDER BY bp.pageNumber ASC LIMIT 0, 1) LEFT JOIN WikiImage ON (WikiImage.id = BookPage.wikiImageId)")
    fun findAllWithCoverPage(): LiveData<List<BookWithCoverPage>>

    @Query("SELECT * FROM Book")
    fun findAll(): LiveData<List<Book>>

    @Query("SELECT * FROM BookPage WHERE bookId = :bookId ORDER BY pageNumber ASC")
    fun getBookPages(bookId: Long): LiveData<List<BookPage>>

    @Query("SELECT '' as imagePath FROM BookPage WHERE bookId = :bookId ORDER BY pageNumber ASC LIMIT 0, 1")
    suspend fun getBookCoverImage(bookId: Long): String?

    @Insert
    suspend fun insert(book: Book): Long

    @Insert
    suspend fun insert(page: BookPage): Long

    @Insert
    suspend fun insert(page: WikiPage): Long

    @Insert
    suspend fun insert(image: WikiImage): Long

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

    @Update
    suspend fun update(wikiPage: WikiPage)

    @Delete(entity = Book::class)
    suspend fun delete(book: Book)

    @Delete(entity = BookPage::class)
    suspend fun delete(bookPage: BookPage)

    @Query("UPDATE BookPage SET pageNumber = :pageNumber WHERE pageNumber = :pageId")
    suspend fun setPageNumber(pageId: Long, pageNumber: Int)

    @Query("SELECT * FROM BookPage WHERE pageNumber = :pageNumber")
    suspend fun getPageNumber(pageNumber: Int): BookPage?

    @Query("SELECT * FROM WikiPage WHERE title = :title")
    fun findWikiPageByTitle(title: String): WikiPage

    @Query("SELECT * FROM WikiImage WHERE filename = :filename")
    fun findWikiImageByName(filename: String): WikiImage

    @Query("SELECT * FROM WikiPage WHERE id = :wikiPageId")
    suspend fun getWikiPage(wikiPageId: Long): WikiPage

    @Query("SELECT * FROM WikiImage WHERE id = :wikiImageId")
    suspend fun getWikiImage(wikiImageId: Long): WikiImage

    @Query("SELECT * FROM WikiImage WHERE wikiPageId = :wikiPageId")
    suspend fun getWikiImages(wikiPageId: Long): List<WikiImage>

}