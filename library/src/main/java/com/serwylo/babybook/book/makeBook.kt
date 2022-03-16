package com.serwylo.babybook.book

import com.serwylo.babybook.mediawiki.makePages
import com.serwylo.babybook.pdf.generatePdf
import kotlinx.coroutines.runBlocking
import java.io.File
import java.net.URL

fun makeBook(
    wikiUrl: URL,
    bookTitle: String,
    pageTitles: List<String>,
    config: BookConfig = BookConfig.Default,
    cacheDir: File = File(System.getProperty("java.io.tmpdir"), "wiki")
): File {

    if (!cacheDir.exists()) {
        cacheDir.mkdirs()
    }

    val filename = "${bookTitle}.${pageTitles.joinToString("_")}.pdf"
    val pdfFile = File(cacheDir, filename)

    if (pdfFile.exists()) {
        println("Using cached book at ${pdfFile.absolutePath}.")
    } else {
        println("Generating new book and caching to ${pdfFile.absolutePath}.")

        val pages: List<Page>
        runBlocking {
            pages = makePages(wikiUrl, pageTitles, config, cacheDir)
        }

        val tmpPdfFile = File.createTempFile("${filename}.", ".tmp")
        tmpPdfFile.deleteOnExit()

        generatePdf(bookTitle, pages, tmpPdfFile, config)

        tmpPdfFile.copyTo(pdfFile)
        tmpPdfFile.delete()
    }

    return pdfFile
}