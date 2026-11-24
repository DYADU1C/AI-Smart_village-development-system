package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AddLocationAlt
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dataset
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.Engineering
import androidx.compose.material.icons.rounded.Groups
import androidx.compose.material.icons.rounded.LocalHospital
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.School
import androidx.compose.material.icons.rounded.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DataSourceType
import com.example.data.model.DevelopmentLevel
import com.example.data.model.UiState
import com.example.data.model.VillageRecord
import com.example.ui.components.DataSourceBadge
import com.example.ui.components.KpiMetricCard
import com.example.ui.components.SectorBarChartCard
import com.example.ui.components.TierBadge
import com.example.ui.components.TierDistributionDonutCard
import com.example.ui.theme.SectorAgriColor
import com.example.ui.theme.SectorEduColor
import com.example.ui.theme.SectorHealthColor
import com.example.ui.theme.SectorInfraColor
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusWarning
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun DashboardScreen(
    villagesState: UiState<List<VillageRecord>>,
    isBusy: Boolean,
    onLoadDemoData: () -> Unit,
    onAddVillageClick: () -> Unit,
    onOpenVillageAnalysis: (VillageRecord) -> Unit,
    onNavigateVillages: () -> Unit
) {
    when (villagesState) {
        is UiState.Loading -> {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        }
        is UiState.Error -> {
            Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                ) {
                    Text(
                        text = villagesState.message,
                        color = MaterialTheme.colorScheme.error,
                        modifier = Modifier.padding(16.dp)
                    )
                }
            }
        }
        is UiState.Success -> {
            val villages = villagesState.data
            val totalPop = villages.sumOf { it.population.total }
            val verifiedCount = villages.count { it.sourceEnum == DataSourceType.VERIFIED_FIELD }
            val demoCount = villages.count { it.sourceEnum == DataSourceType.DEMO_SAMPLE }
            val avgOverall = if (villages.isEmpty()) 0.0 else villages.map { it.scores.overallScore }.average()
            val avgAgri = if (villages.isEmpty()) 0.0 else villages.map { it.scores.agricultureScore }.average()
            val avgHealth = if (villages.isEmpty()) 0.0 else villages.map { it.scores.healthcareScore }.average()
            val avgEdu = if (villages.isEmpty()) 0.0 else villages.map { it.scores.educationScore }.average()
            val avgInfra = if (villages.isEmpty()) 0.0 else villages.map { it.scores.infrastructureScore }.average()

            val alerts = villages.filter {
                it.scores.overallScore < 60.0 ||
                    it.scores.developmentLevel == DevelopmentLevel.CRITICAL.label ||
                    it.healthcare.doctorsCount == 0L ||
                    it.infrastructure.cleanDrinkingWaterPct < 60.0
            }

            LazyColumn(
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                item {
                    // Hero Banner Card
                    Card(
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(165.dp)) {
                            Image(
                                painter = painterResource(id = R.drawable.img_village_hero),
                                contentDescription = "Smart Village Overview Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(
                                        Brush.horizontalGradient(
                                            colors = listOf(
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.90f),
                                                MaterialTheme.colorScheme.primary.copy(alpha = 0.55f)
                                            )
                                        )
                                    )
                            )
                            Column(
                                modifier = Modifier
                                    .align(Alignment.CenterStart)
                                    .padding(18.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Smart Village Development Hub",
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Empowering Villages Through Artificial Intelligence.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.92f)
                                )
                                Spacer(Modifier.height(4.dp))
                                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Surface(
                                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                                        shape = RoundedCornerShape(50)
                                    ) {
                                        Text(
                                            text = "$verifiedCount Verified • $demoCount Demo",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onPrimary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                if (villages.isEmpty()) {
                    item {
                        Card(
                            shape = RoundedCornerShape(20.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.LocationCity,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(48.dp)
                                )
                                Text(
                                    text = "No Villages Registered Yet",
                                    style = MaterialTheme.typography.titleLarge
                                )
                                Text(
                                    text = "Register a verified field village or load our clearly labeled Demo Reference Dataset to explore ML scoring, sector analytics, and PDF reports.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Button(
                                        onClick = onAddVillageClick,
                                        modifier = Modifier.weight(1f).testTag("empty_add_village_btn")
                                    ) {
                                        Icon(Icons.Rounded.AddLocationAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Add Village")
                                    }
                                    OutlinedButton(
                                        onClick = onLoadDemoData,
                                        enabled = !isBusy,
                                        modifier = Modifier.weight(1f).testTag("load_demo_villages_btn")
                                    ) {
                                        Icon(Icons.Rounded.Dataset, contentDescription = null, modifier = Modifier.size(18.dp))
                                        Spacer(Modifier.width(6.dp))
                                        Text("Load Demo Data")
                                    }
                                }
                            }
                        }
                    }
                } else {
                    // KPI Grid
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KpiMetricCard(
                                title = "Registered Villages",
                                value = "${villages.size}",
                                subtitle = "$verifiedCount Verified / $demoCount Demo",
                                icon = Icons.Rounded.LocationCity,
                                accentColor = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Total Population",
                                value = NumberFormat.getIntegerInstance(Locale.US).format(totalPop),
                                subtitle = "${villages.sumOf { it.population.households }} households",
                                icon = Icons.Rounded.Groups,
                                accentColor = MaterialTheme.colorScheme.secondary,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            KpiMetricCard(
                                title = "Avg Dev Score",
                                value = "${(avgOverall * 10).roundToInt() / 10.0}%",
                                subtitle = "Composite ML index",
                                icon = Icons.Rounded.Analytics,
                                accentColor = MaterialTheme.colorScheme.tertiary,
                                modifier = Modifier.weight(1f)
                            )
                            KpiMetricCard(
                                title = "Priority Alerts",
                                value = "${alerts.size}",
                                subtitle = "Villages needing action",
                                icon = Icons.Rounded.WarningAmber,
                                accentColor = if (alerts.isEmpty()) SectorAgriColor else StatusWarning,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    // Sector Quick Stats Row
                    item {
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            MiniSectorPill("Agri", avgAgri, Icons.Rounded.Eco, SectorAgriColor, Modifier.weight(1f))
                            MiniSectorPill("Health", avgHealth, Icons.Rounded.LocalHospital, SectorHealthColor, Modifier.weight(1f))
                            MiniSectorPill("Edu", avgEdu, Icons.Rounded.School, SectorEduColor, Modifier.weight(1f))
                            MiniSectorPill("Infra", avgInfra, Icons.Rounded.Engineering, SectorInfraColor, Modifier.weight(1f))
                        }
                    }

                    // Interactive Bar Chart
                    item {
                        SectorBarChartCard(
                            title = "Sector Performance Overview",
                            subtitle = "Average score across all ${villages.size} registered villages",
                            agricultureAvg = avgAgri,
                            healthcareAvg = avgHealth,
                            educationAvg = avgEdu,
                            infrastructureAvg = avgInfra
                        )
                    }

                    // Tier Distribution Donut Chart
                    item {
                        TierDistributionDonutCard(villages = villages)
                    }

                    // Alerts & Priority Requirements
                    item {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Alerts & Development Requirements",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "View All",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { onNavigateVillages() }
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }

                    if (alerts.isEmpty()) {
                        item {
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = "All registered villages currently meet baseline healthcare, water, and composite score thresholds.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(16.dp)
                                )
                            }
                        }
                    } else {
                        items(alerts, key = { it.id }) { village ->
                            Card(
                                shape = RoundedCornerShape(16.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { onOpenVillageAnalysis(village) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Surface(
                                        color = StatusCritical.copy(alpha = 0.12f),
                                        shape = RoundedCornerShape(12.dp),
                                        modifier = Modifier.size(42.dp)
                                    ) {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                Icons.Rounded.WarningAmber,
                                                contentDescription = "Alert",
                                                tint = StatusCritical
                                            )
                                        }
                                    }
                                    Spacer(Modifier.width(12.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                                        ) {
                                            Text(village.name, style = MaterialTheme.typography.titleSmall)
                                            DataSourceBadge(village.sourceEnum)
                                        }
                                        Text(
                                            text = "Weakest: ${village.scores.weakestSector} • Score: ${village.scores.overallScore}%",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                    TierBadge(village.scores.developmentLevel)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MiniSectorPill(
    label: String,
    score: Double,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: androidx.compose.ui.graphics.Color,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(14.dp),
        tonalElevation = 1.dp,
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 6.dp)
        ) {
            Icon(icon, contentDescription = label, tint = color, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(4.dp))
            Text("${score.roundToInt()}%", style = MaterialTheme.typography.titleSmall, color = color)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
