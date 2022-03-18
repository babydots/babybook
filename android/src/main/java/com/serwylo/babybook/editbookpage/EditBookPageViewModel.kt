package com.serwylo.babybook.editbookpage

import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.db.entities.WikiImage
import com.serwylo.babybook.db.entities.WikiPage
import com.serwylo.babybook.db.entities.WikiSite
import com.serwylo.babybook.db.repositories.BookRepository
import com.serwylo.babybook.mediawiki.downloadWikiImage
import com.serwylo.babybook.mediawiki.loadWikiPage
import com.serwylo.babybook.mediawiki.WikiPage as FetchedWikiPage
import kotlinx.coroutines.*
import java.io.File

class EditBookPageViewModel(private val repository: BookRepository, private val filesDir: File, val bookId: Long, private val existingBookPageId: Long): ViewModel() {

    private var bookPageId = existingBookPageId

    val pageTitle = MutableLiveData<String?>(null)
    val pageText = MutableLiveData<String?>(null)
    val pageNumber = MutableLiveData(0)

    val wikiPage = MutableLiveData<WikiPage?>(null)
    val mainImage = MutableLiveData<WikiImage?>(null)
    val allImages = MutableLiveData(listOf<WikiImage>())

    val isLoading = MutableLiveData(false)
    val isLoadingImages = MutableLiveData(false)
    val isSearchingPages = MutableLiveData(false)
    val isPreparingPage = MutableLiveData(false)

    lateinit var wikiSite: WikiSite

    init {
        viewModelScope.launch {
            wikiSite = withContext(Dispatchers.IO) { repository.getWikiSite(bookId) }
        }

        viewModelScope.launch {
            if (existingBookPageId > 0) {
                val bookPage = withContext(Dispatchers.IO) { repository.getBookPage(existingBookPageId) }

                pageTitle.value = bookPage.title
                pageText.value = bookPage.text
                pageNumber.value = bookPage.pageNumber

                bookPage.wikiPageId?.also { wikiPageId ->
                    val page = repository.getWikiPage(wikiPageId)
                    wikiPage.value = page
                    allImages.value = withContext(Dispatchers.IO) { repository.getWikiImages(page) }

                    bookPage.wikiImageId?.also { wikiImageId ->
                        mainImage.value = withContext(Dispatchers.IO) { repository.getWikiImage(wikiImageId) }
                    }
                }
            }

            isLoading.value = false
        }
    }

    fun title() = pageTitle.value ?: wikiPage.value?.title ?: ""
    fun text() = pageText.value ?: wikiPage.value?.text ?: ""

    fun movePageUp() {
        val currentId = bookPageId
        if (currentId < 0) {
            return
        }

        viewModelScope.launch {
            pageNumber.value = repository.movePageUp(bookPageId, bookId)
        }
    }

    fun movePageDown() {
        val currentId = bookPageId
        if (currentId < 0) {
            return
        }

        viewModelScope.launch {
            pageNumber.value = repository.movePageDown(bookPageId, bookId)
        }
    }

