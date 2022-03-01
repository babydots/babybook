package com.serwylo.babybook.db.migrations

import android.content.ContentValues
import android.database.sqlite.SQLiteDatabase
import android.util.Log
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

private const val TAG = "Migrate3To4"

val Migrate3To4 = object : Migration(3, 4) {

    override fun migrate(database: SupportSQLiteDatabase) {
        Log.i(TAG, "migrate: Migrating DB from version 3 to version 4.")
        for (book in initialBookData) {
            for (page in book.pages) {
                with(page.image) {
                    val values = ContentValues()
                    values.put("title", title)
                    values.put("name", name)
                    values.put("author", author)
                    values.put("license", license)

                    val whereClause = "filename = ?"
                    val whereArgs = arrayOf("file:///android_asset/books/$filename")

                    database.update("WikiImage", SQLiteDatabase.CONFLICT_IGNORE, values, whereClause, whereArgs)
                }
            }
        }
    }

}
