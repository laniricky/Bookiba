package co.booknook.core.domain.model

enum class OrderStatus(val label: String) {
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered")
}
