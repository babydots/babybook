package com.serwylo.babybook.db.entities

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.Embedded
import androidx.room.Relation
import com.serwylo.babybook.mediawiki.processTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/**
 * Combination of the [BookPage] data and any associated [WikiPage]. The data from [BookPage]
 * takes precedence, allowing users to manually override data from Wikipedia if they choose.
 */
data class PageEditingData(

    @Embedded
    val bookPage: BookPage,

    @Embedded(prefix = "wikiImage_")
    val image: WikiImage?,

    @Embedded(prefix = "wikiPage_")
    val wikiPage: WikiPage?,

    @Relation(
        parentColumn = "wikiPage_id",
        entity = WikiImage::class,
        entityColumn = "wikiPageId",
    )
    val availableImages: List<WikiImage>,

) {

    fun title() = bookPage.title ?: processTitle(wikiPage?.title ?: "")

    fun text() = bookPage.text ?: wikiPage?.text ?: ""

    suspend fun image(context: Context): File? = imagePathToFile(context, image!!.filename) // TODO: Deal with null

}

suspend fun imagePathToFile(context: Context, imagePath: String?): File? {
    val TAG = "imagePathToFile"

    val uriString = imagePath ?: return null
    val uri = Uri.parse(uriString)

    val fileName = uri.pathSegments.last()
    val file = File(context.filesDir, fileName)

    return withContext(Dispatchers.IO) {

        if (!file.exists()) {

            Log.d(TAG, "Could not find image at ${file.absoluteFile}, will try to copy from $uriString.")
            when {
                uriString.startsWith("file:///android_asset/") -> {

                    Log.i(TAG, "Copying from $uriString to ${file.absoluteFile}.")
                    val inputStream = context.assets.open(uriString.substring("file:///android_assets/".length - 1))
                    inputStream.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                }
                uriString.startsWith("file:///") -> {

                    val existingFile = File(uriString.substring("file://".length - 1))
                    if (!existingFile.exists()) {
                        Log.e(TAG, "Expected to find file at ${existingFile.absoluteFile}, but could not find it.")
                        return@withContext null
                    }

                    return@withContext existingFile

                }
                else -> {

                    Log.e(TAG, "Can't find file, will not be able to fetch image properly from $uriString.")
                    return@withContext null

                }
            }
        }

        file
    }
}
