package com.serwylo.babybook.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WikiPage(
    val title: String,
    val text: String,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)
