package com.serwylo.babybook.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = Book::class,
            parentColumns = ["id"],
            childColumns = ["bookId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = WikiPage::class,
            parentColumns = ["id"],
            childColumns = ["wikiPageId"],
            onDelete = ForeignKey.SET_NULL
        ),
        ForeignKey(
            entity = WikiImage::class,
            parentColumns = ["id"],
            childColumns = ["wikiImageId"],
            onDelete = ForeignKey.SET_NULL
        ),
    ]
)
data class BookPage(
    val bookId: Long,

    val pageNumber: Int,

    val title: String? = null,
    val text: String? = null,

    val wikiImageId: Long? = null,

    val wikiPageId: Long? = null,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)
