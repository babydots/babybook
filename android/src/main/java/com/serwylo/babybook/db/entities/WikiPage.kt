package com.serwylo.babybook.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = WikiSite::class,
            parentColumns = ["id"],
            childColumns = ["wikiSiteId"],
        )
    ]
)
data class WikiPage(
    val title: String,
    val text: String,

    /**
     * Ideally, the system will pick a good image for each page without the
     * user needing to browse all available images from the article.
     * In order to reduce the downloads required when adding a new page, we
     * default to only selecting the first image.
     *
     * If the user chooses to browse images in order to select a different image,
     * then at that point we will go and fetch all the rest of the images.
     */
    val imagesFetched: Boolean = false,

    val wikiSiteId: Long,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)
