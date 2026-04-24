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

    fun saveCreditsBalance(balance: Int) {
        preferences.edit()
            .putInt(KEY_CREDITS_BALANCE, balance)
            .apply()
        if (BuildConfig.DEBUG) {
            Log.d(logTag, "Saved credits balance: $balance")
        }
    }

    fun getCreditsBalance(): Int {
        val balance = preferences.getInt(KEY_CREDITS_BALANCE, 0)
        if (BuildConfig.DEBUG) {
            Log.d(logTag, "Read credits balance from session: $balance")
        }
        return balance
    }

    fun savePackageDetailsJson(packageDetailsJson: String?) {
        preferences.edit()
            .putString(KEY_PACKAGE_DETAILS_JSON, packageDetailsJson.orEmpty())
            .apply()
        if (BuildConfig.DEBUG) {
            Log.d(logTag, "Saved package details json. hasValue=${!packageDetailsJson.isNullOrBlank()}")
        }
    }

    fun getPackageDetailsJson(): String {
        val packageDetailsJson = preferences.getString(KEY_PACKAGE_DETAILS_JSON, "").orEmpty()
        if (BuildConfig.DEBUG) {
            Log.d(
                logTag,
                "Read package details json from session. hasValue=${packageDetailsJson.isNotBlank()}"
            )
        }
        return packageDetailsJson
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
        const val KEY_CREDITS_BALANCE = "credits_balance"
        const val KEY_PACKAGE_DETAILS_JSON = "package_details_json"
    }
}