    private suspend fun prepareExistingPage(existingWikiPage: WikiPage) = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) {
            wikiPage.value = existingWikiPage
            pageTitle.value = null
            pageText.value = null
        }

        if (existingWikiPage.imagesFetched) {
            Log.d(TAG, "preparePage: Existing images have already been fetched for wiki page ${existingWikiPage.id}.")
            val images = repository.getWikiImages(existingWikiPage)

            withContext(Dispatchers.Main) {
                mainImage.value = images.firstOrNull()
                allImages.value = images
            }
        } else {
            Log.d(TAG, "preparePage: No images yet fetched for wiki page ${existingWikiPage.id}. Will go and fetch wiki details, list of images, and then download the first image of interest.")
            val details = fetchPageDataFromWiki(existingWikiPage.title)
            val image = fetchImageFromWiki(existingWikiPage, details)

            withContext(Dispatchers.Main) {
                mainImage.value = image
                allImages.value = listOf()
            }
        }

        save()
    }

    private suspend fun prepareNewPage(title: String) = withContext(Dispatchers.IO) {
        val details = fetchPageDataFromWiki(title)

        Log.d(TAG, "preparePage: Wikipedia page details loaded, will save to DB.")
        val newWikiPage = repository.addNewWikiPage(wikiSite, title, details.parseParagraphs().firstOrNull() ?: "")

        withContext(Dispatchers.Main) {
            wikiPage.value = newWikiPage
            pageTitle.value = null
            pageText.value = null
        }

        Log.d(TAG, "preparePage: Saving book page to DB now that we have wiki page details, then we'll go fetch the image while this is happening.")
        val initialSaveJob = save()

        val image = fetchImageFromWiki(newWikiPage, details)

        withContext(Dispatchers.Main) {
            allImages.value = listOf()
            mainImage.value = image
        }

        // If we already have cached images, then the above fetching will be faster than saving
        // the record to the database. Hence, we wait for the save to comlete.
        initialSaveJob.join()

        Log.d(TAG, "preparePage: Saving book page again now that we know about its images.")
        save()
    }

    suspend fun preparePage(title: String): Boolean = withContext(Dispatchers.Main) {
        Log.d(TAG, "preparePage: Getting ready to fetch wiki data")
        isPreparingPage.value = true
        allImages.value = listOf()
        mainImage.value = null

        try {
            val existingWikiPage = withContext(Dispatchers.IO) { repository.findWikiPageByTitle(title) }
            if (existingWikiPage != null) {
                Log.d(TAG, "preparePage: Using existing wiki page (id: ${existingWikiPage.id})")
                prepareExistingPage(existingWikiPage)
            } else {
                Log.d(TAG, "preparePage: Page does not yet exist in our local DB, will fetch it.")
                prepareNewPage(title)
            }

            true
        } catch (e: Throwable) {
            Log.e(TAG, "preparePage: Error fetching wiki data: $e", e)
            false
        } finally {
            isPreparingPage.value = false
        }
    }

    private suspend fun ensureDataDir(title: String) = withContext(Dispatchers.IO) {
        val dir = File(filesDir, title)
        if (!dir.exists()) {
            dir.mkdirs()
        }

        dir
    }

    private suspend fun fetchPageDataFromWiki(title: String) = withContext(Dispatchers.IO) {
        val dir = ensureDataDir(title)
        loadWikiPage(wikiSite.url(), title, dir)
    }

    private suspend fun fetchImageFromWiki(wikiPage: WikiPage, details: FetchedWikiPage): WikiImage? = withContext(Dispatchers.IO) {
        val imageName = details.getImageNamesOfInterest().firstOrNull()

        if (imageName == null) {
            Log.d(TAG, "preparePage: No images of interest in this article.")
            null
        } else {
            Log.d(TAG, "preparePage: Fetching image $imageName (in total there are ${details.getImageNamesOfInterest().size} in total - they will be downloaded later when we ask to view all images)")
            val dir = ensureDataDir(wikiPage.title)
            val file = downloadWikiImage(wikiSite.url(), imageName, dir)
            if (file == null) {
                Log.w(TAG, "Couldn't find image for $imageName, so ignoring.")
                null
            } else {
                repository.addNewWikiImage(wikiPage, file)
            }
        }
    }

    suspend fun deletePage() {
        repository.removeBookPage(bookPageId)
    }

    fun clearPage() {
        isPreparingPage.value = false
        isSearchingPages.value = false
        pageText.value = ""
    }

    fun save() = viewModelScope.launch {
        if (bookPageId > 0) {
            Log.d(TAG, "save: Updating existing book page")
            val page = BookPage(
                pageNumber = pageNumber.value!!,
                text = pageText.value,
                title = pageTitle.value,
                bookId = bookId,
                wikiImageId = mainImage.value?.id,
                wikiPageId = wikiPage.value?.id,
                id = bookPageId,
            )

            repository.updateBookPage(page)
        } else {
            Log.d(TAG, "save: Adding new book page")
            val newPageNumber = repository.getNextPageNumber(bookId)
            val page = BookPage(
                pageNumber = repository.getNextPageNumber(bookId),
                text = pageText.value,
                title = pageTitle.value,
                bookId = bookId,
                wikiImageId = mainImage.value?.id,
                wikiPageId = wikiPage.value?.id,
            )

            bookPageId = repository.addNewBookPage(page)
            pageNumber.value = newPageNumber
        }

    }

    companion object {
        private const val TAG = "EditBookPageViewModel"
    }

    fun manuallyUpdateTitle(title: String) {
        pageTitle.value = title
        save()
    }

    fun manuallyUpdateText(title: String) {
        pageText.value = title
        save()
    }

    private var downloadingImagesJob: Job? = null

    fun ensureImagesDownloaded(wikiPage: WikiPage) {
        if (wikiPage.imagesFetched) {
            Log.d(TAG, "ensureImagesDownloaded: Images for \"${wikiPage.title}\" are downloaded, no need to download any.")
            return
        } else if (downloadingImagesJob != null) {
            val message = if (downloadingImagesJob?.isCompleted == true) "Already downloaded" else "Currently downloading"
            Log.d(TAG, "ensureImagesDownloaded: $message images for \"${wikiPage.title}\"")
            return
        }

        Log.d(TAG, "ensureImagesDownloaded: Fetching images for \"${wikiPage.title}\".")
        isLoadingImages.value = true
        downloadingImagesJob = viewModelScope.launch(Dispatchers.IO) {

            val dir = File(filesDir, wikiPage.title)
            if (!dir.exists()) {
                dir.mkdirs()
            }

            // Expect the JSON blob of this to be cached, so shouldn't worry about this extra call...
            val details = loadWikiPage(wikiSite.url(), wikiPage.title, dir)
            val imageNames = details.getImageNamesOfInterest()

            Log.d(TAG, "ensureImagesDownloaded: Ensuring all ${imageNames.size} images are available...")
            val images: List<WikiImage> = if (imageNames.size > 1) { // We already downloaded the first image when showing the article to the user.

                val outstandingImages = imageNames.subList(1, imageNames.size).map { filename ->
                    async {
                        Log.d(TAG, "ensureImagesDownloaded: Downloading $filename...")
                        val file = downloadWikiImage(wikiSite.url(), filename, dir)

                        if (file == null) {
                            Log.w(TAG, "ensureImagesDownloaded: Couldn't find image for $filename, so ignoring.")
                            null
                        } else {
                            Log.d(TAG, "ensureImagesDownloaded: Finished downloading $filename, will save to local DB.")
                            repository.addNewWikiImage(wikiPage, file)
                        }
                    }
                }.awaitAll().filterNotNull()

                // Once we finish downloading all-but-the-first image, we need to add back the first
                // image to the list of "allImages" to display to the user, otherwise it will be
                // missing until they properly reload the activity.
                val main = mainImage.value
                if (main != null) {
                    listOf(main) + outstandingImages
                } else {
                    outstandingImages
                }

            } else {
                emptyList()
            }
            Log.d(TAG, "ensureImagesDownloaded: Finished downloading and saving all ${imageNames.size} images to the local DB. Will record images as downloaded so we don't re-download again.")

            repository.recordImagesAsDownloaded(wikiPage)

            withContext(Dispatchers.Main) {
                allImages.value = images
                isLoadingImages.value = false
            }
        }
    }

}

class EditBookPageViewModelFactory(private val repository: BookRepository, private val filesDir: File, private val bookId: Long, private val existingPageId: Long = 0) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(EditBookPageViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return EditBookPageViewModel(repository, filesDir, bookId, existingPageId) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}