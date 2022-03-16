package com.serwylo.babybook.db.migrations

val Migrate4To5 = object : VanillaSqlMigration(4, 5, listOf(
    """
    CREATE TABLE WikiSite (
        code TEXT NOT NULL,
        title TEXT NOT NULL,
        localisedTitle TEXT NOT NULL,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL
    )
    """,

    // cat index.tsv| awk -F '\t' -v OFS='\t' '{ print $4, $3, $2 }' | sort | awk -F '\t' '{ print "(" NR ", \047" $2 "\047, \047" $3 "\047, \047" $1 "\047)," }'
    """
    INSERT INTO WikiSite (id, localisedTitle, title, code) VALUES
        (1, 'Afrikaans', 'Afrikaans', 'af'),
        (2, 'العربية', 'Arabic', 'ar'),
        (3, 'مصرى (Maṣri)', 'Egyptian Arabic', 'arz'),
        (4, 'Asturianu', 'Asturian', 'ast'),
        (5, 'Azərbaycanca', 'Azerbaijani', 'az'),
        (6, 'تۆرکجه', 'South Azerbaijani', 'azb'),
        (7, 'Беларуская', 'Belarusian', 'be'),
        (8, 'Български', 'Bulgarian', 'bg'),
        (9, 'বাংলা', 'Bengali', 'bn'),
        (10, 'Català', 'Catalan', 'ca'),
        (11, 'Нохчийн', 'Chechen', 'ce'),
        (12, 'Sinugboanong Binisaya', 'Cebuano', 'ceb'),
        (13, 'Čeština', 'Czech', 'cs'),
        (14, 'Cymraeg', 'Welsh', 'cy'),
        (15, 'Dansk', 'Danish', 'da'),
        (16, 'Deutsch', 'German', 'de'),
        (17, 'Ελληνικά', 'Greek', 'el'),
        (18, 'English', 'English', 'en'),
        (19, 'Esperanto', 'Esperanto', 'eo'),
        (20, 'Español', 'Spanish', 'es'),
        (21, 'Eesti', 'Estonian', 'et'),
        (22, 'Euskara', 'Basque', 'eu'),
        (23, 'فارسی', 'Persian', 'fa'),
        (24, 'Suomi', 'Finnish', 'fi'),
        (25, 'Français', 'French', 'fr'),
        (26, 'Galego', 'Galician', 'gl'),
        (27, 'עברית', 'Hebrew', 'he'),
        (28, 'हिन्दी', 'Hindi', 'hi'),
        (29, 'Hrvatski', 'Croatian', 'hr'),
        (30, 'Magyar', 'Hungarian', 'hu'),
        (31, 'Հայերեն', 'Armenian', 'hy'),
        (32, 'Bahasa Indonesia', 'Indonesian', 'id'),
        (33, 'Italiano', 'Italian', 'it'),
        (34, '日本語', 'Japanese', 'ja'),
        (35, 'ქართული', 'Georgian', 'ka'),
        (36, 'Қазақша', 'Kazakh', 'kk'),
        (37, '한국어', 'Korean', 'ko'),
        (38, 'Latina', 'Latin', 'la'),
        (39, 'Lietuvių', 'Lithuanian', 'lt'),
        (40, 'Latviešu', 'Latvian', 'lv'),
        (41, 'Minangkabau', 'Minangkabau', 'min'),
        (42, 'Македонски', 'Macedonian', 'mk'),
        (43, 'Bahasa Melayu', 'Malay', 'ms'),
        (44, 'မြန်မာဘာသာ', 'Burmese', 'my'),
        (45, 'Nederlands', 'Dutch', 'nl'),
        (46, 'Nynorsk', 'Norwegian (Nynorsk)', 'nn'),
        (47, 'Norsk (Bokmål)', 'Norwegian (Bokmål)', 'no'),
        (48, 'Polski', 'Polish', 'pl'),
        (49, 'Português', 'Portuguese', 'pt'),
        (50, 'Română', 'Romanian', 'ro'),
        (51, 'Русский', 'Russian', 'ru'),
        (52, 'Srpskohrvatski / Српскохрватски', 'Serbo-Croatian', 'sh'),
        (53, 'Simple English', 'Simple English', 'simple'),
        (54, 'Slovenčina', 'Slovak', 'sk'),
        (55, 'Slovenščina', 'Slovenian', 'sl'),
        (56, 'Српски / Srpski', 'Serbian', 'sr'),
        (57, 'Svenska', 'Swedish', 'sv'),
        (58, 'தமிழ்', 'Tamil', 'ta'),
        (59, 'Тоҷикӣ', 'Tajik', 'tg'),
        (60, 'ไทย', 'Thai', 'th'),
        (61, 'Türkçe', 'Turkish', 'tr'),
        (62, 'Tatarça / Татарча', 'Tatar', 'tt'),
        (63, 'Українська', 'Ukrainian', 'uk'),
        (64, 'اردو', 'Urdu', 'ur'),
        (65, 'O‘zbek', 'Uzbek', 'uz'),
        (66, 'Tiếng Việt', 'Vietnamese', 'vi'),
        (67, 'Volapük', 'Volapük', 'vo'),
        (68, 'Winaray', 'Waray-Waray', 'war'),
        (69, '中文', 'Chinese', 'zh'),
        (70, 'Bân-lâm-gú', 'Min Nan', 'zh-min-nan'),
        (71, '粵語', 'Cantonese', 'zh-yue')
    """,

    """
    ALTER TABLE Book RENAME TO Book_tmp
    """,

    """
    CREATE TABLE Book (
        title TEXT NOT NULL,
        wikiSiteId INTEGER NOT NULL,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        FOREIGN KEY(wikiSiteId)
            REFERENCES WikiSite(id)
                ON UPDATE NO ACTION
                ON DELETE NO ACTION
    )
    """,

    """
    insert into Book (id, title, wikiSiteId)
    select
        id,
        title,
        53 -- "Simple English Wikipedia"
    from Book_tmp
    """,

    """
    drop table Book_tmp;
    """,

    """
    ALTER TABLE WikiPage RENAME TO WikiPage_tmp
    """,

    """
    CREATE TABLE WikiPage (
        title TEXT NOT NULL,
        text TEXT NOT NULL,
        imagesFetched INTEGER NOT NULL,
        wikiSiteId INTEGER NOT NULL,
        id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        FOREIGN KEY(wikiSiteId)
            REFERENCES WikiSite(id)
                ON UPDATE NO ACTION
                ON DELETE NO ACTION
    )
    """,

    """
    insert into WikiPage (id, title, text, imagesFetched, wikiSiteId)
    select
        id,
        title,
        text, 
        imagesFetched, 
        53 -- "Simple English Wikipedia"
    from WikiPage_tmp
    """,

    """
    drop table WikiPage_tmp;
    """,

    """
    CREATE TABLE IF NOT EXISTS Settings (
        wikiSiteId INTEGER NOT NULL,
        id INTEGER NOT NULL,
        PRIMARY KEY(id),
        FOREIGN KEY(wikiSiteId)
            REFERENCES WikiSite(id)
                ON UPDATE NO ACTION
                ON DELETE NO ACTION
    ) 
    """,

    """
        insert into Settings (id, wikiSiteId)
        values (
            1,
            53 -- "Simple English Wikipedia"
        );
    """
)) {}
