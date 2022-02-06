package com.serwylo.babybook.db.entities

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.serwylo.babybook.mediawiki.processTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

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
            childColumns = ["bookId"],
            onDelete = ForeignKey.SET_NULL
        )
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
) {


}
