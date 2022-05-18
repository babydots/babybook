@file:OptIn(ExperimentalCli::class)

package com.serwylo.babybook.cli

import com.serwylo.babybook.mediawiki.*
import kotlinx.cli.*
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URL
import kotlin.io.path.createTempDirectory

val wikiUrl = URL("https://simple.wikipedia.org")

fun main(args: Array<String>) {

    val parser = ArgParser("babybook")

    parser.subcommands(Wiki())
    parser.subcommands(Recommend())

    parser.parse(args)
}

class Recommend: Subcommand("recommend", "Recommend wiki pages based on input.") {

    private val pageTitles by argument(ArgType.String, "titles", "List of page titles on which to base recommendation").vararg()
    private val outputDir by option(ArgType.String, "output-dir", "o", "Directory to cache wiki data. Defaults to to a temporary directory.")

    override fun execute() {
        val downloadDir = downloadDir(outputDir)

        runBlocking {

            val categoryCounts = mutableMapOf<String, Int>()

            pageTitles.onEach { pageTitle ->
                val pageCacheDir = File(downloadDir, pageTitle)

                if (!pageCacheDir.exists()) {
                    pageCacheDir.mkdirs()
                }

                println("Ascertaining categories for page \"$pageTitle\" to ${pageCacheDir.absolutePath}")
                val categoryList = loadCategoriesForPage(wikiUrl, pageTitle, pageCacheDir)

                println("Categories for $pageTitle:\n - ${categoryList.joinToString("\n - ")}")

                categoryList.onEach { category ->
                    val count = categoryCounts[category] ?: 0
                    categoryCounts[category] = count + 1
                }
            }

            val groupedCategories = groupMapByCount(categoryCounts)
            println(groupedCategories)

            val highestCount = groupedCategories.keys.maxOrNull() ?: return@runBlocking
            val bestCategories = groupedCategories[highestCount] ?: return@runBlocking

            val bestCount = mutableMapOf<String, Int>()
            bestCategories.onEach { category ->

                val pages = loadPagesInCategory(wikiUrl, category, downloadDir).filterNot { title ->
                    pageTitles.contains(title)
                }

                pages.onEach { page ->
                    val count = bestCount[page] ?: 0
                    bestCount[page] = count + 1
                }
                println("Pages in $category: $pages")

            }

            println(bestCount)

            val groupedCandidates = groupMapByCount(bestCount)
            println(groupedCandidates)
        }
    }
}

private fun groupMapByCount(map: Map<String, Int>): Map<Int, List<String>> =
    map.values
        .sortedDescending()
        .associateWith { count ->
            map.entries
                .filter { (_, value) -> value == count }
                .map { it.key }
        }

private fun downloadDir(preferredDir: String?) = if (preferredDir.isNullOrEmpty()) {
    createTempDirectory("babybook.tmp.").toFile()
} else {
    val dir = File(preferredDir)
    if (!dir.exists()) {
        if (!dir.mkdirs()) {
            error("Could not create directory at $dir. Do you have appropriate permissions?")
        }
    }
    dir
}

class Wiki: Subcommand("wiki", "Fetch data from wikipedia") {

    private val pageTitle by option(ArgType.String, "title", "t", "The title of the wiki page to download from.").required()
    private val outputDir by option(ArgType.String, "output-dir", "o", "Directory to output wiki data/images to. Defaults to to a temporary directory.")
    private val singleImage by option(ArgType.Boolean, "single-image", "i", "Only download the first interesting image from the page.")

    override fun execute() {
        val downloadDir = downloadDir(outputDir)
        val pageCacheDir = File(downloadDir, pageTitle)

        if (!pageCacheDir.exists()) {
            pageCacheDir.mkdirs()
        }

        println("Downloading wiki page \"$pageTitle\" to ${pageCacheDir.absolutePath}")
        runBlocking {
            val wikiPage = loadWikiPage(wikiUrl, pageTitle, pageCacheDir)

            if (singleImage == true) {
                wikiPage.getImageNamesOfInterest().firstOrNull()?.also { imageName ->
                    downloadWikiImage(wikiUrl, imageName, pageCacheDir)
                }
            } else {
                downloadImages(wikiUrl, wikiPage.getImageNamesOfInterest(), pageCacheDir).forEach { file ->
                    println("Image: ${file.file.name}")
                    println("  ${file.title}")
                    println("  ${file.description}")
                    println("  by ${file.author}")
                    println("  ${file.license}")
                }
            }
        }
    }
}
