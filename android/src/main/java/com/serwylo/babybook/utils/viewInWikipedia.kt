package com.serwylo.babybook.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.serwylo.babybook.db.entities.WikiSite

fun viewInWikipedia(context: Context, wikiSite: WikiSite, pageTitle: String) {
    val intent = Intent(Intent.ACTION_VIEW, Uri.parse("${wikiSite.url()}/wiki/$pageTitle"))
    context.startActivity(intent)
}