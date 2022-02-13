package com.serwylo.babybook.db.migrations

import android.content.Context
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.serwylo.babybook.db.AppDatabase
import com.serwylo.babybook.db.daos.BookDao
import com.serwylo.babybook.db.entities.Book
import com.serwylo.babybook.db.entities.BookPage
import com.serwylo.babybook.db.entities.WikiImage
import com.serwylo.babybook.db.entities.WikiPage
import com.serwylo.babybook.mediawiki.processTitle
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch

fun makeDatabaseSeeder(context: Context) = object: RoomDatabase.Callback() {
    override fun onCreate(db: SupportSQLiteDatabase) {
        val dao = AppDatabase.getInstance(context).bookDao()
        GlobalScope.launch {
            initialBookData.forEach { createBook(dao, it) }
        }
    }
}

private data class InitBook(val title: String, val pages: List<InitWikiPage>)
private data class InitWikiPage(val title: String, val description: String, val images: List<String>)

private suspend fun createBook(dao: BookDao, book: InitBook) {
    val bookId = dao.insert(Book(book.title))

    book.pages.forEachIndexed { index, wikiPage ->
        val wikiPageId = dao.insert(WikiPage(wikiPage.title, wikiPage.description))
        val wikiImageIds = wikiPage.images.map { filename ->
            dao.insert(
                WikiImage(
                    filename, // TODO: Use the proper filename from commons.
                    "file:///android_asset/books/$filename",
                    null,
                )
            )
        }

        val processedTitle = processTitle(wikiPage.title)

        dao.insert(
            BookPage(
                bookId,
                title = if (processedTitle != wikiPage.title) processedTitle else wikiPage.title,
                pageNumber = index + 1,
                wikiPageId = wikiPageId,
                wikiImageId = wikiImageIds.firstOrNull(),
            )
        )
    }
}

