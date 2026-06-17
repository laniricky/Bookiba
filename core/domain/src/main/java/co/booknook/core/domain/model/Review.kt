package co.booknook.core.domain.model

data class Review(
    val id: String,
    val userId: String,
    val bookId: String,
    val rating: Int,       // 1–5
    val comment: String?,
    val createdAt: String
)
