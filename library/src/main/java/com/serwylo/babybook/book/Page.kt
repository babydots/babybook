package com.serwylo.babybook.book

import java.io.File

data class Page(
    val title: String,
    val image: File,
    val text: String,
)