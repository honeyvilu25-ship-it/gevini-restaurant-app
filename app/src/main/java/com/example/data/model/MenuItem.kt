package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class MenuCategory(
  @DocumentId val id: String = "",
  val name: String = "",
  val displayOrder: Int = 0,
  val icon: String = "local_pizza",
  val active: Boolean = true,
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class ProductSize(
  val name: String = "Regular",
  val priceMultiplier: Double = 1.0,
  val additionalPrice: Double = 0.0
)

@IgnoreExtraProperties
data class ProductModifier(
  val name: String = "",
  val price: Double = 0.0,
  val category: String = "Topping"
)

@IgnoreExtraProperties
data class Product(
  @DocumentId val id: String = "",
  val name: String = "",
  val description: String = "",
  val categoryId: String = "pizza",
  val price: Double = 0.0, // Base price in PKR
  val available: Boolean = true,
  val imageUrl: String = "",
  val station: String = "pizza", // pizza, grill, fryer, beverage, general
  val sizes: List<ProductSize> = emptyList(),
  val modifiers: List<ProductModifier> = emptyList(),
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
