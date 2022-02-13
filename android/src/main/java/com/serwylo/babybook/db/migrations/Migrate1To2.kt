package com.serwylo.babybook.db.migrations

val Migrate1To2 = object : VanillaSqlMigration(1, 2, listOf(
    """
    create table WikiImage
    (
        name       TEXT,
        filename   TEXT,
        wikiPageId INTEGER
            references WikiPage(id)
                on delete cascade,
        id         INTEGER not null
            primary key autoincrement
    )
    """,

    """
    insert into WikiImage (name, filename)
    select imagePath, imagePath from BookPage
    """,

    """
    drop table WikiPage
    """,

    """
    create table WikiPage
    (
        title         TEXT    not null,
        text          TEXT    not null,
        imagesFetched INTEGER not null,
        id            INTEGER not null
            primary key autoincrement
    )
    """,

    """
    insert into WikiPage (title, text, imagesFetched)
    select distinct
        wikiPageTitle,
        wikiPageText,
        0
    from BookPage
    where
        wikiPageTitle is not null
    """,

    """
    create table BookPage_tmp
    (
        bookId      INTEGER not null,
        pageNumber  INTEGER not null,
        title       TEXT,
        text        TEXT,
        wikiImageId INTEGER,
        wikiPageId  INTEGER,
        id          INTEGER not null
            primary key autoincrement
    )
    """,

    """
    insert into BookPage_tmp (
        id,
        bookId,
        pageNumber,
        title,
        text,
        wikiImageId,
        wikiPageId
    )
    select
        id,
        bookId,
        pageNumber,
        pageTitle,
        pageText,
        (select id from WikiImage where filename = imagePath),
        (select id from WikiPage where title = wikiPageTitle)
    from BookPage
    """,

    """
    drop table BookPage
    """,

    """
    create table BookPage
    (
        bookId      INTEGER not null
            references Book(id)
                on delete cascade,
        pageNumber  INTEGER not null,
        title       TEXT,
        text        TEXT,
        wikiImageId INTEGER
            references WikiImage(id)
                on delete set null,
        wikiPageId  INTEGER
            references WikiPage(id)
                on delete set null,
        id INTEGER not null
            primary key autoincrement
    )
    """,

    """
    insert into BookPage (bookId, pageNumber, title, text, wikiImageId, wikiPageId, id)
    select bookId, pageNumber, title, text, wikiImageId, wikiPageId, id from BookPage_tmp;
    """,

    """
    drop table BookPage_tmp;
    """,
)) {}