package co.booknook.core.domain.model

data class Editorial(
    val id: String,
    val label: String,
    val imageUrl: String? = null,
    val queryTag: String,
    val sortOrder: Int = 0
)
