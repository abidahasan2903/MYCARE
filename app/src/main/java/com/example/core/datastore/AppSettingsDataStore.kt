package com.example.core.datastore

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.core.localization.LocaleManager
import com.example.core.theme.AccentTheme
import com.example.core.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mycare_settings")

data class AppSettingsState(
    val isLanguageSelected: Boolean = false,
    val languageCode: String = LocaleManager.LANG_ENGLISH,
    val themeMode: ThemeMode = ThemeMode.LIGHT,
    val accentTheme: AccentTheme = AccentTheme.SOFT_WHITE_GREEN,
    val fontScale: Float = 1.0f,
    val notificationsEnabled: Boolean = true,
    val defaultRingtoneUri: String? = null,
    val defaultRingtoneName: String? = null,
    val defaultRingingDurationSec: Int = 30,
    val defaultSnoozeMinutes: Int = 5,
    val defaultVibration: Boolean = true,
    val notifMedicine: Boolean = true,
    val notifExercise: Boolean = true,
    val notifTasks: Boolean = true,
    val notifGrocery: Boolean = true,
    val notifAppointments: Boolean = true,
    val notifNutrition: Boolean = true,
    val notifTravel: Boolean = true,
    val notifGeneral: Boolean = true,
    val waterDailyGoalMl: Int = 2000
)

class AppSettingsDataStore(private val context: Context) {

    private object Keys {
        val LANGUAGE_SELECTED = booleanPreferencesKey("language_selected")
        val LANGUAGE_CODE = stringPreferencesKey("language_code")
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val ACCENT_THEME = stringPreferencesKey("accent_theme")
        val FONT_SCALE = floatPreferencesKey("font_scale")
        val NOTIFICATIONS_ENABLED = booleanPreferencesKey("notifications_enabled")
        val DEFAULT_RINGTONE_URI = stringPreferencesKey("default_ringtone_uri")
        val DEFAULT_RINGTONE_NAME = stringPreferencesKey("default_ringtone_name")
        val DEFAULT_RINGING_DURATION = intPreferencesKey("default_ringing_duration")
        val DEFAULT_SNOOZE_MINUTES = intPreferencesKey("default_snooze_minutes")
        val DEFAULT_VIBRATION = booleanPreferencesKey("default_vibration")
        val NOTIF_MEDICINE = booleanPreferencesKey("notif_medicine")
        val NOTIF_EXERCISE = booleanPreferencesKey("notif_exercise")
        val NOTIF_TASKS = booleanPreferencesKey("notif_tasks")
        val NOTIF_GROCERY = booleanPreferencesKey("notif_grocery")
        val NOTIF_APPOINTMENTS = booleanPreferencesKey("notif_appointments")
        val NOTIF_NUTRITION = booleanPreferencesKey("notif_nutrition")
        val NOTIF_TRAVEL = booleanPreferencesKey("notif_travel")
        val NOTIF_GENERAL = booleanPreferencesKey("notif_general")
        val WATER_DAILY_GOAL_ML = intPreferencesKey("water_daily_goal_ml")
    }

    val settingsFlow: Flow<AppSettingsState> = context.dataStore.data.map { prefs ->
        AppSettingsState(
            isLanguageSelected = prefs[Keys.LANGUAGE_SELECTED] ?: false,
            languageCode = prefs[Keys.LANGUAGE_CODE] ?: LocaleManager.LANG_ENGLISH,
            themeMode = ThemeMode.fromKey(prefs[Keys.THEME_MODE] ?: ThemeMode.LIGHT.key),
            accentTheme = AccentTheme.fromKey(prefs[Keys.ACCENT_THEME] ?: AccentTheme.SOFT_WHITE_GREEN.key),
            fontScale = prefs[Keys.FONT_SCALE] ?: 1.0f,
            notificationsEnabled = prefs[Keys.NOTIFICATIONS_ENABLED] ?: true,
            defaultRingtoneUri = prefs[Keys.DEFAULT_RINGTONE_URI],
            defaultRingtoneName = prefs[Keys.DEFAULT_RINGTONE_NAME],
            defaultRingingDurationSec = prefs[Keys.DEFAULT_RINGING_DURATION] ?: 30,
            defaultSnoozeMinutes = prefs[Keys.DEFAULT_SNOOZE_MINUTES] ?: 5,
            defaultVibration = prefs[Keys.DEFAULT_VIBRATION] ?: true,
            notifMedicine = prefs[Keys.NOTIF_MEDICINE] ?: true,
            notifExercise = prefs[Keys.NOTIF_EXERCISE] ?: true,
            notifTasks = prefs[Keys.NOTIF_TASKS] ?: true,
            notifGrocery = prefs[Keys.NOTIF_GROCERY] ?: true,
            notifAppointments = prefs[Keys.NOTIF_APPOINTMENTS] ?: true,
            notifNutrition = prefs[Keys.NOTIF_NUTRITION] ?: true,
            notifTravel = prefs[Keys.NOTIF_TRAVEL] ?: true,
            notifGeneral = prefs[Keys.NOTIF_GENERAL] ?: true,
            waterDailyGoalMl = (prefs[Keys.WATER_DAILY_GOAL_ML] ?: 2000).coerceIn(250, 10000)
        )
    }

