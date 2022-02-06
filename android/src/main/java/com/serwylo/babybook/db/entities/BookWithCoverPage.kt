package com.serwylo.babybook.db.entities

import androidx.room.Embedded

data class BookWithCoverPage(
    @Embedded
    val book: Book,

    var coverPageImagePath: String? = null,
)