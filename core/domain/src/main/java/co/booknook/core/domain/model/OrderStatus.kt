package co.booknook.core.domain.model

enum class OrderStatus(val label: String) {
    PENDING_PAYMENT("Pending Payment"),
    PROCESSING("Processing"),
    SHIPPED("Shipped"),
    DELIVERED("Delivered");

    companion object {
        fun fromBackendString(status: String?): OrderStatus {
            return when (status?.uppercase()) {
                "PENDING_PAYMENT" -> PENDING_PAYMENT
                "PROCESSING" -> PROCESSING
                "SHIPPED" -> SHIPPED
                "DELIVERED" -> DELIVERED
                else -> PROCESSING // fallback
            }
        }
    }
}
