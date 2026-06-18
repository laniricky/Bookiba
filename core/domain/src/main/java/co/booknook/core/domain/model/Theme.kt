package co.booknook.core.domain.model

data class Theme(
    val id: String,
    val name: String,
    val tags: List<String> = emptyList()
)
