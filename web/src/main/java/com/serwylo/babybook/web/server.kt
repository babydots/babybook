package com.serwylo.babybook.web

import com.serwylo.babybook.book.makeBook
import io.ktor.application.*
import io.ktor.features.*
import io.ktor.response.*
import io.ktor.routing.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.gson.*
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.IOException

fun main(args: Array<String>) {

    val port = getIntArg("PORT", "port", args) ?: 8080

    embeddedServer(Netty, port = port) {
        install(CORS) {
            host("localhost:3000")
            host("localhost:8080")
        }

        install(ContentNegotiation) {
            gson {
                setPrettyPrinting()
            }
        }

        routing {

            get("/wiki/search") {
                val searchTerms = call.parameters["q"] ?: ""
                if (searchTerms.isNotEmpty()) {
                    call.respond(WikiCacheService.getSearchResults(searchTerms))
                } else {
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            get("/wiki/page") {
                val title = call.parameters["title"] ?: ""
                if (title.isNotEmpty()) {
                    val bookPage = WikiCacheService.getBookPage(title)
                    val bookData = mapOf(
                        "title" to bookPage.title,
                        "image" to bookPage.image?.name,
                        "text" to bookPage.text,
                    )
                    call.respond(bookData)
                } else {
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            get("/wiki/image/{pageTitle}/{imageName}") {
                val title = call.parameters["pageTitle"] ?: ""
                val imageName = call.parameters["imageName"] ?: ""

                if (title.isNotEmpty() && imageName.isNotEmpty()) {
                    try {
                        val imageFile = WikiCacheService.getWikiImage(title, imageName)
                        call.respondFile(imageFile)
                    } catch (e: IOException) {
                        call.response.status(HttpStatusCode.NotFound)
                    }
                } else {
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            get("/book") {
                val title = call.parameters["title"] ?: ""
                val pages = call.parameters["pages"] ?: ""
                if (title.isNotEmpty() && pages.isNotEmpty()) {
                    val pageTitles = pages.split("_")
                    val file = makeBook(wikiUrl, title, pageTitles)
                    call.response.header(
                        HttpHeaders.ContentDisposition,
                        ContentDisposition.Attachment.withParameter(ContentDisposition.Parameters.FileName, "${title}.pdf").toString()
                    )
                    call.respondFile(file)
                } else {
                    call.response.status(HttpStatusCode.BadRequest)
                }
            }

            static {
                resources("public")

                resource("/", "public/index.html")
            }

        }

    }.start(wait = true)
}

private fun getIntArg(envName: String, argName: String, args: Array<String>) =
    getStringArg(envName, argName, args)?.toIntOrNull()

private fun getStringArg(envName: String, argName: String, args: Array<String>): String? {
    val envValue = System.getenv(envName)

    val cliValue = args
        .find { it.startsWith("--$argName=") }
        ?.substring("--$argName=".length)

    return cliValue ?: envValue
}