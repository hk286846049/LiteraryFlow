package taylor.com.util

import android.content.SharedPreferences

/**
 * SharedPreference delegation for shorter code when putting and getting value
 */
class Preference(private val sp: SharedPreferences) : SharedPreferences by sp {
    operator fun <T> set(key: String, isCommit: Boolean = false , value: T) {
        with(sp.edit()) {
            when (value) {
                is Long -> putLong(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
                is Set<*> -> (value as? Set<String>)?.let { putStringSet(key, it) }
                else -> throw IllegalArgumentException("unsupported type of value")
            }
            if (isCommit) {
                commit()
            } else {
                apply()
            }
        }
    }

    operator fun <T> get(key: String, default: T): T = with(sp) {
        when (default) {
            is Long -> getLong(key, default)
            is Int -> getInt(key, default)
            is Boolean -> getBoolean(key, default)
            is Float -> getFloat(key, default)
            is String -> getString(key, default)
            is Set<*> -> getStringSet(key, mutableSetOf())
            else -> throw IllegalArgumentException("unsupported type of value")
        } as T
    }
}

