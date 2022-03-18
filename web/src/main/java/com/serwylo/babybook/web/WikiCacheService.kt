package com.serwylo.babybook.web

import com.serwylo.babybook.book.BookConfig
import com.serwylo.babybook.book.Page
import com.serwylo.babybook.mediawiki.*
import java.io.File
import java.io.IOException
import java.net.URL
import java.nio.file.Files

val wikiUrl = URL("https://en.wikipedia.org")

object WikiCacheService {

    private val cachedSearchResults = mutableMapOf<String, WikiSearchResults>()
    private val cacheDir = File(System.getProperty("java.io.tmpdir"), "wiki")

    init {
        if (!cacheDir.exists()) {
            Files.createDirectories(cacheDir.toPath())
        }
    }

    suspend fun getSearchResults(queryTerms: String): WikiSearchResults {
        val cached = cachedSearchResults[queryTerms]
        if (cached != null) {
            return cached
        }

        val result = searchWikiTitles(wikiUrl, queryTerms)
        cachedSearchResults[queryTerms] = result
        return result
    }

    suspend fun getBookPage(title: String): Page {
        return makeBookPageFromWikiPage(wikiUrl, title, BookConfig.Default, cacheDir)
    }

    suspend fun getWikiImage(title: String, imageName: String): File {
        val dir = File(cacheDir, title)
        val file = File(dir, imageName)

        if (!file.exists()) {
            downloadWikiImage(wikiUrl, imageName, dir)
        }

        if (!file.exists()) {
            throw IOException("Could not download image $imageName for wiki page \"$title\".")
        }

        return file
    }

}
