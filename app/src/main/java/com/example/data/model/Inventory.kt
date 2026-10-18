package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@IgnoreExtraProperties
data class Ingredient(
  @DocumentId val id: String = "",
  val name: String = "",
  val unit: String = "kg", // kg, liters, units, grams
  val currentStock: Double = 0.0,
  val minStock: Double = 5.0,
  val unitCost: Double = 0.0, // PKR
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)

@IgnoreExtraProperties
data class CashShift(
  @DocumentId val id: String = "",
  val cashierId: String = "",
  val cashierName: String = "",
  val openingAmount: Double = 0.0,
  val closingAmount: Double = 0.0,
  val cashSales: Double = 0.0,
  val cardSales: Double = 0.0,
  val digitalSales: Double = 0.0,
  val status: String = "open", // open, closed
  val notes: String = "",
  @ServerTimestamp val openedAt: Timestamp? = null,
  @ServerTimestamp val closedAt: Timestamp? = null
)
