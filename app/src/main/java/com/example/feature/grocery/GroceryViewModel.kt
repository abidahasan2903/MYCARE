package com.example.feature.grocery

import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.database.entity.GroceryItemEntity
import com.example.core.database.entity.GroceryListEntity
import com.example.core.permissions.PermissionManager
import com.example.core.storage.FileStorageManager
import com.example.data.repository.GroceryRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class GroceryViewModel(
    private val groceryRepo: GroceryRepository,
    val fileStorage: FileStorageManager
) : ViewModel() {

    val allLists: StateFlow<List<GroceryListEntity>> = groceryRepo.allLists
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val searchQuery = MutableStateFlow("")
    private val statusFilter = MutableStateFlow("ALL") // ALL, PENDING, PURCHASED
    private val sortMode = MutableStateFlow("DATE") // DATE, NAME, PRICE

    fun setSearch(q: String) { searchQuery.value = q }
    fun setStatusFilter(f: String) { statusFilter.value = f }
    fun setSortMode(s: String) { sortMode.value = s }

    fun observeList(listId: Long): Flow<GroceryListEntity?> = groceryRepo.observeList(listId)

    fun observeFilteredItems(listId: Long): Flow<List<GroceryItemEntity>> = combine(
        groceryRepo.observeItemsForList(listId),
        searchQuery,
        statusFilter,
        sortMode
    ) { items, q, filter, sort ->
        val filtered = items.filter { item ->
            val matchQ = q.isBlank() || item.name.contains(q, ignoreCase = true) || item.notes.contains(q, ignoreCase = true)
            val matchF = when (filter) {
                "PENDING" -> !item.isPurchased
                "PURCHASED" -> item.isPurchased
                else -> true
            }
            matchQ && matchF
        }
        when (sort) {
            "NAME" -> filtered.sortedBy { it.name.lowercase() }
            "PRICE" -> filtered.sortedByDescending { it.actualPrice ?: it.estimatedPrice }
            else -> filtered
        }
    }

    fun observeRawItems(listId: Long): Flow<List<GroceryItemEntity>> = groceryRepo.observeItemsForList(listId)

    fun saveList(existing: GroceryListEntity?, name: String, desc: String, onSaved: (Long) -> Unit) {
        viewModelScope.launch {
            val entity = (existing ?: GroceryListEntity(name = name)).copy(
                name = name.trim(),
                description = desc.trim()
            )
            val id = groceryRepo.saveList(entity)
            onSaved(id)
        }
    }

    fun toggleListCompleted(list: GroceryListEntity) {
        viewModelScope.launch { groceryRepo.saveList(list.copy(isCompleted = !list.isCompleted)) }
    }

    fun deleteList(list: GroceryListEntity, onDone: () -> Unit = {}) {
        viewModelScope.launch {
            groceryRepo.deleteList(list)
            onDone()
        }
    }

    fun importFileToList(list: GroceryListEntity, uri: Uri, onError: () -> Unit) {
        viewModelScope.launch {
            fileStorage.importUriToPrivateStorage(uri).onSuccess { imported ->
                list.importedFilePath?.let { old -> fileStorage.deleteFile(old) }
                groceryRepo.saveList(
                    list.copy(
                        importedFilePath = imported.filePath,
                        importedFileType = imported.mediaType,
                        importedFileName = imported.displayName
                    )
                )
            }.onFailure { onError() }
        }
    }

    fun saveItem(
        listId: Long,
        existing: GroceryItemEntity?,
        name: String,
        qty: Double,
        unit: String,
        estPrice: Double,
        actPrice: Double?,
        notes: String,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            val safeQty = if (qty.isFinite() && qty > 0.0) qty else 1.0
            val safeEst = if (estPrice.isFinite() && estPrice >= 0.0) estPrice else 0.0
            val safeAct = actPrice?.takeIf { it.isFinite() && it >= 0.0 }
            val item = (existing ?: GroceryItemEntity(listId = listId, name = name)).copy(
                listId = listId,
                name = name.trim(),
                quantity = safeQty,
                unit = unit.trim().ifBlank { "pcs" },
                estimatedPrice = safeEst,
                actualPrice = safeAct,
                notes = notes.trim()
            )
            groceryRepo.saveItem(item)
            onDone()
        }
    }

    fun toggleItemPurchased(item: GroceryItemEntity) {
        viewModelScope.launch {
            groceryRepo.saveItem(item.copy(isPurchased = !item.isPurchased))
        }
    }

    fun deleteItem(item: GroceryItemEntity) {
        viewModelScope.launch { groceryRepo.deleteItem(item) }
    }

    fun resetPurchased(listId: Long) {
        viewModelScope.launch { groceryRepo.resetPurchased(listId) }
    }

    fun clearList(listId: Long) {
        viewModelScope.launch { groceryRepo.clearList(listId) }
    }

    fun openNearbySupermarkets(
        context: Context,
        useCurrentLocation: Boolean = false,
        manualLocation: com.example.core.permissions.ManualLocationInput = com.example.core.permissions.ManualLocationInput()
    ): Boolean {
        return com.example.core.permissions.LocationSearchHelper.openGoogleMapsSearch(
            context = context,
            baseQuery = "grocery shop supermarket departmental store",
            useCurrentLocation = useCurrentLocation,
            manualLocation = manualLocation
        )
    }
}
