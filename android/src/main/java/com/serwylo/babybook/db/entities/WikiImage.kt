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
    /**
     * The name used by Wikimedia Commons to refer to this image.
     */
    val name: String?,

    /**
     * URI to the filename *on this system*.
     * Examples are:
     *   - file:///android_assets/blah.jpg - For build in default books packaged with the app.
     *   - file:///data/data/com.serwylo.babybook/files/blah.jpg - For pages added by the user.
     *
     * If null, then we will need to download this image in the future.
     */
    val filename: String?,

    /**
     * Most images will have a wiki page, except for those which are pre-packaged with the app.
     * Ideally, our pre-packaged books would include all images from a wiki page, but this will
     * bloat the app for something which may never be seen by most users. Therefore, we have a
     * mechanism where these images are not attached to to a [WikiPage], and the [WikiPage.imagesFetched]
     * is set to false. When we want to show all images to the user, then we will fetch all images
     * for the page the first time the user asks to see them (thus rendering this built-in image
     * essentially orphaned).
     */
    val wikiPageId: Long?,

    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
)