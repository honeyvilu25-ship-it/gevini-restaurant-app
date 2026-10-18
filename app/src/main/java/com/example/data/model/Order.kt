package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class OrderType(val code: String, val title: String) {
  DINE_IN("dine_in", "Dine-In"),
  TAKEAWAY("takeaway", "Takeaway"),
  DELIVERY("delivery", "Rider Delivery"),
  ONLINE("online", "Online Order");

  companion object {
    fun fromCode(code: String): OrderType = entries.find { it.code == code } ?: DINE_IN
  }
}

enum class OrderStatus(val code: String, val title: String) {
  PENDING("pending", "Pending"),
  PREPARING("preparing", "Preparing"),
  READY("ready", "Ready for Pickup/Service"),
  COMPLETED("completed", "Completed"),
  CANCELLED("cancelled", "Cancelled");

  companion object {
    fun fromCode(code: String): OrderStatus = entries.find { it.code == code } ?: PENDING
  }
}

enum class PaymentStatus(val code: String, val title: String) {
  UNPAID("unpaid", "Unpaid"),
  PAID("paid", "Paid"),
  REFUNDED("refunded", "Refunded");

  companion object {
    fun fromCode(code: String): PaymentStatus = entries.find { it.code == code } ?: UNPAID
  }
}

enum class PaymentMethod(val code: String, val title: String) {
  CASH("cash", "Cash"),
  CARD("card", "Credit/Debit Card"),
  DIGITAL("digital", "Digital (JazzCash/EasyPaisa/Raast)"),
  ONLINE_COD("online_cod", "Cash on Delivery");

  companion object {
    fun fromCode(code: String): PaymentMethod = entries.find { it.code == code } ?: CASH
  }
}

@IgnoreExtraProperties
data class OrderItem(
  val productId: String = "",
  val productName: String = "",
  val quantity: Int = 1,
  val selectedSize: String = "Regular",
  val selectedModifiers: List<String> = emptyList(),
  val unitPrice: Double = 0.0,
  val itemTotal: Double = 0.0,
  val notes: String = "",
  val station: String = "pizza"
)

@IgnoreExtraProperties
data class Order(
  @DocumentId val id: String = "",
  val orderNumber: String = "",
  val orderType: String = OrderType.DINE_IN.code,
  val status: String = OrderStatus.PENDING.code,
  val paymentStatus: String = PaymentStatus.UNPAID.code,
  val paymentMethod: String = PaymentMethod.CASH.code,
  val items: List<OrderItem> = emptyList(),
  val subtotal: Double = 0.0,
  val tax: Double = 0.0,
  val discount: Double = 0.0,
  val deliveryFee: Double = 0.0,
  val total: Double = 0.0,
  val tableNumber: String = "",
  val customerName: String = "",
  val customerPhone: String = "",
  val deliveryAddress: String = "",
  val notes: String = "",
  val branchId: String = "karachi_malir_15",
  val createdByUserId: String = "",
  val rating: Int? = null,
  val ratingFeedback: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
