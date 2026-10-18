package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.R
import com.example.data.model.CashShift
import com.example.data.model.DiningTable
import com.example.data.model.Ingredient
import com.example.data.model.MenuCategory
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ProductModifier
import com.example.data.model.ProductSize
import com.example.data.model.TableStatus
import com.example.data.model.UserProfile
import com.example.data.model.UserRole
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class RestaurantRepository(private val db: FirebaseFirestore) {

  companion object {
    private const val TAG = "RestaurantRepo"

    @Volatile
    private var instance: RestaurantRepository? = null

    fun getInstance(context: Context): RestaurantRepository {
      return instance ?: synchronized(this) {
        val dbId = context.applicationContext.getString(R.string.firestore_database_id)
        val firestore = FirebaseFirestore.getInstance(dbId)
        RestaurantRepository(firestore).also { instance = it }
      }
    }
  }

  private val auth = Firebase.auth

  fun currentUserId(): String = auth.currentUser?.uid ?: ""
  fun currentUserEmail(): String = auth.currentUser?.email ?: ""
  fun currentUserName(): String = auth.currentUser?.displayName ?: "Staff Member"

  // -------------------------------------------------------------
  // Categories & Products
  // -------------------------------------------------------------
  fun observeCategories(): Flow<List<MenuCategory>> = callbackFlow {
    val listener = db.collection("menuCategories")
      .orderBy("displayOrder", Query.Direction.ASCENDING)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Error listening for categories: ${error.message}")
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.documents?.mapNotNull { doc ->
          doc.toObject(MenuCategory::class.java)?.copy(id = doc.id)
        } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  fun observeProducts(): Flow<List<Product>> = callbackFlow {
    val listener = db.collection("products")
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Error listening for products: ${error.message}")
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.documents?.mapNotNull { doc ->
          doc.toObject(Product::class.java)?.copy(id = doc.id)
        } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  suspend fun saveProduct(product: Product): Result<Unit> = runCatching {
    val docRef = if (product.id.isNotBlank()) {
      db.collection("products").document(product.id)
    } else {
      db.collection("products").document()
    }
    val payload = hashMapOf<String, Any>(
      "name" to product.name,
      "description" to product.description,
      "categoryId" to product.categoryId,
      "price" to product.price,
      "available" to product.available,
      "imageUrl" to product.imageUrl,
      "station" to product.station,
      "sizes" to product.sizes.map {
        mapOf("name" to it.name, "priceMultiplier" to it.priceMultiplier, "additionalPrice" to it.additionalPrice)
      },
      "modifiers" to product.modifiers.map {
        mapOf("name" to it.name, "price" to it.price, "category" to it.category)
      },
      "updatedAt" to FieldValue.serverTimestamp()
    )
    if (product.id.isBlank()) {
      payload["createdAt"] = FieldValue.serverTimestamp()
    }
    docRef.set(payload).await()
  }

  suspend fun toggleProductAvailability(productId: String, available: Boolean): Result<Unit> = runCatching {
    db.collection("products").document(productId)
      .update(mapOf("available" to available, "updatedAt" to FieldValue.serverTimestamp()))
      .await()
  }

  // -------------------------------------------------------------
  // Orders
  // -------------------------------------------------------------
  fun observeOrders(): Flow<List<Order>> = callbackFlow {
    val listener = db.collection("orders")
      .orderBy("createdAt", Query.Direction.DESCENDING)
      .limit(100)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Error listening for orders: ${error.message}")
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.documents?.mapNotNull { doc ->
          doc.toObject(Order::class.java)?.copy(id = doc.id)
        } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  suspend fun createOrder(order: Order): Result<String> = runCatching {
    val docRef = db.collection("orders").document()
    val orderNum = if (order.orderNumber.isNotBlank()) order.orderNumber else generateOrderNumber()
    
    val itemsPayload = order.items.map { item ->
      mapOf(
        "productId" to item.productId,
        "productName" to item.productName,
        "quantity" to item.quantity,
        "selectedSize" to item.selectedSize,
        "selectedModifiers" to item.selectedModifiers,
        "unitPrice" to item.unitPrice,
        "itemTotal" to item.itemTotal,
        "notes" to item.notes,
        "station" to item.station
      )
    }

    val payload = hashMapOf<String, Any>(
      "orderNumber" to orderNum,
      "orderType" to order.orderType,
      "status" to order.status,
      "paymentStatus" to order.paymentStatus,
      "paymentMethod" to order.paymentMethod,
      "items" to itemsPayload,
      "subtotal" to order.subtotal,
      "tax" to order.tax,
      "discount" to order.discount,
      "deliveryFee" to order.deliveryFee,
      "total" to order.total,
      "tableNumber" to order.tableNumber,
      "customerName" to order.customerName,
      "customerPhone" to order.customerPhone,
      "deliveryAddress" to order.deliveryAddress,
      "notes" to order.notes,
      "branchId" to order.branchId,
      "createdByUserId" to (auth.currentUser?.uid ?: "online_guest"),
      "createdAt" to FieldValue.serverTimestamp(),
      "updatedAt" to FieldValue.serverTimestamp()
    )

    docRef.set(payload).await()

    // If assigned to a table, update table status to occupied
    if (order.tableNumber.isNotBlank()) {
      assignTableOrder(order.tableNumber, docRef.id)
    }

    docRef.id
  }

  suspend fun updateOrderStatus(orderId: String, newStatus: String): Result<Unit> = runCatching {
    db.collection("orders").document(orderId)
      .update(mapOf("status" to newStatus, "updatedAt" to FieldValue.serverTimestamp()))
      .await()
  }

  suspend fun updateOrderPayment(orderId: String, status: String, method: String): Result<Unit> = runCatching {
    db.collection("orders").document(orderId)
      .update(mapOf(
        "paymentStatus" to status,
        "paymentMethod" to method,
        "updatedAt" to FieldValue.serverTimestamp()
      ))
      .await()
  }

  private suspend fun assignTableOrder(tableNumber: String, orderId: String) {
    try {
      val query = db.collection("tables").whereEqualTo("tableNumber", tableNumber).get().await()
      for (doc in query.documents) {
        doc.reference.update(mapOf(
          "status" to TableStatus.OCCUPIED.code,
          "currentOrderId" to orderId,
          "updatedAt" to FieldValue.serverTimestamp()
        )).await()
      }
    } catch (e: Exception) {
      Log.e(TAG, "Error updating table status: ${e.message}")
    }
  }

  private fun generateOrderNumber(): String {
    val randomDigits = (1000..9999).random()
    return "GV-$randomDigits"
  }

  // -------------------------------------------------------------
  // Tables
  // -------------------------------------------------------------
  fun observeTables(): Flow<List<DiningTable>> = callbackFlow {
    val listener = db.collection("tables")
      .orderBy("tableNumber", Query.Direction.ASCENDING)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Error listening for tables: ${error.message}")
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.documents?.mapNotNull { doc ->
          doc.toObject(DiningTable::class.java)?.copy(id = doc.id)
        } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  suspend fun updateTableStatus(tableId: String, status: String, currentOrderId: String = ""): Result<Unit> = runCatching {
    db.collection("tables").document(tableId)
      .update(mapOf(
        "status" to status,
        "currentOrderId" to currentOrderId,
        "updatedAt" to FieldValue.serverTimestamp()
      ))
      .await()
  }

  // -------------------------------------------------------------
  // Inventory
  // -------------------------------------------------------------
  fun observeIngredients(): Flow<List<Ingredient>> = callbackFlow {
    val listener = db.collection("ingredients")
      .orderBy("name", Query.Direction.ASCENDING)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Error listening for ingredients: ${error.message}")
          trySend(emptyList())
          return@addSnapshotListener
        }
        val list = snapshot?.documents?.mapNotNull { doc ->
          doc.toObject(Ingredient::class.java)?.copy(id = doc.id)
        } ?: emptyList()
        trySend(list)
      }
    awaitClose { listener.remove() }
  }

  suspend fun adjustIngredientStock(ingredientId: String, delta: Double): Result<Unit> = runCatching {
    db.collection("ingredients").document(ingredientId)
      .update(mapOf(
        "currentStock" to FieldValue.increment(delta),
        "updatedAt" to FieldValue.serverTimestamp()
      ))
      .await()
  }

  // -------------------------------------------------------------
  // Cashier Shifts
  // -------------------------------------------------------------
  fun observeCurrentShift(): Flow<CashShift?> = callbackFlow {
    val listener = db.collection("cashShifts")
      .whereEqualTo("status", "open")
      .limit(1)
      .addSnapshotListener { snapshot, error ->
        if (error != null) {
          Log.w(TAG, "Error listening for shift: ${error.message}")
          trySend(null)
          return@addSnapshotListener
        }
        val shift = snapshot?.documents?.firstOrNull()?.let { doc ->
          doc.toObject(CashShift::class.java)?.copy(id = doc.id)
        }
        trySend(shift)
      }
    awaitClose { listener.remove() }
  }

  suspend fun openShift(openingAmount: Double, notes: String = ""): Result<String> = runCatching {
    val docRef = db.collection("cashShifts").document()
    val payload = hashMapOf<String, Any>(
      "cashierId" to (auth.currentUser?.uid ?: "cashier_1"),
      "cashierName" to currentUserName(),
      "openingAmount" to openingAmount,
      "closingAmount" to 0.0,
      "cashSales" to 0.0,
      "cardSales" to 0.0,
      "digitalSales" to 0.0,
      "status" to "open",
      "notes" to notes,
      "openedAt" to FieldValue.serverTimestamp()
    )
    docRef.set(payload).await()
    docRef.id
  }

  suspend fun closeShift(shiftId: String, closingAmount: Double, notes: String = ""): Result<Unit> = runCatching {
    db.collection("cashShifts").document(shiftId)
      .update(mapOf(
        "closingAmount" to closingAmount,
        "status" to "closed",
        "notes" to notes,
        "closedAt" to FieldValue.serverTimestamp()
      ))
      .await()
  }

  // -------------------------------------------------------------
  // Database Seeding for GEVINI Malir 15
  // -------------------------------------------------------------
  suspend fun seedDatabase(): Result<Unit> = runCatching {
    val categoriesCol = db.collection("menuCategories")
    val currentCats = categoriesCol.get().await()
    if (!currentCats.isEmpty) return@runCatching

    // 1. Categories
    val pizzaCat = categoriesCol.add(mapOf("name" to "Pizzas", "displayOrder" to 1, "icon" to "local_pizza", "active" to true, "createdAt" to FieldValue.serverTimestamp())).await()
    val burgerCat = categoriesCol.add(mapOf("name" to "Burgers & Fast Food", "displayOrder" to 2, "icon" to "lunch_dining", "active" to true, "createdAt" to FieldValue.serverTimestamp())).await()
    val sidesCat = categoriesCol.add(mapOf("name" to "Appetizers & Sides", "displayOrder" to 3, "icon" to "bakery_dining", "active" to true, "createdAt" to FieldValue.serverTimestamp())).await()
    val drinksCat = categoriesCol.add(mapOf("name" to "Drinks & Beverages", "displayOrder" to 4, "icon" to "local_drink", "active" to true, "createdAt" to FieldValue.serverTimestamp())).await()

    // 2. Products
    val productsCol = db.collection("products")
    
    val standardSizes = listOf(
      mapOf("name" to "Small (8\")", "priceMultiplier" to 0.65, "additionalPrice" to -400.0),
      mapOf("name" to "Regular (10\")", "priceMultiplier" to 1.0, "additionalPrice" to 0.0),
      mapOf("name" to "Large (13\")", "priceMultiplier" to 1.45, "additionalPrice" to 550.0),
      mapOf("name" to "Jumbo (16\")", "priceMultiplier" to 1.85, "additionalPrice" to 1050.0)
    )

    val pizzaModifiers = listOf(
      mapOf("name" to "Extra Mozzarella Cheese", "price" to 250.0, "category" to "Cheese"),
      mapOf("name" to "Stuffed Cheesy Crust", "price" to 350.0, "category" to "Crust"),
      mapOf("name" to "Black Olives & Jalapenos", "price" to 150.0, "category" to "Toppings"),
      mapOf("name" to "Mushroom Medley", "price" to 180.0, "category" to "Toppings"),
      mapOf("name" to "Extra Garlic Mayo Dip", "price" to 90.0, "category" to "Dips")
    )

    // Items
    productsCol.add(mapOf(
      "name" to "GEVINI Crown Crust Pizza",
      "description" to "Iconic stuffed golden crown pastry crust loaded with grilled chicken chunks, rich mozzarella, capsicum, and signature creamy garlic herb sauce.",
      "categoryId" to pizzaCat.id,
      "price" to 1450.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "pizza",
      "sizes" to standardSizes,
      "modifiers" to pizzaModifiers,
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    productsCol.add(mapOf(
      "name" to "Malir Special Pizza",
      "description" to "Local Malir favorite! Double layer of spicy tikka & fajita chicken, black olives, sweet corn, mushrooms, and jalapeño kick.",
      "categoryId" to pizzaCat.id,
      "price" to 1500.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "pizza",
      "sizes" to standardSizes,
      "modifiers" to pizzaModifiers,
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    productsCol.add(mapOf(
      "name" to "Chicken Tikka Pizza",
      "description" to "Traditional spicy tandoori chicken tikka cubes, fresh sliced onion rings, cilantro, and golden mozzarella.",
      "categoryId" to pizzaCat.id,
      "price" to 1350.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "pizza",
      "sizes" to standardSizes,
      "modifiers" to pizzaModifiers,
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    productsCol.add(mapOf(
      "name" to "Chicken Fajita Sicilian",
      "description" to "Smoked fajita chicken, crispy green bell peppers, mushrooms, and authentic spicy Sicilian herb blend.",
      "categoryId" to pizzaCat.id,
      "price" to 1400.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "pizza",
      "sizes" to standardSizes,
      "modifiers" to pizzaModifiers,
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    productsCol.add(mapOf(
      "name" to "Zinger Crunch Burger",
      "description" to "Thick, ultra-crispy battered chicken breast with iceberg lettuce, pickled jalapeño, and spicy garlic sauce in toasted brioche.",
      "categoryId" to burgerCat.id,
      "price" to 650.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "grill",
      "sizes" to listOf(
        mapOf("name" to "Single Burger", "priceMultiplier" to 1.0, "additionalPrice" to 0.0),
        mapOf("name" to "Make it a Meal (+ Fries & Drink)", "priceMultiplier" to 1.4, "additionalPrice" to 250.0)
      ),
      "modifiers" to listOf(
        mapOf("name" to "Cheese Slice", "price" to 90.0, "category" to "Addon"),
        mapOf("name" to "Extra Zinger Patty", "price" to 320.0, "category" to "Addon")
      ),
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    productsCol.add(mapOf(
      "name" to "Beef Gourmet Burger",
      "description" to "Charcoal-grilled juicy 180g beef patty, caramelized onions, melted cheddar, and smoky barbecue relish.",
      "categoryId" to burgerCat.id,
      "price" to 850.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "grill",
      "sizes" to emptyList<Map<String, Any>>(),
      "modifiers" to listOf(mapOf("name" to "Bacon Strip", "price" to 150.0, "category" to "Addon")),
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    productsCol.add(mapOf(
      "name" to "Cheesy Garlic Bread (4 pcs)",
      "description" to "Freshly baked artisan baguette infused with garlic butter and oven-melted bubbling mozzarella cheese.",
      "categoryId" to sidesCat.id,
      "price" to 450.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "pizza",
      "sizes" to emptyList<Map<String, Any>>(),
      "modifiers" to emptyList<Map<String, Any>>(),
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    productsCol.add(mapOf(
      "name" to "Spicy Buffalo Wings (6 pcs)",
      "description" to "Crispy jumbo wings tossed in fiery red hot pepper sauce, served with cool ranch dip.",
      "categoryId" to sidesCat.id,
      "price" to 550.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "fryer",
      "sizes" to emptyList<Map<String, Any>>(),
      "modifiers" to emptyList<Map<String, Any>>(),
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    productsCol.add(mapOf(
      "name" to "GEVINI Loaded Fries Platter",
      "description" to "Crinkle-cut seasoned fries topped with spicy chicken cubes, melted cheese sauce, and jalapeños.",
      "categoryId" to sidesCat.id,
      "price" to 600.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "fryer",
      "sizes" to emptyList<Map<String, Any>>(),
      "modifiers" to emptyList<Map<String, Any>>(),
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    productsCol.add(mapOf(
      "name" to "Pakola Ice Cream Soda (Can)",
      "description" to "Traditional Pakistan green ice cream soda, chilled to perfection.",
      "categoryId" to drinksCat.id,
      "price" to 120.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "beverage",
      "sizes" to emptyList<Map<String, Any>>(),
      "modifiers" to emptyList<Map<String, Any>>(),
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    productsCol.add(mapOf(
      "name" to "Fresh Mint Lemonade",
      "description" to "Refreshing hand-squeezed lemon, garden fresh mint, black rock salt, and crushed ice.",
      "categoryId" to drinksCat.id,
      "price" to 250.0,
      "available" to true,
      "imageUrl" to "",
      "station" to "beverage",
      "sizes" to emptyList<Map<String, Any>>(),
      "modifiers" to emptyList<Map<String, Any>>(),
      "createdAt" to FieldValue.serverTimestamp()
    )).await()

    // 3. Tables
    val tablesCol = db.collection("tables")
    for (i in 1..8) {
      tablesCol.add(mapOf(
        "tableNumber" to "T-$i",
        "capacity" to if (i % 2 == 0) 6 else 4,
        "status" to TableStatus.AVAILABLE.code,
        "currentOrderId" to "",
        "section" to if (i <= 4) "Ground Floor" else "Family Hall",
        "createdAt" to FieldValue.serverTimestamp()
      )).await()
    }

    // 4. Ingredients
    val ingredientsCol = db.collection("ingredients")
    ingredientsCol.add(mapOf("name" to "Pizza Flour (Fine 00)", "unit" to "kg", "currentStock" to 45.0, "minStock" to 15.0, "unitCost" to 180.0, "createdAt" to FieldValue.serverTimestamp())).await()
    ingredientsCol.add(mapOf("name" to "Mozzarella Cheese (Block)", "unit" to "kg", "currentStock" to 28.5, "minStock" to 10.0, "unitCost" to 1650.0, "createdAt" to FieldValue.serverTimestamp())).await()
    ingredientsCol.add(mapOf("name" to "Boneless Chicken Tikka Chunks", "unit" to "kg", "currentStock" to 18.0, "minStock" to 8.0, "unitCost" to 950.0, "createdAt" to FieldValue.serverTimestamp())).await()
    ingredientsCol.add(mapOf("name" to "Italian Herb Tomato Pizza Sauce", "unit" to "liters", "currentStock" to 14.0, "minStock" to 5.0, "unitCost" to 420.0, "createdAt" to FieldValue.serverTimestamp())).await()
    ingredientsCol.add(mapOf("name" to "Black Sliced Olives", "unit" to "kg", "currentStock" to 4.2, "minStock" to 2.0, "unitCost" to 750.0, "createdAt" to FieldValue.serverTimestamp())).await()
    ingredientsCol.add(mapOf("name" to "Pickled Jalapeno Peppers", "unit" to "kg", "currentStock" to 3.8, "minStock" to 2.0, "unitCost" to 680.0, "createdAt" to FieldValue.serverTimestamp())).await()
    ingredientsCol.add(mapOf("name" to "Pizza Boxes (10\" & 13\")", "unit" to "units", "currentStock" to 220.0, "minStock" to 50.0, "unitCost" to 45.0, "createdAt" to FieldValue.serverTimestamp())).await()

    // 5. Create a sample initial order so POS and KDS have live data immediately
    createOrder(Order(
      orderNumber = "GV-1001",
      orderType = "dine_in",
      status = "preparing",
      paymentStatus = "paid",
      paymentMethod = "cash",
      tableNumber = "T-2",
      items = listOf(
        com.example.data.model.OrderItem(
          productName = "GEVINI Crown Crust Pizza",
          quantity = 1,
          selectedSize = "Regular (10\")",
          selectedModifiers = listOf("Extra Mozzarella Cheese"),
          unitPrice = 1700.0,
          itemTotal = 1700.0,
          station = "pizza"
        ),
        com.example.data.model.OrderItem(
          productName = "Pakola Ice Cream Soda (Can)",
          quantity = 2,
          unitPrice = 120.0,
          itemTotal = 240.0,
          station = "beverage"
        )
      ),
      subtotal = 1940.0,
      tax = 194.0,
      discount = 0.0,
      total = 2134.0,
      customerName = "Bilal Ahmed",
      customerPhone = "0300-1234567"
    ))
  }
}
