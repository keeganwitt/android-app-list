package com.github.keeganwitt.applist

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.preference.PreferenceFragmentCompat
import androidx.preference.SwitchPreferenceCompat
import com.github.keeganwitt.applist.databinding.ActivitySettingsBinding
import com.google.firebase.crashlytics.FirebaseCrashlytics

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

    override fun onSupportNavigateUp(): Boolean {
        onBackPressedDispatcher.onBackPressed()
        return true
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        private lateinit var appSettings: AppSettings

        override fun onCreatePreferences(
            savedInstanceState: Bundle?,
            rootKey: String?,
        ) {
            setPreferencesFromResource(R.xml.settings_preferences, rootKey)
            appSettings = SharedPreferencesAppSettings(requireContext())
            findPreference<SwitchPreferenceCompat>(AppSettings.KEY_CRASH_REPORTING_ENABLED)?.setOnPreferenceChangeListener { _, newValue ->
                val enabled = newValue as Boolean
                appSettings.setCrashReportingEnabled(enabled)
                FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = enabled
                if (!enabled) {
                    FirebaseCrashlytics.getInstance().deleteUnsentReports()
                }
                true
            }
        }

        override fun onResume() {
            super.onResume()
            val switch = findPreference<SwitchPreferenceCompat>(AppSettings.KEY_CRASH_REPORTING_ENABLED)
            switch?.isChecked = appSettings.isCrashReportingEnabled()
        }
    }
}
