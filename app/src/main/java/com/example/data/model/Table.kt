package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class TableStatus(val code: String, val label: String) {
  AVAILABLE("available", "Available"),
  OCCUPIED("occupied", "Occupied"),
  RESERVED("reserved", "Reserved"),
  CLEANING("cleaning", "Needs Cleaning");

  companion object {
    fun fromCode(code: String): TableStatus = entries.find { it.code == code } ?: AVAILABLE
  }
}

@IgnoreExtraProperties
data class DiningTable(
  @DocumentId val id: String = "",
  val tableNumber: String = "",
  val capacity: Int = 4,
  val status: String = TableStatus.AVAILABLE.code,
  val currentOrderId: String = "",
  val section: String = "Main Hall",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
