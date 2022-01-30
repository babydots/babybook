package com.serwylo.babybook.utils

import android.content.Context
import android.content.Intent
import android.net.Uri

fun viewInWikipedia(context: Context, pageTitle: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("https://simple.wikipedia.org/wiki/$pageTitle"));
    context.startActivity(intent)
}