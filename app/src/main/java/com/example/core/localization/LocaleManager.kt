package com.example.core.localization

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

object LocaleManager {
    const val LANG_ENGLISH = "en"
    const val LANG_BANGLA = "bn"

    fun getLocalizedContext(base: Context, languageCode: String): Context {
        val locale = Locale(languageCode)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)
        return base.createConfigurationContext(config)
    }

    fun getLocalizedConfiguration(baseConfig: Configuration, languageCode: String): Configuration {
        val locale = Locale(languageCode)
        val newConfig = Configuration(baseConfig)
        newConfig.setLocale(locale)
        return newConfig
    }
}
