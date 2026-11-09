package com.boletodigital.app.data

import android.content.Context

object AppUpdatePrefs {
    private const val PREFS = "boleto_digital_prefs"
    private const val KEY_LAST_SEEN_VERSION = "last_seen_version_code"

    fun deveMostrarNovidades(context: Context, versionCodeAtual: Int): Boolean {
        val visto = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getInt(KEY_LAST_SEEN_VERSION, 0)
        return versionCodeAtual > visto
    }

    fun marcarNovidadesVistas(context: Context, versionCodeAtual: Int) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_LAST_SEEN_VERSION, versionCodeAtual)
            .apply()
    }
}
