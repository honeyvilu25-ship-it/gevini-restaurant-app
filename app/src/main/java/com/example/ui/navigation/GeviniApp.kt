package com.example.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.data.model.UserRole
import com.example.data.repository.RestaurantRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.dashboard.DashboardScreen
import com.example.ui.kds.KdsScreen
import com.example.ui.menu.MenuManageScreen
import com.example.ui.online.OnlineOrderingScreen
import com.example.ui.pos.PosScreen
import com.example.ui.tables.TableManagementScreen
import com.example.ui.theme.GeviniBurgundy
import com.example.ui.theme.GeviniCharcoal
import com.example.ui.theme.GeviniCharcoalBorder
import com.example.ui.theme.GeviniCharcoalSurface
import com.example.ui.theme.GeviniCream
import com.example.ui.theme.GeviniGold
import com.google.firebase.Firebase
import com.google.firebase.auth.auth

enum class Screen(val title: String) {
  AUTH("Auth"),
  POS("POS Terminal"),
  KDS("Kitchen Display"),
  DASHBOARD("Owner Dashboard"),
  MENU("Menu Catalog"),
  TABLES("Tables & Floor"),
  ONLINE("Online Ordering")
}

@Composable
fun GeviniApp() {
  val context = LocalContext.current
  val repository = remember { RestaurantRepository.getInstance(context) }
  var currentUser by remember { mutableStateOf(Firebase.auth.currentUser) }
  var currentScreen by remember { mutableStateOf(if (currentUser != null) Screen.POS else Screen.AUTH) }
  var userRole by remember { mutableStateOf(UserRole.CASHIER) }

  // Back handling: Return to POS/Dashboard or Auth
  BackHandler(enabled = currentScreen != Screen.POS && currentScreen != Screen.AUTH) {
    currentScreen = if (userRole == UserRole.SUPER_ADMIN) Screen.DASHBOARD else Screen.POS
  }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .statusBarsPadding()
      .navigationBarsPadding(),
    bottomBar = {
      if (currentScreen != Screen.AUTH) {
        NavigationBar(
          containerColor = GeviniCharcoalSurface,
          contentColor = GeviniCream
        ) {
          NavigationBarItem(
            selected = currentScreen == Screen.POS,
            onClick = { currentScreen = Screen.POS },
            icon = { Icon(Icons.Default.PointOfSale, contentDescription = "POS") },
            label = { Text("POS") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GeviniGold,
              selectedTextColor = GeviniGold,
              indicatorColor = GeviniBurgundy,
              unselectedIconColor = Color.Gray,
              unselectedTextColor = Color.Gray
            )
          )

          NavigationBarItem(
            selected = currentScreen == Screen.KDS,
            onClick = { currentScreen = Screen.KDS },
            icon = { Icon(Icons.Default.Restaurant, contentDescription = "KDS") },
            label = { Text("Kitchen") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GeviniGold,
              selectedTextColor = GeviniGold,
              indicatorColor = GeviniBurgundy,
              unselectedIconColor = Color.Gray,
              unselectedTextColor = Color.Gray
            )
          )

          NavigationBarItem(
            selected = currentScreen == Screen.TABLES,
            onClick = { currentScreen = Screen.TABLES },
            icon = { Icon(Icons.Default.Dining, contentDescription = "Tables") },
            label = { Text("Tables") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GeviniGold,
              selectedTextColor = GeviniGold,
              indicatorColor = GeviniBurgundy,
              unselectedIconColor = Color.Gray,
              unselectedTextColor = Color.Gray
            )
          )

          NavigationBarItem(
            selected = currentScreen == Screen.DASHBOARD,
            onClick = { currentScreen = Screen.DASHBOARD },
            icon = { Icon(Icons.Default.AccountCircle, contentDescription = "Dashboard") },
            label = { Text("Reports") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GeviniGold,
              selectedTextColor = GeviniGold,
              indicatorColor = GeviniBurgundy,
              unselectedIconColor = Color.Gray,
              unselectedTextColor = Color.Gray
            )
          )

          NavigationBarItem(
            selected = currentScreen == Screen.ONLINE,
            onClick = { currentScreen = Screen.ONLINE },
            icon = { Icon(Icons.Default.ShoppingBag, contentDescription = "Online") },
            label = { Text("Customer") },
            colors = NavigationBarItemDefaults.colors(
              selectedIconColor = GeviniGold,
              selectedTextColor = GeviniGold,
              indicatorColor = GeviniBurgundy,
              unselectedIconColor = Color.Gray,
              unselectedTextColor = Color.Gray
            )
          )
        }
      }
    }
  ) { innerPadding ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .padding(innerPadding)
        .background(GeviniCharcoal)
    ) {
      when (currentScreen) {
        Screen.AUTH -> {
          AuthScreen(
            onAuthSuccess = { role ->
              userRole = role
              currentUser = Firebase.auth.currentUser
              currentScreen = when (role) {
                UserRole.SUPER_ADMIN, UserRole.MANAGER, UserRole.ACCOUNTANT -> Screen.DASHBOARD
                UserRole.KITCHEN -> Screen.KDS
                UserRole.WAITER -> Screen.TABLES
                UserRole.CUSTOMER -> Screen.ONLINE
                else -> Screen.POS
              }
            }
          )
        }

        Screen.POS -> {
          PosScreen(
            repository = repository,
            onNavigateBack = { currentScreen = Screen.AUTH }
          )
        }

        Screen.KDS -> {
          KdsScreen(
            repository = repository,
            onNavigateBack = { currentScreen = Screen.POS }
          )
        }

        Screen.DASHBOARD -> {
          DashboardScreen(
            repository = repository,
            onNavigateToPos = { currentScreen = Screen.POS },
            onNavigateToKds = { currentScreen = Screen.KDS },
            onNavigateToMenu = { currentScreen = Screen.MENU },
            onNavigateToTables = { currentScreen = Screen.TABLES },
            onNavigateBack = { currentScreen = Screen.POS }
          )
        }

        Screen.MENU -> {
          MenuManageScreen(
            repository = repository,
            onNavigateBack = { currentScreen = Screen.DASHBOARD }
          )
        }

        Screen.TABLES -> {
          TableManagementScreen(
            repository = repository,
            onNavigateBack = { currentScreen = Screen.POS }
          )
        }

        Screen.ONLINE -> {
          OnlineOrderingScreen(
            repository = repository,
            onNavigateBack = { currentScreen = Screen.POS }
          )
        }
      }
    }
  }
}
