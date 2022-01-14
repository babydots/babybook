package com.serwylo.picturebook.web

import com.serwylo.book.mediawiki.WikiSearchResults
import org.springframework.stereotype.Service

@Service
class WikiCacheService {

    private val cachedSearchResults = mutableMapOf<String, WikiSearchResults>()

    fun get(queryTerms: String, block: () -> WikiSearchResults): WikiSearchResults {
        val cached = cachedSearchResults[queryTerms]
        if (cached != null) {
            return cached
        }

        val result = block()
        cachedSearchResults[queryTerms] = result
        return result
    }

}