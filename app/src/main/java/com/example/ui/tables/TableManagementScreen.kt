package com.example.ui.tables

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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.TableBar
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.DiningTable
import com.example.data.model.TableStatus
import com.example.data.repository.RestaurantRepository
import com.example.ui.theme.GeviniBurgundy
import com.example.ui.theme.GeviniCharcoal
import com.example.ui.theme.GeviniCharcoalBorder
import com.example.ui.theme.GeviniCharcoalCard
import com.example.ui.theme.GeviniCream
import com.example.ui.theme.GeviniGold
import kotlinx.coroutines.launch

@Composable
fun TableManagementScreen(
  repository: RestaurantRepository,
  onNavigateBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val tables by repository.observeTables().collectAsState(initial = emptyList())

  Column(
    modifier = Modifier
      .fillMaxSize()
      .background(GeviniCharcoal)
      .padding(16.dp)
  ) {
    // Header
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically
    ) {
      IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("tables_back_button")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GeviniCream)
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "FLOOR & TABLE SERVICE",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
          color = GeviniCream
        )
        Text(
          text = "Dine-in floor plan • Live occupancy & turn status",
          style = MaterialTheme.typography.bodySmall,
          color = GeviniGold
        )
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Table Status Legend
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .clip(RoundedCornerShape(12.dp))
        .background(GeviniCharcoalCard)
        .padding(horizontal = 12.dp, vertical = 8.dp),
      horizontalArrangement = Arrangement.SpaceAround
    ) {
      LegendItem("Available", Color(0xFF4CAF50))
      LegendItem("Occupied", GeviniBurgundy)
      LegendItem("Reserved", GeviniGold)
      LegendItem("Cleaning", Color(0xFFFF9800))
    }

    Spacer(modifier = Modifier.height(14.dp))

    // Tables Grid
    LazyVerticalGrid(
      columns = GridCells.Adaptive(minSize = 150.dp),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      modifier = Modifier.fillMaxSize()
    ) {
      items(tables) { table ->
        val statusColor = when (table.status) {
          TableStatus.AVAILABLE.code -> Color(0xFF4CAF50)
          TableStatus.OCCUPIED.code -> GeviniBurgundy
          TableStatus.RESERVED.code -> GeviniGold
          TableStatus.CLEANING.code -> Color(0xFFFF9800)
          else -> Color.Gray
        }

        Card(
          modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, GeviniCharcoalBorder, RoundedCornerShape(16.dp))
            .clickable {
              // Toggle status cycle: Available -> Occupied -> Cleaning -> Available
              val nextStatus = when (table.status) {
                TableStatus.AVAILABLE.code -> TableStatus.OCCUPIED.code
                TableStatus.OCCUPIED.code -> TableStatus.CLEANING.code
                TableStatus.CLEANING.code -> TableStatus.AVAILABLE.code
                else -> TableStatus.AVAILABLE.code
              }
              scope.launch {
                repository.updateTableStatus(table.id, nextStatus)
              }
            },
          colors = CardDefaults.cardColors(containerColor = GeviniCharcoalCard)
        ) {
          Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
          ) {
            Box(
              modifier = Modifier
                .size(48.dp)
                .clip(CircleShape)
                .background(statusColor.copy(alpha = 0.25f)),
              contentAlignment = Alignment.Center
            ) {
              Icon(Icons.Default.TableBar, contentDescription = null, tint = statusColor, modifier = Modifier.size(28.dp))
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
              text = table.tableNumber,
              style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
              color = GeviniCream
            )

            Text(
              text = "${table.capacity} Seats • ${table.section}",
              style = MaterialTheme.typography.labelSmall,
              color = Color.Gray
            )

            Spacer(modifier = Modifier.height(8.dp))

            Box(
              modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(statusColor)
                .padding(horizontal = 8.dp, vertical = 2.dp)
            ) {
              Text(
                text = TableStatus.fromCode(table.status).label.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.Bold),
                color = Color.White
              )
            }
          }
        }
      }
    }
  }
}

@Composable
fun LegendItem(label: String, color: Color) {
  Row(verticalAlignment = Alignment.CenterVertically) {
    Box(
      modifier = Modifier
        .size(10.dp)
        .clip(CircleShape)
        .background(color)
    )
    Spacer(modifier = Modifier.width(6.dp))
    Text(label, style = MaterialTheme.typography.labelSmall, color = GeviniCream)
  }
}
