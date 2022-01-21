package com.serwylo.babybook.db.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity
data class Book(
   val title: String,
) {

   @PrimaryKey(autoGenerate = true)
   var id: Long = 0

}