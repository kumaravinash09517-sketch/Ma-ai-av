package com.sarkar.jarviscall.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sarkar.jarviscall.MainViewModel
import com.sarkar.jarviscall.RuleBasedResponder
import com.sarkar.jarviscall.data.CallActionType
import com.sarkar.jarviscall.data.RuleEntity
import com.sarkar.jarviscall.ui.theme.*

@Composable
fun RulesScreen(viewModel: MainViewModel) {
    val rules by viewModel.rules.collectAsState()
    val activePersona by viewModel.selectedPersona.collectAsState()
    var voicePitch by remember { mutableStateOf(1.0f) }
    var voiceSpeed by remember { mutableStateOf(1.0f) }

    var showAddRuleDialog by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        contentPadding = PaddingValues(bottom = 90.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Section 1: AI Receptionist Voice & Persona Config
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = "Receptionist Persona & Voice Engine",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = GoldAccent
                        )
                        Icon(Icons.Default.RecordVoiceOver, contentDescription = null, tint = GoldAccent)
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Active Persona Style:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    RuleBasedResponder.DEFAULT_PERSONAS.forEach { persona ->
                        val isSelected = persona.id == activePersona.id
                        FilterChip(
                            selected = isSelected,
                            onClick = { viewModel.selectedPersona.value = persona },
                            label = { Text(persona.name) },
                            leadingIcon = {
                                if (isSelected) Icon(Icons.Default.Check, contentDescription = null, tint = GoldAccent)
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    Text(
                        text = "Voice Pitch: ${String.format("%.1f", voicePitch)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Slider(
                        value = voicePitch,
                        onValueChange = {
                            voicePitch = it
                            viewModel.voicePitch.value = it
                        },
                        valueRange = 0.5f..1.5f,
                        steps = 10,
                        colors = SliderDefaults.colors(thumbColor = GoldAccent, activeTrackColor = GoldAccent)
                    )

                    Text(
                        text = "Voice Speed: ${String.format("%.1f", voiceSpeed)}",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextPrimary
                    )
                    Slider(
                        value = voiceSpeed,
                        onValueChange = {
                            voiceSpeed = it
                            viewModel.voiceSpeed.value = it
                        },
                        valueRange = 0.5f..1.5f,
                        steps = 10,
                        colors = SliderDefaults.colors(thumbColor = CyanAccent, activeTrackColor = CyanAccent)
                    )
                }
            }
        }

        // Section 2: Dialogue Rules Header & Add Button
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Dialogue & Keyword Rules",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Rules evaluated offline against caller speech",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextSecondary
                    )
                }

                Button(
                    onClick = { showAddRuleDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                    modifier = Modifier.testTag("add_new_rule_button"),
                    shape = RoundedCornerShape(10.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Add Rule", fontWeight = FontWeight.Bold)
                }
            }
        }

        // Active Rules List
        items(rules) { rule ->
            RuleCardItem(
                rule = rule,
                onDelete = { viewModel.deleteRule(rule.id) }
            )
        }
    }

    if (showAddRuleDialog) {
        AddRuleDialog(
            onDismiss = { showAddRuleDialog = false },
            onConfirm = { title, keywords, responseText, actionType ->
                viewModel.addRule(title, keywords, responseText, actionType)
                showAddRuleDialog = false
            }
        )
    }
}

@Composable
fun RuleCardItem(rule: RuleEntity, onDelete: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = DarkSurface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = when (rule.actionType) {
                            CallActionType.DECLINE_AND_BLOCK -> SpamRed.copy(alpha = 0.2f)
                            CallActionType.ACCEPT_AND_NOTIFY -> SuccessGreen.copy(alpha = 0.2f)
                            else -> CyanAccent.copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            text = rule.actionType.name,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            style = MaterialTheme.typography.labelSmall,
                            color = when (rule.actionType) {
                                CallActionType.DECLINE_AND_BLOCK -> SpamRed
                                CallActionType.ACCEPT_AND_NOTIFY -> SuccessGreen
                                else -> CyanAccent
                            },
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = rule.title,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                }

                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = TextMuted)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Keywords: ${rule.keywordsCsv}",
                style = MaterialTheme.typography.bodySmall,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "TTS Response: \"${rule.responseText}\"",
                style = MaterialTheme.typography.bodyMedium,
                color = GoldAccent
            )
        }
    }
}

@Composable
fun AddRuleDialog(
    onDismiss: () -> Unit,
    onConfirm: (title: String, keywords: String, responseText: String, actionType: CallActionType) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var keywords by remember { mutableStateOf("") }
    var responseText by remember { mutableStateOf("") }
    var selectedAction by remember { mutableStateOf(CallActionType.DECLINE_AND_BLOCK) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create Custom Dialogue Rule", color = GoldAccent, fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Rule Title (e.g. Block Crypto Spam)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_rule_title")
                )
                OutlinedTextField(
                    value = keywords,
                    onValueChange = { keywords = it },
                    label = { Text("Keywords (comma separated: crypto, bitcoin)") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_rule_keywords")
                )
                OutlinedTextField(
                    value = responseText,
                    onValueChange = { responseText = it },
                    label = { Text("TTS Response to Caller") },
                    modifier = Modifier.fillMaxWidth().testTag("dialog_rule_response")
                )

                Text("Action Type:", style = MaterialTheme.typography.labelMedium, color = TextMuted)
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = selectedAction == CallActionType.DECLINE_AND_BLOCK,
                        onClick = { selectedAction = CallActionType.DECLINE_AND_BLOCK },
                        label = { Text("Block") }
                    )
                    FilterChip(
                        selected = selectedAction == CallActionType.TAKE_MESSAGE,
                        onClick = { selectedAction = CallActionType.TAKE_MESSAGE },
                        label = { Text("Take Msg") }
                    )
                    FilterChip(
                        selected = selectedAction == CallActionType.ACCEPT_AND_NOTIFY,
                        onClick = { selectedAction = CallActionType.ACCEPT_AND_NOTIFY },
                        label = { Text("Connect") }
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (title.isNotBlank() && keywords.isNotBlank()) {
                        onConfirm(title, keywords, responseText.ifBlank { "Thank you, goodbye." }, selectedAction)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                modifier = Modifier.testTag("save_rule_dialog_button")
            ) {
                Text("Save Rule", fontWeight = FontWeight.Bold)
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
