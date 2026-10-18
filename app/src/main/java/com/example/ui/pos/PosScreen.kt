package com.example.ui.pos

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Print
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.model.DiningTable
import com.example.data.model.MenuCategory
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.model.ProductModifier
import com.example.data.model.ProductSize
import com.example.data.repository.RestaurantRepository
import com.example.ui.theme.GeviniBurgundy
import com.example.ui.theme.GeviniBurgundyDark
import com.example.ui.theme.GeviniCharcoal
import com.example.ui.theme.GeviniCharcoalBorder
import com.example.ui.theme.GeviniCharcoalCard
import com.example.ui.theme.GeviniCharcoalSurface
import com.example.ui.theme.GeviniCream
import com.example.ui.theme.GeviniGold
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PosScreen(
  repository: RestaurantRepository,
  onNavigateBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val categories by repository.observeCategories().collectAsState(initial = emptyList())
  val products by repository.observeProducts().collectAsState(initial = emptyList())
  val tables by repository.observeTables().collectAsState(initial = emptyList())

  var selectedCategoryId by remember { mutableStateOf<String?>("all") }
  var searchQuery by remember { mutableStateOf("") }

  // Cart State
  val cartItems = remember { mutableStateListOf<OrderItem>() }
  var selectedOrderType by remember { mutableStateOf(OrderType.DINE_IN) }
  var selectedTableNumber by remember { mutableStateOf("T-1") }
  var customerName by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }
  var deliveryAddress by remember { mutableStateOf("") }
  var discountAmount by remember { mutableDoubleStateOf(0.0) }
  var taxRatePercent by remember { mutableDoubleStateOf(5.0) } // 5% Sindh sales tax standard on POS card/services

  // Modals & Dialogs
  var customizingProduct by remember { mutableStateOf<Product?>(null) }
  var showPaymentDialog by remember { mutableStateOf(false) }
  var completedOrder by remember { mutableStateOf<Order?>(null) }
  var isSubmitting by remember { mutableStateOf(false) }
  var showCartBottomSheet by remember { mutableStateOf(false) }

  // Auto seed menu if empty
  LaunchedEffect(Unit) {
    repository.seedDatabase()
  }

  // Filter products
  val filteredProducts = remember(products, selectedCategoryId, searchQuery) {
    products.filter { product ->
      val matchesCategory = selectedCategoryId == "all" || product.categoryId == selectedCategoryId
      val matchesSearch = searchQuery.isBlank() || product.name.contains(searchQuery, ignoreCase = true) || product.description.contains(searchQuery, ignoreCase = true)
      matchesCategory && matchesSearch
    }
  }

  // Cart Calculations
  val subtotal = cartItems.sumOf { it.itemTotal }
  val deliveryFee = if (selectedOrderType == OrderType.DELIVERY) 150.0 else 0.0
  val tax = (subtotal - discountAmount).coerceAtLeast(0.0) * (taxRatePercent / 100.0)
  val grandTotal = (subtotal - discountAmount + tax + deliveryFee).coerceAtLeast(0.0)

  val currencyFormat = remember {
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(GeviniCharcoal)
  ) {
    Row(modifier = Modifier.fillMaxSize()) {
      // -------------------------------------------------------------
      // Left Column: Catalog & Categories (Wide Area)
      // -------------------------------------------------------------
      Column(
        modifier = Modifier
          .weight(1.6f)
          .fillMaxHeight()
          .padding(16.dp)
      ) {
        // Top POS Action Bar
        Row(
          modifier = Modifier.fillMaxWidth(),
          verticalAlignment = Alignment.CenterVertically
        ) {
          IconButton(
            onClick = onNavigateBack,
            modifier = Modifier.testTag("pos_back_button")
          ) {
            Icon(
              imageVector = Icons.AutoMirrored.Filled.ArrowBack,
              contentDescription = "Back",
              tint = GeviniCream
            )
          }

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = "GEVINI POS TERMINAL",
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
              color = GeviniCream
            )
            Text(
              text = "Malir 15, Karachi • Cashier: ${repository.currentUserName()}",
              style = MaterialTheme.typography.bodySmall,
              color = GeviniGold
            )
          }

          // Cart Floating Badge for mobile view
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(12.dp))
              .background(GeviniBurgundy)
              .clickable { showCartBottomSheet = true }
              .padding(horizontal = 12.dp, vertical = 8.dp)
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.ShoppingCart, contentDescription = "Cart", tint = GeviniGold, modifier = Modifier.size(20.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text(
                text = "${cartItems.sumOf { it.quantity }} items • PKR ${currencyFormat.format(grandTotal)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = GeviniCream
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
          value = searchQuery,
          onValueChange = { searchQuery = it },
          placeholder = { Text("Search pizzas, burgers, toppings, wings...", color = Color.Gray) },
          leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = GeviniGold) },
          trailingIcon = {
            if (searchQuery.isNotEmpty()) {
              IconButton(onClick = { searchQuery = "" }) {
                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = Color.Gray)
              }
            }
          },
          modifier = Modifier
            .fillMaxWidth()
            .testTag("pos_search_input"),
          shape = RoundedCornerShape(12.dp),
          colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = GeviniGold,
            unfocusedBorderColor = GeviniCharcoalBorder,
            focusedContainerColor = GeviniCharcoalSurface,
            unfocusedContainerColor = GeviniCharcoalSurface,
            focusedTextColor = GeviniCream,
            unfocusedTextColor = GeviniCream
          ),
          singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Category Filter Chips
        LazyRow(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          item {
            FilterChip(
              selected = selectedCategoryId == "all",
              onClick = { selectedCategoryId = "all" },
              label = { Text("All Items (${products.size})") },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GeviniBurgundy,
                selectedLabelColor = GeviniCream,
                containerColor = GeviniCharcoalCard,
                labelColor = Color.LightGray
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = GeviniCharcoalBorder,
                selectedBorderColor = GeviniBurgundy,
                enabled = true,
                selected = selectedCategoryId == "all"
              )
            )
          }

          items(categories) { cat ->
            FilterChip(
              selected = selectedCategoryId == cat.id,
              onClick = { selectedCategoryId = cat.id },
              label = { Text(cat.name) },
              colors = FilterChipDefaults.filterChipColors(
                selectedContainerColor = GeviniBurgundy,
                selectedLabelColor = GeviniCream,
                containerColor = GeviniCharcoalCard,
                labelColor = Color.LightGray
              ),
              border = FilterChipDefaults.filterChipBorder(
                borderColor = GeviniCharcoalBorder,
                selectedBorderColor = GeviniBurgundy,
                enabled = true,
                selected = selectedCategoryId == cat.id
              )
            )
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Product Catalog Grid
        if (filteredProducts.isEmpty()) {
          Box(
            modifier = Modifier
              .fillMaxSize()
              .padding(32.dp),
            contentAlignment = Alignment.Center
          ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Icon(Icons.Default.LocalPizza, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(56.dp))
              Spacer(modifier = Modifier.height(8.dp))
              Text("No food items match the filter.", color = Color.Gray, style = MaterialTheme.typography.bodyLarge)
            }
          }
        } else {
          LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 175.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxSize()
          ) {
            items(filteredProducts) { product ->
              PosProductCard(
                product = product,
                currencyFormat = currencyFormat,
                onClick = {
                  customizingProduct = product
                }
              )
            }
          }
        }
      }

      // -------------------------------------------------------------
      // Right Column: Active Order Cart (Split Screen Layout)
      // -------------------------------------------------------------
      Card(
        modifier = Modifier
          .weight(1.0f)
          .fillMaxHeight()
          .padding(top = 16.dp, bottom = 16.dp, end = 16.dp),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = GeviniCharcoalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
      ) {
        CartContent(
          cartItems = cartItems,
          selectedOrderType = selectedOrderType,
          onOrderTypeChange = { selectedOrderType = it },
          selectedTableNumber = selectedTableNumber,
          onTableChange = { selectedTableNumber = it },
          tables = tables,
          customerName = customerName,
          onCustomerNameChange = { customerName = it },
          customerPhone = customerPhone,
          onCustomerPhoneChange = { customerPhone = it },
          deliveryAddress = deliveryAddress,
          onDeliveryAddressChange = { deliveryAddress = it },
          subtotal = subtotal,
          tax = tax,
          discount = discountAmount,
          deliveryFee = deliveryFee,
          grandTotal = grandTotal,
          currencyFormat = currencyFormat,
          onPayClicked = { showPaymentDialog = true },
          onClearCart = { cartItems.clear() }
        )
      }
    }

    // Product Customization Dialog (Size, Toppings, Notes)
    customizingProduct?.let { product ->
      ProductCustomizationDialog(
        product = product,
        currencyFormat = currencyFormat,
        onDismiss = { customizingProduct = null },
        onAddToCart = { size, modifiers, notes, quantity, total ->
          cartItems.add(
            OrderItem(
              productId = product.id,
              productName = product.name,
              quantity = quantity,
              selectedSize = size,
              selectedModifiers = modifiers,
              unitPrice = total / quantity,
              itemTotal = total,
              notes = notes,
              station = product.station
            )
          )
          customizingProduct = null
        }
      )
    }

    // Payment Settlement Modal
    if (showPaymentDialog) {
      PaymentSettlementDialog(
        grandTotal = grandTotal,
        currencyFormat = currencyFormat,
        isSubmitting = isSubmitting,
        onDismiss = { showPaymentDialog = false },
        onConfirmPayment = { method, tenderCash ->
          isSubmitting = true
          scope.launch {
            val newOrder = Order(
              orderType = selectedOrderType.code,
              status = OrderStatus.PENDING.code,
              paymentStatus = PaymentStatus.PAID.code,
              paymentMethod = method.code,
              items = cartItems.toList(),
              subtotal = subtotal,
              tax = tax,
              discount = discountAmount,
              deliveryFee = deliveryFee,
              total = grandTotal,
              tableNumber = if (selectedOrderType == OrderType.DINE_IN) selectedTableNumber else "",
              customerName = customerName.ifBlank { "Walk-in Guest" },
              customerPhone = customerPhone,
              deliveryAddress = deliveryAddress,
              branchId = "karachi_malir_15"
            )

            val result = repository.createOrder(newOrder)
            isSubmitting = false
            showPaymentDialog = false
            if (result.isSuccess) {
              completedOrder = newOrder.copy(id = result.getOrNull() ?: "")
              cartItems.clear()
            }
          }
        }
      )
    }

    // Receipt Modal
    completedOrder?.let { order ->
      ReceiptDialog(
        order = order,
        currencyFormat = currencyFormat,
        onDismiss = { completedOrder = null }
      )
    }
  }
}

