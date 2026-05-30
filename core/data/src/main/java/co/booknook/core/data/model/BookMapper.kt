package co.booknook.core.data.model

import co.booknook.core.database.model.BookEntity
import co.booknook.core.domain.model.Book
import co.booknook.core.network.model.NetworkBook

fun NetworkBook.toEntity(): BookEntity {
    return BookEntity(
        id = id,
        title = title,
        author = author,
        description = description,
        price = price,
        condition = condition,
        coverImageUrl = coverUrl,
        isAvailable = true,
        sellerId = sellerId,
        createdAt = System.currentTimeMillis()
    )
}

fun BookEntity.toDomain(): Book {
    return Book(
        id = id,
        title = title,
        author = author,
        description = description,
        priceKsh = price.toLong(),
        condition = condition,
        coverUrl = coverImageUrl,
        category = "General", // Fallback since it's not in DB entity
        sellerId = sellerId
    )
}

fun NetworkBook.toDomain(): Book {
    return Book(
        id = id,
        title = title,
        author = author,
        description = description,
        priceKsh = price.toLong(),
        condition = condition,
        coverUrl = coverUrl,
        category = "General", // Fallback since it's not in Network API currently
        sellerId = sellerId
    )
}
