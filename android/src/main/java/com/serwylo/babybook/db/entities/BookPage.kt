package com.serwylo.babybook.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.serwylo.babybook.mediawiki.processTitle

@Entity(
    foreignKeys = [ForeignKey(
        entity = Book::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BookPage(
    val bookId: Long,

    val pageNumber: Int,

    /**
     * The title used to
     */
    val wikiPageTitle: String? = null,

    /**
     * Generally the same as wikiPageTitle.
     *
     * Allow overriding the page title to something more palatable.
     * For example, we will always strip off trailing parentheses from a wikipedia title,
     * as they are used to disambiguate titles - not useful for these pages.
     */
    val pageTitle: String? = null,

    val imagePath: String? = null,

    val wikiPageText: String? = null,

    val pageText: String? = null,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    fun title() = pageTitle ?: processTitle(wikiPageTitle ?: "")

    fun text() = pageText ?: wikiPageText

}
