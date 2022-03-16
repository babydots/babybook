package com.serwylo.babybook.db.entities

import android.net.Uri
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.net.URL

@Entity
data class WikiSite(

    /**
     * e.g. "en", "de", "simple", etc.
     */
    val code: String,

    val title: String,

    val localisedTitle: String,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

) {

    fun url(): URL = URL("https", "$code.wikipedia.org", "")

}