package com.serwylo.picturebook.web

import com.serwylo.picturebook.book.makeBook
import kotlinx.serialization.ExperimentalSerializationApi
import org.springframework.core.io.FileSystemResource
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseBody
import javax.servlet.http.HttpServletResponse

@Controller
class BookController {

    @ExperimentalSerializationApi
    @GetMapping("/book")
    @ResponseBody
    fun generateBook(
        response: HttpServletResponse,
        @RequestParam("title") title: String,
        @RequestParam("pages") pages: String
    ): FileSystemResource {

        val pageTitles = pages.split("_")

        val pdfFile = makeBook(title, pageTitles)

        response.contentType = "application/pdf";
        response.setHeader("Content-Disposition", "attachment; filename=$title.pdf");

        return FileSystemResource(pdfFile)
    }

}