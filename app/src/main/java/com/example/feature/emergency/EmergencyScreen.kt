package com.example.feature.emergency

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Emergency
import androidx.compose.material.icons.filled.MedicalInformation
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.example.R
import com.example.core.database.entity.EmergencyContactEntity
import com.example.core.database.entity.EmergencyProfileEntity
import com.example.core.ui.components.ConfirmDeleteDialog
import com.example.core.ui.components.MyCareEmptyState
import com.example.core.ui.components.MyCareTopBar
import com.example.data.repository.EmergencyRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class EmergencyUiState(
    val contacts: List<EmergencyContactEntity> = emptyList(),
    val profile: EmergencyProfileEntity = EmergencyProfileEntity()
)

class EmergencyViewModel(
    private val repo: EmergencyRepository
) : ViewModel() {

    val uiState: StateFlow<EmergencyUiState> = combine(
        repo.allContacts,
        repo.profile
    ) { contacts, profile ->
        EmergencyUiState(
            contacts = contacts,
            profile = profile ?: EmergencyProfileEntity()
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), EmergencyUiState())

    fun saveContact(contact: EmergencyContactEntity) {
        viewModelScope.launch {
            repo.saveContact(contact)
        }
    }

    fun deleteContact(contact: EmergencyContactEntity) {
        viewModelScope.launch {
            repo.deleteContact(contact)
        }
    }

    fun saveProfile(profile: EmergencyProfileEntity) {
        viewModelScope.launch {
            repo.saveProfile(profile)
        }
    }
}

@Composable
fun EmergencyScreen(
    viewModel: EmergencyViewModel,
    onBack: () -> Unit
) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var showContactDialog by remember { mutableStateOf(false) }
    var editingContact by remember { mutableStateOf<EmergencyContactEntity?>(null) }
    var contactToDelete by remember { mutableStateOf<EmergencyContactEntity?>(null) }

    var bloodGroup by remember { mutableStateOf("") }
    var allergies by remember { mutableStateOf("") }
    var medicalNotes by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }

    LaunchedEffect(state.profile) {
        bloodGroup = state.profile.bloodGroup
        allergies = state.profile.allergies
        medicalNotes = state.profile.medicalNotes
        instructions = state.profile.emergencyInstructions
    }

    val savedMsg = stringResource(R.string.msg_saved_success)

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = stringResource(R.string.feature_emergency_title),
                onBack = onBack
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingContact = null
                    showContactDialog = true
                },
                modifier = Modifier.testTag("emergency_add_button")
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.emg_add_contact))
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 720.dp),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (state.contacts.isEmpty()) {
                    item {
                        MyCareEmptyState(
                            icon = Icons.Default.Emergency,
                            message = stringResource(R.string.emg_empty)
                        )
                    }
                } else {
                    items(state.contacts, key = { it.id }) { contact ->
                        ElevatedCard(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(18.dp),
                            colors = CardDefaults.elevatedCardColors(
                                containerColor = MaterialTheme.colorScheme.surface
                            )
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(48.dp)
                                        .clip(CircleShape)
                                        .background(MaterialTheme.colorScheme.errorContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        Icons.Default.Emergency,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Spacer(Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = contact.name,
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    if (contact.relationship.isNotBlank()) {
                                        Text(
                                            text = contact.relationship,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                    Text(
                                        text = contact.phoneNumber,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    if (contact.notes.isNotBlank()) {
                                        Text(
                                            text = contact.notes,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                                IconButton(
                                    onClick = {
                                        editingContact = contact
                                        showContactDialog = true
                                    }
                                ) {
                                    Icon(Icons.Default.Edit, contentDescription = stringResource(R.string.action_edit))
                                }
                                IconButton(
                                    onClick = { contactToDelete = contact }
                                ) {
                                    Icon(
                                        Icons.Default.Delete,
                                        contentDescription = stringResource(R.string.action_delete),
                                        tint = MaterialTheme.colorScheme.error
                                    )
                                }
                                Button(
                                    onClick = {
                                        val opened = com.example.core.permissions.LocationSearchHelper.dialPhoneNumber(
                                            context,
                                            contact.phoneNumber
                                        )
                                        if (!opened) {
                                            scope.launch {
                                                snackbarHostState.showSnackbar(context.getString(R.string.msg_no_app_found))
                                            }
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                    modifier = Modifier.testTag("emergency_dial_button")
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.emg_call))
                                }
                            }
                        }
                    }
                }

                // Personal Emergency Medical Information Card
                item {
                    Spacer(Modifier.height(6.dp))
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Default.MedicalInformation,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.emg_medical_info),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            OutlinedTextField(
                                value = bloodGroup,
                                onValueChange = { bloodGroup = it },
                                label = { Text(stringResource(R.string.emg_blood_group)) },
                                singleLine = true,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("emg_blood_group_input")
                            )
                            OutlinedTextField(
                                value = allergies,
                                onValueChange = { allergies = it },
                                label = { Text(stringResource(R.string.emg_allergies)) },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = medicalNotes,
                                onValueChange = { medicalNotes = it },
                                label = { Text(stringResource(R.string.emg_medical_notes)) },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )
                            OutlinedTextField(
                                value = instructions,
                                onValueChange = { instructions = it },
                                label = { Text(stringResource(R.string.emg_instructions)) },
                                minLines = 2,
                                modifier = Modifier.fillMaxWidth()
                            )

                            Button(
                                onClick = {
                                    viewModel.saveProfile(
                                        EmergencyProfileEntity(
                                            id = 1,
                                            bloodGroup = bloodGroup.trim(),
                                            allergies = allergies.trim(),
                                            medicalNotes = medicalNotes.trim(),
                                            emergencyInstructions = instructions.trim()
                                        )
                                    )
                                    scope.launch { snackbarHostState.showSnackbar(savedMsg) }
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("emergency_save_profile_button"),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text(stringResource(R.string.action_save))
                            }
                        }
                    }
                }

                item { Spacer(Modifier.height(72.dp)) }
            }
        }
    }

    if (showContactDialog) {
        EmergencyContactDialog(
            initial = editingContact,
            onDismiss = { showContactDialog = false },
            onSave = { contact ->
                viewModel.saveContact(contact)
                showContactDialog = false
            }
        )
    }

    contactToDelete?.let { target ->
        ConfirmDeleteDialog(
            onConfirm = {
                viewModel.deleteContact(target)
                contactToDelete = null
            },
            onDismiss = { contactToDelete = null }
        )
    }
}

