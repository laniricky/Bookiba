package co.booknook.core.data.model

import co.booknook.core.database.model.BookEntity
import co.booknook.core.domain.model.Book
import co.booknook.core.network.model.NetworkBook

fun NetworkBook.toEntity(): BookEntity {
    return BookEntity(
        id = id,
        title = title,
        author = author,
        description = description ?: "",
        price = priceKsh.toDouble(),
        condition = condition ?: "Good",
        coverImageUrl = coverUrl,
        isAvailable = true,
        sellerId = "",
        createdAt = System.currentTimeMillis(),
        averageRating = averageRating ?: 0.0,
        reviewCount = reviewCount ?: 0
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
        sellerId = sellerId,
        averageRating = averageRating,
        reviewCount = reviewCount
    )
}

fun NetworkBook.toDomain(): Book {
    return Book(
        id = id,
        title = title,
        author = author,
        description = description ?: "",
        priceKsh = priceKsh,
        condition = condition ?: "Good",
        coverUrl = coverUrl,
        imageUrls = imageUrls,
        category = category,
        sellerId = "",
        averageRating = averageRating ?: 0.0,
        reviewCount = reviewCount ?: 0
    )
}
