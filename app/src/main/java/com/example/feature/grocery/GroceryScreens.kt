package com.example.feature.grocery

import android.Manifest
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.R
import com.example.core.database.entity.GroceryItemEntity
import com.example.core.database.entity.GroceryListEntity
import com.example.core.permissions.ManualLocationInput
import com.example.core.permissions.PermissionManager
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.LocationSelectorSection
import com.example.core.ui.components.MyCareEmptyState
import com.example.core.ui.components.MyCareSearchBar
import com.example.core.ui.components.MyCareTopBar
import com.example.core.ui.components.formatDateShort
import kotlinx.coroutines.launch

@Composable
fun GroceryListsScreen(
    viewModel: GroceryViewModel,
    onBack: () -> Unit,
    onOpenList: (Long) -> Unit
) {
    val lists by viewModel.allLists.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    var showNewDialog by remember { mutableStateOf(false) }
    var listToDelete by remember { mutableStateOf<GroceryListEntity?>(null) }
    var historyFilter by remember { mutableStateOf("ACTIVE") }

    var showNearbySearchCard by remember { mutableStateOf(true) }
    var useCurrentLocation by remember { mutableStateOf(false) }
    var manualLocation by remember { mutableStateOf(ManualLocationInput()) }

    val visibleLists = remember(lists, historyFilter) {
        when (historyFilter) {
            "ACTIVE" -> lists.filter { !it.isCompleted }
            "HISTORY" -> lists.filter { it.isCompleted }
            else -> lists
        }
    }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = stringResource(R.string.feature_grocery_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { showNearbySearchCard = !showNearbySearchCard }) {
                        Icon(Icons.Default.Storefront, contentDescription = stringResource(R.string.grocery_nearby_stores))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showNewDialog = true },
                modifier = Modifier.testTag("fab_add_grocery_list")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.grocery_new_list))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
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
                                Icon(
                                    Icons.Default.Storefront,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.grocery_search_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            IconButton(onClick = { showNearbySearchCard = !showNearbySearchCard }) {
                                Icon(
                                    if (showNearbySearchCard) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }

                        if (showNearbySearchCard) {
                            Text(
                                text = stringResource(R.string.grocery_search_notice),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            LocationSelectorSection(
                                useCurrentLocation = useCurrentLocation,
                                onUseCurrentLocationChange = { useCurrentLocation = it },
                                manualLocation = manualLocation,
                                onManualLocationChange = { manualLocation = it },
                                onPermissionDenied = {
                                    scope.launch {
                                        snackbarHostState.showSnackbar(context.getString(R.string.doc_location_permission_needed))
                                    }
                                }
                            )
                            Button(
                                onClick = {
                                    val opened = viewModel.openNearbySupermarkets(
                                        context = context,
                                        useCurrentLocation = useCurrentLocation,
                                        manualLocation = manualLocation
                                    )
                                    if (!opened) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.msg_no_app_found))
                                        }
                                    } else if (!PermissionManager.isNetworkAvailable(context)) {
                                        scope.launch {
                                            snackbarHostState.showSnackbar(context.getString(R.string.doc_offline_msg))
                                        }
                                    }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("grocery_maps_search_btn"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Map, contentDescription = null)
                                Spacer(Modifier.width(8.dp))
                                Text(stringResource(R.string.loc_search_btn))
                            }
                        }
                    }
                }
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf("ACTIVE" to R.string.label_active, "HISTORY" to R.string.label_history, "ALL" to R.string.label_all).forEach { (k, res) ->
                        FilterChip(
                            selected = historyFilter == k,
                            onClick = { historyFilter = k },
                            label = { Text(stringResource(res)) }
                        )
                    }
                }
            }

            if (visibleLists.isEmpty()) {
                item {
                    MyCareEmptyState(
                        icon = Icons.Default.ShoppingCart,
                        message = stringResource(R.string.grocery_empty)
                    )
                }
            } else {
                items(visibleLists, key = { it.id }) { list ->
                    ElevatedCard(
                        onClick = { onOpenList(list.id) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(18.dp)
                    ) {
                        Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(list.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                                Text(
                                    formatDateShort(list.createdAt) + if (list.isCompleted) " • ${stringResource(R.string.label_completed)}" else "",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                if (list.description.isNotBlank()) {
                                    Text(list.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                }
                            }
                            IconButton(onClick = { viewModel.toggleListCompleted(list) }) {
                                Icon(Icons.Default.CheckCircle, contentDescription = stringResource(R.string.label_completed))
                            }
                            IconButton(onClick = { listToDelete = list }) {
                                Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                            }
                        }
                    }
                }
            }
        }
    }

    if (showNewDialog) {
        var name by remember { mutableStateOf("") }
        var desc by remember { mutableStateOf("") }
        AlertDialog(
            onDismissRequest = { showNewDialog = false },
            title = { Text(stringResource(R.string.grocery_new_list)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(stringResource(R.string.grocery_list_name) + " *") },
                        singleLine = true,
                        modifier = Modifier.testTag("grocery_list_name_input")
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        label = { Text(stringResource(R.string.label_description)) },
                        minLines = 2
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (name.isNotBlank()) {
                            viewModel.saveList(null, name, desc) { newId ->
                                showNewDialog = false
                                onOpenList(newId)
                            }
                        }
                    },
                    modifier = Modifier.testTag("save_grocery_list_btn")
                ) { Text(stringResource(R.string.action_save)) }
            },
            dismissButton = {
                TextButton(onClick = { showNewDialog = false }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }

    listToDelete?.let { l ->
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteList(l); listToDelete = null },
            onDismiss = { listToDelete = null }
        )
    }
}

