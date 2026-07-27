package com.sarkar.jarviscall.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.sarkar.jarviscall.MainViewModel
import com.sarkar.jarviscall.data.CallLogEntity
import com.sarkar.jarviscall.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun TranscriptsScreen(viewModel: MainViewModel) {
    val callLogs by viewModel.callLogs.collectAsState()
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf("All") }

    val filteredLogs = callLogs.filter { log ->
        val matchesSearch = log.phoneNumber.contains(searchQuery, ignoreCase = true) ||
                log.actionTaken.contains(searchQuery, ignoreCase = true) ||
                log.categoryTag.contains(searchQuery, ignoreCase = true)
        val matchesCategory = when (selectedFilter) {
            "Spam" -> log.isSpam
            "Delivery" -> log.categoryTag.equals("Delivery", ignoreCase = true)
            "Urgent" -> log.categoryTag.equals("Urgent", ignoreCase = true)
            else -> true
        }
        matchesSearch && matchesCategory
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Screened Call Transcripts",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Full audio dialogue logs & summary notes",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            if (callLogs.isNotEmpty()) {
                IconButton(
                    onClick = { viewModel.clearAllCallHistory() },
                    modifier = Modifier.testTag("clear_logs_button")
                ) {
                    Icon(Icons.Default.DeleteSweep, contentDescription = "Clear All", tint = SpamRed)
                }
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            placeholder = { Text("Search by phone number, tag, or action...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = TextMuted) },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("transcript_search_input"),
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
        )

        Spacer(modifier = Modifier.height(10.dp))

        // Filter Pills
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf("All", "Spam", "Delivery", "Urgent").forEach { filter ->
                FilterChip(
                    selected = selectedFilter == filter,
                    onClick = { selectedFilter = filter },
                    label = { Text(filter) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = CyanAccent,
                        selectedLabelColor = Color.Black
                    )
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (filteredLogs.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.FolderOpen, contentDescription = null, tint = TextMuted, modifier = Modifier.size(54.dp))
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("No transcript logs found", style = MaterialTheme.typography.bodyMedium, color = TextMuted)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(filteredLogs) { log ->
                    TranscriptCardDetail(log = log, onDelete = { viewModel.deleteCallLog(log.id) })
                }
            }
        }
    }
}

@Composable
fun TranscriptCardDetail(log: CallLogEntity, onDelete: () -> Unit) {
    var isExpanded by remember { mutableStateOf(false) }
    val dateFormat = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val dateStr = dateFormat.format(Date(log.timestamp))

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { isExpanded = !isExpanded },
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
                    Box(
                        modifier = Modifier
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(if (log.isSpam) SpamRed.copy(alpha = 0.2f) else CyanAccent.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (log.isSpam) Icons.Default.Block else Icons.Default.ReceiptLong,
                            contentDescription = null,
                            tint = if (log.isSpam) SpamRed else CyanAccent
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = log.phoneNumber,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.bodySmall,
                            color = TextMuted
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (log.isSpam) SpamRed.copy(alpha = 0.2f) else GoldAccent.copy(alpha = 0.2f)
                ) {
                    Text(
                        text = log.categoryTag,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (log.isSpam) SpamRed else GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = "Action Taken: ${log.actionTaken}",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium,
                color = TextPrimary
            )

            if (log.summaryNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Summary: ${log.summaryNote}",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }

            AnimatedVisibility(visible = isExpanded) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    HorizontalDivider(color = DarkSurfaceVariant)
                    Spacer(modifier = Modifier.height(10.dp))
                    Text(
                        text = "Turn-by-Turn Conversation Transcript:",
                        style = MaterialTheme.typography.labelMedium,
                        color = GoldAccent,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))

                    val turns = parseJsonTranscript(log.transcriptJson)
                    turns.forEach { (sender, text) ->
                        val isAi = sender.contains("AI") || sender.contains("Sarkar")
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = if (isAi) Arrangement.End else Arrangement.Start
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isAi) BluePrimaryContainer else DarkSurfaceVariant,
                                modifier = Modifier.widthIn(max = 280.dp)
                            ) {
                                Column(modifier = Modifier.padding(8.dp)) {
                                    Text(
                                        text = sender,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAi) GoldAccent else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = text,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = onDelete) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = SpamRed)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Delete Log", color = SpamRed)
                        }
                    }
                }
            }
        }
    }
}

fun parseJsonTranscript(json: String): List<Pair<String, String>> {
    val list = mutableListOf<Pair<String, String>>()
    if (json.isBlank()) return list
    try {
        val regex = """\{"sender":"(.*?)","text":"(.*?)"\}""".toRegex()
        val matches = regex.findAll(json)
        for (match in matches) {
            val sender = match.groupValues[1].replace("\\\"", "\"")
            val text = match.groupValues[2].replace("\\\"", "\"")
            list.add(sender to text)
        }
    } catch (e: Exception) {
        list.add("Transcript" to json)
    }
    return list
}
