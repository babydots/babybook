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
                    dao.insert(BookPage(1, "Elephant", "file:///android_asset/books/animals/elephant.jpg", "Elephants are large grey animals with big ears, long noses and white tusks. They are the largest living land mammals. The largest elephant recorded was one shot in Angola, 1974. It weighed 27,060 pounds and stood 13 feet 8 inches tall.", animalsId))
                    dao.insert(BookPage(2, "Turtle", "file:///android_asset/books/animals/turtle.jpg", "Turtles are the reptile order Testudines. They have a special bony or cartilaginous shell developed from their ribs that acts as a shield.", animalsId))
                    dao.insert(BookPage(3, "Sheep", "file:///android_asset/books/animals/sheep.jpg", "A domestic sheep is a domesticated mammal related to wild sheep and goats. Sheep are owned and looked after by a sheep farmer. Female sheep are called ewes. Male sheep are called rams. Young sheep are called lambs.", animalsId))
                    dao.insert(BookPage(4, "Dog", "file:///android_asset/books/animals/dog.jpg", "Dogs are domesticated mammals, not natural wild animals. They were originally bred from wolves. They have been bred by humans for a long time, and were the first animals ever to be domesticated. There are different studies that suggest that this happened between 15.000 and 100.000 years before our time. The dingo is also a dog, but many dingos have become wild animals again and live independently of humans in the range where they occur.", animalsId))
                    dao.insert(BookPage(5, "Cat", "file:///android_asset/books/animals/cat.jpg", "Cats, also called domestic cats, are small, carnivorous mammals, of the family Felidae.", animalsId))
                    dao.insert(BookPage(6, "Horse", "file:///android_asset/books/animals/horse.jpg", "Horses are a diverse group of animals of the family Equidae. They are herbivores, which means they eat grass and other plants. Some plants are dangerous for them like ragwort, lemongrass and sometimes acorns.", animalsId))

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
