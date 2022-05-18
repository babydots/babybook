package com.serwylo.babybook

import android.content.Context
import android.text.Html
import androidx.appcompat.app.AppCompatActivity
import io.github.tonnyl.whatsnew.WhatsNew
import io.github.tonnyl.whatsnew.item.WhatsNewItem
import io.github.tonnyl.whatsnew.util.PresentationOption

object Changelog {

    @JvmStatic
    fun show(activity: AppCompatActivity) {

        val whatsNew = buildDialog(activity)
        whatsNew.presentationOption = PresentationOption.IF_NEEDED
        whatsNew.presentAutomatically(activity)

    }

    private fun buildDialog(context: Context): WhatsNew {

        return WhatsNew.newInstance(
            WhatsNewItem(
                "Baby lock",
                "Prevent stray little fingers from closing the app and ordering online shopping while you are not looking.",
                R.drawable.ic_lock,
            ),
            WhatsNewItem(
                "Swipe (or tap) to turn pages",
                "Select your preferred option from the settings menu. Volume can still be used to change pages too.",
                R.drawable.ic_book,
            ),
            WhatsNewItem(
                "Support Baby Book Development",
                Html.fromHtml("Baby Book is and always will be free and open source. If you are able, please <a href=\"https://github.com/babydots/babybook#support\">show your support and contribute to its further development</a>."),
                R.drawable.ic_heart
            ),
        ).apply {
            titleText = "What's New?"
            buttonText = "Continue"
        }

    }

}
