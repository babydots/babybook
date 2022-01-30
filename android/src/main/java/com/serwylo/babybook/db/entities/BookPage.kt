package com.serwylo.babybook.db.entities

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.PrimaryKey
import com.serwylo.babybook.mediawiki.processTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

@Entity(
    foreignKeys = [ForeignKey(
        entity = Book::class,
        parentColumns = ["id"],
        childColumns = ["bookId"],
        onDelete = ForeignKey.CASCADE
    )]
)
data class BookPage(
    val bookId: Long,

    val pageNumber: Int,

    /**
     * The title used to
     */
    val wikiPageTitle: String? = null,

    /**
     * Generally the same as wikiPageTitle.
     *
     * Allow overriding the page title to something more palatable.
     * For example, we will always strip off trailing parentheses from a wikipedia title,
     * as they are used to disambiguate titles - not useful for these pages.
     */
    val pageTitle: String? = null,

    val imagePath: String? = null,

    val wikiPageText: String? = null,

    val pageText: String? = null,
) {
    @PrimaryKey(autoGenerate = true)
    var id: Long = 0

    fun title() = pageTitle ?: processTitle(wikiPageTitle ?: "")

    fun text() = pageText ?: wikiPageText

    suspend fun imageFile(context: Context): File? {
        return withContext(Dispatchers.IO) {
            val uriString = imagePath ?: return@withContext null
            val uri = Uri.parse(uriString)

            val fileName = uri.pathSegments.last()
            val file = File(context.filesDir, fileName)

            if (!file.exists()) {
                
                Log.d(TAG, "Could not find image at ${file.absoluteFile}, will try to copy from $uriString.")
                if (uriString.startsWith("file:///android_asset/")) {

                    Log.i(TAG, "Copying from $uriString to ${file.absoluteFile}.")
                    val inputStream = context.assets.open(uriString.substring("file:///android_assets/".length - 1))
                    inputStream.use { input ->
                        file.outputStream().use { output ->
                            input.copyTo(output)
                        }
                    }

                } else if (uriString.startsWith("file:///")) {

                    val existingFile = File(uriString.substring("file://".length - 1))
                    if (!existingFile.exists()) {
                        Log.e(TAG, "Expected to find file at ${existingFile.absoluteFile}, but could not find it.")
                        return@withContext null
                    }

                    return@withContext existingFile

                } else {

                    Log.e(TAG, "Can't find file, will not be able to fetch image properly from $uriString.")
                    return@withContext null

                }
            }

            file
        }
    }

    companion object {

        private const val TAG = "BookPage"

    }

}
