package com.serwylo.picturebook.book

import com.serwylo.book.mediawiki.makePages
import com.serwylo.book.pdf.generatePdf
import kotlinx.coroutines.runBlocking
import kotlinx.serialization.ExperimentalSerializationApi
import java.io.File

@ExperimentalSerializationApi
fun makeBook(
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
            pages = makePages(pageTitles, config, cacheDir)
        }

        val tmpPdfFile = File.createTempFile("${filename}.", ".tmp")
        tmpPdfFile.deleteOnExit()

        generatePdf(bookTitle, pages, tmpPdfFile, config)

        tmpPdfFile.copyTo(pdfFile)
        tmpPdfFile.delete()
    }

    return pdfFile
}