@Composable
fun PosProductCard(
  product: Product,
  currencyFormat: NumberFormat,
  onClick: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .clickable(enabled = product.available) { onClick() }
      .testTag("product_card_${product.id}"),
    colors = CardDefaults.cardColors(containerColor = GeviniCharcoalCard),
    border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(GeviniBurgundy.copy(alpha = 0.3f)),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.LocalPizza,
            contentDescription = null,
            tint = GeviniGold,
            modifier = Modifier.size(20.dp)
          )
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = product.station.uppercase(),
          style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
          color = GeviniGold
        )
      }

      Spacer(modifier = Modifier.height(10.dp))

      Text(
        text = product.name,
        style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
        color = GeviniCream,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
      )

      Text(
        text = product.description,
        style = MaterialTheme.typography.bodySmall,
        color = Color.Gray,
        maxLines = 2,
        overflow = TextOverflow.Ellipsis,
        modifier = Modifier.padding(top = 4.dp, bottom = 8.dp)
      )

      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = "PKR ${currencyFormat.format(product.price)}",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
            color = GeviniGold
          )
          if (product.sizes.isNotEmpty()) {
            Text(
              text = "${product.sizes.size} sizes",
              style = MaterialTheme.typography.labelSmall,
              color = Color.LightGray
            )
          }
        }

        Box(
          modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(GeviniBurgundy),
          contentAlignment = Alignment.Center
        ) {
          Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White, modifier = Modifier.size(20.dp))
        }
      }
    }
  }
}