    suspend fun completeLanguageSelection(languageCode: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_CODE] = languageCode
            prefs[Keys.LANGUAGE_SELECTED] = true
        }
    }

    suspend fun setLanguageCode(languageCode: String) {
        context.dataStore.edit { it[Keys.LANGUAGE_CODE] = languageCode }
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { it[Keys.THEME_MODE] = mode.key }
    }

    suspend fun setAccentTheme(accent: AccentTheme) {
        context.dataStore.edit { it[Keys.ACCENT_THEME] = accent.key }
    }

    suspend fun setFontScale(scale: Float) {
        context.dataStore.edit { it[Keys.FONT_SCALE] = scale }
    }

    suspend fun setNotificationsEnabled(enabled: Boolean) {
        context.dataStore.edit { it[Keys.NOTIFICATIONS_ENABLED] = enabled }
    }

    suspend fun setDefaultRingtone(uri: String?, displayName: String?) {
        context.dataStore.edit { prefs ->
            if (uri == null) {
                prefs.remove(Keys.DEFAULT_RINGTONE_URI)
                prefs.remove(Keys.DEFAULT_RINGTONE_NAME)
            } else {
                prefs[Keys.DEFAULT_RINGTONE_URI] = uri
                if (displayName != null) prefs[Keys.DEFAULT_RINGTONE_NAME] = displayName
                else prefs.remove(Keys.DEFAULT_RINGTONE_NAME)
            }
        }
    }

    suspend fun setDefaultRingingDurationSec(seconds: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_RINGING_DURATION] = seconds }
    }

    suspend fun setDefaultSnoozeMinutes(minutes: Int) {
        context.dataStore.edit { it[Keys.DEFAULT_SNOOZE_MINUTES] = minutes }
    }

    suspend fun setDefaultVibration(enabled: Boolean) {
        context.dataStore.edit { it[Keys.DEFAULT_VIBRATION] = enabled }
    }

    suspend fun setWaterDailyGoalMl(goalMl: Int) {
        context.dataStore.edit { it[Keys.WATER_DAILY_GOAL_ML] = goalMl.coerceIn(250, 10000) }
    }

    suspend fun setFeatureNotification(featureKey: String, enabled: Boolean) {
        context.dataStore.edit { prefs ->
            when (featureKey) {
                "MEDICINE" -> prefs[Keys.NOTIF_MEDICINE] = enabled
                "EXERCISE" -> prefs[Keys.NOTIF_EXERCISE] = enabled
                "TASK" -> prefs[Keys.NOTIF_TASKS] = enabled
                "GROCERY" -> prefs[Keys.NOTIF_GROCERY] = enabled
                "APPOINTMENT" -> prefs[Keys.NOTIF_APPOINTMENTS] = enabled
                "NUTRITION" -> prefs[Keys.NOTIF_NUTRITION] = enabled
                "TRAVEL" -> prefs[Keys.NOTIF_TRAVEL] = enabled
                "GENERAL" -> prefs[Keys.NOTIF_GENERAL] = enabled
            }
        }
    }

    suspend fun restoreFromState(state: AppSettingsState) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE_SELECTED] = true
            prefs[Keys.LANGUAGE_CODE] = state.languageCode
            prefs[Keys.THEME_MODE] = state.themeMode.key
            prefs[Keys.ACCENT_THEME] = state.accentTheme.key
            prefs[Keys.FONT_SCALE] = state.fontScale
            prefs[Keys.NOTIFICATIONS_ENABLED] = state.notificationsEnabled
            if (state.defaultRingtoneUri == null) {
                prefs.remove(Keys.DEFAULT_RINGTONE_URI)
                prefs.remove(Keys.DEFAULT_RINGTONE_NAME)
            } else {
                prefs[Keys.DEFAULT_RINGTONE_URI] = state.defaultRingtoneUri
                if (state.defaultRingtoneName != null) prefs[Keys.DEFAULT_RINGTONE_NAME] = state.defaultRingtoneName
            }
            prefs[Keys.DEFAULT_RINGING_DURATION] = state.defaultRingingDurationSec
            prefs[Keys.DEFAULT_SNOOZE_MINUTES] = state.defaultSnoozeMinutes
            prefs[Keys.DEFAULT_VIBRATION] = state.defaultVibration
            prefs[Keys.NOTIF_MEDICINE] = state.notifMedicine
            prefs[Keys.NOTIF_EXERCISE] = state.notifExercise
            prefs[Keys.NOTIF_TASKS] = state.notifTasks
            prefs[Keys.NOTIF_GROCERY] = state.notifGrocery
            prefs[Keys.NOTIF_APPOINTMENTS] = state.notifAppointments
            prefs[Keys.NOTIF_NUTRITION] = state.notifNutrition
            prefs[Keys.NOTIF_TRAVEL] = state.notifTravel
            prefs[Keys.NOTIF_GENERAL] = state.notifGeneral
            prefs[Keys.WATER_DAILY_GOAL_ML] = state.waterDailyGoalMl.coerceIn(250, 10000)
        }
    }
}