@Composable
private fun EmergencyContactDialog(
    initial: EmergencyContactEntity?,
    onDismiss: () -> Unit,
    onSave: (EmergencyContactEntity) -> Unit
) {
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var relationship by remember { mutableStateOf(initial?.relationship ?: "") }
    var phone by remember { mutableStateOf(initial?.phoneNumber ?: "") }
    var notes by remember { mutableStateOf(initial?.notes ?: "") }
    var showError by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                stringResource(
                    if (initial == null) R.string.emg_add_contact else R.string.action_edit
                )
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it; showError = false },
                    label = { Text(stringResource(R.string.emg_contact_name)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emg_contact_name_input")
                )
                OutlinedTextField(
                    value = relationship,
                    onValueChange = { relationship = it },
                    label = { Text(stringResource(R.string.emg_relationship)) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = phone,
                    onValueChange = { phone = it; showError = false },
                    label = { Text(stringResource(R.string.emg_phone)) },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("emg_contact_phone_input")
                )
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text(stringResource(R.string.label_notes)) },
                    minLines = 2,
                    modifier = Modifier.fillMaxWidth()
                )
                if (showError) {
                    Text(
                        text = stringResource(R.string.label_required_error),
                        color = MaterialTheme.colorScheme.error,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (name.isBlank() || phone.isBlank()) {
                        showError = true
                    } else {
                        onSave(
                            EmergencyContactEntity(
                                id = initial?.id ?: 0L,
                                name = name.trim(),
                                relationship = relationship.trim(),
                                phoneNumber = phone.trim(),
                                notes = notes.trim(),
                                createdAt = initial?.createdAt ?: System.currentTimeMillis()
                            )
                        )
                    }
                },
                modifier = Modifier.testTag("emg_contact_save_button")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
