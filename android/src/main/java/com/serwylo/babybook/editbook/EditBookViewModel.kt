package com.serwylo.babybook.editbook

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.serwylo.babybook.db.entities.BookPage

class EditBookViewModel : ViewModel() {

    var bookTitle: MutableLiveData<String> = MutableLiveData("")
    var pages: LiveData<List<BookPage>>? = null

}