@Composable
fun GroceryListDetailScreen(
    listId: Long,
    viewModel: GroceryViewModel,
    onBack: () -> Unit,
    onOpenMedia: (title: String, path: String, mediaType: String, mimeType: String) -> Unit
) {
    val list by viewModel.observeList(listId).collectAsStateWithLifecycle(initialValue = null)
    val allItems by viewModel.observeRawItems(listId).collectAsStateWithLifecycle(initialValue = emptyList())
    val filteredItems by viewModel.observeFilteredItems(listId).collectAsStateWithLifecycle(initialValue = emptyList())
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var searchQuery by remember { mutableStateOf("") }
    var filterKey by remember { mutableStateOf("ALL") }
    var showItemSheet by remember { mutableStateOf(false) }
    var editingItem by remember { mutableStateOf<GroceryItemEntity?>(null) }
    var itemToDelete by remember { mutableStateOf<GroceryItemEntity?>(null) }
    var confirmAction by remember { mutableStateOf<String?>(null) } // RESET or CLEAR

    val docPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        val l = list
        if (uri != null && l != null) {
            viewModel.importFileToList(l, uri) {
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_attachment_error)) }
            }
        }
    }

    val estTotal = allItems.sumOf { it.estimatedPrice }
    val actTotal = allItems.filter { it.isPurchased }.sumOf { it.actualPrice ?: it.estimatedPrice }
    val remainingCount = allItems.count { !it.isPurchased }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = list?.name ?: stringResource(R.string.feature_grocery_title),
                onBack = onBack,
                actions = {
                    IconButton(onClick = { docPicker.launch(arrayOf("image/*", "application/pdf", "*/*")) }) {
                        Icon(Icons.Default.AttachFile, contentDescription = stringResource(R.string.grocery_import_file))
                    }
                    IconButton(onClick = { confirmAction = "RESET" }) {
                        Icon(Icons.Default.Refresh, contentDescription = stringResource(R.string.grocery_reset_purchased))
                    }
                    IconButton(onClick = { confirmAction = "CLEAR" }) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.grocery_clear_list))
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { editingItem = null; showItemSheet = true },
                modifier = Modifier.testTag("fab_add_grocery_item")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.grocery_add_item))
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(stringResource(R.string.grocery_est_total), style = MaterialTheme.typography.labelSmall)
                            Text(String.format("%.2f", estTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Column {
                            Text(stringResource(R.string.grocery_act_total), style = MaterialTheme.typography.labelSmall)
                            Text(String.format("%.2f", actTotal), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(stringResource(R.string.grocery_remaining_items), style = MaterialTheme.typography.labelSmall)
                            Text("$remainingCount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            list?.importedFilePath?.let { path ->
                item {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(list?.importedFileName ?: "Imported File", style = MaterialTheme.typography.titleSmall)
                                Text(stringResource(R.string.grocery_import_hint), style = MaterialTheme.typography.bodySmall)
                            }
                            OutlinedButton(onClick = {
                                onOpenMedia(
                                    list?.importedFileName ?: "File",
                                    path,
                                    list?.importedFileType ?: "DOCUMENT",
                                    "*/*"
                                )
                            }) {
                                Icon(Icons.Default.Visibility, contentDescription = null)
                                Spacer(Modifier.width(4.dp))
                                Text(stringResource(R.string.action_preview))
                            }
                        }
                    }
                }
            }

            item {
                MyCareSearchBar(
                    query = searchQuery,
                    onQueryChange = { searchQuery = it; viewModel.setSearch(it) },
                    placeholder = stringResource(R.string.action_search)
                )
            }

            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(
                        "ALL" to R.string.label_all,
                        "PENDING" to R.string.label_pending,
                        "PURCHASED" to R.string.grocery_purchased
                    ).forEach { (k, res) ->
                        FilterChip(
                            selected = filterKey == k,
                            onClick = { filterKey = k; viewModel.setStatusFilter(k) },
                            label = { Text(stringResource(res)) }
                        )
                    }
                }
            }

            items(filteredItems, key = { it.id }) { item ->
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(16.dp)) {
                    Row(modifier = Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = item.isPurchased,
                            onCheckedChange = { viewModel.toggleItemPurchased(item) }
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "${item.name} (${item.quantity} ${item.unit})",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                textDecoration = if (item.isPurchased) TextDecoration.LineThrough else null
                            )
                            Text(
                                text = "Est: ${String.format("%.2f", item.estimatedPrice)}" +
                                    (item.actualPrice?.let { " • Paid: ${String.format("%.2f", it)}" } ?: ""),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (item.notes.isNotBlank()) {
                                Text(item.notes, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        IconButton(onClick = { editingItem = item; showItemSheet = true }) {
                            Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                        }
                        IconButton(onClick = { itemToDelete = item }) {
                            Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.action_delete))
                        }
                    }
                }
            }
        }
    }

    if (showItemSheet) {
        GroceryItemEditorSheet(
            existing = editingItem,
            onDismiss = { showItemSheet = false },
            onSave = { name, qty, unit, est, act, notes ->
                viewModel.saveItem(listId, editingItem, name, qty, unit, est, act, notes) {
                    showItemSheet = false
                }
            }
        )
    }

    itemToDelete?.let { itDel ->
        ConfirmDeleteDialog(
            onConfirm = { viewModel.deleteItem(itDel); itemToDelete = null },
            onDismiss = { itemToDelete = null }
        )
    }

    confirmAction?.let { action ->
        AlertDialog(
            onDismissRequest = { confirmAction = null },
            title = { Text(stringResource(if (action == "RESET") R.string.grocery_reset_purchased else R.string.grocery_clear_list)) },
            text = { Text(stringResource(if (action == "RESET") R.string.grocery_confirm_reset else R.string.grocery_confirm_clear)) },
            confirmButton = {
                Button(onClick = {
                    if (action == "RESET") viewModel.resetPurchased(listId) else viewModel.clearList(listId)
                    confirmAction = null
                }) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmAction = null }) { Text(stringResource(R.string.action_cancel)) }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun GroceryItemEditorSheet(
    existing: GroceryItemEntity?,
    onDismiss: () -> Unit,
    onSave: (name: String, qty: Double, unit: String, est: Double, act: Double?, notes: String) -> Unit
) {
    var name by remember { mutableStateOf(existing?.name ?: "") }
    var qtyStr by remember { mutableStateOf((existing?.quantity ?: 1.0).toString()) }
    var unit by remember { mutableStateOf(existing?.unit ?: "kg") }
    var estStr by remember { mutableStateOf(existing?.estimatedPrice?.takeIf { it > 0 }?.toString() ?: "") }
    var actStr by remember { mutableStateOf(existing?.actualPrice?.toString() ?: "") }
    var notes by remember { mutableStateOf(existing?.notes ?: "") }
    var showError by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(stringResource(R.string.grocery_add_item), style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            OutlinedTextField(
                value = name,
                onValueChange = { name = it; showError = false },
                label = { Text(stringResource(R.string.grocery_item_name) + " *") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = qtyStr,
                    onValueChange = { qtyStr = it },
                    label = { Text(stringResource(R.string.grocery_quantity)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = unit,
                    onValueChange = { unit = it },
                    label = { Text(stringResource(R.string.grocery_unit)) },
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = estStr,
                    onValueChange = { estStr = it },
                    label = { Text(stringResource(R.string.grocery_est_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
                OutlinedTextField(
                    value = actStr,
                    onValueChange = { actStr = it },
                    label = { Text(stringResource(R.string.grocery_act_price)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    singleLine = true,
                    modifier = Modifier.weight(1f)
                )
            }
            OutlinedTextField(
                value = notes,
                onValueChange = { notes = it },
                label = { Text(stringResource(R.string.label_notes)) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
            if (showError) Text(stringResource(R.string.label_required_error), color = MaterialTheme.colorScheme.error)
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                TextButton(onClick = onDismiss, modifier = Modifier.weight(1f)) { Text(stringResource(R.string.action_cancel)) }
                Button(
                    onClick = {
                        val qtyVal = if (qtyStr.isBlank()) 1.0 else qtyStr.toDoubleOrNull()
                        val estVal = if (estStr.isBlank()) 0.0 else estStr.toDoubleOrNull()
                        val actVal = if (actStr.isBlank()) null else actStr.toDoubleOrNull()
                        val actInvalid = actStr.isNotBlank() && (actVal == null || !actVal.isFinite() || actVal < 0.0)
                        if (
                            name.isBlank() ||
                            qtyVal == null || !qtyVal.isFinite() || qtyVal <= 0.0 ||
                            estVal == null || !estVal.isFinite() || estVal < 0.0 ||
                            actInvalid
                        ) {
                            showError = true
                        } else {
                            onSave(name, qtyVal, unit, estVal, actVal, notes)
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) { Text(stringResource(R.string.action_save)) }
            }
        }
    }
}
