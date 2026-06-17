package co.booknook.core.domain.model

data class Address(
    val id: String,
    val label: String,
    val fullAddress: String,
    val isDefault: Boolean
)
