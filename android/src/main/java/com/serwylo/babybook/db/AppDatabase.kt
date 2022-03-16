package com.serwylo.babybook.db

import android.content.Context
import androidx.room.AutoMigration
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.serwylo.babybook.db.daos.BookDao
import com.serwylo.babybook.db.entities.*
import com.serwylo.babybook.db.migrations.Migrate1To2
import com.serwylo.babybook.db.migrations.Migrate3To4
import com.serwylo.babybook.db.migrations.makeDatabaseSeeder


@Database(
    entities = [
        Book::class,
        BookPage::class,
        WikiPage::class,
        WikiImage::class,
        WikiSite::class,
        Settings::class,
    ],
    version = 4,
    autoMigrations = [
        AutoMigration(from = 2, to = 3),
    ]
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {

        private var db: AppDatabase? = null

        fun getInstance(context: Context) =
            db ?: synchronized(this) {
                db ?: buildDatabase(context).also { db = it }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "database.db")
                .addCallback(makeDatabaseSeeder(context))
                .addMigrations(Migrate1To2)
                .addMigrations(Migrate3To4)
                .build()
    }
}