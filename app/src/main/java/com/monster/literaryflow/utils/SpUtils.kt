package com.monster.literaryflow.utils


import android.content.Context
import android.content.SharedPreferences
import android.util.Base64
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

import java.lang.reflect.InvocationTargetException
import java.lang.reflect.Method

/**
 * 本地缓存
 * * 1.缓存数据
 * * 2.获取数据
 * * 3.删除数据
 * * 4.清空数据
 */
object SPUtils {
    /**
     * 保存在手机里面的文件名
     */
    private const val FILE_NAME = "local_store"

    /**
     * 保存数据的方法，我们需要拿到保存数据的具体类型，然后根据类型调用不同的保存方法
     *
     * @param context
     * @param key
     * @param object
     */
    fun put(context: Context, key: String, `object`: Any?) {

        if ( `object` ==null){
            return
        }
        val sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val editor = sp.edit()

        when (`object`) {
            is String -> {
                editor.putString(key, `object`)
            }
            is Int -> {
                editor.putInt(key, `object`)
            }
            is Boolean -> {
                editor.putBoolean(key, `object`)
            }
            is Float -> {
                editor.putFloat(key, `object`)
            }
            is Long -> {
                editor.putLong(key, `object`)
            }
            else -> {
                editor.putString(key, `object`.toString())
            }
        }

        SharedPreferencesCompat.apply(editor)
    }
    fun saveByteArrayToSharedPreferences(context: Context, key: String, byteArray: ByteArray) {
        val base64String = Base64.encodeToString(byteArray, Base64.DEFAULT)
        val sharedPreferences = context.getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        editor.putString(key, base64String)
        editor.apply()
    }
    fun getByteArrayFromSharedPreferences(context: Context, key: String): ByteArray? {
        val sharedPreferences = context.getSharedPreferences("YourSharedPreferencesName", Context.MODE_PRIVATE)
        val base64String = sharedPreferences.getString(key, null)
        return if (base64String != null) {
            Base64.decode(base64String, Base64.DEFAULT)
        } else {
            null
        }
    }
    /**
     * 得到保存数据的方法，我们根据默认值得到保存的数据的具体类型，然后调用相对于的方法获取值
     *
     * @param context
     * @param key
     * @param defaultObject
     * @return
     */
    operator fun get(context: Context, key: String, defaultObject: Any): Any? {
        val sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)

        if (defaultObject is String) {
            return sp.getString(key, defaultObject)
        } else if (defaultObject is Int) {
            return sp.getInt(key, defaultObject)
        } else if (defaultObject is Boolean) {
            return sp.getBoolean(key, defaultObject)
        } else if (defaultObject is Float) {
            return sp.getFloat(key, defaultObject)
        } else if (defaultObject is Long) {
            return sp.getLong(key, defaultObject)
        }

        return null
    }

    /**
     * 移除某个key值已经对应的值
     *
     * @param context
     * @param key
     */
    fun remove(context: Context, key: String) {
        val sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.remove(key)
        SharedPreferencesCompat.apply(editor)
    }

    /**
     * 清除所有数据
     *
     * @param context
     */
    fun clear(context: Context) {
        val sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        val editor = sp.edit()
        editor.clear()
        SharedPreferencesCompat.apply(editor)
    }

    /**
     * 查询某个key是否已经存在
     *
     * @param context
     * @param key
     * @return
     */
    fun contains(context: Context, key: String): Boolean {
        val sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        return sp.contains(key)
    }

    /**
     * 返回所有的键值对
     *
     * @param context
     * @return
     */
    fun getAll(context: Context): Map<String, *> {
        val sp = context.getSharedPreferences(FILE_NAME, Context.MODE_PRIVATE)
        return sp.all
    }

    /**
     * 创建一个解决SharedPreferencesCompat.apply方法的一个兼容类
     *
     * @author zhy
     */
    private object SharedPreferencesCompat {
        private val sApplyMethod = findApplyMethod()

        /**
         * 反射查找apply的方法
         *
         * @return
         */
        private fun findApplyMethod(): Method? {
            try {
                val clz = SharedPreferences.Editor::class.java
                return clz.getMethod("apply")
            } catch (e: NoSuchMethodException) {
            }

            return null
        }

        /**
         * 如果找到则使用apply执行，否则使用commit
         *
         * @param editor
         */
        fun apply(editor: SharedPreferences.Editor) {
            try {
                if (sApplyMethod != null) {
                    sApplyMethod.invoke(editor)
                    return
                }
            } catch (e: IllegalArgumentException) {
            } catch (e: IllegalAccessException) {
            } catch (e: InvocationTargetException) {
            }

            editor.commit()
        }
    }


}