package com.aji.wa_gateway.util

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey

object EncryptionUtil {
    private const val PREFS_NAME = "secure_prefs"
    private const val KEY_SA_KEY = "sa_key_encrypted"

    private fun getPrefs(context: Context): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()
        return EncryptedSharedPreferences.create(
            context,
            PREFS_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }

    fun saveSaKey(context: Context, saKeyJson: String) {
        getPrefs(context).edit().putString(KEY_SA_KEY, saKeyJson).apply()
    }

    fun getSaKey(context: Context): String? {
        return getPrefs(context).getString(KEY_SA_KEY, null)
    }

    fun hasSaKey(context: Context): Boolean {
        return getSaKey(context) != null
    }

    fun clearSaKey(context: Context) {
        getPrefs(context).edit().remove(KEY_SA_KEY).apply()
    }
}
