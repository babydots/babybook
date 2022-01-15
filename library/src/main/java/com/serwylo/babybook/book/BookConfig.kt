package com.serwylo.babybook.book

data class BookConfig(
    val titleFontSize: Float = 42f,
    val pageTitleFontSize: Float = 24f,
    val textFontSize: Float = 16f,
    val padding: Float = 20f,
    val pageWidth: Float = 400f,
    val pageHeight: Float = 400f,
    val summary: Summary = Summary.Short,
) {

    companion object {

        val SingleSentencePerPage = BookConfig(
            textFontSize = 16f,
            summary = Summary.Short,
        )

        val SingleParagraphPerPage = BookConfig(
            textFontSize = 12f,
            summary = Summary.Full,
        )

        val Default = SingleParagraphPerPage

    }

    enum class Summary {
        Short,
        Full,
    }

}