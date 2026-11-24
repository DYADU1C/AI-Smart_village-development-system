package com.example.ui.screens

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.UiState
import com.example.data.model.VillageRecord
import com.example.ml.VillageMlEngine
import com.example.ui.components.DataSourceBadge
import com.example.ui.theme.SectorEduColor
import com.example.ui.theme.SectorHealthColor
import com.example.ui.theme.SectorInfraColor
import com.example.ui.theme.StatusGood
import com.example.ui.theme.StatusWarning
import kotlin.math.roundToInt

@Composable
fun SectorAnalysisScreen(
    villagesState: UiState<List<VillageRecord>>,
    selectedVillageId: String?,
    initialTab: Int = 0,
    onSelectVillage: (String) -> Unit
) {
    var selectedTab by remember(initialTab) { mutableIntStateOf(initialTab) }
    val tabs = listOf("Healthcare", "Education", "Infrastructure")
    val villages = (villagesState as? UiState.Success)?.data.orEmpty()
    val village = villages.firstOrNull { it.id == selectedVillageId } ?: villages.firstOrNull()

    Column(modifier = Modifier.fillMaxSize()) {
        TabRow(selectedTabIndex = selectedTab) {
            tabs.forEachIndexed { index, title ->
                Tab(
                    selected = selectedTab == index,
                    onClick = { selectedTab = index },
                    text = { Text(title) },
                    icon = {
                        Icon(
                            imageVector = when (index) {
                                0 -> Icons.Rounded.LocalHospital
                                1 -> Icons.Rounded.School
                                else -> Icons.Rounded.Engineering
                            },
                            contentDescription = title
                        )
                    }
                )
            }
        }

        if (villages.isEmpty() || village == null) {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(
                    "Add a village or load Demo Reference Data to inspect Healthcare, Education, and Infrastructure sector diagnostics.",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            val ml = VillageMlEngine.analyzeVillage(village)

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        villages.forEach { v ->
                            FilterChip(
                                selected = v.id == village.id,
                                onClick = { onSelectVillage(v.id) },
                                label = { Text(v.name) }
                            )
                        }
                    }
                }

                when (selectedTab) {
                    0 -> {
                        val pop = village.population.total.coerceAtLeast(1L).toDouble()
                        val docsPer10k = ((village.healthcare.doctorsCount / pop) * 10_000.0 * 10).roundToInt() / 10.0
                        item {
                            SectorHeaderCard(
                                title = "Healthcare Facility Audit — ${village.name}",
                                score = ml.scores.healthcareScore,
                                accent = SectorHealthColor,
                                sourceEnum = village.sourceEnum
                            )
                        }
                        item {
                            MetricsGridCard(
                                rows = listOf(
                                    "Primary Health Centers (PHCs)" to "${village.healthcare.primaryHealthCenters}",
                                    "Sub-Health Centers" to "${village.healthcare.subCenters}",
                                    "Doctors (Ratio / 10k)" to "${village.healthcare.doctorsCount} ($docsPer10k per 10k)",
                                    "Nurses & Paramedics" to "${village.healthcare.nursesCount}",
                                    "Hospital Beds" to "${village.healthcare.hospitalBeds}",
                                    "Ambulance Service" to if (village.healthcare.ambulanceAvailable) "Available" else "Unavailable",
                                    "Nearest Referral Hospital" to "${village.healthcare.avgDistanceToHospitalKm} km",
                                    "Immunization Coverage" to "${village.healthcare.immunizationPct}%"
                                )
                            )
                        }
                        item {
                            Text("Healthcare Gap Diagnostics & Improvements", style = MaterialTheme.typography.titleMedium)
                        }
                        if (ml.healthcareGaps.isEmpty()) {
                            item { AdequateCard("Healthcare facilities meet rural baseline benchmarks. Maintain stock of essential medicines & telemedicine upkeep.") }
                        } else {
                            items(ml.healthcareGaps) { gap -> GapCard(gap) }
                        }
                    }

                    1 -> {
                        item {
                            SectorHeaderCard(
                                title = "Education & Literacy Audit — ${village.name}",
                                score = ml.scores.educationScore,
                                accent = SectorEduColor,
                                sourceEnum = village.sourceEnum
                            )
                        }
                        item {
                            MetricsGridCard(
                                rows = listOf(
                                    "Primary Schools" to "${village.education.primarySchools}",
                                    "Secondary Schools" to "${village.education.secondarySchools}",
                                    "Overall Literacy Rate" to "${village.education.literacyRatePct}%",
                                    "Female Literacy Rate" to "${village.education.femaleLiteracyPct}%",
                                    "Teaching Staff" to "${village.education.teachersCount}",
                                    "Smart / Digital Classrooms" to "${village.education.digitalClassrooms}",
                                    "School Dropout Rate" to "${village.education.dropoutRatePct}%"
                                )
                            )
                        }
                        item {
                            Text("Education Gaps & Recommended Improvements", style = MaterialTheme.typography.titleMedium)
                        }
                        if (ml.educationGaps.isEmpty()) {
                            item { AdequateCard("Educational indicators are strong. Continue STEM digital literacy & scholarship tracking.") }
                        } else {
                            items(ml.educationGaps) { gap -> GapCard(gap) }
                        }
                    }

                    else -> {
                        item {
                            SectorHeaderCard(
                                title = "Infrastructure & Connectivity Audit — ${village.name}",
                                score = ml.scores.infrastructureScore,
                                accent = SectorInfraColor,
                                sourceEnum = village.sourceEnum
                            )
                        }
                        item {
                            MetricsGridCard(
                                rows = listOf(
                                    "All-Weather Paved Roads" to "${village.infrastructure.pavedRoadPct}%",
                                    "Electricity Availability" to "${village.infrastructure.electricityHoursPerDay} hrs/day",
                                    "Clean Drinking Water" to "${village.infrastructure.cleanDrinkingWaterPct}%",
                                    "Sanitation Coverage" to "${village.infrastructure.sanitationCoveragePct}%",
                                    "Internet / Broadband Coverage" to "${village.infrastructure.internetCoveragePct}%",
                                    "Solar Street Lights" to "${village.infrastructure.solarStreetLights}"
                                )
                            )
                        }
                        item {
                            Text("Priority Infrastructure Problems", style = MaterialTheme.typography.titleMedium)
                        }
                        if (ml.infrastructureGaps.isEmpty()) {
                            item { AdequateCard("Core physical & digital infrastructure exceeds smart-village thresholds.") }
                        } else {
                            items(ml.infrastructureGaps) { gap -> GapCard(gap) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SectorHeaderCard(
    title: String,
    score: Double,
    accent: androidx.compose.ui.graphics.Color,
    sourceEnum: com.example.data.model.DataSourceType
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                DataSourceBadge(sourceEnum)
            }
            Text("Sector Score: $score / 100", style = MaterialTheme.typography.titleSmall, color = accent)
            LinearProgressIndicator(
                progress = { (score / 100.0).toFloat().coerceIn(0f, 1f) },
                color = accent,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

@Composable
private fun MetricsGridCard(rows: List<Pair<String, String>>) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            rows.forEach { (k, v) ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(k, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(v, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold)
                }
            }
        }
    }
}

@Composable
private fun GapCard(text: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StatusWarning.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.WarningAmber, contentDescription = null, tint = StatusWarning, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

@Composable
private fun AdequateCard(text: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = StatusGood.copy(alpha = 0.10f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Rounded.CheckCircle, contentDescription = null, tint = StatusGood, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(10.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
