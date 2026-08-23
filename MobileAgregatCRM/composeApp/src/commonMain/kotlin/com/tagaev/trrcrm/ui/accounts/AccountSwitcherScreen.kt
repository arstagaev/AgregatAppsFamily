package com.tagaev.trrcrm.ui.accounts

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tagaev.trrcrm.data.accounts.AccountSlot
import com.tagaev.trrcrm.ui.i18n.s
import compose.icons.FeatherIcons
import compose.icons.feathericons.ArrowLeft
import compose.icons.feathericons.Check
import compose.icons.feathericons.Plus
import compose.icons.feathericons.Trash2

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountSwitcherScreen(component: IAccountSwitcherComponent) {
    LaunchedEffect(Unit) {
        component.refresh()
    }
    val state by component.uiState.collectAsState()
    val enterPin = state.pinDialog as? PinDialogState.EnterPin

    Box(modifier = Modifier.fillMaxSize()) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(s("accounts_title")) },
                navigationIcon = {
                    IconButton(onClick = component::onBack) {
                        Icon(FeatherIcons.ArrowLeft, contentDescription = s("settings_nazad"))
                    }
                }
            )
        }
    ) { inner ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(inner)
        ) {
            LazyColumn(modifier = Modifier.weight(1f)) {
                items(state.accounts, key = { it.id }) { account ->
                    AccountRow(
                        account = account,
                        isActive = account.id == state.activeAccountId,
                        onClick = { component.onAccountClick(account.id) },
                        onSetPin = { component.onSetPinClick(account.id) },
                        onRemovePin = { component.onRemovePinClick(account.id) },
                        showPinStatus = state.showPinStatus,
                        canRemovePin = state.accounts.size == 1 && account.hasPin,
                        onDelete = { component.onDeleteClick(account.id) },
                    )
                    HorizontalDivider()
                }
                if (state.canAdd) {
                    item {
                        ListItem(
                            headlineContent = { Text(s("accounts_add")) },
                            leadingContent = {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.surfaceVariant),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(FeatherIcons.Plus, contentDescription = null)
                                }
                            },
                            modifier = Modifier.clickable(onClick = component::onAddAccount)
                        )
                    }
                }
            }
        }
    }

    when (val dialog = state.pinDialog) {
        PinDialogState.Hidden -> Unit
        is PinDialogState.SetPin -> {
            val confirming = dialog.firstEntry.length >= 4
            PinPadDialog(
                title = if (confirming) s("accounts_pin_confirm") else s("accounts_set_pin_title"),
                filled = if (confirming) dialog.confirmEntry.length else dialog.firstEntry.length,
                error = state.pinError,
                onDigit = component::onPinDigit,
                onBackspace = component::onPinBackspace,
                onDismiss = component::dismissPinDialog,
            )
        }
        is PinDialogState.EnterPin -> Unit
    }

    state.confirmRemovePinId?.let { id ->
        AlertDialog(
            onDismissRequest = component::cancelRemovePin,
            title = { Text(s("accounts_remove_pin")) },
            text = { Text(s("accounts_remove_pin_confirm")) },
            confirmButton = {
                TextButton(onClick = component::confirmRemovePin) {
                    Text(s("settings_da"))
                }
            },
            dismissButton = {
                TextButton(onClick = component::cancelRemovePin) {
                    Text(s("settings_otmena"))
                }
            }
        )
    }

    state.infoDialog?.let { message ->
        AlertDialog(
            onDismissRequest = component::consumeInfoDialog,
            title = { Text(s("accounts_pin_gate_title")) },
            text = { Text(message) },
            confirmButton = {
                TextButton(onClick = component::consumeInfoDialog) {
                    Text(s("login_ok"))
                }
            }
        )
    }

    state.confirmDeleteId?.let { id ->
        val name = state.accounts.firstOrNull { it.id == id }?.displayName.orEmpty()
        AlertDialog(
            onDismissRequest = component::cancelDelete,
            title = { Text(s("accounts_delete")) },
            text = { Text(s("accounts_delete_confirm", name)) },
            confirmButton = {
                TextButton(onClick = component::confirmDelete) {
                    Text(s("settings_da"))
                }
            },
            dismissButton = {
                TextButton(onClick = component::cancelDelete) {
                    Text(s("settings_otmena"))
                }
            }
        )
    }

    if (enterPin != null) {
        PinUnlockScreen(
            accounts = state.accounts,
            selectedAccountId = enterPin.accountId,
            filled = enterPin.entry.length,
            error = state.pinError,
            onSelectAccount = component::onUnlockAccountSelected,
            onDigit = component::onPinDigit,
            onBackspace = component::onPinBackspace,
            onForgotPin = component::onForgotPin,
            onDismiss = component::dismissPinDialog,
        )
    }
    }
}

