package com.example

import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ProductSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestaurantOrderUnitTest {

  @Test
  fun testOrderCalculation_subtotalAndTax() {
    val items = listOf(
      OrderItem(
        productId = "p1",
        productName = "GEVINI Crown Crust Pizza",
        quantity = 2,
        selectedSize = "Regular",
        unitPrice = 1450.0,
        itemTotal = 2900.0
      ),
      OrderItem(
        productId = "p2",
        productName = "Pakola Soda",
        quantity = 3,
        selectedSize = "Can",
        unitPrice = 120.0,
        itemTotal = 360.0
      )
    )

    val subtotal = items.sumOf { it.itemTotal }
    val taxRate = 0.05 // 5% SST
    val tax = subtotal * taxRate
    val deliveryFee = 150.0
    val total = subtotal + tax + deliveryFee

    assertEquals(3260.0, subtotal, 0.001)
    assertEquals(163.0, tax, 0.001)
    assertEquals(3573.0, total, 0.001)
  }

  @Test
  fun testProductSizePriceCalculation() {
    val basePrice = 1000.0
    val sizeSmall = ProductSize(name = "Small", priceMultiplier = 0.65, additionalPrice = -200.0)
    val calculatedPrice = (basePrice * sizeSmall.priceMultiplier) + sizeSmall.additionalPrice

    assertEquals(450.0, calculatedPrice, 0.001)
  }

  @Test
  fun testOrderStatusTransitions() {
    val pending = OrderStatus.fromCode("pending")
    val preparing = OrderStatus.fromCode("preparing")
    val ready = OrderStatus.fromCode("ready")
    val completed = OrderStatus.fromCode("completed")

    assertEquals(OrderStatus.PENDING, pending)
    assertEquals(OrderStatus.PREPARING, preparing)
    assertEquals(OrderStatus.READY, ready)
    assertEquals(OrderStatus.COMPLETED, completed)
  }

  @Test
  fun testOrderModelDefaults() {
    val order = Order()
    assertEquals(OrderType.DINE_IN.code, order.orderType)
    assertEquals(OrderStatus.PENDING.code, order.status)
    assertEquals(PaymentStatus.UNPAID.code, order.paymentStatus)
    assertEquals(PaymentMethod.CASH.code, order.paymentMethod)
    assertTrue(order.items.isEmpty())
  }
}
