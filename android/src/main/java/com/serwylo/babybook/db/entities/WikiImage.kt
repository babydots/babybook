package com.serwylo.babybook.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [ForeignKey(
        entity = WikiPage::class,
        parentColumns = ["id"],
        childColumns = ["wikiPageId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class WikiImage(
    val filename: String,

    val wikiPageId: Long,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)