@Composable
fun CartContent(
  cartItems: MutableList<OrderItem>,
  selectedOrderType: OrderType,
  onOrderTypeChange: (OrderType) -> Unit,
  selectedTableNumber: String,
  onTableChange: (String) -> Unit,
  tables: List<DiningTable>,
  customerName: String,
  onCustomerNameChange: (String) -> Unit,
  customerPhone: String,
  onCustomerPhoneChange: (String) -> Unit,
  deliveryAddress: String,
  onDeliveryAddressChange: (String) -> Unit,
  subtotal: Double,
  tax: Double,
  discount: Double,
  deliveryFee: Double,
  grandTotal: Double,
  currencyFormat: NumberFormat,
  onPayClicked: () -> Unit,
  onClearCart: () -> Unit
) {
  Column(
    modifier = Modifier
      .fillMaxSize()
      .padding(16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.ShoppingCart, contentDescription = null, tint = GeviniGold)
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Active Ticket",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = GeviniCream
        )
      }

      if (cartItems.isNotEmpty()) {
        Text(
          text = "Clear",
          style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
          color = Color(0xFFFF6B6B),
          modifier = Modifier
            .clickable { onClearCart() }
            .padding(4.dp)
        )
      }
    }

    Spacer(modifier = Modifier.height(10.dp))

    // Order Type Selector
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
      OrderType.entries.filter { it != OrderType.ONLINE }.forEach { type ->
        val isSelected = selectedOrderType == type
        Box(
          modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(8.dp))
            .background(if (isSelected) GeviniBurgundy else GeviniCharcoalCard)
            .clickable { onOrderTypeChange(type) }
            .padding(vertical = 8.dp),
          contentAlignment = Alignment.Center
        ) {
          Text(
            text = type.title,
            style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
            color = if (isSelected) Color.White else Color.Gray,
            textAlign = TextAlign.Center
          )
        }
      }
    }

    Spacer(modifier = Modifier.height(8.dp))

    // Contextual Dine-in / Delivery fields
    if (selectedOrderType == OrderType.DINE_IN) {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text("Table:", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        LazyRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
          val tableList = if (tables.isEmpty()) (1..8).map { "T-$it" } else tables.map { it.tableNumber }
          items(tableList) { tNum ->
            val isSelected = selectedTableNumber == tNum
            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(if (isSelected) GeviniGold else GeviniCharcoalCard)
                .clickable { onTableChange(tNum) }
                .padding(horizontal = 10.dp, vertical = 6.dp)
            ) {
              Text(
                text = tNum,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) Color.Black else GeviniCream
              )
            }
          }
        }
      }
    } else if (selectedOrderType == OrderType.DELIVERY) {
      OutlinedTextField(
        value = deliveryAddress,
        onValueChange = onDeliveryAddressChange,
        placeholder = { Text("Delivery Address (Malir / Karachi)", color = Color.Gray, fontSize = 12.sp) },
        modifier = Modifier.fillMaxWidth(),
        maxLines = 1,
        colors = OutlinedTextFieldDefaults.colors(
          focusedContainerColor = GeviniCharcoalCard,
          unfocusedContainerColor = GeviniCharcoalCard,
          focusedTextColor = GeviniCream,
          unfocusedTextColor = GeviniCream
        )
      )
    }

    Spacer(modifier = Modifier.height(10.dp))
    HorizontalDivider(color = GeviniCharcoalBorder)
    Spacer(modifier = Modifier.height(8.dp))

    // Cart Items List
    if (cartItems.isEmpty()) {
      Box(
        modifier = Modifier
          .weight(1f)
          .fillMaxWidth(),
        contentAlignment = Alignment.Center
      ) {
        Text(
          text = "No items added.\nTap food items to add to order.",
          color = Color.Gray,
          style = MaterialTheme.typography.bodySmall,
          textAlign = TextAlign.Center
        )
      }
    } else {
      LazyColumn(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        items(cartItems) { item ->
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .clip(RoundedCornerShape(10.dp))
              .background(GeviniCharcoalCard)
              .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.productName,
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                color = GeviniCream,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              Text(
                text = "${item.selectedSize}${if (item.selectedModifiers.isNotEmpty()) " • " + item.selectedModifiers.joinToString(", ") else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = GeviniGold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
              )
              if (item.notes.isNotBlank()) {
                Text(
                  text = "Note: ${item.notes}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.Gray
                )
              }
              Text(
                text = "PKR ${currencyFormat.format(item.itemTotal)}",
                style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold),
                color = Color.White
              )
            }

            // Quantity Control
            Row(verticalAlignment = Alignment.CenterVertically) {
              IconButton(
                onClick = {
                  val idx = cartItems.indexOf(item)
                  if (item.quantity > 1) {
                    val unit = item.itemTotal / item.quantity
                    cartItems[idx] = item.copy(
                      quantity = item.quantity - 1,
                      itemTotal = unit * (item.quantity - 1)
                    )
                  } else {
                    cartItems.remove(item)
                  }
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(
                  imageVector = if (item.quantity == 1) Icons.Default.Delete else Icons.Default.Remove,
                  contentDescription = "Decrease",
                  tint = Color.LightGray,
                  modifier = Modifier.size(16.dp)
                )
              }

              Text(
                text = "${item.quantity}",
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = GeviniCream,
                modifier = Modifier.padding(horizontal = 6.dp)
              )

              IconButton(
                onClick = {
                  val idx = cartItems.indexOf(item)
                  val unit = item.itemTotal / item.quantity
                  cartItems[idx] = item.copy(
                    quantity = item.quantity + 1,
                    itemTotal = unit * (item.quantity + 1)
                  )
                },
                modifier = Modifier.size(28.dp)
              ) {
                Icon(Icons.Default.Add, contentDescription = "Increase", tint = GeviniGold, modifier = Modifier.size(16.dp))
              }
            }
          }
        }
      }
    }

    // Bill Summary
    Spacer(modifier = Modifier.height(8.dp))
    HorizontalDivider(color = GeviniCharcoalBorder)
    Spacer(modifier = Modifier.height(8.dp))

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Subtotal", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text("PKR ${currencyFormat.format(subtotal)}", style = MaterialTheme.typography.bodySmall, color = GeviniCream)
      }
      if (deliveryFee > 0) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Delivery Fee", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
          Text("PKR ${currencyFormat.format(deliveryFee)}", style = MaterialTheme.typography.bodySmall, color = GeviniCream)
        }
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Tax (SST)", style = MaterialTheme.typography.bodySmall, color = Color.Gray)
        Text("PKR ${currencyFormat.format(tax)}", style = MaterialTheme.typography.bodySmall, color = GeviniCream)
      }
      Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
        Text("Grand Total", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = GeviniGold)
        Text("PKR ${currencyFormat.format(grandTotal)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = GeviniGold)
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Checkout / Pay Button
    Button(
      onClick = onPayClicked,
      enabled = cartItems.isNotEmpty(),
      modifier = Modifier
        .fillMaxWidth()
        .height(48.dp)
        .testTag("charge_order_button"),
      shape = RoundedCornerShape(12.dp),
      colors = ButtonDefaults.buttonColors(
        containerColor = GeviniBurgundy,
        contentColor = Color.White
      )
    ) {
      Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(Icons.Default.Paid, contentDescription = null, tint = GeviniGold, modifier = Modifier.size(20.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
          text = "Pay PKR ${currencyFormat.format(grandTotal)}",
          style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
        )
      }
    }
  }
}

