package com.serwylo.babybook.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = Book::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BookPage(
    val pageNumber: Int,
    val title: String,
    val imagePath: String?,
    val text: String?,

    val bookId: Long,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0
}
