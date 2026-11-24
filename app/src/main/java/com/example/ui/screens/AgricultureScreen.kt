package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.WaterDrop
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.AiTaskState
import com.example.data.model.UiState
import com.example.data.model.VillageRecord
import com.example.ml.CropRecommendation
import com.example.ml.VillageMlEngine

@Composable
fun AgricultureScreen(
    villagesState: UiState<List<VillageRecord>>,
    selectedVillageId: String?,
    aiAgronomyState: AiTaskState<String>,
    onSelectVillage: (String) -> Unit,
    onAskAiAgronomy: (String, Double, Double, String, Double, List<CropRecommendation>) -> Unit
) {
    val villages = (villagesState as? UiState.Success)?.data.orEmpty()
    val activeVillage = villages.firstOrNull { it.id == selectedVillageId } ?: villages.firstOrNull()

    var soilType by remember { mutableStateOf(activeVillage?.agriculture?.soilType ?: "Alluvial") }
    var rainfallText by remember { mutableStateOf((activeVillage?.agriculture?.annualRainfallMm ?: 850.0).toString()) }
    var tempText by remember { mutableStateOf((activeVillage?.agriculture?.avgTempC ?: 27.0).toString()) }
    var season by remember { mutableStateOf(activeVillage?.agriculture?.primarySeason ?: "Kharif") }
    var irrigatedPct by remember { mutableStateOf(activeVillage?.agriculture?.irrigatedLandPct ?: 55.0) }

    LaunchedEffect(activeVillage?.id) {
        activeVillage?.let {
            soilType = it.agriculture.soilType
            rainfallText = it.agriculture.annualRainfallMm.toString()
            tempText = it.agriculture.avgTempC.toString()
            season = it.agriculture.primarySeason
            irrigatedPct = it.agriculture.irrigatedLandPct
        }
    }

    val rainVal = rainfallText.toDoubleOrNull() ?: 850.0
    val tempVal = tempText.toDoubleOrNull() ?: 27.0
    val recommendations = remember(soilType, rainVal, tempVal, season) {
        VillageMlEngine.recommendCrops(soilType, rainVal, tempVal, season)
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Card(shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
                Box(modifier = Modifier.fillMaxWidth().height(135.dp)) {
                    Image(
                        painter = painterResource(id = R.drawable.img_agriculture_banner),
                        contentDescription = "Smart Agriculture Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.88f),
                                        MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                                    )
                                )
                            )
                    )
                    Column(
                        modifier = Modifier.align(Alignment.CenterStart).padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(
                            "Smart Agriculture & Precision Irrigation",
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.onPrimary,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            "Crop suitability, NPK fertilization & water-saving methods",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.9f)
                        )
                    }
                }
            }
        }

        if (villages.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Load Parameters from Registered Village", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        villages.forEach { v ->
                            FilterChip(
                                selected = v.id == activeVillage?.id,
                                onClick = { onSelectVillage(v.id) },
                                label = { Text(v.name) }
                            )
                        }
                    }
                }
            }
        }

        // Agricultural Stats Summary if a village is selected
        activeVillage?.let { v ->
            item {
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        StatMini("Cultivable", "${v.agriculture.cultivableHectares} ha")
                        StatMini("Irrigated", "${v.agriculture.irrigatedLandPct}%")
                        StatMini("Agri Score", "${v.scores.agricultureScore}%")
                    }
                }
            }
        }

        // Input Parameters Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("Soil & Climate Parameters", style = MaterialTheme.typography.titleMedium)

                    Text("Soil Type", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Alluvial", "Black Cotton", "Red Loamy", "Laterite", "Sandy", "Clayey").forEach { s ->
                            FilterChip(
                                selected = soilType.equals(s, ignoreCase = true),
                                onClick = { soilType = s },
                                label = { Text(s) }
                            )
                        }
                    }

                    Text("Season", style = MaterialTheme.typography.labelMedium)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("Kharif", "Rabi", "Zaid", "All-Season").forEach { sn ->
                            FilterChip(
                                selected = season.equals(sn, ignoreCase = true),
                                onClick = { season = sn },
                                label = { Text(sn) }
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        OutlinedTextField(
                            value = rainfallText,
                            onValueChange = { rainfallText = it },
                            label = { Text("Rainfall (mm)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = tempText,
                            onValueChange = { tempText = it },
                            label = { Text("Avg Temp (°C)") },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        item {
            Text("Top Recommended Crops & Irrigation Plan", style = MaterialTheme.typography.titleMedium)
        }

        items(recommendations, key = { it.cropName }) { rec ->
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(Icons.Rounded.Eco, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(8.dp))
                            Text(rec.cropName, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        }
                        Surface(
                            color = MaterialTheme.colorScheme.primaryContainer,
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                text = "${rec.suitabilityScore}% Match",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    }

                    LinearProgressIndicator(
                        progress = { rec.suitabilityScore / 100f },
                        modifier = Modifier.fillMaxWidth().height(6.dp),
                        color = MaterialTheme.colorScheme.primary
                    )

                    Text(rec.reason, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text("Fertilizer: ${rec.npkFertilizer}", style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
                    Row(verticalAlignment = Alignment.Top) {
                        Icon(
                            Icons.Rounded.WaterDrop,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary,
                            modifier = Modifier.size(16.dp).padding(top = 2.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "${rec.irrigationMethod} — ${rec.waterSavingTip}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }
                }
            }
        }

        // Gemini AI Agronomy Advisor
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text("AI Agronomy & Water Conservation Advisor", style = MaterialTheme.typography.titleMedium)
                    Button(
                        onClick = {
                            onAskAiAgronomy(soilType, rainVal, tempVal, season, irrigatedPct, recommendations)
                        },
                        enabled = aiAgronomyState !is AiTaskState.Loading,
                        modifier = Modifier.fillMaxWidth().testTag("ai_agronomy_btn")
                    ) {
                        if (aiAgronomyState is AiTaskState.Loading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = MaterialTheme.colorScheme.onPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(8.dp))
                            Text("Generating AI Agronomy Plan...")
                        } else {
                            Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(8.dp))
                            Text("Get Gemini Agronomy Advisory")
                        }
                    }

                    when (aiAgronomyState) {
                        is AiTaskState.Success -> Text(aiAgronomyState.data, style = MaterialTheme.typography.bodyMedium)
                        is AiTaskState.Error -> Text(aiAgronomyState.message, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        else -> {}
                    }
                }
            }
        }
    }
}

@Composable
private fun StatMini(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}
