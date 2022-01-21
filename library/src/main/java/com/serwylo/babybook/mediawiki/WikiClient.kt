package com.serwylo.babybook.mediawiki

import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import com.serwylo.babybook.book.BookConfig
import com.serwylo.babybook.book.Page
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.*

suspend fun makePages(
    pageTitles: List<String>,
    config: BookConfig,
    cacheDir: File,
): List<Page> = coroutineScope {
    val pageJobs = pageTitles.map { pageTitle ->
        async(Dispatchers.IO) { makeBookPageFromWikiPage(pageTitle, config, cacheDir) }
    }

    return@coroutineScope pageJobs.awaitAll()
}

suspend fun makeBookPageFromWikiPage(title: String, config: BookConfig, cacheDir: File): Page {
    val pageCacheDir = File(cacheDir, title)
    if (!pageCacheDir.exists()) {
        pageCacheDir.mkdirs()
    }

    val page = loadWikiPage(title, pageCacheDir)
    val images = downloadImages(page.getImageNamesOfInterest(), pageCacheDir)

    val text = when (config.summary) {
        BookConfig.Summary.Full -> page.parseParagraphs()[0]
        BookConfig.Summary.Short -> chooseSentences(page)[0]
    }

    return Page(
        title = processTitle(title),
        image = images[0],
        text,
    )
}

fun processTitle(title: String): String {
    return title.replace(Regex(" \\(.*\\)"), "")
}

val http = HttpClient(CIO) {
    install(JsonFeature)  {
        serializer = GsonSerializer() {
            setLenient()
        }
    }
}

fun chooseSentences(page: WikiPage): List<String> {
    val paragraphs = page.parseParagraphs()
    val sentences = extractSentencesFromParagraphs(paragraphs)
    return pickSentences(sentences, 5)
}

/**
 * Helper function to remove uninteresting images after we've updated our list of images we don't care about.
 * After loading a few different pages, we will probably have an increasingly better knowledge of what images
 * tend to crop up on many pages. When that happens, add them to the [knownUninterestingImages] and then run
 * this function to clear the cache directory of said images.
 */
fun deleteUninterestingCachedImages(cacheDir: File) {
    knownUninterestingImages.onEach {
        val file = File(cacheDir, it)
        if (file.exists()) {
            file.delete()
        }
    }
}

/**
 * List of icons, WikeMedia logos, and other uninteresting images that we have seen in our
 * experience which we don't care for.
 */
private val knownUninterestingImages: Set<String> = setOf(
    "Dagger-14-plain.png",
    "Status_iucn3.1.*.svg",
    "Red_Pencil_Icon.png",
    "Increase.*.svg",
    "Decrease.*.svg",
    "OOjs_UI_icon_edit-ltr-progressive.svg",
    "Semi-protection-shackle.svg",
    "[Ww]iki.*-[Ll]ogo.*.svg",
    "Wiktionary-logo.*",
    "Commons-logo.svg",
    "Crystal_Clear_action_run.png",
)

private val allowedImageExtensions = setOf(
    "jpg", "jpeg", "png",
    // Many good .svg files exist, but unfortunately they are not well supported by the PDF generation part of this app yet.
)

/**
 * Right now just crudely pick the first [numSentences] sentences. In the future, this may take into account a few
 * more heuristics, such as length, type of words found in each, etc.
 */
fun pickSentences(sentences: List<String>, numSentences: Int = 1): List<String> {
    return if (sentences.isEmpty()) {
        listOf("")
    } else if (sentences.size == 1) {
        sentences
    } else {
        // The first sentence is often a bit boring and technical/dry.
        // The second sentence tends to be much more fun.
        listOf(sentences[0])
    }
}

suspend fun searchWikiTitles(searchTerms: String): WikiSearchResults {
    val url = "https://simple.wikipedia.org/w/api.php?action=query&list=search&srsearch=$searchTerms&utf8=&format=json"
    println("Searching wiki pages from $url")

    val response: ParsedWikiSearchResults
    withContext(Dispatchers.IO) {
        response = http.get(url)
    }

    return WikiSearchResults(response)
}

class WikiSearchResults(
    data: ParsedWikiSearchResults,
) {

    val results = data.query.search.map {
        SearchResult(
            it.title,
            it.snippet,
            it.pageid,
        )
    }

    data class SearchResult(
        val title: String,
        val snippet: String,
        val pageid: Int,
    ) {
        override fun toString() = title
    }
}

data class ParsedWikiSearchResults(
    val batchcomplete: String,
    val `continue`: Continue? = null,
    val query: Query,
) {

    data class Query(
        val searchinfo: SearchInfo,
        val search: List<SearchResult>,
    )

    data class SearchResult(
        val ns: Int,
        val title: String,
        val pageid: Int,
        val size: Int,
        val wordcount: Int,
        val snippet: String,
        val timestamp: String,
    )

    data class SearchInfo(
        val totalhits: Int,
    )

    data class Continue(
        val sroffset: Int,
        val `continue`: String,
    )
}

/**
 * Fetch metadata about a wikipedia page.
 * Will cache the response, and if a cached response already exists on disk, will use that.
 */
