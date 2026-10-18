package com.example.data.model

import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class UserRole(val label: String, val roleId: String) {
  SUPER_ADMIN("Super Admin / Owner", "super_admin"),
  MANAGER("Branch Manager", "manager"),
  CASHIER("Cashier (POS)", "cashier"),
  KITCHEN("Kitchen Staff (KDS)", "kitchen"),
  WAITER("Waiter / Floor Staff", "waiter"),
  INVENTORY("Inventory Manager", "inventory"),
  ACCOUNTANT("Accountant", "accountant"),
  DELIVERY("Delivery Rider", "delivery"),
  CUSTOMER("Customer", "customer");

  companion object {
    fun fromId(id: String): UserRole {
      return entries.find { it.roleId == id } ?: CASHIER
    }
  }
}

@IgnoreExtraProperties
data class UserProfile(
  @DocumentId val id: String = "",
  val uid: String = "",
  val email: String = "",
  val displayName: String = "",
  val role: String = UserRole.CASHIER.roleId,
  val branchId: String = "karachi_malir_15",
  val phone: String = "",
  @ServerTimestamp val createdAt: Timestamp? = null,
  @ServerTimestamp val updatedAt: Timestamp? = null
)
