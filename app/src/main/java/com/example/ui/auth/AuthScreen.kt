package com.example.ui.auth

import android.app.Activity
import android.content.Context
import android.util.Log
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Dining
import androidx.compose.material.icons.filled.LocalPizza
import androidx.compose.material.icons.filled.PointOfSale
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import com.example.R
import com.example.data.model.UserRole
import com.example.ui.theme.GeviniBurgundy
import com.example.ui.theme.GeviniCharcoal
import com.example.ui.theme.GeviniCharcoalBorder
import com.example.ui.theme.GeviniCharcoalCard
import com.example.ui.theme.GeviniCream
import com.example.ui.theme.GeviniGold
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.Companion.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
import com.google.firebase.Firebase
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

private const val TAG = "GeviniAuth"

fun onGoogleSignInClicked(
  context: Context,
  credentialManager: CredentialManager,
  onAuthSuccess: () -> Unit,
  onAuthError: (String) -> Unit,
  scope: CoroutineScope,
  onAuthCancelled: () -> Unit = {}
) {
  val clientId = try {
    context.getString(R.string.default_web_client_id)
  } catch (e: Exception) {
    onAuthError("Google Sign-In configuration missing: default_web_client_id not found")
    return
  }

  val signInOption = GetSignInWithGoogleOption.Builder(serverClientId = clientId).build()
  val request = GetCredentialRequest.Builder().addCredentialOption(signInOption).build()

  scope.launch {
    try {
      val result = credentialManager.getCredential(context as Activity, request)
      val credential = result.credential
      if (credential is CustomCredential && credential.type == TYPE_GOOGLE_ID_TOKEN_CREDENTIAL) {
        val googleIdToken = GoogleIdTokenCredential.createFrom(credential.data).idToken
        val authCredential = GoogleAuthProvider.getCredential(googleIdToken, null)
        Firebase.auth.signInWithCredential(authCredential).await()
        onAuthSuccess()
      } else {
        onAuthError("Unexpected credential type")
      }
    } catch (e: GetCredentialCancellationException) {
      Log.w(TAG, "Google Sign-In flow cancelled or dismissed: ${e.message}", e)
      onAuthCancelled()
    } catch (e: Exception) {
      Log.e(TAG, "Google Sign-In failed", e)
      onAuthError(e.localizedMessage ?: "Sign in failed")
    }
  }
}

fun signOutUser(
  context: Context,
  credentialManager: CredentialManager,
  onSignOutComplete: () -> Unit,
  scope: CoroutineScope
) {
  Firebase.auth.signOut()
  scope.launch {
    try {
      credentialManager.clearCredentialState(ClearCredentialStateRequest())
    } catch (e: Exception) {
      Log.e(TAG, "Failed to clear credential state", e)
    } finally {
      onSignOutComplete()
    }
  }
}

