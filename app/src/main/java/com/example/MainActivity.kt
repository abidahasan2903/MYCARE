package com.example

import android.content.Context
import android.content.ContextWrapper
import android.content.res.AssetManager
import android.content.res.Resources
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.localization.LocaleManager
import com.example.core.navigation.MyCareNavHost
import com.example.core.theme.MyCareTheme
import kotlinx.coroutines.flow.map

private class LocalizedContextWrapper(
    base: Context,
    private val localizedResourcesContext: Context
) : ContextWrapper(base) {
    override fun getResources(): Resources = localizedResourcesContext.resources
    override fun getAssets(): AssetManager = localizedResourcesContext.assets
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val appContainer = (application as MyCareApplication).container

        setContent {
            val loadedSettings by remember {
                appContainer.settingsStore.settingsFlow.map<_, com.example.core.datastore.AppSettingsState?> { it }
            }.collectAsStateWithLifecycle(initialValue = null)

            val settings = loadedSettings
            if (settings != null) {
                val baseConfig = LocalConfiguration.current
                val localizedConfig = remember(baseConfig, settings.languageCode) {
                    LocaleManager.getLocalizedConfiguration(baseConfig, settings.languageCode)
                }
                val localizedContext = remember(this, settings.languageCode) {
                    val resContext = LocaleManager.getLocalizedContext(this, settings.languageCode)
                    LocalizedContextWrapper(this, resContext)
                }

                CompositionLocalProvider(
                    LocalConfiguration provides localizedConfig,
                    LocalContext provides localizedContext
                ) {
                    MyCareTheme(
                        themeMode = settings.themeMode,
                        accentTheme = settings.accentTheme,
                        fontScale = settings.fontScale,
                        isBangla = settings.languageCode == LocaleManager.LANG_BANGLA
                    ) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = MaterialTheme.colorScheme.background
                        ) {
                            MyCareNavHost(
                                container = appContainer,
                                isLanguageSelected = settings.isLanguageSelected
                            )
                        }
                    }
                }
            }
        }
    }
}
