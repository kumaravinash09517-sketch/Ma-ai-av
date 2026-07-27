package com.sarkar.jarviscall.ui.screens

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import android.telecom.TelecomManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sarkar.jarviscall.MainViewModel
import com.sarkar.jarviscall.data.ContactFilterEntity
import com.sarkar.jarviscall.data.ContactFilterType
import com.sarkar.jarviscall.ui.theme.*

@Composable
fun SettingsScreen(viewModel: MainViewModel) {
    val context = LocalContext.current
    val contactFilters by viewModel.contactFilters.collectAsState()

    var showAddFilterDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: Default Phone App & System Services Setup
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "System Integration & Permissions",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    // Default Phone App Setup
                    PermissionStatusRow(
                        title = "Default Call Screening App",
                        subtitle = "Required to intercept incoming calls automatically",
                        icon = Icons.Default.PhoneCallback,
                        isGranted = isDefaultPhoneApp(context),
                        actionLabel = "Set Default",
                        onAction = {
                            try {
                                val intent = Intent(TelecomManager.ACTION_CHANGE_DEFAULT_DIALER).apply {
                                    putExtra(TelecomManager.EXTRA_CHANGE_DEFAULT_DIALER_PACKAGE_NAME, context.packageName)
                                }
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                openAppSettings(context)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkSurfaceVariant)

                    // Accessibility Auto-Answer Service
                    PermissionStatusRow(
                        title = "Sarkar Accessibility Assistant",
                        subtitle = "Fallback service for hands-free auto-answering",
                        icon = Icons.Default.AccessibilityNew,
                        isGranted = true, // Status indicator
                        actionLabel = "Configure",
                        onAction = {
                            try {
                                val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                openAppSettings(context)
                            }
                        }
                    )

                    HorizontalDivider(modifier = Modifier.padding(vertical = 10.dp), color = DarkSurfaceVariant)

                    // App System Settings
                    PermissionStatusRow(
                        title = "Microphone & Call State Permissions",
                        subtitle = "RECORD_AUDIO, READ_PHONE_STATE, MANAGE_OWN_CALLS",
                        icon = Icons.Default.Security,
                        isGranted = true,
                        actionLabel = "Permissions",
                        onAction = { openAppSettings(context) }
                    )
                }
            }
        }

        // Section 2: Whitelist & Blacklist Contact Rules
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Whitelist & Blacklist Manager",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Bypass screening for VIPs or block spam instantly",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { showAddFilterDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                    modifier = Modifier.testTag("add_contact_filter_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.PersonAdd, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add", fontWeight = FontWeight.Bold)
                }
            }
        }

        if (contactFilters.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = DarkSurface)
                ) {
                    Box(modifier = Modifier.padding(20.dp), contentAlignment = Alignment.Center) {
                        Text(
                            text = "No contact filters set yet. Add VIPs or Blocked numbers above.",
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }
            }
        } else {
            items(contactFilters) { filter ->
                ContactFilterCardItem(
                    filter = filter,
                    onDelete = { viewModel.deleteContactFilter(filter.id) }
                )
            }
        }

        // Section 3: App Info & Engine Details
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Engine & Offline Runtime",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(text = "App Version: 1.0.0 (Sarkar Jarvis Call)", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    Text(text = "Speech Engine: Android SpeechRecognizer + TTS", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = "Rule Engine: Local Offline Keyword & Intent Matrix", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    Text(text = "Database: Room SQLite (Encrypted Local Storage)", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
            }
        }
    }

    if (showAddFilterDialog) {
        AddContactFilterDialog(
            onDismiss = { showAddFilterDialog = false },
            onConfirm = { number, name, type, note ->
                viewModel.addContactFilter(number, name, type, note)
                showAddFilterDialog = false
            }
        )
    }
}

@Composable
fun PermissionStatusRow(
    title: String,
    subtitle: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    isGranted: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.weight(1f)
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(if (isGranted) SuccessGreen.copy(alpha = 0.2f) else WarningOrange.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = if (isGranted) SuccessGreen else WarningOrange,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
        }

        OutlinedButton(
            onClick = onAction,
            shape = RoundedCornerShape(8.dp)
        ) {
            Text(actionLabel, color = CyanAccent, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ContactFilterCardItem(filter: ContactFilterEntity, onDelete: () -> Unit) {
    val isWhitelist = filter.filterType == ContactFilterType.WHITELIST
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(if (isWhitelist) SuccessGreen.copy(alpha = 0.2f) else SpamRed.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (isWhitelist) Icons.Default.VerifiedUser else Icons.Default.Block,
                        contentDescription = null,
                        tint = if (isWhitelist) SuccessGreen else SpamRed
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = filter.contactName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = filter.phoneNumber,
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (isWhitelist) SuccessGreen.copy(alpha = 0.2f) else SpamRed.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = if (isWhitelist) "VIP WHITELIST" else "SPAM BLACKLIST",
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isWhitelist) SuccessGreen else SpamRed,
                        fontWeight = FontWeight.Bold
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
                }
            }
        }
    }
}

@Composable
fun AddContactFilterDialog(
    onDismiss: () -> Unit,
    onConfirm: (number: String, name: String, type: ContactFilterType, note: String) -> Unit
) {
    var number by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var selectedType by remember { mutableStateOf(ContactFilterType.WHITELIST) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Contact Filter", color = GoldAccent, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Contact Name (e.g. Mom / CEO)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_contact_name")
                )
                OutlinedTextField(
                    value = number,
                    onValueChange = { number = it },
                    label = { Text("Phone Number") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_contact_number")
                )

                Text("Filter Mode:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = selectedType == ContactFilterType.WHITELIST,
                        onClick = { selectedType = ContactFilterType.WHITELIST },
                        label = { Text("VIP Whitelist (Bypass)") }
                    )
                    FilterChip(
                        selected = selectedType == ContactFilterType.BLACKLIST,
                        onClick = { selectedType = ContactFilterType.BLACKLIST },
                        label = { Text("Spam Blacklist (Block)") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (number.isNotBlank()) {
                        onConfirm(number, name.ifBlank { "Unknown" }, selectedType, "")
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                modifier = Modifier.testTag("save_contact_filter_button")
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = TextMuted)
            }
        },
        containerColor = DarkSurface
    )
}

fun isDefaultPhoneApp(context: Context): Boolean {
    val telecomManager = context.getSystemService(Context.TELECOM_SERVICE) as? TelecomManager
    return telecomManager?.defaultDialerPackage == context.packageName
}

fun openAppSettings(context: Context) {
    try {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.fromParts("package", context.packageName, null)
        }
        context.startActivity(intent)
    } catch (e: Exception) {
        // Fallback
    }
}
