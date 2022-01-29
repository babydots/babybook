package com.serwylo.babybook

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager

object Preferences {

    private const val KEY_ONBOARDING_COMPLETE = "onboarding_complete"

    private fun prefs(context: Context) = PreferenceManager.getDefaultSharedPreferences(context)

    fun isOnboardingComplete(context: Context) = prefs(context).getBoolean(KEY_ONBOARDING_COMPLETE, false)

    fun setOnboardingComplete(context: Context) = prefs(context).edit {
        putBoolean(KEY_ONBOARDING_COMPLETE, true)
    }

}