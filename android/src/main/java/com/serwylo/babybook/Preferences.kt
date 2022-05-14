package com.serwylo.babybook

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

object Preferences {

    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"
    private const val KEY_HAS_SHOWN_CONTENT_WARNING_COMPLETE = "has_shown_content_warning"
    private const val KEY_PAGE_TURN_TYPE = "page_turn_type"

    const val PAGE_TURN_TYPE_SWIPING = "swipe"
    const val PAGE_TURN_TYPE_BUTTONS_OVERLAYED = "buttons_overlayed"

    private fun prefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    fun isOnboardingComplete(context: Context) = prefs(context).getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun setOnboardingComplete(context: Context) = prefs(context).edit {
        putBoolean(KEY_ONBOARDING_COMPLETE, true)
    }

    fun hasShownContentWarning(context: Context) = prefs(context).getBoolean(KEY_HAS_SHOWN_CONTENT_WARNING_COMPLETE, false)

    fun setHasShownContentWarning(context: Context) = prefs(context).edit {
        putBoolean(KEY_HAS_SHOWN_CONTENT_WARNING_COMPLETE, true)
    }

    fun pageTurnType(context: Context) = prefs(context).getString(KEY_PAGE_TURN_TYPE, null) ?: PAGE_TURN_TYPE_SWIPING

}