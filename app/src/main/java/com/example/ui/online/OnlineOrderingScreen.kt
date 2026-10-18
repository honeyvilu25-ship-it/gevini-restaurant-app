package com.example.ui.online

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.OrderItem
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
import com.example.data.model.PaymentStatus
import com.example.data.model.Product
import com.example.data.repository.RestaurantRepository
import com.example.ui.theme.GeviniBurgundy
import com.example.ui.theme.GeviniCharcoal
import com.example.ui.theme.GeviniCharcoalBorder
import com.example.ui.theme.GeviniCharcoalCard
import com.example.ui.theme.GeviniCharcoalSurface
import com.example.ui.theme.GeviniCream
import com.example.ui.theme.GeviniGold
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
fun OnlineOrderingScreen(
  repository: RestaurantRepository,
  onNavigateBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val products by repository.observeProducts().collectAsState(initial = emptyList())
  val allOrders by repository.observeOrders().collectAsState(initial = emptyList())

  val cartItems = remember { mutableStateListOf<OrderItem>() }
  var customerName by remember { mutableStateOf("") }
  var customerPhone by remember { mutableStateOf("") }
  var deliveryAddress by remember { mutableStateOf("") }
  var orderNotes by remember { mutableStateOf("") }

  var activePlacedOrderId by remember { mutableStateOf<String?>(null) }
  var isPlacingOrder by remember { mutableStateOf(false) }

  val subtotal = cartItems.sumOf { it.itemTotal }
  val deliveryFee = if (cartItems.isNotEmpty()) 150.0 else 0.0
  val total = subtotal + deliveryFee

  val currencyFormat = remember {
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
  }

  // Find active placed order if any
  val trackedOrder = allOrders.find { it.id == activePlacedOrderId }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(GeviniCharcoal)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(14.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("online_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GeviniCream)
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "GEVINI ONLINE ORDERING",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = GeviniCream
          )
          Text(
            text = "Direct delivery to your doorstep in Malir & Karachi",
            style = MaterialTheme.typography.bodySmall,
            color = GeviniGold
          )
        }
      }
    }

    // Active Live Order Tracker Banner (if customer just placed an order)
    if (trackedOrder != null) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = GeviniBurgundy.copy(alpha = 0.4f)),
          border = androidx.compose.foundation.BorderStroke(1.dp, GeviniGold)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Row(
              modifier = Modifier.fillMaxWidth(),
              horizontalArrangement = Arrangement.SpaceBetween,
              verticalAlignment = Alignment.CenterVertically
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DeliveryDining, contentDescription = null, tint = GeviniGold)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                  text = "Order #${trackedOrder.orderNumber}",
                  style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                  color = GeviniCream
                )
              }
              Box(
                modifier = Modifier
                  .clip(RoundedCornerShape(6.dp))
                  .background(GeviniGold)
                  .padding(horizontal = 8.dp, vertical = 4.dp)
              ) {
                Text(
                  text = OrderStatus.fromCode(trackedOrder.status).title.uppercase(),
                  style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                  color = Color.Black
                )
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
              text = when (trackedOrder.status) {
                OrderStatus.PENDING.code -> "Order received by GEVINI kitchen. Preparing soon!"
                OrderStatus.PREPARING.code -> "In the oven! Our chefs are preparing your meal."
                OrderStatus.READY.code -> "Dispatched with rider! Arriving at your doorstep."
                OrderStatus.COMPLETED.code -> "Delivered! Enjoy your hot meal from GEVINI."
                else -> "Status: ${trackedOrder.status}"
              },
              style = MaterialTheme.typography.bodySmall,
              color = GeviniCream
            )
          }
        }
      }
    }

    // Customer Cart Summary if items added
    if (cartItems.isNotEmpty()) {
      item {
        Card(
          modifier = Modifier.fillMaxWidth(),
          shape = RoundedCornerShape(16.dp),
          colors = CardDefaults.cardColors(containerColor = GeviniCharcoalSurface),
          border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
        ) {
          Column(modifier = Modifier.padding(16.dp)) {
            Text(
              text = "Your Cart (${cartItems.size} items)",
              style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
              color = GeviniCream
            )

            Spacer(modifier = Modifier.height(8.dp))

            cartItems.forEach { item ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween
              ) {
                Text("${item.quantity}x ${item.productName}", style = MaterialTheme.typography.bodySmall, color = GeviniCream)
                Text("PKR ${currencyFormat.format(item.itemTotal)}", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = GeviniGold)
              }
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = GeviniCharcoalBorder)
            Spacer(modifier = Modifier.height(8.dp))

            // Delivery Details inputs
            OutlinedTextField(
              value = customerName,
              onValueChange = { customerName = it },
              label = { Text("Your Full Name", color = Color.Gray) },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(focusedTextColor = GeviniCream, unfocusedTextColor = GeviniCream, focusedContainerColor = GeviniCharcoalCard, unfocusedContainerColor = GeviniCharcoalCard)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = customerPhone,
              onValueChange = { customerPhone = it },
              label = { Text("Mobile Number (for delivery rider)", color = Color.Gray) },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(focusedTextColor = GeviniCream, unfocusedTextColor = GeviniCream, focusedContainerColor = GeviniCharcoalCard, unfocusedContainerColor = GeviniCharcoalCard)
            )
            Spacer(modifier = Modifier.height(6.dp))
            OutlinedTextField(
              value = deliveryAddress,
              onValueChange = { deliveryAddress = it },
              label = { Text("Complete Delivery Address (Malir / Karachi)", color = Color.Gray) },
              modifier = Modifier.fillMaxWidth(),
              colors = OutlinedTextFieldDefaults.colors(focusedTextColor = GeviniCream, unfocusedTextColor = GeviniCream, focusedContainerColor = GeviniCharcoalCard, unfocusedContainerColor = GeviniCharcoalCard)
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
              Text("Total to Pay (Cash on Delivery):", style = MaterialTheme.typography.bodyMedium, color = GeviniCream)
              Text("PKR ${currencyFormat.format(total)}", style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black), color = GeviniGold)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Button(
              onClick = {
                isPlacingOrder = true
                scope.launch {
                  val newOrder = Order(
                    orderType = OrderType.ONLINE.code,
                    status = OrderStatus.PENDING.code,
                    paymentStatus = PaymentStatus.UNPAID.code,
                    paymentMethod = PaymentMethod.ONLINE_COD.code,
                    items = cartItems.toList(),
                    subtotal = subtotal,
                    tax = 0.0,
                    deliveryFee = deliveryFee,
                    total = total,
                    customerName = customerName.ifBlank { "Online Customer" },
                    customerPhone = customerPhone,
                    deliveryAddress = deliveryAddress,
                    notes = orderNotes
                  )
                  val res = repository.createOrder(newOrder)
                  isPlacingOrder = false
                  if (res.isSuccess) {
                    activePlacedOrderId = res.getOrNull()
                    cartItems.clear()
                  }
                }
              },
              enabled = !isPlacingOrder && customerPhone.isNotBlank(),
              modifier = Modifier
                .fillMaxWidth()
                .testTag("place_online_order_button"),
              shape = RoundedCornerShape(12.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GeviniBurgundy, contentColor = Color.White)
            ) {
              Text("Confirm Order (Cash on Delivery)", style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      }
    }

    // Menu Explorer
    item {
      Text(
        text = "Explore Our Menu",
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
        color = GeviniCream
      )
    }

    items(products) { product ->
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = GeviniCharcoalCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
      ) {
        Row(
          modifier = Modifier
            .fillMaxWidth()
            .padding(14.dp),
          verticalAlignment = Alignment.CenterVertically
        ) {
          Box(
            modifier = Modifier
              .size(42.dp)
              .clip(CircleShape)
              .background(GeviniBurgundy.copy(alpha = 0.3f)),
            contentAlignment = Alignment.Center
          ) {
            Icon(Icons.Default.LocalPizza, contentDescription = null, tint = GeviniGold, modifier = Modifier.size(24.dp))
          }

          Spacer(modifier = Modifier.width(12.dp))

          Column(modifier = Modifier.weight(1f)) {
            Text(
              text = product.name,
              style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold),
              color = GeviniCream
            )
            Text(
              text = product.description,
              style = MaterialTheme.typography.bodySmall,
              color = Color.Gray,
              maxLines = 2
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
              text = "PKR ${currencyFormat.format(product.price)}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
              color = GeviniGold
            )
          }

          Button(
            onClick = {
              cartItems.add(
                OrderItem(
                  productId = product.id,
                  productName = product.name,
                  quantity = 1,
                  selectedSize = "Regular",
                  unitPrice = product.price,
                  itemTotal = product.price,
                  station = product.station
                )
              )
            },
            shape = RoundedCornerShape(10.dp),
            colors = ButtonDefaults.buttonColors(containerColor = GeviniBurgundy, contentColor = Color.White)
          ) {
            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
            Spacer(modifier = Modifier.width(4.dp))
            Text("Add")
          }
        }
      }
    }
  }
}
