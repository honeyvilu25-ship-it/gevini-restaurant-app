package com.example.ui.menu

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
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
fun MenuManageScreen(
  repository: RestaurantRepository,
  onNavigateBack: () -> Unit
) {
  val scope = rememberCoroutineScope()
  val products by repository.observeProducts().collectAsState(initial = emptyList())
  val categories by repository.observeCategories().collectAsState(initial = emptyList())

  var editingProduct by remember { mutableStateOf<Product?>(null) }
  var showAddDialog by remember { mutableStateOf(false) }

  val currencyFormat = remember {
    NumberFormat.getNumberInstance(Locale.US).apply { maximumFractionDigits = 0 }
  }

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
      IconButton(onClick = onNavigateBack, modifier = Modifier.testTag("menu_back_button")) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = GeviniCream)
      }

      Column(modifier = Modifier.weight(1f)) {
        Text(
          text = "MENU & RECIPE CATALOG",
          style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.Black),
          color = GeviniCream
        )
        Text(
          text = "Live price adjustments & kitchen station routing",
          style = MaterialTheme.typography.bodySmall,
          color = GeviniGold
        )
      }

      Button(
        onClick = { showAddDialog = true },
        shape = RoundedCornerShape(10.dp),
        colors = ButtonDefaults.buttonColors(containerColor = GeviniBurgundy, contentColor = Color.White)
      ) {
        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
        Spacer(modifier = Modifier.width(6.dp))
        Text("Add Item", style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.Bold))
      }
    }

    Spacer(modifier = Modifier.height(14.dp))

    LazyColumn(
      modifier = Modifier.fillMaxSize(),
      verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
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
                .size(44.dp)
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
                text = "PKR ${currencyFormat.format(product.price)} • Station: ${product.station.uppercase()}",
                style = MaterialTheme.typography.bodySmall,
                color = GeviniGold
              )
              Text(
                text = product.description,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray,
                maxLines = 1
              )
            }

            // Availability Switch
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
              Switch(
                checked = product.available,
                onCheckedChange = { isAvailable ->
                  scope.launch {
                    repository.toggleProductAvailability(product.id, isAvailable)
                  }
                },
                colors = SwitchDefaults.colors(
                  checkedThumbColor = GeviniGold,
                  checkedTrackColor = GeviniBurgundy,
                  uncheckedThumbColor = Color.LightGray,
                  uncheckedTrackColor = GeviniCharcoalBorder
                )
              )
              Text(
                text = if (product.available) "Active" else "Sold Out",
                style = MaterialTheme.typography.labelSmall,
                color = if (product.available) Color(0xFF4CAF50) else Color(0xFFFF6B6B)
              )
            }

            Spacer(modifier = Modifier.width(8.dp))

            IconButton(onClick = { editingProduct = product }) {
              Icon(Icons.Default.Edit, contentDescription = "Edit Price", tint = GeviniCream)
            }
          }
        }
      }
    }
  }

  // Edit Product Price / Details Dialog
  editingProduct?.let { product ->
    EditProductDialog(
      product = product,
      onDismiss = { editingProduct = null },
      onSave = { updatedProduct ->
        scope.launch {
          repository.saveProduct(updatedProduct)
          editingProduct = null
        }
      }
    )
  }

  // Add New Product Dialog
  if (showAddDialog) {
    EditProductDialog(
      product = Product(
        name = "",
        description = "",
        categoryId = categories.firstOrNull()?.id ?: "pizza",
        price = 1000.0,
        available = true,
        station = "pizza"
      ),
      isNew = true,
      onDismiss = { showAddDialog = false },
      onSave = { newProduct ->
        scope.launch {
          repository.saveProduct(newProduct)
          showAddDialog = false
        }
      }
    )
  }
}

@Composable
fun EditProductDialog(
  product: Product,
  isNew: Boolean = false,
  onDismiss: () -> Unit,
  onSave: (Product) -> Unit
) {
  var name by remember { mutableStateOf(product.name) }
  var description by remember { mutableStateOf(product.description) }
  var priceText by remember { mutableStateOf("${product.price.toInt()}") }
  var station by remember { mutableStateOf(product.station) }

  Dialog(onDismissRequest = onDismiss) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .padding(8.dp),
      shape = RoundedCornerShape(16.dp),
      colors = CardDefaults.cardColors(containerColor = GeviniCharcoalSurface),
      border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
    ) {
      Column(modifier = Modifier.padding(20.dp)) {
        Text(
          text = if (isNew) "Add Food Item" else "Edit Item Details",
          style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
          color = GeviniCream
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
          value = name,
          onValueChange = { name = it },
          label = { Text("Product Name", color = Color.Gray) },
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = GeviniCream,
            unfocusedTextColor = GeviniCream,
            focusedContainerColor = GeviniCharcoalCard,
            unfocusedContainerColor = GeviniCharcoalCard
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = priceText,
          onValueChange = { priceText = it },
          label = { Text("Base Price (PKR)", color = Color.Gray) },
          keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = GeviniCream,
            unfocusedTextColor = GeviniCream,
            focusedContainerColor = GeviniCharcoalCard,
            unfocusedContainerColor = GeviniCharcoalCard
          )
        )

        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
          value = description,
          onValueChange = { description = it },
          label = { Text("Description & Ingredients", color = Color.Gray) },
          maxLines = 2,
          modifier = Modifier.fillMaxWidth(),
          colors = OutlinedTextFieldDefaults.colors(
            focusedTextColor = GeviniCream,
            unfocusedTextColor = GeviniCream,
            focusedContainerColor = GeviniCharcoalCard,
            unfocusedContainerColor = GeviniCharcoalCard
          )
        )

        Spacer(modifier = Modifier.height(16.dp))

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
            onClick = {
              val parsedPrice = priceText.toDoubleOrNull() ?: product.price
              onSave(
                product.copy(
                  name = name,
                  description = description,
                  price = parsedPrice,
                  station = station
                )
              )
            },
            modifier = Modifier.weight(1.5f),
            colors = ButtonDefaults.buttonColors(containerColor = GeviniBurgundy, contentColor = Color.White)
          ) {
            Text("Save Product")
          }
        }
      }
    }
  }
}
