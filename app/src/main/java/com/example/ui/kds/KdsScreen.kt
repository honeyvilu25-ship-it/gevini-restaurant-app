package com.example.ui.kds

import androidx.compose.animation.animateColorAsState
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.Order
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.repository.RestaurantRepository
import com.example.ui.theme.GeviniBurgundy
import com.example.ui.theme.GeviniCharcoal
import com.example.ui.theme.GeviniCharcoalBorder
import com.example.ui.theme.GeviniCharcoalCard
import com.example.ui.theme.GeviniCharcoalSurface
import com.example.ui.theme.GeviniCream
import com.example.ui.theme.GeviniGold
import com.example.ui.theme.StatusCompleted
import com.example.ui.theme.StatusPending
import com.example.ui.theme.StatusPreparing
import com.example.ui.theme.StatusReady
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun KdsScreen(
  repository: RestaurantRepository,
  onNavigateBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val allOrders by repository.observeOrders().collectAsState(initial = emptyList())

  var selectedStation by remember { mutableStateOf("all") }
  var selectedStatusFilter by remember { mutableStateOf("active") } // active (pending + preparing), ready, all

  // Real-time ticking timer for elapsed times
  var currentTimeMillis by remember { mutableLongStateOf(System.currentTimeMillis()) }
  LaunchedEffect(Unit) {
    while (true) {
      delay(1000L)
      currentTimeMillis = System.currentTimeMillis()
    }
  }

  // Filter orders for KDS
  val kdsOrders = remember(allOrders, selectedStation, selectedStatusFilter) {
    allOrders.filter { order ->
      val matchesStatus = when (selectedStatusFilter) {
        "active" -> order.status == OrderStatus.PENDING.code || order.status == OrderStatus.PREPARING.code
        "ready" -> order.status == OrderStatus.READY.code
        "completed" -> order.status == OrderStatus.COMPLETED.code
        else -> order.status != OrderStatus.CANCELLED.code
      }

      val matchesStation = if (selectedStation == "all") {
        true
      } else {
        order.items.any { it.station.equals(selectedStation, ignoreCase = true) }
      }

      matchesStatus && matchesStation
    }
  }

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(GeviniCharcoal)
      .padding(16.dp)
  ) {
    // KDS Header Bar
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("kds_back_button")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GeviniCream)
      }

      Column(modifier = Modifier.weight(1f)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
          Text(
            text = "KITCHEN DISPLAY SYSTEM",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = GeviniCream
          )
          Spacer(modifier = Modifier.width(10.dp))
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(GeviniBurgundy)
              .padding(horizontal = 8.dp, vertical = 2.dp)
          ) {
            Text(
              text = "${kdsOrders.size} TICKETS",
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = Color.White
            )
          }
        }
        Text(
          text = "Real-time order line • Station routing & preparation tracking",
          style = MaterialTheme.typography.bodySmall,
          color = GeviniGold
        )
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Filter controls: Stations and Status
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // Station chips
      LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        val stations = listOf(
          "all" to "All Stations",
          "pizza" to "Pizza Oven",
          "grill" to "Grill & Burgers",
          "fryer" to "Fryer & Sides",
          "beverage" to "Beverages"
        )
        items(stations) { (code, title) ->
          val isSelected = selectedStation == code
          FilterChip(
            selected = isSelected,
            onClick = { selectedStation = code },
            label = { Text(title) },
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
              selected = isSelected
            )
          )
        }
      }

      // Status Switcher
      Row(
        modifier = Modifier
          .clip(RoundedCornerShape(8.dp))
          .background(GeviniCharcoalCard)
          .padding(2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        listOf("active" to "In Prep", "ready" to "Ready", "completed" to "History").forEach { (code, title) ->
          val isSelected = selectedStatusFilter == code
          Box(
            modifier = Modifier
              .clip(RoundedCornerShape(6.dp))
              .background(if (isSelected) GeviniGold else Color.Transparent)
              .clickable { selectedStatusFilter = code }
              .padding(horizontal = 10.dp, vertical = 6.dp)
          ) {
            Text(
              text = title,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = if (isSelected) Color.Black else Color.LightGray
            )
          }
        }
      }
    }

    Spacer(modifier = Modifier.height(12.dp))

    // Tickets Grid
    if (kdsOrders.isEmpty()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .padding(32.dp),
        contentAlignment = Alignment.Center
      ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
          Icon(Icons.Default.DoneAll, contentDescription = null, tint = GeviniGold, modifier = Modifier.size(64.dp))
          Spacer(modifier = Modifier.height(12.dp))
          Text(
            text = "Kitchen is clear!",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = GeviniCream
          )
          Text(
            text = "No pending tickets in this station.",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.Gray
          )
        }
      }
    } else {
      LazyVerticalGrid(
        columns = GridCells.Adaptive(minSize = 280.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxSize()
      ) {
        items(kdsOrders) { order ->
          KdsTicketCard(
            order = order,
            currentTimeMillis = currentTimeMillis,
            onUpdateStatus = { newStatus ->
              scope.launch {
                repository.updateOrderStatus(order.id, newStatus)
              }
            }
          )
        }
      }
    }
  }
}

