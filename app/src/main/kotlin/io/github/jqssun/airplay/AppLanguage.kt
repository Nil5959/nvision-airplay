package io.github.jqssun.airplay

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** N Vision: the app speaks Hebrew whatever the TV's own language (Fire OS has no Hebrew). Pref "app_language":
 *  a language tag such as "he" (default) or "en", or "system" to follow the device. */
object AppLanguage {
    fun wrap(base: Context): Context {
        val tag = base.getSharedPreferences(Prefs.NAME, Context.MODE_PRIVATE).getString(Prefs.LANGUAGE.key, Prefs.LANGUAGE.def)
        if (tag.isNullOrBlank() || tag == "system") return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration)
        config.setLocale(locale)   // also sets the RTL/LTR layout direction
        return base.createConfigurationContext(config)
    }
}
