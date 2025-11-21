package com.github.keeganwitt.applist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.github.keeganwitt.applist.databinding.ActivitySettingsBinding

class SettingsActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySettingsBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager
            .beginTransaction()
            .replace(R.id.settings_container, SettingsFragment())
            .commit()
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)

            findPreference<SwitchPreferenceCompat>(AppSettings.KEY_CRASH_REPORTING_ENABLED)
                ?.setOnPreferenceChangeListener { _, newValue ->
                    val enabled = newValue as Boolean
                    try {
                        val crashlytics =
                            com.google.firebase.crashlytics.FirebaseCrashlytics
                                .getInstance()
                        crashlytics.isCrashlyticsCollectionEnabled = enabled
                        if (!enabled) {
                            crashlytics.deleteUnsentReports()
                        }
                    } catch (e: Throwable) {
                        android.util.Log.w(
                            "SettingsActivity",
                            "Failed to update Firebase Crashlytics",
                            e,
                        )
                    }
                    true
                }
        }
    }
}
