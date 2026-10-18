package com.example.ui.dashboard

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
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.OrderStatus
import com.example.data.model.OrderType
import com.example.data.model.PaymentMethod
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
fun DashboardScreen(
  repository: RestaurantRepository,
  onNavigateToPos: () -> Unit,
  onNavigateToKds: () -> Unit,
  onNavigateToMenu: () -> Unit,
  onNavigateToTables: () -> Unit,
  onNavigateBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val orders by repository.observeOrders().collectAsState(initial = emptyList())
  val ingredients by repository.observeIngredients().collectAsState(initial = emptyList())

  val currencyFormat = remember {
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
  }

  // Financial Metrics
  val completedOrders = orders.filter { it.status != OrderStatus.CANCELLED.code }
  val grossSales = completedOrders.sumOf { it.total }
  val totalTax = completedOrders.sumOf { it.tax }
  val netSales = (grossSales - totalTax).coerceAtLeast(0.0)
  val orderCount = completedOrders.size
  val avgOrderValue = if (orderCount > 0) grossSales / orderCount else 0.0

  // Channel Breakdown
  val dineInSales = completedOrders.filter { it.orderType == OrderType.DINE_IN.code }.sumOf { it.total }
  val takeawaySales = completedOrders.filter { it.orderType == OrderType.TAKEAWAY.code }.sumOf { it.total }
  val deliverySales = completedOrders.filter { it.orderType == OrderType.DELIVERY.code || it.orderType == OrderType.ONLINE.code }.sumOf { it.total }

  // Low stock ingredients
  val lowStockList = ingredients.filter { it.currentStock <= it.minStock }

  LazyColumn(
    modifier = Modifier
      .fillMaxSize()
      .background(GeviniCharcoal)
      .padding(16.dp),
    verticalArrangement = Arrangement.spacedBy(16.dp)
  ) {
    // Header
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
      ) {
        IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("dashboard_back_button")) {
          Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GeviniCream)
        }

        Column(modifier = Modifier.weight(1f)) {
          Text(
            text = "OWNER DASHBOARD",
            style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
            color = GeviniCream
          )
          Text(
            text = "GEVINI Malir 15 • Financial Sales & Operational Metrics",
            style = MaterialTheme.typography.bodySmall,
            color = GeviniGold
          )
        }

        IconButton(onClick = { scope.launch { repository.seedDatabase() } }) {
          Icon(Icons.Default.Refresh, contentDescription = "Refresh / Seed", tint = GeviniGold)
        }
      }
    }

    // Top Metric Cards (Gross Sales, Net Sales, Orders, AOV)
    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricCard(
          title = "Gross Revenue",
          value = "PKR ${currencyFormat.format(grossSales)}",
          subtitle = "Total collected",
          accentColor = GeviniGold,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Net Sales",
          value = "PKR ${currencyFormat.format(netSales)}",
          subtitle = "Excl. sales tax",
          accentColor = Color(0xFF4CAF50),
          modifier = Modifier.weight(1f)
        )
      }
    }

    item {
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
      ) {
        MetricCard(
          title = "Total Orders",
          value = "$orderCount",
          subtitle = "Processed tickets",
          accentColor = GeviniCream,
          modifier = Modifier.weight(1f)
        )
        MetricCard(
          title = "Avg Order Value",
          value = "PKR ${currencyFormat.format(avgOrderValue)}",
          subtitle = "Per ticket spend",
          accentColor = Color(0xFF64B5F6),
          modifier = Modifier.weight(1f)
        )
      }
    }

    // Channel Sales Breakdown
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GeviniCharcoalCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Channel Sales Distribution",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = GeviniCream
          )
          Text(
            text = "Real-time revenue segmented by order channel",
            style = MaterialTheme.typography.bodySmall,
            color = Color.Gray
          )

          Spacer(modifier = Modifier.height(14.dp))

          ChannelProgressBar(label = "Dine-In Hall (Tables)", amount = dineInSales, total = grossSales, currencyFormat = currencyFormat, color = GeviniGold)
          Spacer(modifier = Modifier.height(8.dp))
          ChannelProgressBar(label = "Takeaway Counter", amount = takeawaySales, total = grossSales, currencyFormat = currencyFormat, color = GeviniBurgundy)
          Spacer(modifier = Modifier.height(8.dp))
          ChannelProgressBar(label = "Delivery & Online", amount = deliverySales, total = grossSales, currencyFormat = currencyFormat, color = Color(0xFF1976D2))
        }
      }
    }

    // Quick Terminal Switcher
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GeviniCharcoalSurface),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Text(
            text = "Terminal Operations",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            color = GeviniCream
          )
          Spacer(modifier = Modifier.height(10.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onNavigateToPos,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GeviniBurgundy, contentColor = Color.White)
            ) {
              Icon(Icons.Default.PointOfSale, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("POS Terminal", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }

            Button(
              onClick = onNavigateToKds,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GeviniCharcoalCard, contentColor = GeviniGold)
            ) {
              Icon(Icons.Default.Restaurant, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Kitchen Display", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
          ) {
            Button(
              onClick = onNavigateToMenu,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GeviniCharcoalCard, contentColor = Color.LightGray)
            ) {
              Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Menu Editor", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }

            Button(
              onClick = onNavigateToTables,
              modifier = Modifier.weight(1f),
              shape = RoundedCornerShape(10.dp),
              colors = ButtonDefaults.buttonColors(containerColor = GeviniCharcoalCard, contentColor = Color.LightGray)
            ) {
              Icon(Icons.Default.TableBar, contentDescription = null, modifier = Modifier.size(18.dp))
              Spacer(modifier = Modifier.width(6.dp))
              Text("Floor & Tables", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
            }
          }
        }
      }
    }

    // Inventory Alerts
    item {
      Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = GeviniCharcoalCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
      ) {
        Column(modifier = Modifier.padding(16.dp)) {
          Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
          ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
              Icon(Icons.Default.Warning, contentDescription = null, tint = Color(0xFFFFB74D))
              Spacer(modifier = Modifier.width(8.dp))
              Text(
                text = "Low Stock Alerts (${lowStockList.size})",
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = GeviniCream
              )
            }
          }

          Spacer(modifier = Modifier.height(8.dp))

          if (lowStockList.isEmpty()) {
            Text(
              text = "All kitchen ingredients are well above threshold reserves.",
              style = MaterialTheme.typography.bodySmall,
              color = Color.Gray
            )
          } else {
            lowStockList.forEach { ing ->
              Row(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(vertical = 4.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
              ) {
                Text(ing.name, style = MaterialTheme.typography.bodySmall, color = GeviniCream)
                Text(
                  "${ing.currentStock} ${ing.unit} (Min: ${ing.minStock})",
                  style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold),
                  color = Color(0xFFFF6B6B)
                )
              }
            }
          }
        }
      }
    }
  }
}

