package com.serwylo.picturebook.web

import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.runApplication

@SpringBootApplication
class PictureBookApplication

fun main(args: Array<String>) {
	runApplication<PictureBookApplication>(*args)
}
