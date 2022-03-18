package com.serwylo.babybook.mediawiki

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.google.gson.annotations.SerializedName
import com.google.gson.stream.JsonReader
import com.serwylo.babybook.book.BookConfig
import com.serwylo.babybook.book.Page
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.features.json.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.*
import org.apache.commons.codec.digest.DigestUtils
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.File
import java.io.StringReader
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.Charset
import java.util.*

/**
 * @param wikiUrl Base URL of the wikipedia installation to use (e.g. https://en.wikipedia.org)
 */
suspend fun makePages(
    wikiUrl: URL,
    pageTitles: List<String>,
    config: BookConfig,
    cacheDir: File,
): List<Page> = coroutineScope {
    val pageJobs = pageTitles.map { pageTitle ->
        async(Dispatchers.IO) { makeBookPageFromWikiPage(wikiUrl, pageTitle, config, cacheDir) }
    }

    return@coroutineScope pageJobs.awaitAll()
}

/**
 * @param wikiUrl Base URL of the wikipedia installation to use (e.g. https://en.wikipedia.org)
 */
suspend fun makeBookPageFromWikiPage(wikiUrl: URL, title: String, config: BookConfig, cacheDir: File): Page {
    val pageCacheDir = File(cacheDir, title)
    if (!pageCacheDir.exists()) {
        pageCacheDir.mkdirs()
    }

    val page = loadWikiPage(wikiUrl, title, pageCacheDir)
    val images = downloadImages(wikiUrl, page.getImageNamesOfInterest(), pageCacheDir)

    val text = when (config.summary) {
        BookConfig.Summary.Full -> page.parseParagraphs()[0]
        BookConfig.Summary.Short -> chooseSentences(page)[0]
    }

    return Page(
        title = processTitle(title),
        image = images[0].file,
        text,
    )
}

fun processTitle(title: String): String {
    return title.replace(Regex(" \\(.*\\)"), "")
}

val nonRedirectingHttp = HttpClient(CIO) {
    install(JsonFeature)  {
        serializer = GsonSerializer {
            setLenient()
        }
    }

    followRedirects = false
    expectSuccess = false
}

