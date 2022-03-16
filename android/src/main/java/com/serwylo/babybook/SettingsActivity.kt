package com.serwylo.babybook

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.preference.*
import com.serwylo.babybook.contentwarning.ContentWarningActivity
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.repositories.BookRepository
import com.serwylo.babybook.onboarding.OnboardingActivity
import kotlinx.coroutines.withContext

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

            findPreference<ListPreference>("select_wiki_site")?.apply {
                lifecycleScope.launchWhenCreated {

                    val repo = BookRepository(AppDatabase.getInstance(context).bookDao())
                    val sites = repo.getAllWikiSites()
                    val selectedSite = repo.getDefaultWikiSite()

                    entries = sites.map { it.localisedTitle }.toTypedArray()
                    entryValues = sites.map { it.code }.toTypedArray()
                    summary = selectedSite.localisedTitle

                    setOnPreferenceChangeListener { _, code ->
                        sites.firstOrNull { it.code == code }?.also { site ->
                            summary = site.localisedTitle
                            lifecycleScope.launchWhenCreated {
                                repo.setDefaultWikiSite(site)
                            }
                        }
                        true
                    }

                }
            }

            findPreference<Preference>("show_onboarding")?.apply {
                setOnPreferenceClickListener {
                    startActivity(Intent(context, OnboardingActivity::class.java))
                    true
                }
            }

            findPreference<Preference>("show_content_warning")?.apply {
                setOnPreferenceClickListener {
                    startActivity(Intent(context, ContentWarningActivity::class.java))
                    true
                }
            }
        }

    }

}
