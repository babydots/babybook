package com.serwylo.babybook.db.entities

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey

@Entity(
    foreignKeys = [
        ForeignKey(
            entity = WikiSite::class,
            parentColumns = [ "id" ],
            childColumns = [ "wikiSiteId" ],
        )
    ]
)
data class Settings(
    val wikiSiteId: Long,

    @PrimaryKey
    val id: Long = 0,
)