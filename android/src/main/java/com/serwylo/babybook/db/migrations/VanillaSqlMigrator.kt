package com.serwylo.babybook.db.migrations

import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

abstract class VanillaSqlMigration(private val start: Int, private val end: Int, private val queries: List<String>): Migration(start, end) {

    override fun migrate(database: SupportSQLiteDatabase) {
        Log.i(TAG, "migrate: Migrating DB from version $start to version $end.")
        for (query in queries) {
            Log.d(TAG, "migrate: Running query: $query")
            database.execSQL(query);
        }
    }

    companion object {
        private const val TAG = "VanillaSQLMigration"
    }

}