suspend fun loadWikiPage(title: String, cacheDir: File): WikiPage {
    val cachedFile = File(cacheDir, "${title}.json")
    if (cachedFile.exists()) {
        try {
            println("Cached wiki data exists at ${cachedFile.absolutePath}")
            val wikiData: ParsedWikiPage = Gson().fromJson(cachedFile.readText(), ParsedWikiPage::class.java)
            return WikiPage(wikiData)
        } catch (e: Exception) {
            println("Error parsing cached wiki data, will remove it and then load from wikipedia again: ${e.message}")
            cachedFile.delete()
        }
    }

    val url = "https://simple.wikipedia.org/w/api.php?action=parse&page=$title&format=json"
    println("Loading wiki data from $url")

    var response: ParsedWikiPage
    var responseWikiPage: WikiPage
    withContext(Dispatchers.IO) {
        response = http.get(url)
        responseWikiPage = WikiPage(response)

        val redirect = responseWikiPage.getJsoup().select(".redirectText a")
        if (redirect.size > 0) {

            val redirectedTitle = redirect.text()
            val redirectedUrl = "https://simple.wikipedia.org/w/api.php?action=parse&page=$redirectedTitle&format=json"
            println("Redirected to \"$redirectedTitle\". Loading wiki data from $redirectedUrl")

            response = http.get(redirectedUrl)
            responseWikiPage = WikiPage(response)
        }
    }

    println("Caching wiki data to ${cachedFile.absolutePath}")
    cachedFile.writeText(Gson().toJson(response))

    return responseWikiPage
}

suspend fun downloadImages(imageNames: List<String>, destDir: File): List<File> = coroutineScope {
    println("Downloading ${imageNames.size} images (but really just downloading the first for now).")

    val jobs = imageNames.subList(0, 1).map { filename ->
        async(Dispatchers.IO) { downloadWikiImage(filename, destDir) }
    }

    return@coroutineScope jobs.awaitAll()
}

suspend fun downloadWikiImage(filename: String, destDir: File): File {
    val outputFile = File(destDir, filename)

    if (outputFile.exists()) {
        println("No need to download $filename, already downloaded.")
        return outputFile
    }

    val url = "https://simple.wikipedia.org/wiki/Special:FilePath/$filename"

    println("Downloading $url")

    val response: HttpResponse = http.get(url)
    response.content.copyTo(outputFile.outputStream())

    return outputFile
}

fun extractSentencesFromParagraphs(paragraphs: List<String>): List<String> {
    val sentences = paragraphs.map { paragraph ->
        // TODO: This is very crude. It fails for things like "P. T. tigris" on the "Tiger" article.
        paragraph.split(". ").map { sentence -> sentence.trim() }
    }.flatten()

    return sentences
}

class WikiPage(
    val page: ParsedWikiPage
) {

    private var jsoup: Document? = null

    fun getJsoup(): Document {
        if (null == jsoup) {
            jsoup = Jsoup.parse(getHtml())
        }

        return jsoup!!
    }

    fun getHtml() = page.parse.text.value

    fun getAllImageNames(): List<String> {

        val imageNamesFromJson = page.parse.images

        val imageUrlsFromHtml = getJsoup()
            .select("img")
            .mapNotNull { it.attr("src") }
            .map { URLDecoder.decode(it, Charset.defaultCharset().name()) }

        val sortedImages = imageNamesFromJson.sortedBy { imageName ->
            imageUrlsFromHtml.indexOfFirst { url -> url.contains(imageName) }
        }

        return (getInfoBoxImages() + sortedImages).distinct()

    }

    fun getInfoBoxImages(): List<String> {
        return Jsoup.parse(getHtml())
            .select(".infobox-image img")
            .map { it.attr("src") }
            .mapNotNull { src -> page.parse.images.find { src.endsWith(it) } }
    }

    /**
     * Some images are just wikimedia related logos, others are icons, and some are sound files.
     * They are excluded from the list of images.
     */
    fun getImageNamesOfInterest() = getAllImageNames().filter { imageName ->
        knownUninterestingImages.none { Regex(it).matches(imageName) }
                && File(imageName).extension.lowercase(Locale.getDefault()) in allowedImageExtensions
    }

    /**
     * Look for <p> tags in the article (excluding those in the infobox), strip HTML tags from them, and just return
     * the text content for each.
     */
    fun parseParagraphs(): List<String> {
        val html = getHtml()

        val doc = Jsoup.parse(html);
        val infobox = doc.select("table.infobox")
        val body = doc.select(".mw-parser-output")

        return body.select("p")
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .filterNot { p -> infobox.select("p").map { it.text() }.contains(p) } // There is sometimes <p> tags in the info box, we don't want those.
            .map { it.replace(Regex("\\[.*?\\]"), "") }
            .map { it.replace(Regex(" \\(.*?\\)"), "") }
    }

}

data class ParsedWikiPage(
    val parse: Parse,
) {

    data class Parse(
        val title: String,
        val pageid: Long,
        val revid: Long,
        val displaytitle: String,
        val text: Text,
        val langlinks: List<LangLink>,
        val categories: List<Category>,
        val links: List<Link>,
        val iwlinks: List<IWLink>,
        val templates: List<Template>,
        val externallinks: List<String>,
        val sections: List<Section>,
        val images: List<String>,
        val properties: List<Property>,
    )

    data class Section(
        val toclevel: Int,
        val level: String,
        val line: String,
        val number: String,
        val index: String,
        val fromtitle: String,
        val byteoffset: Long,
        val anchor: String,
    )

    data class Property(
        val name: String,

        @SerializedName("*")
        val value: String,
    )

    data class Template(
        val ns: Long,

        @SerializedName("*")
        val value: String,

        val exists: String = "",
    )

    data class Link(
        val ns: Long,

        @SerializedName("*")
        val value: String,

        val exists: String = "",
    )

    data class Category(
        val sortkey: String,

        @SerializedName("*")
        val value: String,

        val hidden: String = "",
    )

    data class IWLink(
        val prefix: String,
        val url: String,

        @SerializedName("*")
        val value: String,
    )

    data class LangLink(
        val lang: String,
        val url: String,
        val langname: String,
        val autonym: String,

        @SerializedName("*")
        val value: String,
    )

    data class Text(
        @SerializedName("*")
        val value: String,
    )
}