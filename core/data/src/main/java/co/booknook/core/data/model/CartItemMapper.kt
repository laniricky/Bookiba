package co.booknook.core.data.model

import co.booknook.core.database.model.CartItemEntity
import co.booknook.core.domain.model.CartItem

fun CartItemEntity.toDomain(): CartItem {
    return CartItem(
        bookId = bookId,
        title = title,
        author = author,
        coverUrl = coverUrl,
        priceKsh = priceKsh,
        quantity = quantity
    )
}