@Composable
fun MetricCard(
  title: String,
  value: String,
  subtitle: String,
  accentColor: Color,
  modifier: Modifier = Modifier
) {
  Card(
    modifier = modifier,
    shape = RoundedCornerShape(16.dp),
    colors = CardDefaults.cardColors(containerColor = GeviniCharcoalCard),
    border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
  ) {
    Column(modifier = Modifier.padding(16.dp)) {
      Text(title, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
      Spacer(modifier = Modifier.height(6.dp))
      Text(
        text = value,
        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Black),
        color = accentColor,
        maxLines = 1
      )
      Spacer(modifier = Modifier.height(4.dp))
      Text(subtitle, style = MaterialTheme.typography.labelSmall, color = Color.Gray)
    }
  }
}

@Composable
fun ChannelProgressBar(
  label: String,
  amount: Double,
  total: Double,
  currencyFormat: NumberFormat,
  color: Color
) {
  val fraction = if (total > 0) (amount / total).toFloat().coerceIn(0f, 1f) else 0f
  Column {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween
    ) {
      Text(label, style = MaterialTheme.typography.bodySmall, color = GeviniCream)
      Text("PKR ${currencyFormat.format(amount)} (${(fraction * 100).toInt()}%)", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold), color = color)
    }
    Spacer(modifier = Modifier.height(4.dp))
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(6.dp)
        .clip(RoundedCornerShape(3.dp))
        .background(GeviniCharcoalSurface)
    ) {
      Box(
        modifier = Modifier
          .fillMaxWidth(fraction)
          .height(6.dp)
          .clip(RoundedCornerShape(3.dp))
          .background(color)
      )
    }
  }
}
