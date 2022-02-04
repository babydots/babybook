package com.serwylo.babybook.db.entities

import androidx.room.Embedded
import androidx.room.Relation

data class BookWithPages(
    @Embedded
    val book: Book,

    @Relation(
        parentColumn = "id",
        entityColumn = "bookId"
    )
    val pages: List<BookPage>
)