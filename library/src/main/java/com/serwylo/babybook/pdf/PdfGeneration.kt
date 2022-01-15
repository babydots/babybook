package com.serwylo.babybook.pdf

import com.itextpdf.text.*
import com.itextpdf.text.pdf.ColumnText
import com.itextpdf.text.pdf.PdfWriter
import com.serwylo.babybook.book.BookConfig
import com.serwylo.babybook.book.Page
import com.sksamuel.scrimage.ImmutableImage
import com.sksamuel.scrimage.nio.JpegWriter
import java.io.File


fun generatePdf(bookTitle: String, pages: List<Page>, outputFile: File, config: BookConfig) {

    println("Writing PDF to $outputFile")

    val document = Document(Rectangle(config.pageWidth, config.pageHeight))
    val writer = PdfWriter.getInstance(document, outputFile.outputStream())

    document.open()

    val headingFont: Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, config.titleFontSize, BaseColor.BLACK)
    val pageTitleFont: Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, config.pageTitleFontSize, BaseColor.BLACK)
    val font: Font = FontFactory.getFont(FontFactory.HELVETICA_BOLD, config.textFontSize, BaseColor.BLACK)

    document.add(Paragraph(bookTitle, headingFont))

    val textBackgroundColour = BaseColor(1f, 1f, 1f, 0.5f)
    val textBackgroundPadding = 3f

    pages.onEach { page ->
        println("Writing page: ${page.title}")
        document.newPage()

        val image: Image = Image.getInstance(scaleImage(page.image).absolutePath)
        val xScaleRequired = document.pageSize.width / image.width
        val yScaleRequired = document.pageSize.height / image.height
        val scale = xScaleRequired.coerceAtLeast(yScaleRequired)
        image.scaleAbsolute(image.width * scale, image.height * scale)
        image.setAbsolutePosition((document.pageSize.width - image.scaledWidth) / 2, (document.pageSize.height - image.scaledHeight) / 2)

        document.add(image)

        document.add(
            Paragraph(
                Chunk(page.title, pageTitleFont).apply {
                    setBackground(textBackgroundColour, textBackgroundPadding * 2, textBackgroundPadding, textBackgroundPadding * 2, textBackgroundPadding)
                }
            )
        )

        val element = Paragraph(
            Chunk(page.text, font).apply {
                setBackground(textBackgroundColour, textBackgroundPadding * 2, textBackgroundPadding, textBackgroundPadding * 2, textBackgroundPadding)
            }
        )

        val textHeight = ColumnText(writer.directContent).apply {
            setSimpleColumn(0f, 0f, config.pageWidth - config.padding * 2, config.pageHeight - config.padding * 2)
            setText(element)
            go(true)
        }.yLine

        ColumnText(writer.directContent).apply {
            setSimpleColumn(config.padding, config.padding, config.pageWidth - config.padding * 2, config.pageHeight - config.padding - textHeight)
            setText(element)
            go()
        }
    }

    document.close()

    println("Wrote $outputFile")
}

fun scaleImage(file: File, ignoreCache: Boolean = false): File {
    val scaledFile = File(file.parentFile, "${file.nameWithoutExtension}.scaled.${file.extension}")

    if (!scaledFile.exists() || ignoreCache) {
        println("Scaling image ${file.absolutePath}...")
        val image = ImmutableImage.loader().fromFile(file)

        val scaledImage = if (image.width < image.height) {
            image.scaleToWidth(512)
        } else {
            image.scaleToHeight(512)
        }

        scaledImage.output(JpegWriter.Default, scaledFile)
    }

    return scaledFile
}
