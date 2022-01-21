package com.serwylo.babybook.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.serwylo.babybook.db.daos.BookDao
import com.serwylo.babybook.db.entities.Book
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.db.entities.WikiPage
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors


@Database(entities = [Book::class, BookPage::class, WikiPage::class], version = 1)
abstract class AppDatabase : RoomDatabase() {
    abstract fun bookDao(): BookDao

    companion object {

        val executor: ExecutorService = Executors.newSingleThreadExecutor()

        private var db: AppDatabase? = null

        fun getInstance(context: Context) =
            db ?: synchronized(this) {
                db ?: buildDatabase(context).also { db = it }
            }

        private fun buildDatabase(context: Context): AppDatabase =
            Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "database.db")
                .addCallback(makeSeeder(context))
                .build()

        private fun makeSeeder(context: Context) = object: RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                executor.execute {
                    val dao = getInstance(context).bookDao()

                    val animalsId = dao.insert(Book("Animals"))
                    dao.insert(BookPage(1, "Elephant", null, null, animalsId))
                    dao.insert(BookPage(2, "Turtle", null, null, animalsId))
                    dao.insert(BookPage(3, "Sheep", null, null, animalsId))
                    dao.insert(BookPage(4, "Dog", null, null, animalsId))
                    dao.insert(BookPage(5, "Cat", null, null, animalsId))
                    dao.insert(BookPage(6, "Horse", null, null, animalsId))

                    val foodId = dao.insert(Book("Food"))
                    dao.insert(BookPage(1, "Apple", null, null, foodId))
                    dao.insert(BookPage(2, "Banana", null, null, foodId))

                    val spaceId = dao.insert(Book("Space"))
                    dao.insert(BookPage(1, "Earth", null, null, spaceId))
                    dao.insert(BookPage(2, "Sun", null, null, spaceId))
                    dao.insert(BookPage(3, "Moon", null, null, spaceId))
                    dao.insert(BookPage(4, "Mercury (planet)", null, null, spaceId))
                    dao.insert(BookPage(5, "Venus", null, null, spaceId))
                    dao.insert(BookPage(6, "Mars", null, null, spaceId))
                    dao.insert(BookPage(7, "Jupiter", null, null, spaceId))
                    dao.insert(BookPage(8, "Saturn", null, null, spaceId))
                    dao.insert(BookPage(9, "Uranus", null, null, spaceId))
                    dao.insert(BookPage(10, "Neptune", null, null, spaceId))
                    dao.insert(BookPage(11, "Pluto", null, null, spaceId))

                    val machinesId = dao.insert(Book("Machines"))
                    dao.insert(BookPage(1, "Car", null, null, machinesId))
                    dao.insert(BookPage(2, "Excavator", null, null, machinesId))
                    dao.insert(BookPage(3, "Truck", null, null, machinesId))
                    dao.insert(BookPage(4, "Bus", null, null, machinesId))
                }
            }
        }
    }
}