@Composable
private fun AccountRow(
    account: AccountSlot,
    isActive: Boolean,
    showPinStatus: Boolean,
    canRemovePin: Boolean,
    onClick: () -> Unit,
    onSetPin: () -> Unit,
    onRemovePin: () -> Unit,
    onDelete: () -> Unit,
) {
    ListItem(
        headlineContent = {
            Text(account.displayName, fontWeight = if (isActive) FontWeight.SemiBold else FontWeight.Normal)
        },
        supportingContent = {
            if (isActive) Text(s("accounts_active"))
        },
        leadingContent = {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    account.initials,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                    fontWeight = FontWeight.Bold,
                )
            }
        },
        trailingContent = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (isActive) {
                    Icon(
                        FeatherIcons.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                if (showPinStatus) {
                    Column(horizontalAlignment = Alignment.End) {
                        Text(
                            text = if (account.hasPin) s("accounts_pin_set") else s("accounts_set_pin"),
                            color = if (account.hasPin) Color(0xFF2E7D32) else MaterialTheme.colorScheme.error,
                            fontWeight = FontWeight.Medium,
                            modifier = Modifier
                                .then(
                                    if (account.hasPin) Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    else Modifier.clickable(onClick = onSetPin).padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                        )
                        if (canRemovePin) {
                            Text(
                                text = s("accounts_remove_pin"),
                                color = MaterialTheme.colorScheme.error,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier
                                    .clickable(onClick = onRemovePin)
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(FeatherIcons.Trash2, contentDescription = s("accounts_delete"))
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick)
    )
}

@Composable
private fun PinPadDialog(
    title: String,
    filled: Int,
    error: String?,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()) },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.padding(vertical = 12.dp)
                ) {
                    repeat(4) { index ->
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .clip(CircleShape)
                                .background(
                                    if (index < filled) MaterialTheme.colorScheme.primary
                                    else MaterialTheme.colorScheme.outlineVariant
                                )
                        )
                    }
                }
                if (!error.isNullOrBlank()) {
                    Text(
                        error,
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.height(8.dp))
                }
                val keys = listOf(
                    listOf("1", "2", "3"),
                    listOf("4", "5", "6"),
                    listOf("7", "8", "9"),
                    listOf("", "0", "<"),
                )
                keys.forEach { row ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .let { base ->
                                        if (key.isNotEmpty()) base.clickable {
                                            if (key == "<") onBackspace() else onDigit(key)
                                        } else base
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (key.isNotEmpty()) {
                                    Text(
                                        if (key == "<") "⌫" else key,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text(s("settings_otmena")) }
        }
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PinUnlockScreen(
    accounts: List<AccountSlot>,
    selectedAccountId: String,
    filled: Int,
    error: String?,
    onSelectAccount: (String) -> Unit,
    onDigit: (String) -> Unit,
    onBackspace: () -> Unit,
    onForgotPin: () -> Unit,
    onDismiss: () -> Unit,
) {
    val selected = accounts.firstOrNull { it.id == selectedAccountId } ?: accounts.firstOrNull()
    var expanded by remember { mutableStateOf(false) }
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            IconButton(onClick = onDismiss) {
                Icon(FeatherIcons.ArrowLeft, contentDescription = s("settings_nazad"))
            }
            Text(
                s("accounts_enter_pin"),
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = it },
            ) {
                OutlinedTextField(
                    value = selected?.let { accountLabel(it) }.orEmpty(),
                    onValueChange = {},
                    readOnly = true,
                    label = { Text(s("accounts_select_account")) },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier
                        .menuAnchor()
                        .fillMaxWidth(),
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false },
                ) {
                    accounts.forEach { account ->
                        DropdownMenuItem(
                            text = { Text(accountLabel(account)) },
                            onClick = {
                                expanded = false
                                onSelectAccount(account.id)
                            },
                        )
                    }
                }
            }
            Spacer(Modifier.height(20.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
            ) {
                Spacer(Modifier.weight(1f))
                repeat(4) { index ->
                    Box(
                        modifier = Modifier
                            .size(22.dp)
                            .clip(CircleShape)
                            .background(
                                if (index < filled) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.outlineVariant
                            )
                    )
                }
                Spacer(Modifier.weight(1f))
            }
            if (!error.isNullOrBlank()) {
                Text(
                    error,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodyLarge,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
            Spacer(Modifier.height(8.dp))
            val keys = listOf(
                listOf("1", "2", "3"),
                listOf("4", "5", "6"),
                listOf("7", "8", "9"),
                listOf("", "0", "<"),
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                keys.forEach { row ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .weight(1f),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        row.forEach { key ->
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxHeight(),
                                contentAlignment = Alignment.Center,
                            ) {
                                if (key.isNotEmpty()) {
                                    Box(
                                        modifier = Modifier
                                            .padding(4.dp)
                                            .fillMaxSize()
                                            .clip(RoundedCornerShape(16.dp))
                                            .background(MaterialTheme.colorScheme.surfaceVariant)
                                            .clickable {
                                                if (key == "<") onBackspace() else onDigit(key)
                                            },
                                        contentAlignment = Alignment.Center,
                                    ) {
                                        Text(
                                            if (key == "<") "⌫" else key,
                                            fontSize = 36.sp,
                                            fontWeight = FontWeight.Medium,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
            TextButton(
                onClick = onForgotPin,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp, bottom = 12.dp),
            ) {
                Text(s("accounts_forgot_pin"), fontSize = 18.sp)
            }
        }
    }
}

private fun accountLabel(account: AccountSlot): String {
    val dept = account.department.trim()
    return if (dept.isBlank()) account.displayName else "${account.displayName} · $dept"
}