@Composable
fun ProductCustomizationDialog(
  product: Product,
  currencyFormat: NumberFormat,
  onDismiss: () -> Unit,
  onAddToCart: (String, List<String>, String, Int, Double) -> Unit
) {
  var selectedSize by remember { mutableStateOf(product.sizes.firstOrNull()?.name ?: "Regular") }
  val selectedModifiers = remember { mutableStateListOf<String>() }
  var quantity by remember { mutableStateOf(1) }
  var specialNotes by remember { mutableStateOf("") }

  // Size base
  val sizeObj = product.sizes.find { it.name == selectedSize }
  val basePrice = if (sizeObj != null) {
    (product.price * sizeObj.priceMultiplier) + sizeObj.additionalPrice
  } else {
    product.price
  }

  // Modifiers addition
  val modifiersTotal = selectedModifiers.sumOf { modName ->
    product.modifiers.find { it.name == modName }?.price ?: 0.0
  }

  val singleItemTotal = basePrice + modifiersTotal
  val calculatedTotal = singleItemTotal * quantity

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = GeviniCharcoalSurface),
      border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Text(
            text = product.name,
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = GeviniCream
          )
          IconButton(onClick = onDismiss, modifier = Modifier.size(28.dp)) {
            Icon(Icons.Default.Clear, contentDescription = "Close", tint = Color.Gray)
          }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Sizes Options
        if (product.sizes.isNotEmpty()) {
          Text(
            text = "Select Size",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = GeviniGold
          )
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
          ) {
            product.sizes.forEach { size ->
              val isSelected = selectedSize == size.name
              Box(
                modifier = Modifier
                  .weight(1f)
                  .clip(RoundedCornerShape(8.dp))
                  .background(if (isSelected) GeviniBurgundy else GeviniCharcoalCard)
                  .clickable { selectedSize = size.name }
                  .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
              ) {
                Text(
                  text = size.name,
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = if (isSelected) Color.White else Color.LightGray,
                  textAlign = TextAlign.Center
                )
              }
            }
          }
        }

        // Modifiers / Toppings
        if (product.modifiers.isNotEmpty()) {
          Spacer(modifier = Modifier.height(10.dp))
          Text(
            text = "Extra Toppings & Crusts",
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
            color = GeviniGold
          )
          LazyColumn(
            modifier = Modifier
              .fillMaxWidth()
              .height(130.dp)
              .padding(vertical = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
          ) {
            items(product.modifiers) { mod ->
              val isChecked = selectedModifiers.contains(mod.name)
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .clip(RoundedCornerShape(8.dp))
                  .background(GeviniCharcoalCard)
                  .clickable {
                    if (isChecked) selectedModifiers.remove(mod.name)
                    else selectedModifiers.add(mod.name)
                  }
                  .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
              ) {
                Checkbox(
                  checked = isChecked,
                  onCheckedChange = { checked ->
                    if (checked) selectedModifiers.add(mod.name)
                    else selectedModifiers.remove(mod.name)
                  },
                  colors = CheckboxDefaults.colors(
                    checkedColor = GeviniBurgundy,
                    checkmarkColor = GeviniGold
                  )
                )
                Text(
                  text = mod.name,
                  style = MaterialTheme.typography.bodySmall,
                  color = GeviniCream,
                  modifier = Modifier.weight(1f)
                )
                Text(
                  text = "+ PKR ${currencyFormat.format(mod.price)}",
                  style = MaterialTheme.typography.labelSmall,
                  color = GeviniGold
                )
              }
            }
          }
        }

        // Special Instructions
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
          value = specialNotes,
          onValueChange = { specialNotes = it },
          placeholder = { Text("Special kitchen notes (e.g., less spicy, well done)", color = Color.Gray, fontSize = 12.sp) },
          modifier = Modifier.fillMaxWidth(),
          maxLines = 2,
          colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = GeviniCharcoalCard,
            unfocusedContainerColor = GeviniCharcoalCard,
            focusedTextColor = GeviniCream,
            unfocusedTextColor = GeviniCream
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Quantity and Add to Cart Button
        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.SpaceBetween,
          verticalAlignment = Alignment.CenterVertically
        ) {
          Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
              .clip(RoundedCornerShape(8.dp))
              .background(GeviniCharcoalCard)
              .padding(horizontal = 8.dp, vertical = 4.dp)
          ) {
            IconButton(
              onClick = { if (quantity > 1) quantity-- },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Remove, contentDescription = "Dec", tint = Color.LightGray)
            }
            Text(
              text = "$quantity",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
              color = GeviniCream,
              modifier = Modifier.padding(horizontal = 8.dp)
            )
            IconButton(
              onClick = { quantity++ },
              modifier = Modifier.size(28.dp)
            ) {
              Icon(Icons.Default.Add, contentDescription = "Inc", tint = GeviniGold)
            }
          }

          Button(
            onClick = {
              onAddToCart(selectedSize, selectedModifiers.toList(), specialNotes, quantity, calculatedTotal)
            },
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = GeviniBurgundy,
              contentColor = Color.White
            )
          ) {
            Text(
              text = "Add • PKR ${currencyFormat.format(calculatedTotal)}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }
  }
}

