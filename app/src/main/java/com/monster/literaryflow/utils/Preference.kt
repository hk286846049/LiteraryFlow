package taylor.com.util

import android.content.SharedPreferences

/**
 * SharedPreference delegation for shorter code when putting and getting value
 */

import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

class Preference(private val sp: SharedPreferences) : SharedPreferences by sp {
    private val gson = Gson()

    operator fun <T> set(key: String, isCommit: Boolean = false, value: T) {
        with(sp.edit()) {
            when (value) {
                is Long -> putLong(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Float -> putFloat(key, value)
                is String -> putString(key, value)
                is Set<*> -> (value as? Set<String>)?.let { putStringSet(key, it) }
                is List<*> -> {
                    // 将 List 转为 JSON 字符串
                    putString(key, gson.toJson(value))
                }
                else -> throw IllegalArgumentException("unsupported type of value")
            }
            if (isCommit) {
                commit()
            } else {
                apply()
            }
        }
    }

    @Suppress("UNCHECKED_CAST")
    operator fun <T> get(key: String, default: T): T = with(sp) {
        when (default) {
            is Long -> getLong(key, default)
            is Int -> getInt(key, default)
            is Boolean -> getBoolean(key, default)
            is Float -> getFloat(key, default)
            is String -> getString(key, default)
            is Set<*> -> getStringSet(key, mutableSetOf())
            is List<*> -> {
                // 从 JSON 字符串解析 List
                val json = getString(key, null)
                if (json != null) {
                    gson.fromJson<List<String>>(json, object : TypeToken<List<String>>() {}.type)
                } else {
                    default
                }
            }
            else -> throw IllegalArgumentException("unsupported type of value")
        } as T
    }
}

