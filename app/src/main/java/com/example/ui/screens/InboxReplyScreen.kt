package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.AssistantSettings
import com.example.data.model.InboxItem
import com.example.viewmodel.UiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun InboxReplyScreen(
    inboxState: UiState<List<InboxItem>>,
    settings: AssistantSettings,
    onProcessPendingQueue: () -> Unit,
    onCheckAndAutoReplyIncoming: (channel: String, sender: String, subject: String, body: String) -> Unit,
    onGenerateAiReplyForItem: (InboxItem, Boolean) -> Unit,
    onApproveDraft: (InboxItem, String) -> Unit,
    onDeleteItem: (String) -> Unit
) {
    var selectedFilter by remember { mutableStateOf("ALL") }
    var showNewIncomingDialog by remember { mutableStateOf(false) }

    val allItems = (inboxState as? UiState.Success)?.data.orEmpty()
    val filteredItems = remember(allItems, selectedFilter) {
        when (selectedFilter) {
            "EMAIL" -> allItems.filter { it.channel == "EMAIL" }
            "MESSAGE" -> allItems.filter { it.channel == "MESSAGE" }
            "AUTO_REPLIED" -> allItems.filter { it.status == "AUTO_REPLIED" }
            "PENDING" -> allItems.filter { it.status != "AUTO_REPLIED" }
            else -> allItems
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .testTag("inbox_reply_screen")
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = MaterialTheme.shapes.large,
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Background Message & Email Auto-Reply",
                            style = MaterialTheme.typography.titleLarge
                        )
                        Text(
                            text = "Active Tone: ${settings.replyTone} • Email Auto-Reply: ${if (settings.autoReplyEmails) "ON" else "OFF"} • Message Auto-Reply: ${if (settings.autoReplyMessages) "ON" else "OFF"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Button(
                                onClick = onProcessPendingQueue,
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("inbox_check_queue_button")
                            ) {
                                Icon(Icons.Default.Bolt, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Scan & Auto-Reply")
                            }
                            OutlinedButton(
                                onClick = { showNewIncomingDialog = true },
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("inbox_add_incoming_button")
                            ) {
                                Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("New Incoming")
                            }
                        }
                    }
                }
            }

            item {
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "ALL" to "All (${allItems.size})",
                        "EMAIL" to "Emails",
                        "MESSAGE" to "Messages",
                        "AUTO_REPLIED" to "Auto-Replied",
                        "PENDING" to "Needs Review"
                    ).forEach { (key, label) ->
                        FilterChip(
                            selected = selectedFilter == key,
                            onClick = { selectedFilter = key },
                            label = { Text(label) }
                        )
                    }
                }
            }

            if (filteredItems.isEmpty()) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = "No messages or emails in this view",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Tap 'Scan & Auto-Reply' or 'New Incoming' to test background AI email and message replies.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            } else {
                items(filteredItems, key = { it.id }) { item ->
                    InboxItemCard(
                        item = item,
                        onGenerateDraft = { autoSend -> onGenerateAiReplyForItem(item, autoSend) },
                        onSendReply = { editedDraft -> onApproveDraft(item, editedDraft) },
                        onDelete = { onDeleteItem(item.id) }
                    )
                }
            }
        }
    }

    if (showNewIncomingDialog) {
        NewIncomingMessageDialog(
            onDismiss = { showNewIncomingDialog = false },
            onSubmit = { channel, sender, subject, body ->
                showNewIncomingDialog = false
                onCheckAndAutoReplyIncoming(channel, sender, subject, body)
            }
        )
    }
}

@Composable
private fun InboxItemCard(
    item: InboxItem,
    onGenerateDraft: (Boolean) -> Unit,
    onSendReply: (String) -> Unit,
    onDelete: () -> Unit
) {
    var draftText by remember(item.id, item.aiReplyDraft) { mutableStateOf(item.aiReplyDraft) }
    var isEditingDraft by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("inbox_item_card_${item.id}"),
        shape = MaterialTheme.shapes.large,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = if (item.channel == "EMAIL") Icons.Default.Email else Icons.Default.Sms,
                        contentDescription = item.channel,
                        tint = if (item.channel == "EMAIL") MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = "${item.channel} • ${item.priority} PRIORITY",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item.status.replace('_', ' '),
                        style = MaterialTheme.typography.labelMedium,
                        color = if (item.status == "AUTO_REPLIED") {
                            MaterialTheme.colorScheme.tertiary
                        } else {
                            MaterialTheme.colorScheme.secondary
                        },
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.DeleteOutline,
                            contentDescription = "Delete item",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Text(
                text = item.sender,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = item.subject,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = item.incomingBody,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // AI Reply Box
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(MaterialTheme.shapes.medium)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (item.status == "AUTO_REPLIED") "AI Auto-Replied Message" else "AI Suggested Reply Draft",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.tertiary
                    )
                    if (item.aiReplyDraft.isNotBlank()) {
                        TextButton(onClick = { isEditingDraft = !isEditingDraft }) {
                            Text(if (isEditingDraft) "Done" else "Edit")
                        }
                    }
                }

                if (isEditingDraft) {
                    OutlinedTextField(
                        value = draftText,
                        onValueChange = { draftText = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Customize Reply") }
                    )
                } else {
                    Text(
                        text = draftText.ifBlank { "No reply drafted yet. Tap 'AI Auto-Reply' below." },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { onGenerateDraft(false) },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (item.aiReplyDraft.isBlank()) "AI Draft" else "Regenerate")
                }

                Button(
                    onClick = {
                        if (draftText.isBlank()) {
                            onGenerateDraft(true)
                        } else {
                            onSendReply(draftText)
                        }
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.tertiary,
                        contentColor = MaterialTheme.colorScheme.onTertiary
                    )
                ) {
                    Icon(Icons.AutoMirrored.Filled.Send, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (item.status == "AUTO_REPLIED") "Resend Reply" else "Auto-Reply Now")
                }
            }
        }
    }
}

@Composable
private fun NewIncomingMessageDialog(
    onDismiss: () -> Unit,
    onSubmit: (channel: String, sender: String, subject: String, body: String) -> Unit
) {
    var channel by remember { mutableStateOf("EMAIL") }
    var sender by remember { mutableStateOf("Aditi Nair • Head of Marketing (Mumbai)") }
    var subject by remember { mutableStateOf("Keynote Speaker Confirmation for India AI Summit") }
    var body by remember {
        mutableStateOf("Hi! Can you confirm if you are available for the Friday 4 PM panel in Mumbai, and whether we should highlight the new automation metrics?")
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Check & Reply Incoming Email / Message") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(
                        selected = channel == "EMAIL",
                        onClick = { channel = "EMAIL" },
                        label = { Text("Email") }
                    )
                    FilterChip(
                        selected = channel == "MESSAGE",
                        onClick = { channel = "MESSAGE" },
                        label = { Text("Direct Message") }
                    )
                }
                OutlinedTextField(
                    value = sender,
                    onValueChange = { sender = it },
                    label = { Text("From (Sender)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = subject,
                    onValueChange = { subject = it },
                    label = { Text("Subject / Topic") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = body,
                    onValueChange = { body = it },
                    label = { Text("Incoming Message / Email Content") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (sender.isNotBlank() && body.isNotBlank()) {
                        onSubmit(channel, sender, subject.ifBlank { "Incoming Message" }, body)
                    }
                }
            ) {
                Text("AI Check & Reply")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
