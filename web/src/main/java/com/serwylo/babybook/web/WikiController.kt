package com.serwylo.picturebook.web

import com.serwylo.book.mediawiki.WikiSearchResults
import com.serwylo.book.mediawiki.loadWikiPage
import com.serwylo.book.mediawiki.searchWikiTitles
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.*
import java.io.File

@CrossOrigin(origins = ["http://localhost:3000", "http://localhost:8080"])
@RestController
class WikiController {

    @Autowired
    lateinit var cacheService: WikiCacheService

    @ExperimentalSerializationApi
    @GetMapping("/wiki/search")
    @ResponseBody
    fun generateBook(@RequestParam("q") queryTerms: String): WikiSearchResults {
        return cacheService.get(queryTerms) {
            runBlocking {
                searchWikiTitles(queryTerms)
            }
        }
    }

    @ExperimentalSerializationApi
    @GetMapping("/wiki/cache")
    @ResponseBody
    fun cachePage(@RequestParam("title") title: String) {
        runBlocking {
            val cacheDir = File(System.getProperty("java.io.tmpdir"), "wiki")
            loadWikiPage(title, File(cacheDir, title))
        }
    }

}