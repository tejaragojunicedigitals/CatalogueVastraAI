package com.nice.cataloguevastra.utils

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.nice.cataloguevastra.BuildConfig

class SessionManager(context: Context) {

    private val logTag = "SessionManager"

    private val preferences: SharedPreferences = context.getSharedPreferences(
        PREF_NAME,
        Context.MODE_PRIVATE
    )

    fun hasToken(): Boolean {
        val hasToken = getToken().isNotBlank()
        if (BuildConfig.DEBUG) {
            Log.d(logTag, "hasToken check. token=${getToken()}, hasToken=$hasToken")
        }
        return hasToken
    }

    fun saveToken(token: String) {
        preferences.edit()
            .putString(KEY_AUTH_TOKEN, token)
            .apply()
        if (BuildConfig.DEBUG) {
            Log.d(logTag, "Saved API token: $token")
        }
    }

    fun getToken(): String {
        val token = preferences.getString(KEY_AUTH_TOKEN, "").orEmpty()
        if (BuildConfig.DEBUG) {
            Log.d(logTag, "Read API token from session: $token")
        }
        return token
    }

    fun clearSession() {
        preferences.edit().clear().apply()
        if (BuildConfig.DEBUG) {
            Log.d(logTag, "Session cleared. token removed.")
        }
    }

    private companion object {
        const val PREF_NAME = "catalogue_vastra_session"
        const val KEY_AUTH_TOKEN = "auth_token"
    }
}