@Composable
fun PaymentSettlementDialog(
  grandTotal: Double,
  currencyFormat: NumberFormat,
  isSubmitting: Boolean,
  onDismiss: () -> Unit,
  onConfirmPayment: (PaymentMethod, Double) -> Unit
) {
  var selectedMethod by remember { mutableStateOf(PaymentMethod.CASH) }
  var tenderedCashText by remember { mutableStateOf("${grandTotal.toInt()}") }

  val tenderedCash = tenderedCashText.toDoubleOrNull() ?: 0.0
  val changeDue = (tenderedCash - grandTotal).coerceAtLeast(0.0)

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      shape = RoundedCornerShape(20.dp),
      colors = CardDefaults.cardColors(containerColor = GeviniCharcoalSurface),
      border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = "Bill Settlement",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Bold),
          color = GeviniCream
        )
        Text(
          text = "Select payment tender and complete order",
          style = MaterialTheme.typography.bodySmall,
          color = Color.Gray
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Total Due Banner
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(GeviniCharcoalCard)
            .padding(16.dp),
          contentAlignment = Alignment.Center
        ) {
          Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text("Total Amount Due", style = MaterialTheme.typography.labelMedium, color = Color.Gray)
            Text(
              text = "PKR ${currencyFormat.format(grandTotal)}",
              style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Black),
              color = GeviniGold
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Payment Method Options
        Text("Payment Tender", style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold), color = GeviniCream)
        Spacer(modifier = Modifier.height(6.dp))

        val methods = listOf(
          PaymentMethod.CASH,
          PaymentMethod.CARD,
          PaymentMethod.DIGITAL
        )

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
          methods.forEach { method ->
            val isSelected = selectedMethod == method
            Box(
              modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(if (isSelected) GeviniBurgundy else GeviniCharcoalCard)
                .clickable { selectedMethod = method }
                .padding(vertical = 10.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = method.title,
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = if (isSelected) Color.White else Color.LightGray,
                textAlign = TextAlign.Center
              )
            }
          }
        }

        // Cash Change Calculator
        if (selectedMethod == PaymentMethod.CASH) {
          Spacer(modifier = Modifier.height(14.dp))
          OutlinedTextField(
            value = tenderedCashText,
            onValueChange = { tenderedCashText = it },
            label = { Text("Tendered Cash (PKR)", color = Color.Gray) },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
              focusedContainerColor = GeviniCharcoalCard,
              unfocusedContainerColor = GeviniCharcoalCard,
              focusedTextColor = GeviniCream,
              unfocusedTextColor = GeviniCream
            )
          )

          Spacer(modifier = Modifier.height(8.dp))
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Text("Change Return:", style = MaterialTheme.typography.bodyMedium, color = Color.Gray)
            Text(
              text = "PKR ${currencyFormat.format(changeDue)}",
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
              color = if (changeDue >= 0) Color(0xFF4CAF50) else Color(0xFFFF6B6B)
            )
          }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Row(
          modifier = Modifier.fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
          Button(
            onClick = onDismiss,
            modifier = Modifier.weight(1f),
            colors = ButtonDefaults.buttonColors(containerColor = GeviniCharcoalCard, contentColor = Color.LightGray)
          ) {
            Text("Cancel")
          }

          Button(
            onClick = { onConfirmPayment(selectedMethod, tenderedCash) },
            enabled = !isSubmitting,
            modifier = Modifier
              .weight(1.5f)
              .testTag("confirm_payment_button"),
            colors = ButtonDefaults.buttonColors(containerColor = GeviniBurgundy, contentColor = Color.White)
          ) {
            if (isSubmitting) {
              CircularProgressIndicator(modifier = Modifier.size(20.dp), color = Color.White, strokeWidth = 2.dp)
            } else {
              Text("Complete Sale", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      }
    }
  }
}

