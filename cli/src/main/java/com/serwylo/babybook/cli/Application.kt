@file:OptIn(ExperimentalCli::class)

package com.serwylo.babybook.cli

import com.serwylo.babybook.mediawiki.downloadImages
import com.serwylo.babybook.mediawiki.loadWikiPage
import kotlinx.cli.*
import kotlinx.coroutines.runBlocking
import java.io.File
import kotlin.io.path.createTempDirectory

fun main(args: Array<String>) {

    val parser = ArgParser("babybook")

    val wiki = Wiki()
    parser.subcommands(wiki)

    parser.parse(args)
}

class Wiki: Subcommand("wiki", "Fetch data from wikipedia") {

    private val cacheDir = createTempDirectory("babybook.tmp.")

    private val pageTitle by option(ArgType.String, "title", "t", "The title of the wiki page to download from").required()

    override fun execute() {
        val pageCacheDir = File(cacheDir.toFile(), pageTitle)

        if (!pageCacheDir.exists()) {
            pageCacheDir.mkdirs()
        }

        println("Downloading wiki page \"$pageTitle\" to ${pageCacheDir.absolutePath}")
        runBlocking {
            val wikiPage = loadWikiPage(pageTitle, pageCacheDir)

            downloadImages(wikiPage.getImageNamesOfInterest(), pageCacheDir)
        }
    }
}
