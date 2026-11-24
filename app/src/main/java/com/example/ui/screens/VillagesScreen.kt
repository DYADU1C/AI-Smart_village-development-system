package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.data.model.AgricultureMetrics
import com.example.data.model.DataSourceType
import com.example.data.model.DevelopmentLevel
import com.example.data.model.EducationMetrics
import com.example.data.model.HealthcareMetrics
import com.example.data.model.InfrastructureMetrics
import com.example.data.model.PopulationMetrics
import com.example.data.model.UiState
import com.example.data.model.VillageRecord
import com.example.ui.components.DataSourceBadge
import com.example.ui.components.TierBadge

@Composable
fun VillagesScreen(
    villagesState: UiState<List<VillageRecord>>,
    isBusy: Boolean,
    onSaveVillage: (VillageRecord, () -> Unit) -> Unit,
    onDeleteVillage: (String, String) -> Unit,
    onLoadDemoVillages: () -> Unit,
    onAnalyzeVillage: (VillageRecord) -> Unit,
    openEditorTrigger: Boolean = false,
    onEditorConsumed: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedTierFilter by remember { mutableStateOf("All") }
    var selectedSourceFilter by remember { mutableStateOf("All") }

    var editingVillage by remember { mutableStateOf<VillageRecord?>(null) }
    var showFormDialog by remember { mutableStateOf(false) }
    var deleteCandidate by remember { mutableStateOf<VillageRecord?>(null) }

    if (openEditorTrigger && !showFormDialog) {
        editingVillage = null
        showFormDialog = true
        onEditorConsumed()
    }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = {
                    editingVillage = null
                    showFormDialog = true
                },
                containerColor = MaterialTheme.colorScheme.primary,
                modifier = Modifier.testTag("add_village_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Village")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Search & Filter Header
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text("Search village, district, or state...") },
                    leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("village_search_input")
                )

                Row(
                    modifier = Modifier.horizontalScroll(rememberScrollState()),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Verified", "Demo").forEach { src ->
                        FilterChip(
                            selected = selectedSourceFilter == src,
                            onClick = { selectedSourceFilter = src },
                            label = { Text(src) }
                        )
                    }
                    listOf(
                        "All",
                        DevelopmentLevel.MODEL_SMART.label,
                        DevelopmentLevel.EMERGING.label,
                        DevelopmentLevel.DEVELOPING.label,
                        DevelopmentLevel.CRITICAL.label
                    ).forEach { tier ->
                        if (tier != "All") {
                            FilterChip(
                                selected = selectedTierFilter == tier,
                                onClick = {
                                    selectedTierFilter = if (selectedTierFilter == tier) "All" else tier
                                },
                                label = { Text(tier) }
                            )
                        }
                    }
                }
            }

            when (villagesState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(villagesState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val filtered = villagesState.data.filter { v ->
                        val matchesQuery = searchQuery.isBlank() ||
                            v.name.contains(searchQuery, ignoreCase = true) ||
                            v.district.contains(searchQuery, ignoreCase = true) ||
                            v.state.contains(searchQuery, ignoreCase = true)
                        val matchesTier = selectedTierFilter == "All" || v.scores.developmentLevel == selectedTierFilter
                        val matchesSource = when (selectedSourceFilter) {
                            "Verified" -> v.sourceEnum == DataSourceType.VERIFIED_FIELD
                            "Demo" -> v.sourceEnum == DataSourceType.DEMO_SAMPLE
                            else -> true
                        }
                        matchesQuery && matchesTier && matchesSource
                    }

                    if (filtered.isEmpty()) {
                        Box(
                            modifier = Modifier.fillMaxSize().padding(24.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text("No matching villages found.", style = MaterialTheme.typography.titleMedium)
                                if (villagesState.data.isEmpty()) {
                                    OutlinedButton(onClick = onLoadDemoVillages, enabled = !isBusy) {
                                        Icon(Icons.Rounded.Dataset, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Load Sample Reference Villages")
                                    }
                                }
                            }
                        }
                    } else {
                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            items(filtered, key = { it.id }) { village ->
                                VillageCard(
                                    village = village,
                                    onEdit = {
                                        editingVillage = village
                                        showFormDialog = true
                                    },
                                    onDelete = { deleteCandidate = village },
                                    onAnalyze = { onAnalyzeVillage(village) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showFormDialog) {
        VillageFormDialog(
            initial = editingVillage,
            isBusy = isBusy,
            onDismiss = { showFormDialog = false },
            onSave = { updated ->
                onSaveVillage(updated) { showFormDialog = false }
            }
        )
    }

    deleteCandidate?.let { victim ->
        AlertDialog(
            onDismissRequest = { deleteCandidate = null },
            title = { Text("Delete Village Record") },
            text = { Text("Are you sure you want to permanently remove '${victim.name}' from Cloud Firestore?") },
            confirmButton = {
                Button(
                    onClick = {
                        onDeleteVillage(victim.id, victim.name)
                        deleteCandidate = null
                    }
                ) { Text("Delete") }
            },
            dismissButton = {
                TextButton(onClick = { deleteCandidate = null }) { Text("Cancel") }
            }
        )
    }
}

@Composable
private fun VillageCard(
    village: VillageRecord,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onAnalyze: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onAnalyze() }
            .testTag("village_card_${village.name}")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(village.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Text(
                        "${village.district}, ${village.state} • Pop: ${village.population.total}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                DataSourceBadge(village.sourceEnum)
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TierBadge(village.scores.developmentLevel)
                Text(
                    text = "Score: ${village.scores.overallScore}%",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            Text(
                text = "Lit: ${village.education.literacyRatePct}% • Water: ${village.infrastructure.cleanDrinkingWaterPct}% • Roads: ${village.infrastructure.pavedRoadPct}% • Weakest: ${village.scores.weakestSector}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onAnalyze) {
                    Icon(Icons.Rounded.Analytics, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(Modifier.width(4.dp))
                    Text("AI Analyze")
                }
                IconButton(onClick = onEdit) {
                    Icon(Icons.Rounded.Edit, contentDescription = "Edit Village")
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Rounded.Delete, contentDescription = "Delete Village", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
private fun VillageFormDialog(
    initial: VillageRecord?,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (VillageRecord) -> Unit
) {
    BackHandler { onDismiss() }

    var name by remember { mutableStateOf(initial?.name ?: "") }
    var district by remember { mutableStateOf(initial?.district ?: "") }
    var state by remember { mutableStateOf(initial?.state ?: "") }
    var areaSqKm by remember { mutableStateOf((initial?.areaSqKm ?: 8.5).toString()) }
    var lat by remember { mutableStateOf((initial?.latitude ?: 20.5).toString()) }
    var lon by remember { mutableStateOf((initial?.longitude ?: 78.9).toString()) }
    var isDemo by remember { mutableStateOf(initial?.sourceEnum == DataSourceType.DEMO_SAMPLE) }

    var popTotal by remember { mutableStateOf((initial?.population?.total ?: 1800L).toString()) }
    var households by remember { mutableStateOf((initial?.population?.households ?: 380L).toString()) }

    var soilType by remember { mutableStateOf(initial?.agriculture?.soilType ?: "Alluvial") }
    var rainfall by remember { mutableStateOf((initial?.agriculture?.annualRainfallMm ?: 820.0).toString()) }
    var avgTemp by remember { mutableStateOf((initial?.agriculture?.avgTempC ?: 27.0).toString()) }
    var season by remember { mutableStateOf(initial?.agriculture?.primarySeason ?: "Kharif") }
    var irrigatedPct by remember { mutableStateOf((initial?.agriculture?.irrigatedLandPct ?: 55.0).toString()) }
    var majorCrops by remember { mutableStateOf(initial?.agriculture?.majorCrops ?: "Wheat, Rice, Pulses") }

    var phcCount by remember { mutableStateOf((initial?.healthcare?.primaryHealthCenters ?: 1L).toString()) }
    var doctors by remember { mutableStateOf((initial?.healthcare?.doctorsCount ?: 1L).toString()) }
    var beds by remember { mutableStateOf((initial?.healthcare?.hospitalBeds ?: 6L).toString()) }
    var hospitalDist by remember { mutableStateOf((initial?.healthcare?.avgDistanceToHospitalKm ?: 8.0).toString()) }
    var immunization by remember { mutableStateOf((initial?.healthcare?.immunizationPct ?: 82.0).toString()) }
    var ambulance by remember { mutableStateOf(initial?.healthcare?.ambulanceAvailable ?: true) }

    var primarySchools by remember { mutableStateOf((initial?.education?.primarySchools ?: 2L).toString()) }
    var secondarySchools by remember { mutableStateOf((initial?.education?.secondarySchools ?: 1L).toString()) }
    var literacyPct by remember { mutableStateOf((initial?.education?.literacyRatePct ?: 74.0).toString()) }
    var femaleLitPct by remember { mutableStateOf((initial?.education?.femaleLiteracyPct ?: 66.0).toString()) }
    var digitalClassrooms by remember { mutableStateOf((initial?.education?.digitalClassrooms ?: 2L).toString()) }
    var dropoutPct by remember { mutableStateOf((initial?.education?.dropoutRatePct ?: 7.5).toString()) }

    var pavedRoadPct by remember { mutableStateOf((initial?.infrastructure?.pavedRoadPct ?: 70.0).toString()) }
    var elecHours by remember { mutableStateOf((initial?.infrastructure?.electricityHoursPerDay ?: 18.0).toString()) }
    var waterPct by remember { mutableStateOf((initial?.infrastructure?.cleanDrinkingWaterPct ?: 75.0).toString()) }
    var sanitationPct by remember { mutableStateOf((initial?.infrastructure?.sanitationCoveragePct ?: 80.0).toString()) }
    var internetPct by remember { mutableStateOf((initial?.infrastructure?.internetCoveragePct ?: 48.0).toString()) }

    var validationError by remember { mutableStateOf<String?>(null) }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            shape = RoundedCornerShape(22.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier
                .fillMaxWidth(0.94f)
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(18.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = if (initial == null) "Register Village Record" else "Update Village Record",
                    style = MaterialTheme.typography.headlineSmall
                )

                Text("1. Geography & Classification", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                FormField("Village Name*", name) { name = it }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("District*", district, Modifier.weight(1f)) { district = it }
                    FormField("State*", state, Modifier.weight(1f)) { state = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Area (sq.km)", areaSqKm, Modifier.weight(1f), true) { areaSqKm = it }
                    FormField("Population*", popTotal, Modifier.weight(1f), true) { popTotal = it }
                    FormField("Households*", households, Modifier.weight(1f), true) { households = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Latitude", lat, Modifier.weight(1f), true) { lat = it }
                    FormField("Longitude", lon, Modifier.weight(1f), true) { lon = it }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Mark as Demo / Sample Record", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = isDemo, onCheckedChange = { isDemo = it })
                }

                Text("2. Agriculture", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Soil Type", soilType, Modifier.weight(1f)) { soilType = it }
                    FormField("Season (Kharif/Rabi/Zaid)", season, Modifier.weight(1f)) { season = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Rainfall (mm)", rainfall, Modifier.weight(1f), true) { rainfall = it }
                    FormField("Temp (°C)", avgTemp, Modifier.weight(1f), true) { avgTemp = it }
                    FormField("Irrigated %", irrigatedPct, Modifier.weight(1f), true) { irrigatedPct = it }
                }
                FormField("Major Crops", majorCrops) { majorCrops = it }

                Text("3. Healthcare", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("PHCs", phcCount, Modifier.weight(1f), true) { phcCount = it }
                    FormField("Doctors", doctors, Modifier.weight(1f), true) { doctors = it }
                    FormField("Beds", beds, Modifier.weight(1f), true) { beds = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Hospital Dist (km)", hospitalDist, Modifier.weight(1f), true) { hospitalDist = it }
                    FormField("Immunization %", immunization, Modifier.weight(1f), true) { immunization = it }
                }
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Ambulance Available", style = MaterialTheme.typography.bodyMedium)
                    Switch(checked = ambulance, onCheckedChange = { ambulance = it })
                }

                Text("4. Education", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Primary Schools", primarySchools, Modifier.weight(1f), true) { primarySchools = it }
                    FormField("Secondary", secondarySchools, Modifier.weight(1f), true) { secondarySchools = it }
                    FormField("Digital Rooms", digitalClassrooms, Modifier.weight(1f), true) { digitalClassrooms = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Literacy %", literacyPct, Modifier.weight(1f), true) { literacyPct = it }
                    FormField("Female Lit %", femaleLitPct, Modifier.weight(1f), true) { femaleLitPct = it }
                    FormField("Dropout %", dropoutPct, Modifier.weight(1f), true) { dropoutPct = it }
                }

                Text("5. Infrastructure", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Paved Road %", pavedRoadPct, Modifier.weight(1f), true) { pavedRoadPct = it }
                    FormField("Power hrs/day", elecHours, Modifier.weight(1f), true) { elecHours = it }
                    FormField("Water %", waterPct, Modifier.weight(1f), true) { waterPct = it }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FormField("Sanitation %", sanitationPct, Modifier.weight(1f), true) { sanitationPct = it }
                    FormField("Internet %", internetPct, Modifier.weight(1f), true) { internetPct = it }
                }

                if (!validationError.isNullOrBlank()) {
                    Text(validationError!!, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall)
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(onClick = onDismiss) { Text("Cancel") }
                    Spacer(Modifier.width(8.dp))
                    Button(
                        enabled = !isBusy,
                        onClick = {
                            if (name.trim().length < 2 || district.trim().length < 2 || state.trim().length < 2) {
                                validationError = "Village, District, and State must each be at least 2 characters."
                                return@Button
                            }
                            val pop = popTotal.toLongOrNull()?.coerceAtLeast(1L)
                            val hh = households.toLongOrNull()?.coerceAtLeast(1L)
                            if (pop == null || hh == null) {
                                validationError = "Population and households must be valid positive numbers."
                                return@Button
                            }
                            val safeSeason = when (season.trim()) {
                                "Kharif", "Rabi", "Zaid", "All-Season" -> season.trim()
                                else -> "Kharif"
                            }
                            val record = VillageRecord(
                                id = initial?.id ?: "",
                                userId = initial?.userId ?: "",
                                name = name.trim(),
                                district = district.trim(),
                                state = state.trim(),
                                latitude = (lat.toDoubleOrNull() ?: 20.5).coerceIn(-90.0, 90.0),
                                longitude = (lon.toDoubleOrNull() ?: 78.9).coerceIn(-180.0, 180.0),
                                areaSqKm = (areaSqKm.toDoubleOrNull() ?: 8.0).coerceIn(0.1, 5000.0),
                                dataSourceType = if (isDemo) DataSourceType.DEMO_SAMPLE.code else DataSourceType.VERIFIED_FIELD.code,
                                population = PopulationMetrics(
                                    total = pop,
                                    maleCount = (pop * 0.51).toLong(),
                                    femaleCount = pop - (pop * 0.51).toLong(),
                                    households = hh,
                                    youthRatioPct = initial?.population?.youthRatioPct ?: 36.0,
                                    workingAgePct = initial?.population?.workingAgePct ?: 58.0
                                ),
                                agriculture = AgricultureMetrics(
                                    soilType = soilType.trim().ifBlank { "Alluvial" },
                                    annualRainfallMm = (rainfall.toDoubleOrNull() ?: 800.0).coerceAtLeast(0.0),
                                    avgTempC = (avgTemp.toDoubleOrNull() ?: 27.0).coerceIn(-20.0, 60.0),
                                    primarySeason = safeSeason,
                                    irrigatedLandPct = (irrigatedPct.toDoubleOrNull() ?: 50.0).coerceIn(0.0, 100.0),
                                    cultivableHectares = initial?.agriculture?.cultivableHectares ?: 450.0,
                                    majorCrops = majorCrops.trim()
                                ),
                                healthcare = HealthcareMetrics(
                                    primaryHealthCenters = (phcCount.toLongOrNull() ?: 1L).coerceAtLeast(0L),
                                    subCenters = initial?.healthcare?.subCenters ?: 1L,
                                    doctorsCount = (doctors.toLongOrNull() ?: 1L).coerceAtLeast(0L),
                                    nursesCount = initial?.healthcare?.nursesCount ?: 3L,
                                    hospitalBeds = (beds.toLongOrNull() ?: 6L).coerceAtLeast(0L),
                                    ambulanceAvailable = ambulance,
                                    avgDistanceToHospitalKm = (hospitalDist.toDoubleOrNull() ?: 8.0).coerceAtLeast(0.0),
                                    immunizationPct = (immunization.toDoubleOrNull() ?: 80.0).coerceIn(0.0, 100.0)
                                ),
                                education = EducationMetrics(
                                    primarySchools = (primarySchools.toLongOrNull() ?: 2L).coerceAtLeast(0L),
                                    secondarySchools = (secondarySchools.toLongOrNull() ?: 1L).coerceAtLeast(0L),
                                    literacyRatePct = (literacyPct.toDoubleOrNull() ?: 72.0).coerceIn(0.0, 100.0),
                                    femaleLiteracyPct = (femaleLitPct.toDoubleOrNull() ?: 65.0).coerceIn(0.0, 100.0),
                                    teachersCount = initial?.education?.teachersCount ?: 10L,
                                    digitalClassrooms = (digitalClassrooms.toLongOrNull() ?: 1L).coerceAtLeast(0L),
                                    dropoutRatePct = (dropoutPct.toDoubleOrNull() ?: 8.0).coerceIn(0.0, 100.0)
                                ),
                                infrastructure = InfrastructureMetrics(
                                    pavedRoadPct = (pavedRoadPct.toDoubleOrNull() ?: 65.0).coerceIn(0.0, 100.0),
                                    electricityHoursPerDay = (elecHours.toDoubleOrNull() ?: 16.0).coerceIn(0.0, 24.0),
                                    cleanDrinkingWaterPct = (waterPct.toDoubleOrNull() ?: 70.0).coerceIn(0.0, 100.0),
                                    sanitationCoveragePct = (sanitationPct.toDoubleOrNull() ?: 75.0).coerceIn(0.0, 100.0),
                                    internetCoveragePct = (internetPct.toDoubleOrNull() ?: 45.0).coerceIn(0.0, 100.0),
                                    solarStreetLights = initial?.infrastructure?.solarStreetLights ?: 20L
                                )
                            )
                            onSave(record)
                        },
                        modifier = Modifier.testTag("save_village_btn")
                    ) {
                        Text("Save & Score")
                    }
                }
            }
        }
    }
}

@Composable
private fun FormField(
    label: String,
    value: String,
    modifier: Modifier = Modifier.fillMaxWidth(),
    numeric: Boolean = false,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label, style = MaterialTheme.typography.bodySmall) },
        singleLine = true,
        keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Decimal) else KeyboardOptions.Default,
        shape = RoundedCornerShape(12.dp),
        modifier = modifier
    )
}