private val initialBookData = listOf(

    InitBook(
        "Animals",
        listOf(
            InitWikiPage(
                "Elephant",
                "Elephants are large grey animals with big ears, long noses and white tusks. They are the largest living land mammals. The largest elephant recorded was one shot in Angola, 1974. It weighed 27,060 pounds and stood 13 feet 8 inches tall.",
                listOf("animals/elephant.jpg")),
            InitWikiPage(
                "Turtle",
                "Turtles are the reptile order Testudines. They have a special bony or cartilaginous shell developed from their ribs that acts as a shield.",
                listOf("animals/turtle.jpg")),
            InitWikiPage(
                "Sheep",
                "A domestic sheep is a domesticated mammal related to wild sheep and goats. Sheep are owned and looked after by a sheep farmer. Female sheep are called ewes. Male sheep are called rams. Young sheep are called lambs.",
                listOf("animals/sheep.jpg")),
            InitWikiPage(
                "Dog",
                "Dogs are domesticated mammals, not natural wild animals. They were originally bred from wolves. They have been bred by humans for a long time, and were the first animals ever to be domesticated. There are different studies that suggest that this happened between 15.000 and 100.000 years before our time. The dingo is also a dog, but many dingos have become wild animals again and live independently of humans in the range where they occur.",
                listOf("animals/dog.jpg")),
            InitWikiPage(
                "Cat",
                "Cats, also called domestic cats, are small, carnivorous mammals, of the family Felidae.",
                listOf("animals/cat.jpg")),
            InitWikiPage(
                "Horse",
                "Horses are a diverse group of animals of the family Equidae. They are herbivores, which means they eat grass and other plants. Some plants are dangerous for them like ragwort, lemongrass and sometimes acorns.",
                listOf("animals/horse.jpg")),
            InitWikiPage(
                "Beetle",
                "Beetles, the order Coleoptera, are the largest group of insects. There are 350,000 different species of beetles which have been named: about 40% of all known insects. There are an estimated 800,000 to a million living species. Beetles live almost everywhere, though not in the ocean or in places that are very cold, such as Antarctica.",
                listOf("animals/beetle.jpg")),
            InitWikiPage(
                "Caterpillar",
                "A caterpillar is a young butterfly or moth that has just hatched out of its egg. A caterpillar is a kind of larva. When it is older, the caterpillar will turn into a pupa, and then later the pupa will turn into a butterfly.",
                listOf("animals/caterpillar.jpg")),
            InitWikiPage(
                "Butterfly",
                "A butterfly is a usually day-flying insect of the order Lepidoptera. They are grouped together in the suborder Rhopalocera. Butterflies are closely related to moths, from which they evolved. The earliest discovered fossil moth dates to 200 million years ago.",
                listOf("animals/butterfly.jpg")),
            InitWikiPage(
                "Dung beetle",
                "Dung beetles are beetles that feed partly or only on the dung of mammals. They are a kind of scarab beetle. All these species belong to the superfamily Scarabaeoidea, and most of them to the family Scarabaeidae. The subfamily Scarabaeinae alone has more than 5,000 species. There are dung-feeding beetles in other related families, such as the Geotrupidae.",
                listOf("animals/dung-beetle.jpg")),
            InitWikiPage(
                "Ant",
                "Ants are a kind of insect that lives together in large colonies. They are the family Formicidae.",
                listOf("animals/ant.jpg")),
            InitWikiPage(
                "Parrot",
                "Parrots are birds of the order Psittaciformes. There are about 372 species in 86 genera. They are found in most tropical and subtropical regions. The greatest diversity of parrots is found in South America and Australasia.",
                listOf("animals/parrot.jpg")),
            InitWikiPage(
                "Kangaroo",
                "A kangaroo is an Australian marsupial. It belongs to the genus Macropus. The common name 'kangaroo' is used for the four large species, and there are another 50 species of smaller macropods. The kangaroos are common in Australia and can also be found in New Guinea.",
                listOf("animals/kangaroo.jpg")),
            InitWikiPage(
                "Giant Panda",
                "The giant panda, Ailuropoda melanoleuca, is a bear. It lives in south central China.",
                listOf("animals/panda.jpg")),
            InitWikiPage(
                "Wombat",
                "A wombat is a marsupial in the family Vombatidae. It lives in the Australian eucalyptus forests. There are two genera with three living wombat species; the Common Wombat and the Hairy-nosed Wombats.",
                listOf("animals/wombat.jpg")),
            InitWikiPage(
                "Bilby",
                "The bilby is a rabbit-like marsupial. It lives in deserts, dry forests, dry grasslands, and dry shrubby areas in Australia. The bilby's pouch faces backwards. These big-eared, burrowing mammals were in danger of extinction, but now they are back living in New South Wales.",
                listOf("animals/bilby.jpg")),
            InitWikiPage(
                "Rabbit",
                "Rabbits are mammals of the order Lagomorpha. There are about fifty different species of rabbits and hares. The order Lagomorpha is made of rabbits, pikas and hares. Rabbits can be found in many parts of the world. They live in families and eat vegetables and hay. In the wild, rabbits live in burrows, that they dig themselves. A group of rabbits living together in a burrow is called a warren. Rabbits are famous for hopping and eating carrots.",
                listOf("animals/rabbit.jpg")),
            InitWikiPage(
                "Platypus",
                "The duck-billed platypus is a small mammal. It is one of only two monotremes to survive today. It lives in eastern Australia. The plural of platypus is just 'platypus'.",
                listOf("animals/platypus.jpg")),
        )
    ),

    InitBook(
        "Space",
        listOf(
            InitWikiPage(
                "Sun",
                "The Sun is a star which is located at the center of our solar system. It is a yellow dwarf star that gives off different types of energy such as infra-red energy, ultraviolet light, radio waves and light. It also gives off a stream of particles, which reaches Earth as \"solar wind\". The source of all this energy is nuclear fusion. Nuclear fusion is the reaction in the star which turns hydrogen into helium and makes huge amounts of energy.",
                listOf("space/sun.jpg")),
            InitWikiPage(
                "Mercury (planet)",
                "Mercury is the smallest planet in the Solar System. It is the closest planet to the sun. It makes one trip around the Sun once every 87.969 days. Mercury is bright when we can see it from Earth. It has an apparent magnitude ranging from −2.0 to 5.5. It cannot be seen easily because it is usually too close to the Sun. Because of this, Mercury can only be seen in the morning or evening twilight or when there is a solar eclipse.",
                listOf("space/mercury.jpg")),
            InitWikiPage(
                "Venus",
                "Venus is the second planet from the sun. It has a day longer than a year. The year length of Venus is 225 Earth days. The day length of Venus is 243 Earth days.",
                listOf("space/venus.jpg")),
            InitWikiPage(
                "Earth",
                "Earth is the third planet of the solar system. It is the only planet known to have life on it. The Earth formed around 4.5 billion years ago. It is one of four rocky planets on the inside of the Solar System. The other three are Mercury, Venus, and Mars.",
                listOf("space/earth.jpg")),
            InitWikiPage(
                "Moon",
                "The Moon is Earth's only natural satellite. We usually see it in the night sky. Some other planets also have moons or natural satellites.",
                listOf("space/moon.jpg")),
            InitWikiPage(
                "Mars",
                "Mars is the fourth planet from the Sun in the Solar System and the second-smallest planet. Mars is a terrestrial planet with polar ice caps of frozen water and carbon dioxide. It has the largest volcano in the Solar System, and some very large impact craters. Mars is named after the mythological Roman god of war because it appears of red color.",
                listOf("space/mars.jpg")),
            InitWikiPage(
                "Jupiter",
                "Jupiter is the largest planet in the Solar System. It is the fifth planet from the Sun. Jupiter is a gas giant, both because it is so large and made up of gas. The other gas giants are Saturn, Uranus, and Neptune.",
                listOf("space/jupiter.jpg")),
            InitWikiPage(
                "Saturn",
                "Saturn is the sixth planet from the Sun located in the Solar System. It is the second largest planet in the Solar System, after Jupiter. Saturn is one of the four gas giant planets, along with Jupiter, Uranus, and Neptune.",
                listOf("space/saturn.jpg")),
            InitWikiPage(
                "Uranus",
                "Uranus is the seventh planet from the Sun in the Solar System. It is an ice giant as Neptune. It is the third largest planet in the solar system.",
                listOf("space/uranus.jpg")),
            InitWikiPage(
                "Neptune",
                "Neptune is the eighth and last planet from the Sun in the Solar System. It is an ice giant. It is the fourth largest planet and third heaviest. Neptune has five rings which are hard to see from the Earth.",
                listOf("space/neptune.jpg")),
            InitWikiPage(
                "Pluto",
                "Pluto is a dwarf planet in the Solar System. Its formal name is 134340 Pluto. Pluto is the ninth largest body that moves around the Sun. Upon first being discovered, Pluto was considered a planet, but was reclassified to a dwarf planet in 2006. It is the largest body in the Kuiper belt.",
                listOf("space/pluto.jpg")),
            InitWikiPage(
                "Outer space",
                "Space, also known as outer space, is the near-vacuum between celestial bodies. It is where everything is found.",
                listOf("space/outer-space.jpg")),
            InitWikiPage(
                "Asteroid",
                "An asteroid is a space rock. It is a small object in the Solar System that travels around the Sun. It is like a planet but smaller. They range from very small to 600 miles across. A few asteroids have asteroid moon.",
                listOf("space/asteroid.jpg")),
            InitWikiPage(
                "Meteor",
                "A meteor is what you see when a space rock falls to Earth. It is often known as a shooting star or falling star and can be a bright light in the night sky, though most are faint. A few survive long enough to hit the ground. That is called a meteorite, and a large one sometimes leaves a hole in the ground called a crater.",
                listOf("space/meteor.jpg")),
        )
    ),

    InitBook(
        "Food",
        listOf(
            InitWikiPage(
                "Apple",
                "Apple is the edible fruit of a number of trees, known for this juicy, green or red fruits. The tree is grown worldwide. Its fruit is low-cost, and is harvested all over the world.",
                listOf("food/apple.jpg")),
            InitWikiPage(
                "Banana",
                "A banana is the common name for a type of fruit and also the name for the herbaceous plants that grow it. These plants belong to the genus Musa. They are native to the tropical region of southeast Asia.",
                listOf("food/banana.jpg")),
            InitWikiPage(
                "Orange (fruit)",
                "The term orange may refer to a number of citrus trees that produces fruit for people to eat. Oranges are a very good source of Vitamin C. Orange juice is an important part of many people's breakfast. The \"sweet orange\", which is the kind that are most often eaten today, grew first in South and East Asia but now grows in lots of parts of the world.",
                listOf("food/orange.jpg")),
            InitWikiPage(
                "Carrot",
                "The carrot is a type of plant. Many different types exist. The Latin name of the plant is usually given as Daucus carota. The plant has an edible, orange root, and usually white flowers. Wild carrots grow naturally in Eurasia. Domesticated carrots are grown for food in many parts of the world.",
                listOf("food/carrot.jpg")),
            InitWikiPage(
                "Strawberry",
                "A strawberry is a short plant in the wild strawberry genus of the rose family. The name is also used for its very common sweet edible \"fruit\" and for flavors that taste like it. The real fruit of the plant are the tiny \"seeds\" around the \"fruit\", which is actually a sweet swelling of the plant's stem around the fruit,the plant grown today is a mix of two other species of wild strawberries and was first grown in the 1750s.",
                listOf("food/strawberry.jpg")),
            InitWikiPage(
                "Blackberry",
                "The blackberry is a berry made by any of several species in the Rubus genus of the Rosaceae family. The blackberry shrub is called \"bramble\" in Britain, but in the western U.S. \"caneberry\" is the term is used for both blackberries and raspberries.",
                listOf("food/blackberry.jpg")),
            InitWikiPage(
                "Passionfruit",
                "The Passionfruit is a small, spherical fruit. It is purple when ripe, and green when not ripe. The fruit contains many small, black seeds covered with the fruit's flesh. It is tart and sweet. The seeds can be eaten on their own or used for various cooking recipes. Passion fruit is not a very common fruit in England. In Venezuela, the juice of passionfruits are common drinks.",
                listOf("food/passionfruit.jpg")),
            InitWikiPage(
                "Grape",
                "Grapes are the fruit of a woody grape vine. Grapes can be eaten raw, or used for making wine, juice, and jelly/jam. Grapes come in different colours; red, purple, white, and green are some examples. Today, grapes can be seedless, by using machines to pit the fruit. Wild grapevines are often considered a nuisance weed, as they cover other plants with their usually rather aggressive growth.",
                listOf("food/grape.jpg")),
            InitWikiPage(
                "Tomato",
                "The tomato is a culinary vegetable/botanical fruit, or specifically, a berry.",
                listOf("food/tomato.jpg")),
            InitWikiPage(
                "Cucumber",
                "The cucumber is a widely grown plant in the family Cucurbitaceae. This family also includes squash. A cucumber looks similar to a zucchini.",
                listOf("food/cucumber.jpg")),
            InitWikiPage(
                "Potato",
                "A potato is a vegetable, the Solanum tuberosum. It is a small plant with large leaves. The part of the potato that people eat is a tuber that grows under the ground.",
                listOf("food/potato.jpg")),
        )
    ),

    InitBook(
        "Machines",
        listOf(
            InitWikiPage(
                "Excavator",
                "An Excavator is a piece of heavy construction equipment. Excavators are used to dig in the ground or to move large objects. At the base of the machine, it usually has two tracks to move it. Above the tracks is a platform that rotates. The operator sits in a cab and controls the excavator. One set of controls moves the machine forward and backward. Another set of controls operates the arm and the tilting bucket. hydraulic fluid, hydraulic cylinders and hydraulic motors control the machine.",
                listOf("machines/excavator.jpg")),
            InitWikiPage(
                "Car",
                "An automobile is a land vehicle used to carry passengers. Automobiles usually have four wheels, and an engine or motor to make them move.",
                listOf("machines/car.jpg")),
            InitWikiPage(
                "Truck",
                "A truck is a motor vehicle used to transport goods. The word \"truck\" comes from the Greek word \"trochos\", which means \"wheel\". Most trucks use diesel fuel.",
                listOf("machines/truck.jpg")),
            InitWikiPage(
                "Bus",
                "A bus is a large wheeled vehicle meant to carry many passengers along with the driver. It is larger than a car. The name is a shortened version of omnibus, which means \"for everyone\" in Latin. Buses used to be called omnibuses, but people now simply call them \"buses\".",
                listOf("machines/bus.jpg")),
            InitWikiPage(
                "Sputnik 1",
                "Sputnik 1 was the first artificial satellite to go around the Earth. It was made by the Soviet Union. It was launched on 4 October 1957 at Baikonur Cosmodrome. It orbited the Earth for three months. It carried a radio transmitter. It did 1,440 orbits of the Earth during this time. It went down into Earth's atmosphere on 4 January 1958 and burned up.",
                listOf("machines/sputnik.jpg")),
            InitWikiPage(
                "Hubble Space Telescope",
                "The Hubble Space Telescope is the first big optical space observatory telescope. Being above the atmosphere means it can see the sky more clearly than a telescope on the ground. The atmosphere blurs starlight before it reaches Earth. Named after the astronomer Edwin Hubble, the Hubble Space Telescope can observe 24 hours a day. The main mirror is 94.5 inches across. The telescope can take pictures of things so far away it would be nearly impossible to see them from anywhere else.",
                listOf("machines/hubble.jpg")),
            InitWikiPage(
                "Tractor",
                "A tractor is a strong work vehicle used for farming. It usually has a seat for only the driver, and can be used to pull many different tools or trailers.",
                listOf("machines/tractor.jpg")),
            InitWikiPage(
                "Boat",
                "A boat is a vehicle used to travel on water. It is smaller than a ship and can be lifted out of the water and carried on a ship. Some boats have sails, some are powered by rowing with oars, and some use motors. Those that use steam engines are steamboats",
                listOf("machines/boat.jpg")),
            InitWikiPage(
                "Bicycle",
                "A bicycle is a small, human powered land vehicle with a seat, two wheels, two pedals, and a metal chain connected to cogs on the pedals and rear wheel. A frame gives the bike strength, and the other parts are attached to the frame. The name comes from these two words - the prefix \"bi-\" meaning two, and the suffix \"-cycle\" meaning wheel. It is powered by a person riding on top, who pushes the pedals around with his or her feet.",
                listOf("machines/bicycle.jpg")),
            InitWikiPage(
                "Space shuttle",
                "The Space Shuttle was a spacecraft which was used by the American National Aeronautics and Space Administration, or NASA. Space Shuttles were used to carry astronauts and cargo into space. Cargo such as satellites, parts of a space station or scientific instruments were taken up into space by the space shuttle. It was a new kind of spacecraft because it could be used many times.",
                listOf("machines/shuttle.jpg")),
            InitWikiPage(
                "Curiosity rover",
                "The Curiosity rover is a robotic car-sized Mars rover. It is exploring Gale Crater, which is near the equator of Mars. The rover uses a nuclear power and is part of NASA's Mars Science Laboratory.",
                listOf("machines/curiosity.jpg")),
        )
    )
)
