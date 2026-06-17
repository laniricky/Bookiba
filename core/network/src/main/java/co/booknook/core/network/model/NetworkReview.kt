package co.booknook.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkReview(
    @SerialName("id") val id: String,
    @SerialName("userId") val userId: String,
    @SerialName("bookId") val bookId: String,
    @SerialName("rating") val rating: Int,
    @SerialName("comment") val comment: String? = null,
    @SerialName("createdAt") val createdAt: String = ""
)

@Serializable
data class NetworkReviewsResponse(
    val reviews: List<NetworkReview> = emptyList()
)

@Serializable
data class NetworkSubmitReviewRequest(
    val rating: Int,
    val comment: String? = null
)
