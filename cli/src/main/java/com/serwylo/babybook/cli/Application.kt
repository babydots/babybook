@file:OptIn(ExperimentalCli::class)

package com.serwylo.babybook.cli

import com.serwylo.babybook.mediawiki.downloadImages
import com.serwylo.babybook.mediawiki.downloadWikiImage
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

    private val pageTitle by option(ArgType.String, "title", "t", "The title of the wiki page to download from.").required()
    private val outputDir by option(ArgType.String, "output-dir", "o", "Directory to output wiki data/images to. Defaults to to a temporary directory.")
    private val singleImage by option(ArgType.Boolean, "single-image", "i", "Only download the first interesting image from the page.")

    override fun execute() {
        val downloadDir = if (outputDir.isNullOrEmpty()) {
            createTempDirectory("babybook.tmp.").toFile()
        } else {
            val dir = File(outputDir!!)
            if (!dir.exists()) {
                if (!dir.mkdirs()) {
                    println("Could not create directory at $dir. Do you have appropriate permissions?")
                    return
                }
            }
            dir
        }

        val pageCacheDir = File(downloadDir, pageTitle)

        if (!pageCacheDir.exists()) {
            pageCacheDir.mkdirs()
        }

        println("Downloading wiki page \"$pageTitle\" to ${pageCacheDir.absolutePath}")
        runBlocking {
            val wikiPage = loadWikiPage(pageTitle, pageCacheDir)

            if (singleImage == true) {
                wikiPage.getImageNamesOfInterest().firstOrNull()?.also { imageName ->
                    downloadWikiImage(imageName, pageCacheDir)
                }
            } else {
                downloadImages(wikiPage.getImageNamesOfInterest(), pageCacheDir)
            }
        }
    }
}
