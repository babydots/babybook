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

                    val spaceId = dao.insert(Book("Space"))
                    dao.insert(BookPage(1, "Earth", "file:///android_asset/books/space/earth.jpg", "Earth is the third planet of the solar system. It is the only planet known to have life on it. The Earth formed around 4.5 billion years ago. It is one of four rocky planets on the inside of the Solar System. The other three are Mercury, Venus, and Mars.", spaceId))
                    dao.insert(BookPage(2, "Sun", "file:///android_asset/books/space/sun.jpg", "The Sun is a star which is located at the center of our solar system. It is a yellow dwarf star that gives off different types of energy such as infra-red energy, ultraviolet light, radio waves and light. It also gives off a stream of particles, which reaches Earth as \"solar wind\". The source of all this energy is nuclear fusion. Nuclear fusion is the reaction in the star which turns hydrogen into helium and makes huge amounts of energy.", spaceId))
                    dao.insert(BookPage(3, "Moon", "file:///android_asset/books/space/moon.jpg", "The Moon is Earth's only natural satellite. We usually see it in the night sky. Some other planets also have moons or natural satellites.", spaceId))
                    dao.insert(BookPage(4, "Mercury (planet)", "file:///android_asset/books/space/mercury.jpg", "Mercury is the smallest planet in the Solar System. It is the closest planet to the sun. It makes one trip around the Sun once every 87.969 days. Mercury is bright when we can see it from Earth. It has an apparent magnitude ranging from −2.0 to 5.5. It cannot be seen easily because it is usually too close to the Sun. Because of this, Mercury can only be seen in the morning or evening twilight or when there is a solar eclipse.", spaceId))
                    dao.insert(BookPage(5, "Venus", "file:///android_asset/books/space/venus.jpg", "Venus is the second planet from the sun. It has a day longer than a year. The year length of Venus is 225 Earth days. The day length of Venus is 243 Earth days.", spaceId))
                    dao.insert(BookPage(6, "Mars", "file:///android_asset/books/space/mars.jpg", "Mars is the fourth planet from the Sun in the Solar System and the second-smallest planet. Mars is a terrestrial planet with polar ice caps of frozen water and carbon dioxide. It has the largest volcano in the Solar System, and some very large impact craters. Mars is named after the mythological Roman god of war because it appears of red color.", spaceId))
                    dao.insert(BookPage(7, "Jupiter", "file:///android_asset/books/space/jupiter.jpg", "Jupiter is the largest planet in the Solar System. It is the fifth planet from the Sun. Jupiter is a gas giant, both because it is so large and made up of gas. The other gas giants are Saturn, Uranus, and Neptune.", spaceId))
                    dao.insert(BookPage(8, "Saturn", "file:///android_asset/books/space/saturn.jpg", "Saturn is the sixth planet from the Sun located in the Solar System. It is the second largest planet in the Solar System, after Jupiter. Saturn is one of the four gas giant planets, along with Jupiter, Uranus, and Neptune.", spaceId))
                    dao.insert(BookPage(9, "Uranus", "file:///android_asset/books/space/uranus.jpg", "Uranus is the seventh planet from the Sun in the Solar System. It is an ice giant as Neptune. It is the third largest planet in the solar system.", spaceId))
                    dao.insert(BookPage(10, "Neptune", "file:///android_asset/books/space/neptune.jpg", "Neptune is the eighth and last planet from the Sun in the Solar System. It is an ice giant. It is the fourth largest planet and third heaviest. Neptune has five rings which are hard to see from the Earth.", spaceId))
                    dao.insert(BookPage(11, "Pluto", "file:///android_asset/books/space/pluto.jpg", "Pluto is a dwarf planet in the Solar System. Its formal name is 134340 Pluto. Pluto is the ninth largest body that moves around the Sun. Upon first being discovered, Pluto was considered a planet, but was reclassified to a dwarf planet in 2006. It is the largest body in the Kuiper belt.", spaceId))
                    dao.insert(BookPage(12, "Outer space", "file:///android_asset/books/space/outer-space.jpg", "Space, also known as outer space, is the near-vacuum between celestial bodies. It is where everything is found.", spaceId))
                    dao.insert(BookPage(13, "Asteroid", "file:///android_asset/books/space/asteroid.jpg", "An asteroid is a space rock. It is a small object in the Solar System that travels around the Sun. It is like a planet but smaller. They range from very small to 600 miles across. A few asteroids have asteroid moon.", spaceId))
                    dao.insert(BookPage(14, "Meteor", "file:///android_asset/books/space/meteor.jpg", "A meteor is what you see when a space rock falls to Earth. It is often known as a shooting star or falling star and can be a bright light in the night sky, though most are faint. A few survive long enough to hit the ground. That is called a meteorite, and a large one sometimes leaves a hole in the ground called a crater.", spaceId))

                    val foodId = dao.insert(Book("Food"))
                    dao.insert(BookPage(1, "Apple", "file:///android_asset/books/food/apple.jpg", "Apple is the edible fruit of a number of trees, known for this juicy, green or red fruits. The tree is grown worldwide. Its fruit is low-cost, and is harvested all over the world.", foodId))
                    dao.insert(BookPage(2, "Banana", "file:///android_asset/books/food/banana.jpg", "A banana is the common name for a type of fruit and also the name for the herbaceous plants that grow it. These plants belong to the genus Musa. They are native to the tropical region of southeast Asia.", foodId))
                    dao.insert(BookPage(3, "Orange (fruit)", "file:///android_asset/books/food/orange.jpg", "The term orange may refer to a number of citrus trees that produces fruit for people to eat. Oranges are a very good source of Vitamin C. Orange juice is an important part of many people's breakfast. The \"sweet orange\", which is the kind that are most often eaten today, grew first in South and East Asia but now grows in lots of parts of the world.", foodId))
                    dao.insert(BookPage(4, "Carrot", "file:///android_asset/books/food/carrot.jpg", "The carrot is a type of plant. Many different types exist. The Latin name of the plant is usually given as Daucus carota. The plant has an edible, orange root, and usually white flowers. Wild carrots grow naturally in Eurasia. Domesticated carrots are grown for food in many parts of the world.", foodId))
                    dao.insert(BookPage(5, "Strawberry", "file:///android_asset/books/food/strawberry.jpg", "A strawberry is a short plant in the wild strawberry genus of the rose family. The name is also used for its very common sweet edible \"fruit\" and for flavors that taste like it. The real fruit of the plant are the tiny \"seeds\" around the \"fruit\", which is actually a sweet swelling of the plant's stem around the fruit,the plant grown today is a mix of two other species of wild strawberries and was first grown in the 1750s.", foodId))
                    dao.insert(BookPage(6, "Blackberry", "file:///android_asset/books/food/blackberry.jpg", "The blackberry is a berry made by any of several species in the Rubus genus of the Rosaceae family. The blackberry shrub is called \"bramble\" in Britain, but in the western U.S. \"caneberry\" is the term is used for both blackberries and raspberries.", foodId))
                    dao.insert(BookPage(7, "Passionfruit", "file:///android_asset/books/food/passionfruit.jpg", "The Passionfruit is a small, spherical fruit. It is purple when ripe, and green when not ripe. The fruit contains many small, black seeds covered with the fruit's flesh. It is tart and sweet. The seeds can be eaten on their own or used for various cooking recipes. Passion fruit is not a very common fruit in England. In Venezuela, the juice of passionfruits are common drinks.", foodId))
                    dao.insert(BookPage(8, "Grape", "file:///android_asset/books/food/grape.jpg", "Grapes are the fruit of a woody grape vine. Grapes can be eaten raw, or used for making wine, juice, and jelly/jam. Grapes come in different colours; red, purple, white, and green are some examples. Today, grapes can be seedless, by using machines to pit the fruit. Wild grapevines are often considered a nuisance weed, as they cover other plants with their usually rather aggressive growth.", foodId))
                    dao.insert(BookPage(9, "Tomato", "file:///android_asset/books/food/tomato.jpg", "The tomato is a culinary vegetable/botanical fruit, or specifically, a berry.", foodId))
                    dao.insert(BookPage(10, "Cucumber", "file:///android_asset/books/food/cucumber.jpg", "The cucumber is a widely grown plant in the family Cucurbitaceae. This family also includes squash. A cucumber looks similar to a zucchini.", foodId))
                    dao.insert(BookPage(11, "Potato", "file:///android_asset/books/food/potato.jpg", "A potato is a vegetable, the Solanum tuberosum. It is a small plant with large leaves. The part of the potato that people eat is a tuber that grows under the ground.", foodId))

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
