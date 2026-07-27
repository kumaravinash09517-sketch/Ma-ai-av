package com.sarkar.jarviscall.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
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
import androidx.compose.ui.unit.sp
import com.sarkar.jarviscall.MainViewModel
import com.sarkar.jarviscall.ui.theme.*
import kotlinx.coroutines.launch

@Composable
fun SimulatorScreen(viewModel: MainViewModel) {
    val simState by viewModel.simState.collectAsState()
    val listState = rememberLazyListState()
    val scope = rememberCoroutineScope()

    var customCallerNumber by remember { mutableStateOf("+1 (800) 555-0199") }
    var customCallerName by remember { mutableStateOf("Unknown Caller") }
    var inputSpeechText by remember { mutableStateOf("") }

    LaunchedEffect(simState.transcriptTurns.size) {
        if (simState.transcriptTurns.isNotEmpty()) {
            scope.launch {
                listState.animateScrollToItem(simState.transcriptTurns.size - 1)
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp)
            .padding(bottom = 80.dp)
    ) {
        // Top Title Header
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Live Screening Test Bench",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = "Test real-time dialogue rules & voice engine",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )
            }
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = GoldAccent.copy(alpha = 0.2f)
            ) {
                Text(
                    text = "OFFLINE MODE",
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    style = MaterialTheme.typography.labelSmall,
                    color = GoldAccent,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (!simState.isSimulatingCall) {
            // Call Configuration & Quick Presets
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "1. Configure Test Call",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = GoldAccent
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    OutlinedTextField(
                        value = customCallerName,
                        onValueChange = { customCallerName = it },
                        label = { Text("Caller Name / Title") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("caller_name_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = customCallerNumber,
                        onValueChange = { customCallerNumber = it },
                        label = { Text("Phone Number") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("caller_number_input"),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                    )

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = "Quick Test Scenarios:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    // Scenario Presets
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        FilterChip(
                            selected = false,
                            onClick = {
                                customCallerName = "Axis Bank Loans"
                                customCallerNumber = "+91 98200 11223"
                            },
                            label = { Text("Spam Loan") },
                            leadingIcon = { Icon(Icons.Default.Block, contentDescription = null, tint = SpamRed) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                customCallerName = "Swiggy Delivery"
                                customCallerNumber = "+91 88900 44556"
                            },
                            label = { Text("Delivery") },
                            leadingIcon = { Icon(Icons.Default.LocalShipping, contentDescription = null, tint = CyanAccent) }
                        )
                        FilterChip(
                            selected = false,
                            onClick = {
                                customCallerName = "Boss / Office"
                                customCallerNumber = "+1 (555) 019-2834"
                            },
                            label = { Text("Urgent Work") },
                            leadingIcon = { Icon(Icons.Default.Warning, contentDescription = null, tint = GoldAccent) }
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = {
                            viewModel.startSimulatedCall(
                                callerNumber = customCallerNumber,
                                callerName = customCallerName
                            )
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("start_simulation_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = CyanAccent, contentColor = Color.Black),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Call, contentDescription = "Call")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Start Incoming Call Simulation", fontWeight = FontWeight.Bold)
                    }
                }
            }
        } else {
            // Active Live In-Call Screen
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, CyanAccent, RoundedCornerShape(16.dp)),
                shape = RoundedCornerShape(16.dp),
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
                        PulsingCallIndicator()
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = simState.callerName,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Text(
                                text = "${simState.callerNumber} • ${simState.callStateString}",
                                style = MaterialTheme.typography.bodySmall,
                                color = CyanAccent
                            )
                        }
                    }

                    IconButton(
                        onClick = { viewModel.endSimulatedCall("Ended manually") },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(SpamRed)
                            .testTag("end_simulation_button")
                    ) {
                        Icon(Icons.Default.CallEnd, contentDescription = "End Call", tint = Color.White)
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // AI Status Bar
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(10.dp),
                color = DarkSurfaceVariant
            ) {
                Row(
                    modifier = Modifier.padding(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.AutoAwesome,
                        contentDescription = "AI Status",
                        tint = GoldAccent,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Status: ${simState.currentAiAction}",
                        style = MaterialTheme.typography.bodySmall,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Live Transcript Feed
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(DarkBackground, RoundedCornerShape(12.dp))
                    .padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(simState.transcriptTurns) { turn ->
                    val isAi = turn.sender.contains("AI") || turn.sender.contains("Sarkar")
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = if (isAi) Arrangement.End else Arrangement.Start
                    ) {
                        Card(
                            shape = RoundedCornerShape(
                                topStart = 16.dp,
                                topEnd = 16.dp,
                                bottomStart = if (isAi) 16.dp else 2.dp,
                                bottomEnd = if (isAi) 2.dp else 16.dp
                            ),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isAi) BluePrimaryContainer else DarkSurfaceVariant
                            ),
                            modifier = Modifier.widthIn(max = 280.dp)
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Icon(
                                        imageVector = if (isAi) Icons.Default.SmartToy else Icons.Default.Person,
                                        contentDescription = null,
                                        tint = if (isAi) GoldAccent else TextSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = turn.sender,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = if (isAi) GoldAccent else TextSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = turn.text,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Simulated Caller Speech Input Row
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = DarkSurface)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = "Simulate Caller Spoken Response:",
                        style = MaterialTheme.typography.labelMedium,
                        color = TextMuted
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        OutlinedTextField(
                            value = inputSpeechText,
                            onValueChange = { inputSpeechText = it },
                            placeholder = { Text("e.g. 'I am calling regarding a personal loan offer'") },
                            modifier = Modifier
                                .weight(1f)
                                .testTag("simulated_caller_speech_input"),
                            singleLine = true,
                            colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = CyanAccent)
                        )

                        Spacer(modifier = Modifier.width(8.dp))

                        IconButton(
                            onClick = {
                                if (inputSpeechText.isNotBlank()) {
                                    viewModel.submitSimulatedCallerSpeech(inputSpeechText)
                                    inputSpeechText = ""
                                }
                            },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(CyanAccent)
                                .testTag("send_speech_button")
                        ) {
                            Icon(Icons.Default.Send, contentDescription = "Send", tint = Color.Black)
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Canned Quick Spoken Statements
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        SuggestionChip(
                            onClick = {
                                inputSpeechText = "Hi, calling from bank regarding instant credit card approval"
                            },
                            label = { Text("Say Loan", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                inputSpeechText = "I have a Swiggy food parcel delivery at your doorstep"
                            },
                            label = { Text("Say Parcel", fontSize = 11.sp) }
                        )
                        SuggestionChip(
                            onClick = {
                                inputSpeechText = "This is urgent! Production database down at office"
                            },
                            label = { Text("Say Urgent", fontSize = 11.sp) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun PulsingCallIndicator() {
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val alpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "Alpha"
    )

    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(CircleShape)
            .background(CyanAccent.copy(alpha = alpha))
    )
}