val http = HttpClient(CIO) {
    install(JsonFeature)  {
        serializer = GsonSerializer {
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
 * List of icons, WikiMedia logos, and other uninteresting images that we have seen in our
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
    return when(sentences.size) {
        0 -> listOf("")
        1 -> sentences
        else -> listOf(sentences[0])
    }
}

/**
 * @param wikiUrl Base URL of the wikipedia installation to use (e.g. https://en.wikipedia.org)
 */
suspend fun searchWikiTitles(wikiUrl: URL, searchTerms: String): WikiSearchResults {
    val url = "$wikiUrl/w/api.php?action=query&list=search&srsearch=$searchTerms&utf8=&format=json"
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
 *
 * @param wikiUrl Base URL of the wikipedia installation to use (e.g. https://en.wikipedia.org)
 */
suspend fun loadWikiPage(wikiUrl: URL, title: String, cacheDir: File): WikiPage = withContext(Dispatchers.IO) {
    val cachedFile = File(cacheDir, "${title}.json")
    if (cachedFile.exists()) {
        try {
            println("Cached wiki data exists at ${cachedFile.absolutePath}")
            val wikiData: ParsedWikiPage = Gson().fromJson(cachedFile.readText(), ParsedWikiPage::class.java)
            return@withContext WikiPage(wikiData)
        } catch (e: Exception) {
            println("Error parsing cached wiki data, will remove it and then load from wikipedia again: ${e.message}")
            cachedFile.delete()
        }
    }

    val url = "$wikiUrl/w/api.php?action=parse&page=$title&format=json"
    println("Loading wiki data from $url")

    var response: ParsedWikiPage
    var responseWikiPage: WikiPage
    withContext(Dispatchers.IO) {
        response = http.get(url)
        responseWikiPage = WikiPage(response)

        val redirect = responseWikiPage.getJsoup().select(".redirectText a")
        if (redirect.size > 0) {

            val redirectedTitle = redirect.text()
            val redirectedUrl = "$wikiUrl/w/api.php?action=parse&page=$redirectedTitle&format=json"
            println("Redirected to \"$redirectedTitle\". Loading wiki data from $redirectedUrl")

            response = http.get(redirectedUrl)
            responseWikiPage = WikiPage(response)
        }
    }

    println("Caching wiki data to ${cachedFile.absolutePath}")
    cachedFile.writeText(Gson().toJson(response))

    return@withContext responseWikiPage
}

/**
 * @param wikiUrl Base URL of the wikipedia installation to use (e.g. https://en.wikipedia.org)
 */
suspend fun downloadImages(wikiUrl: URL, imageNames: List<String>, destDir: File): List<WikipediaCommonsFile> = coroutineScope {
    println("Downloading ${imageNames.size} images (but really just downloading the first for now).")

    val jobs = imageNames.map { filename ->
        async(Dispatchers.IO) { downloadWikiImage(wikiUrl, filename, destDir) }
    }

    return@coroutineScope jobs.awaitAll().filterNotNull()
}

fun commonsUrlForImage(filename: String): String {
    val name = filename.replace(' ', '_')
    val md5 = DigestUtils.md5Hex(name)
    val width = 1024
    val suffix = "${width}px-$name"
    return "https://upload.wikimedia.org/wikipedia/commons/thumb/${md5.substring(0, 1)}/${md5.substring(0, 2)}/$name/$suffix"
}

private suspend fun downloadWikiImageMetadata(filename: String): WikipediaCommonsMetadata? {

    // e.g. https://commons.wikimedia.org/w/api.php?action=query&titles=Image:Gallina_de_Guinea_(Numida_meleagris),_parque_nacional_Kruger,_Sud%C3%A1frica,_2018-07-25,_DD_48.jpg&prop=imageinfo&iiprop=extmetadata
    val metadataUrl = "https://commons.wikimedia.org/w/api.php?action=query&titles=Image:${filename.replace(' ', '_')}&prop=imageinfo&iiprop=extmetadata&format=json"
    val metadataResponse: String = try {
        withContext(Dispatchers.IO) { http.get(metadataUrl) }
    } catch (e: Exception) {
        println("Couldn't fetch metadata for $filename from $metadataUrl")
        return null
    }

    val metadataJson = JsonParser.parseReader(
        JsonReader(StringReader(metadataResponse)).also { reader ->
            reader.isLenient = true
        }
    )

    val metadataJsonPages = metadataJson.asJsonObject.get("query")?.asJsonObject?.get("pages")?.asJsonObject ?: return null
    val pageId = metadataJsonPages.keySet().firstOrNull() ?: return null
    val metadata = metadataJsonPages[pageId]?.asJsonObject?.get("imageinfo")?.asJsonArray?.firstOrNull()?.asJsonObject?.get("extmetadata")?.asJsonObject ?: return null

    val getVal = { key: String ->
        metadata[key]?.asJsonObject?.get("value")?.asString
    }

    val clean = { value: String -> Jsoup.parse(value).text() }

    val extractAuthor = { value: String ->
        val html = Jsoup.parse(value)

        val userLink = html.selectFirst("a[href~=/wiki/User:]")
        if (userLink != null) {
            userLink.text()
        } else {
            html.text()
        }
    }

    return WikipediaCommonsMetadata(
        title = clean(getVal("ObjectName") ?: ""),
        description = clean(getVal("ImageDescription") ?: ""),
        license = clean(getVal("LicenseShortName") ?: ""),
        author = extractAuthor(getVal("Attribution") ?: getVal("Artist") ?: ""),
    )

}

/**
 * @param wikiUrl Base URL of the wikipedia installation to use (e.g. https://en.wikipedia.org)
 */
private suspend fun downloadWikiImageContents(wikiUrl: URL, filename: String, destDir: File, lookForRenamedImageOnFailure: Boolean = false): File? {
    val outputFile = File(destDir, filename)
    if (outputFile.exists()) {
        println("No need to download $filename, already downloaded.")
        return outputFile
    }

    val url = commonsUrlForImage(filename)

    println("Downloading $url")

    // Some images have been renamed on wikipedia but not commons. An example is the main image
    // from the "Horse" article, which on wikipedia refers to "Farmer_plowing.jpg". If you try
    // to download that image from commons using
    // https://upload.wikimedia.org/wikipedia/commons/thumb/a/a4/Farmer_plowing.jpg/1024px-Farmer_plowing.jpg
    // then it will fail.
    //
    // This is because the image has actually been renamed, as can be seen when you try to view the
    // image via https://en.wikipedia.org/wiki/Special:Redirect/file/Farmer_plowing.jpg which
    // results in a 301 to https://upload.wikimedia.org/wikipedia/commons/a/a2/04-09-12-Schaupfl%C3%BCgen-Fahrenwalde-RalfR-IMG_1232.jpg
    //
    // Therefore, if we get a 404 for the first more predictable URL (which assume no redirects)
    // then we try again by performing a HEAD request to "/wiki/Special:Redirect/file/..." and
    // then capturing the final path segment (in the example above it would be 04-09-12-Schaupfl%C3%BCgen-Fahrenwalde-RalfR-IMG_1232.jpg)
    // and trying once more.
    val response: HttpResponse = try {
        http.get(url)
    } catch (originalException: Exception) {
        if (!lookForRenamedImageOnFailure) {
            println("Ignoring $filename because it wasn't found and we are not looking for renamed variants.")
            return null
        }

        println("Noticed an error (${originalException.message}) fetching $url, so will try to see if it has been redirected to a different image.")
        val newFilename = discoverFilenameFromRenamedImage(wikiUrl, filename)
        if (newFilename == null) {
            println("Still couldn't find image $filename, so giving up.")
            return null
        }

        val newUrl = commonsUrlForImage(newFilename)

        println("Downloading from alternate URL (probably a renamed image): $newUrl")
        try {
            http.get(newUrl)
        } catch (innerException: Exception) {
            println("Error downloading from alternate URL $newUrl: ${innerException.message}")
            return null
        }
    } finally {}

    response.content.copyTo(outputFile.outputStream())
    return outputFile
}

/**
 * @param lookForRenamedImageOnFailure After implementing this functionality for the "Horse" wiki
 * page, it became clear that the renamed file was included twice and so perhaps it isn't really
 * all that important to follow up on renamed images.
 *
 * @param wikiUrl Base URL of the wikipedia installation to use (e.g. https://en.wikipedia.org)
 */
suspend fun downloadWikiImage(wikiUrl: URL, filename: String, destDir: File, lookForRenamedImageOnFailure: Boolean = false): WikipediaCommonsFile? {

    val metadata = downloadWikiImageMetadata(filename) ?: return null
    val file = downloadWikiImageContents(wikiUrl, filename, destDir, lookForRenamedImageOnFailure) ?: return null

    return WikipediaCommonsFile(
        file = file,
        title = metadata.title,
        description = metadata.description,
        license = metadata.license,
        author = metadata.author,
    )

}

/**
 * @param wikiUrl Base URL of the wikipedia installation to use (e.g. https://en.wikipedia.org)
 */
private suspend fun discoverFilenameFromRenamedImage(wikiUrl: URL, filename: String): String? {
    val sourceUrl = "$wikiUrl/wiki/Special:Redirect/file/$filename"
    println("Checking if $filename has been renamed by looking for redirects in a HEAD request to $sourceUrl")
    try {
        val headResponse: HttpResponse = nonRedirectingHttp.head(sourceUrl)
        val redirectUrl = headResponse.headers["location"]
        if (redirectUrl == null) {
            println("No \"location\" header present, only saw: [\"${headResponse.headers.names().joinToString("\", \"")}\"]")
            return null
        }

        val newFilename = Url(redirectUrl).encodedPath.split("/").last()
        println("We think the new filename for $filename is $newFilename (URL Decoded value is ${URLDecoder.decode(newFilename)}).")
        return URLDecoder.decode(newFilename)
    } catch (innerException: Exception) {
        println("Still got an error looking for the original image at $sourceUrl, so will return null")
        return null
    }
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

    private fun getHtml() = page.parse.text.value

    private fun getAllImageNames(): List<String> {

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

    private fun getInfoBoxImages(): List<String> {
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

        val doc = Jsoup.parse(html)
        val infobox = doc.select("table.infobox")
        val body = doc.select(".mw-parser-output")

        return body.select("p")
            .asSequence()
            .map { it.text().trim() }
            .filter { it.isNotEmpty() }
            .filterNot { p -> infobox.select("p").map { it.text() }.contains(p) } // There is sometimes <p> tags in the info box, we don't want those.
            .map { it.replace(Regex("\\[.*?\\]"), "") }
            .map { it.replace(Regex(" \\(.*?\\)"), "") }
            .toList()
    }

}

// e.g. https://commons.wikimedia.org/w/api.php?action=query&titles=Image:Gallina_de_Guinea_(Numida_meleagris),_parque_nacional_Kruger,_Sud%C3%A1frica,_2018-07-25,_DD_48.jpg&prop=imageinfo&iiprop=extmetadata
data class WikipediaCommonsFileData(
    val file: File,
    val title: String,
    val description: String,
    val license: String,
)

data class WikipediaCommonsMetadata(
    val title: String,
    val description: String,
    val license: String,
    val author: String,
)

data class WikipediaCommonsFile(
    val file: File,
    val title: String,
    val description: String,
    val license: String,
    val author: String,
)

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