@Composable
fun ReceiptDialog(
  order: Order,
  currencyFormat: NumberFormat,
  onDismiss: () -> Unit
) {
  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
      Column(
        modifier = Modifier.padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        Text("GEVINI RESTAURANT", style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black), color = Color.Black)
        Text("Malir 15, Main National Hwy, Karachi", style = MaterialTheme.typography.bodySmall, color = Color.DarkGray)
        Text("UAN / WhatsApp: 0300-GEVINI-1", style = MaterialTheme.typography.labelSmall, color = Color.Gray)

        Spacer(modifier = Modifier.height(8.dp))
        HorizontalDivider(color = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Order: ${order.orderNumber}", fontWeight = FontWeight.Bold, color = Color.Black, fontSize = 13.sp)
          Text("Type: ${OrderType.fromCode(order.orderType).title}", color = Color.DarkGray, fontSize = 13.sp)
        }
        if (order.tableNumber.isNotBlank()) {
          Text("Table: ${order.tableNumber}", color = Color.Black, fontWeight = FontWeight.SemiBold, modifier = Modifier.align(Alignment.Start), fontSize = 13.sp)
        }
        if (order.customerName.isNotBlank()) {
          Text("Customer: ${order.customerName}", color = Color.DarkGray, modifier = Modifier.align(Alignment.Start), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))

        // Item List
        order.items.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
          ) {
            Column(modifier = Modifier.weight(1f)) {
              Text("${item.quantity}x ${item.productName}", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.Medium)
              if (item.selectedModifiers.isNotEmpty()) {
                Text("  + ${item.selectedModifiers.joinToString(", ")}", color = Color.Gray, fontSize = 11.sp)
              }
            }
            Text("PKR ${currencyFormat.format(item.itemTotal)}", color = Color.Black, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
          }
          Spacer(modifier = Modifier.height(4.dp))
        }

        Spacer(modifier = Modifier.height(10.dp))
        HorizontalDivider(color = Color.LightGray)
        Spacer(modifier = Modifier.height(8.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Subtotal", color = Color.DarkGray, fontSize = 12.sp)
          Text("PKR ${currencyFormat.format(order.subtotal)}", color = Color.Black, fontSize = 12.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("Sales Tax (SST)", color = Color.DarkGray, fontSize = 12.sp)
          Text("PKR ${currencyFormat.format(order.tax)}", color = Color.Black, fontSize = 12.sp)
        }
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
          Text("TOTAL PAID", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 15.sp)
          Text("PKR ${currencyFormat.format(order.total)}", fontWeight = FontWeight.Black, color = Color.Black, fontSize = 15.sp)
        }

        Spacer(modifier = Modifier.height(8.dp))
        Text("Payment: ${PaymentMethod.fromCode(order.paymentMethod).title}", color = Color.DarkGray, fontSize = 12.sp)

        Spacer(modifier = Modifier.height(16.dp))

        Button(
          onClick = onDismiss,
          modifier = Modifier.fillMaxWidth(),
          colors = ButtonDefaults.buttonColors(containerColor = GeviniBurgundy, contentColor = Color.White)
        ) {
          Text("Print & New Order")
        }
      }
    }
  }
}
