package com.serwylo.babybook.db.entities

/**
 * Enough information to display a book page.
 *
 * Not to be confused with [PageEditingData] that includes a lot more info - used for when a page
 * is being edited.
 */
data class PageViewingData(
    val text: String,
    val title: String,
    val imagePath: String?,
)