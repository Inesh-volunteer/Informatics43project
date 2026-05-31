package com.yolojj333.heythere.utils

import android.content.SharedPreferences

/**
 * A minimal in-memory [SharedPreferences] for local unit tests, so the real
 * [LocationUtils.applyLocationNoise] persistence path can run without an Android device.
 * Test-only infrastructure — it is NOT part of the app.
 */
class FakeSharedPreferences(
    private val store: MutableMap<String, Any?> = mutableMapOf()
) : SharedPreferences {

    override fun getAll(): MutableMap<String, *> = HashMap<String, Any?>(store)

    override fun getString(key: String?, defValue: String?): String? =
        store[key] as? String ?: defValue

    @Suppress("UNCHECKED_CAST")
    override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
        store[key] as? MutableSet<String> ?: defValues

    override fun getInt(key: String?, defValue: Int): Int = store[key] as? Int ?: defValue

    override fun getLong(key: String?, defValue: Long): Long = store[key] as? Long ?: defValue

    override fun getFloat(key: String?, defValue: Float): Float = store[key] as? Float ?: defValue

    override fun getBoolean(key: String?, defValue: Boolean): Boolean =
        store[key] as? Boolean ?: defValue

    override fun contains(key: String?): Boolean = store.containsKey(key)

    override fun edit(): SharedPreferences.Editor = FakeEditor(store)

    override fun registerOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) { /* no-op */ }

    override fun unregisterOnSharedPreferenceChangeListener(
        listener: SharedPreferences.OnSharedPreferenceChangeListener?
    ) { /* no-op */ }

    private class FakeEditor(private val store: MutableMap<String, Any?>) : SharedPreferences.Editor {
        private val staged = mutableMapOf<String, Any?>()
        private val removals = mutableSetOf<String>()
        private var clearAll = false

        override fun putString(key: String?, value: String?): SharedPreferences.Editor {
            staged[key!!] = value; return this
        }

        override fun putStringSet(key: String?, values: MutableSet<String>?): SharedPreferences.Editor {
            staged[key!!] = values; return this
        }

        override fun putInt(key: String?, value: Int): SharedPreferences.Editor {
            staged[key!!] = value; return this
        }

        override fun putLong(key: String?, value: Long): SharedPreferences.Editor {
            staged[key!!] = value; return this
        }

        override fun putFloat(key: String?, value: Float): SharedPreferences.Editor {
            staged[key!!] = value; return this
        }

        override fun putBoolean(key: String?, value: Boolean): SharedPreferences.Editor {
            staged[key!!] = value; return this
        }

        override fun remove(key: String?): SharedPreferences.Editor {
            removals += key!!; return this
        }

        override fun clear(): SharedPreferences.Editor {
            clearAll = true; return this
        }

        override fun commit(): Boolean { flush(); return true }

        override fun apply() { flush() }

        private fun flush() {
            if (clearAll) store.clear()
            removals.forEach { store.remove(it) }
            store.putAll(staged)
            staged.clear(); removals.clear(); clearAll = false
        }
    }
}
