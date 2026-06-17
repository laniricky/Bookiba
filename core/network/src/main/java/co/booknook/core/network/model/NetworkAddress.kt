package co.booknook.core.network.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NetworkAddress(
    @SerialName("id") val id: String,
    @SerialName("label") val label: String,
    @SerialName("fullAddress") val fullAddress: String,
    @SerialName("isDefault") val isDefault: Boolean
)

@Serializable
data class NetworkAddressesResponse(
    val addresses: List<NetworkAddress> = emptyList()
)

@Serializable
data class NetworkCreateAddressRequest(
    val label: String,
    val fullAddress: String,
    val isDefault: Boolean = false
)

@Serializable
data class NetworkUpdateAddressRequest(
    val label: String? = null,
    val fullAddress: String? = null,
    val isDefault: Boolean? = null
)