@Composable
fun AuthScreen(
  onAuthSuccess: (UserRole) -> Unit
) {
  val context = LocalContext.current
  val coroutineScope = rememberCoroutineScope()
  val credentialManager = remember { CredentialManager.create(context) }
  var isLoading by remember { mutableStateOf(false) }
  var errorMessage by remember { mutableStateOf<String?>(null) }
  var selectedRole by remember { mutableStateOf(UserRole.CASHIER) }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .background(GeviniCharcoal)
      .padding(24.dp),
    contentAlignment = Alignment.Center
  ) {
    Card(
      modifier = Modifier
        .fillMaxWidth()
        .verticalScroll(rememberScrollState()),
      shape = RoundedCornerShape(24.dp),
      colors = CardDefaults.cardColors(containerColor = GeviniCharcoalCard),
      border = androidx.compose.foundation.BorderStroke(1.dp, GeviniCharcoalBorder)
    ) {
      Column(
        modifier = Modifier.padding(28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
      ) {
        // Brand Header
        Box(
          modifier = Modifier
            .size(72.dp)
            .clip(CircleShape)
            .background(GeviniBurgundy),
          contentAlignment = Alignment.Center
        ) {
          Icon(
            imageVector = Icons.Default.LocalPizza,
            contentDescription = "GEVINI Logo",
            tint = GeviniGold,
            modifier = Modifier.size(40.dp)
          )
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
          text = "GEVINI RESTAURANT OS",
          style = MaterialTheme.typography.titleLarge.copy(
            fontWeight = FontWeight.Black,
            letterSpacing = 1.5.sp
          ),
          color = GeviniCream
        )

        Text(
          text = "Malir 15, Karachi • Omnichannel POS & KDS",
          style = MaterialTheme.typography.bodyMedium,
          color = GeviniGold,
          modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(24.dp))
        HorizontalDivider(color = GeviniCharcoalBorder)
        Spacer(modifier = Modifier.height(20.dp))

        // Role Selector Section
        Text(
          text = "Select Terminal Access Role",
          style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.Bold),
          color = GeviniCream,
          modifier = Modifier.align(Alignment.Start)
        )
        Text(
          text = "Configure terminal for cashier POS, kitchen display, or management",
          style = MaterialTheme.typography.bodySmall,
          color = Color.Gray,
          modifier = Modifier
            .align(Alignment.Start)
            .padding(bottom = 12.dp)
        )

        val roles = listOf(
          Triple(UserRole.CASHIER, "POS & Cashier Terminal", Icons.Default.PointOfSale),
          Triple(UserRole.KITCHEN, "Kitchen Display System (KDS)", Icons.Default.Restaurant),
          Triple(UserRole.SUPER_ADMIN, "Owner & Analytics Dashboard", Icons.Default.AccountCircle),
          Triple(UserRole.WAITER, "Dine-In & Tables Management", Icons.Default.Dining),
          Triple(UserRole.CUSTOMER, "Customer Online Ordering", Icons.Default.ShoppingBag)
        )

        roles.forEach { (role, label, icon) ->
          val isSelected = selectedRole == role
          Row(
            modifier = Modifier
              .fillMaxWidth()
              .padding(vertical = 4.dp)
              .clip(RoundedCornerShape(12.dp))
              .background(if (isSelected) GeviniBurgundy.copy(alpha = 0.35f) else Color.Transparent)
              .border(
                1.dp,
                if (isSelected) GeviniBurgundy else GeviniCharcoalBorder,
                RoundedCornerShape(12.dp)
              )
              .clickable { selectedRole = role }
              .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              imageVector = icon,
              contentDescription = label,
              tint = if (isSelected) GeviniGold else Color.LightGray,
              modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
              Text(
                text = role.label,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = if (isSelected) GeviniCream else Color.LightGray
              )
              Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Color.Gray
              )
            }
            if (isSelected) {
              Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = "Selected",
                tint = GeviniGold,
                modifier = Modifier.size(20.dp)
              )
            }
          }
        }

        Spacer(modifier = Modifier.height(24.dp))

        if (errorMessage != null) {
          Text(
            text = errorMessage ?: "",
            color = Color(0xFFFF6B6B),
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier.padding(bottom = 12.dp),
            textAlign = TextAlign.Center
          )
        }

        // Sign in with Google Button
        Button(
          onClick = {
            isLoading = true
            errorMessage = null
            onGoogleSignInClicked(
              context = context,
              credentialManager = credentialManager,
              onAuthSuccess = {
                isLoading = false
                onAuthSuccess(selectedRole)
              },
              onAuthError = { errorMsg ->
                isLoading = false
                errorMessage = errorMsg
              },
              scope = coroutineScope,
              onAuthCancelled = {
                isLoading = false
              }
            )
          },
          enabled = !isLoading,
          modifier = Modifier
            .fillMaxWidth()
            .height(52.dp)
            .testTag("google_signin_button"),
          shape = RoundedCornerShape(14.dp),
          colors = ButtonDefaults.buttonColors(
            containerColor = GeviniBurgundy,
            contentColor = Color.White
          )
        ) {
          if (isLoading) {
            CircularProgressIndicator(
              modifier = Modifier.size(24.dp),
              color = GeviniGold,
              strokeWidth = 2.dp
            )
          } else {
            Row(
              verticalAlignment = Alignment.CenterVertically,
              horizontalArrangement = Arrangement.Center
            ) {
              Icon(
                imageVector = Icons.Default.AccountCircle,
                contentDescription = null,
                tint = GeviniGold,
                modifier = Modifier.size(22.dp)
              )
              Spacer(modifier = Modifier.width(10.dp))
              Text(
                text = "Sign in with Google",
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Bold)
              )
            }
          }
        }

        // Quick Terminal Launch Button (when signed in or entering chosen role directly)
        if (Firebase.auth.currentUser != null) {
          Spacer(modifier = Modifier.height(12.dp))
          Button(
            onClick = { onAuthSuccess(selectedRole) },
            modifier = Modifier
              .fillMaxWidth()
              .height(48.dp)
              .testTag("launch_terminal_button"),
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(
              containerColor = GeviniGold,
              contentColor = Color.Black
            )
          ) {
            Text(
              text = "Launch ${selectedRole.label}",
              style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold)
            )
          }
        }
      }
    }
  }
}
