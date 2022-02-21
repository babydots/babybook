package com.serwylo.babybook

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.*
import com.serwylo.babybook.contentwarning.ContentWarningActivity
import com.serwylo.babybook.onboarding.OnboardingActivity

class SettingsActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_settings)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.beginTransaction().replace(R.id.settings_wrapper, Fragment()).commit()
    }

    class Fragment : PreferenceFragmentCompat() {

        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.settings)

            val showOnboarding = findPreference<Preference>("show_onboarding")
            showOnboarding?.setOnPreferenceClickListener {
                startActivity(Intent(context, OnboardingActivity::class.java))
                true
            }

            val showContentWarning = findPreference<Preference>("show_content_warning")
            showContentWarning?.setOnPreferenceClickListener {
                startActivity(Intent(context, ContentWarningActivity::class.java))
                true
            }
        }

    }

}
