package com.example.feature.doctorsearch

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.LocalHospital
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.Public
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.core.permissions.LocationSearchHelper
import com.example.core.permissions.ManualLocationInput
import com.example.core.permissions.PermissionManager
import com.example.core.ui.components.LocationSelectorSection
import com.example.core.ui.components.MyCareSearchBar
import com.example.core.ui.components.MyCareTopBar
import kotlinx.coroutines.launch

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DoctorSearchScreen(
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var symptomQuery by remember { mutableStateOf("") }
    var useCurrentLocation by remember { mutableStateOf(false) }
    var manualLocation by remember { mutableStateOf(ManualLocationInput()) }

    var showClinicQuickActions by remember { mutableStateOf(false) }
    var clinicPhone by remember { mutableStateOf("") }
    var clinicWebsite by remember { mutableStateOf("") }
    var clinicBookingUrl by remember { mutableStateOf("") }

    val suggestions = remember(symptomQuery) {
        DoctorSearchDictionary.searchSpecialties(symptomQuery)
    }

    val quickExamples = listOf("Fever", "জ্বর", "Chokher somossa", "Gastric", "ডায়াবেটিস", "Matha betha", "Skin")

    fun launchMapsSearch(specialty: String) {
        val opened = LocationSearchHelper.openGoogleMapsSearch(
            context = context,
            baseQuery = specialty,
            useCurrentLocation = useCurrentLocation,
            manualLocation = manualLocation
        )
        if (!opened) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_no_app_found)) }
        } else if (!PermissionManager.isNetworkAvailable(context)) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.doc_offline_msg)) }
        }
    }

    fun launchWebSearch(specialty: String) {
        val query = LocationSearchHelper.buildMapsQueryText(
            baseQuery = "$specialty doctor clinic appointment",
            useCurrentLocation = useCurrentLocation,
            manualLocation = manualLocation
        )
        val opened = LocationSearchHelper.openWebSearch(context, query)
        if (!opened) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_no_app_found)) }
        } else if (!PermissionManager.isNetworkAvailable(context)) {
            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.doc_offline_msg)) }
        }
    }

    Scaffold(
        topBar = {
            MyCareTopBar(
                title = stringResource(R.string.feature_doctor_search_title),
                onBack = onBack
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(padding),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            text = stringResource(R.string.doc_disclaimer),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Text(
                            text = stringResource(R.string.doc_external_notice),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                        )
                    }
                }
            }

            // Section A: Health Problem / Disease
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.doc_problem_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        MyCareSearchBar(
                            query = symptomQuery,
                            onQueryChange = { symptomQuery = it },
                            placeholder = stringResource(R.string.doc_search_hint),
                            modifier = Modifier.testTag("doctor_symptom_input")
                        )
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            quickExamples.forEach { ex ->
                                AssistChip(
                                    onClick = { symptomQuery = ex },
                                    label = { Text(ex) }
                                )
                            }
                        }
                    }
                }
            }

            // Section B: Location Selection (Current Location OR Manual District/Thana/City/Area)
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(
                            text = stringResource(R.string.doc_location_section),
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.primary
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
                            onClick = { launchMapsSearch("hospital clinic doctor") },
                            modifier = Modifier.fillMaxWidth().testTag("doctor_find_hospitals_btn"),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.LocalHospital, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(stringResource(R.string.doc_find_hospitals), maxLines = 1, overflow = TextOverflow.Ellipsis)
                        }
                    }
                }
            }

            // Suggested Specialties Results
            if (suggestions.isNotEmpty()) {
                item {
                    Text(
                        text = stringResource(R.string.doc_suggested_specialties),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                items(suggestions) { item ->
                    ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = item.specialtyEnglish,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Text(
                                text = item.specialtyBangla,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Text(
                                text = item.matchedCondition,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                Button(
                                    onClick = { launchMapsSearch(item.specialtyEnglish) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.doc_search_maps), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                                OutlinedButton(
                                    onClick = { launchWebSearch(item.specialtyEnglish) },
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Icon(Icons.Default.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(6.dp))
                                    Text(stringResource(R.string.doc_search_web), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }

            // Optional External Clinic Quick Actions (Call / Open Website / Open Booking Page)
            item {
                ElevatedCard(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(R.string.doc_clinic_actions),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.weight(1f)
                            )
                            IconButton(onClick = { showClinicQuickActions = !showClinicQuickActions }) {
                                Icon(
                                    if (showClinicQuickActions) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                                    contentDescription = null
                                )
                            }
                        }
                        if (showClinicQuickActions) {
                            Text(
                                text = stringResource(R.string.doc_clinic_actions_desc),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = clinicPhone,
                                    onValueChange = { clinicPhone = it },
                                    label = { Text(stringResource(R.string.doc_clinic_phone_hint)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (!LocationSearchHelper.dialPhoneNumber(context, clinicPhone)) {
                                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_no_app_found)) }
                                        }
                                    },
                                    enabled = clinicPhone.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Call, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.doc_call_clinic))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = clinicWebsite,
                                    onValueChange = { clinicWebsite = it },
                                    label = { Text(stringResource(R.string.doc_clinic_website_hint)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                OutlinedButton(
                                    onClick = {
                                        if (!LocationSearchHelper.openExternalHttpUrl(context, clinicWebsite)) {
                                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_no_app_found)) }
                                        }
                                    },
                                    enabled = clinicWebsite.isNotBlank()
                                ) {
                                    Icon(Icons.Default.Language, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.doc_open_website))
                                }
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = clinicBookingUrl,
                                    onValueChange = { clinicBookingUrl = it },
                                    label = { Text(stringResource(R.string.doc_clinic_booking_hint)) },
                                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                                    singleLine = true,
                                    modifier = Modifier.weight(1f)
                                )
                                Button(
                                    onClick = {
                                        if (!LocationSearchHelper.openExternalHttpUrl(context, clinicBookingUrl)) {
                                            scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.msg_no_app_found)) }
                                        }
                                    },
                                    enabled = clinicBookingUrl.isNotBlank()
                                ) {
                                    Icon(Icons.Default.CalendarMonth, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(4.dp))
                                    Text(stringResource(R.string.doc_open_booking), maxLines = 1, overflow = TextOverflow.Ellipsis)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
