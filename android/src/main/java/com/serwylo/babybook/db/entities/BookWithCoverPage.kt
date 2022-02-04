package com.serwylo.babybook.db.entities

import androidx.room.Embedded

class BookWithCoverPage {
    @Embedded lateinit var book: Book

    var coverPageImagePath: String? = null
}