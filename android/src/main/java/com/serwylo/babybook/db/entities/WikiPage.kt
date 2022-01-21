package com.serwylo.babybook.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class WikiPage(
    @PrimaryKey val id: Int,
    val title: String,
    val imagePath: String,
    val text: String,
)
