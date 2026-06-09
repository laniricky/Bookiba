package co.booknook.core.network.model

import kotlinx.serialization.Serializable

@Serializable
data class NetworkReel(
    val id: String,
    val title: String,
    val videoUrl: String,
    val thumbnailUrl: String? = null,
    val bookId: String? = null,
    val bookTitle: String? = null,
    val isActive: Boolean,
    val createdAt: String
)
