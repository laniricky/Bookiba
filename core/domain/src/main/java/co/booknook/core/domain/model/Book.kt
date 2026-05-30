package co.booknook.core.domain.model

data class Book(
    val id: String,
    val title: String,
    val author: String,
    val description: String? = null,
    val priceKsh: Long = 0,             // price in Kenyan Shillings
    val condition: String? = null,      // e.g. "Good Condition", "Like New"
    val coverUrl: String = "",
    val imageUrls: List<String> = emptyList(),
    val category: String = "",
    val edition: String? = null,        // e.g. "1966 Edition"
    val publisher: String? = null,
    val genre: String? = null,
    val sellerId: String = "",
    val isRare: Boolean = false,
    val isFeatured: Boolean = false,
    val isStaffPick: Boolean = false,
    val tags: List<String> = emptyList()  // e.g. "Vintage", "Annotated"
)