@Composable
fun KdsTicketCard(
  order: Order,
  currentTimeMillis: Long,
  onUpdateStatus: (String) -> Unit
) {
  // Elapsed time calculation
  val orderTimeMillis = order.createdAt?.toDate()?.time ?: currentTimeMillis
  val elapsedSeconds = ((currentTimeMillis - orderTimeMillis) / 1000).coerceAtLeast(0)
  val elapsedMinutes = elapsedSeconds / 60
  val elapsedRemainingSec = elapsedSeconds % 60

  val isUrgent = elapsedMinutes >= 15
  val isDelayed = elapsedMinutes >= 20

  val statusColor = when (order.status) {
    OrderStatus.PENDING.code -> StatusPending
    OrderStatus.PREPARING.code -> StatusPreparing
    OrderStatus.READY.code -> StatusReady
    OrderStatus.COMPLETED.code -> StatusCompleted
    else -> Color.Gray
  }

  Card(
    modifier = Modifier
      .fillMaxWidth()
      .clip(RoundedCornerShape(16.dp))
      .border(
        width = if (isUrgent) 2.dp else 1.dp,
        color = if (isDelayed) Color(0xFFFF4444) else if (isUrgent) GeviniGold else GeviniCharcoalBorder,
        shape = RoundedCornerShape(16.dp)
      ),
    colors = CardDefaults.cardColors(containerColor = GeviniCharcoalCard)
  ) {
    Column(modifier = Modifier.padding(14.dp)) {
      // Ticket Header
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
      ) {
        Column {
          Text(
            text = order.orderNumber,
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = GeviniCream
          )
          Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
              text = OrderType.fromCode(order.orderType).title,
              style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
              color = GeviniGold
            )
            if (order.tableNumber.isNotBlank()) {
              Text(
                text = " • Table ${order.tableNumber}",
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = GeviniCream
              )
            }
          }
        }

        // Live Elapsed Timer Badge
        Box(
          modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(
              if (isDelayed) Color(0xFF8B0000)
              else if (isUrgent) Color(0xFF6B4500)
              else GeviniCharcoalSurface
            )
            .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
          Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
              imageVector = Icons.Default.Timer,
              contentDescription = null,
              tint = if (isUrgent) Color.White else GeviniGold,
              modifier = Modifier.size(14.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
              text = String.format("%02d:%02d", elapsedMinutes, elapsedRemainingSec),
              style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
              color = if (isUrgent) Color.White else GeviniCream
            )
          }
        }
      }

      Spacer(modifier = Modifier.height(10.dp))
      HorizontalDivider(color = GeviniCharcoalBorder)
      Spacer(modifier = Modifier.height(8.dp))

      // Items in order
      Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(6.dp)
      ) {
        order.items.forEach { item ->
          Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
          ) {
            Text(
              text = "${item.quantity}x",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Black),
              color = GeviniGold,
              modifier = Modifier.width(28.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = item.productName,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = GeviniCream
              )
              if (item.selectedSize.isNotBlank()) {
                Text(
                  text = "Size: ${item.selectedSize}",
                  style = MaterialTheme.typography.labelSmall,
                  color = Color.LightGray
                )
              }
              if (item.selectedModifiers.isNotEmpty()) {
                Text(
                  text = "Modifiers: ${item.selectedModifiers.joinToString(", ")}",
                  style = MaterialTheme.typography.labelSmall,
                  color = GeviniGold
                )
              }
              if (item.notes.isNotBlank()) {
                Box(
                  modifier = Modifier
                    .padding(top = 2.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFF332200))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                  Text(
                    text = "Special Note: ${item.notes}",
                    style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = Color(0xFFFFD54F)
                  )
                }
              }
            }
          }
        }
      }

      Spacer(modifier = Modifier.height(14.dp))
      HorizontalDivider(color = GeviniCharcoalBorder)
      Spacer(modifier = Modifier.height(10.dp))

      // Status Update Actions
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
      ) {
        when (order.status) {
          OrderStatus.PENDING.code -> {
            Button(
              onClick = { onUpdateStatus(OrderStatus.PREPARING.code) },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("kds_start_button_${order.id}"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = StatusPreparing, contentColor = Color.White)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Start Preparing", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
              }
            }
          }

          OrderStatus.PREPARING.code -> {
            Button(
              onClick = { onUpdateStatus(OrderStatus.READY.code) },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("kds_ready_button_${order.id}"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = StatusReady, contentColor = Color.White)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Mark Ready for Service", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
              }
            }
          }

          OrderStatus.READY.code -> {
            Button(
              onClick = { onUpdateStatus(OrderStatus.COMPLETED.code) },
              modifier = Modifier
                .fillMaxWidth()
                .testTag("kds_complete_button_${order.id}"),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = StatusCompleted, contentColor = Color.White)
            ) {
              Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.DoneAll, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(6.dp))
                Text("Complete & Served", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
              }
            }
          }

          else -> {
            Box(
              modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
              contentAlignment = Alignment.Center
            ) {
              Text(
                text = "Order ${order.status.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
              )
            }
          }
        }
      }
    }
  }
}
