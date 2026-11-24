package com.example.ui.screens

import android.content.Intent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material.icons.rounded.Share
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiVillageDeepReport
import com.example.data.model.AiTaskState
import com.example.data.model.DevelopmentAnalysis
import com.example.data.model.GovernmentScheme
import com.example.data.model.UiState
import com.example.data.model.VillageRecord
import com.example.ml.VillageMlEngine
import com.example.ui.components.DataSourceBadge
import com.example.ui.components.SectorBarChartCard
import com.example.ui.components.TierBadge
import com.example.util.VillagePdfGenerator

@Composable
fun ReportsScreen(
    villagesState: UiState<List<VillageRecord>>,
    selectedVillageId: String?,
    schemesState: UiState<List<GovernmentScheme>>,
    analysesState: UiState<List<DevelopmentAnalysis>>,
    aiReportState: AiTaskState<AiVillageDeepReport>,
    onSelectVillage: (String) -> Unit,
    onShowMessage: (String) -> Unit
) {
    val context = LocalContext.current
    val villages = (villagesState as? UiState.Success)?.data.orEmpty()
    val schemes = (schemesState as? UiState.Success)?.data.orEmpty()
    val analyses = (analysesState as? UiState.Success)?.data.orEmpty()
    val village = villages.firstOrNull { it.id == selectedVillageId } ?: villages.firstOrNull()

    val activeMl = village?.let { VillageMlEngine.analyzeVillage(it) }
    val matchedSchemes = if (activeMl != null) {
        schemes.filter { it.sector in activeMl.weakSectors || it.sector == "Multi-Sector" }
    } else emptyList()

    val savedSummary = village?.let { v ->
        (aiReportState as? AiTaskState.Success)?.data?.executiveSummary
            ?: analyses.firstOrNull { it.villageId == v.id }?.aiExecutiveSummary
    }

    val savePdfLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/pdf")
    ) { uri ->
        if (uri != null && village != null && activeMl != null) {
            try {
                context.contentResolver.openOutputStream(uri)?.use { stream ->
                    VillagePdfGenerator.writeReportToStream(
                        outputStream = stream,
                        village = village,
                        mlResult = activeMl,
                        aiSummary = savedSummary,
                        matchedSchemes = matchedSchemes
                    )
                }
                onShowMessage("PDF report saved successfully.")
            } catch (e: Exception) {
                onShowMessage(e.localizedMessage ?: "Failed to save PDF report.")
            }
        }
    }

    if (villages.isEmpty() || village == null || activeMl == null) {
        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(
                "Register a village or load Demo Reference Data first to preview and export PDF development reports.",
                style = MaterialTheme.typography.bodyMedium
            )
        }
        return
    }

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        item {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text("Select Village for PDF Report", style = MaterialTheme.typography.titleSmall)
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
        }

        // Export Actions Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Rounded.PictureAsPdf, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(8.dp))
                        Text("Download & Share Village PDF Report", style = MaterialTheme.typography.titleMedium)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
                        Button(
                            onClick = {
                                val cleanName = village.name.replace(Regex("[^a-zA-Z0-9_\\-]"), "_")
                                savePdfLauncher.launch("SmartVillage_${cleanName}_Report.pdf")
                            },
                            modifier = Modifier.weight(1f).testTag("download_pdf_btn")
                        ) {
                            Icon(Icons.Rounded.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Save PDF")
                        }
                        OutlinedButton(
                            onClick = {
                                try {
                                    val uri = VillagePdfGenerator.writeReportToCacheAndGetUri(
                                        context = context,
                                        village = village,
                                        mlResult = activeMl,
                                        aiSummary = savedSummary,
                                        matchedSchemes = matchedSchemes
                                    )
                                    val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                        type = "application/pdf"
                                        putExtra(Intent.EXTRA_STREAM, uri)
                                        putExtra(Intent.EXTRA_SUBJECT, "Smart Village Report: ${village.name}")
                                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                                    }
                                    context.startActivity(Intent.createChooser(shareIntent, "Share Village PDF Report"))
                                } catch (e: Exception) {
                                    onShowMessage(e.localizedMessage ?: "Failed to share PDF.")
                                }
                            },
                            modifier = Modifier.weight(1f).testTag("share_pdf_btn")
                        ) {
                            Icon(Icons.Rounded.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(Modifier.width(6.dp))
                            Text("Share PDF")
                        }
                    }
                }
            }
        }

        // Report Preview
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Report Preview", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        DataSourceBadge(village.sourceEnum)
                    }
                    Text("${village.name}, ${village.district}, ${village.state}", style = MaterialTheme.typography.titleLarge)
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                        TierBadge(activeMl.scores.developmentLevel)
                        Text("Composite Score: ${activeMl.scores.overallScore}%", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                    }
                    HorizontalDivider()
                    Text("ML Classification: ${activeMl.knnClassifiedTier.label} • Regression: ${activeMl.predictedRegressionScore}/100", style = MaterialTheme.typography.bodySmall)
                    Text("Weakest Sector: ${activeMl.scores.weakestSector}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)

                    if (!savedSummary.isNullOrBlank()) {
                        HorizontalDivider()
                        Text("AI Executive Summary", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                        Text(savedSummary, style = MaterialTheme.typography.bodySmall)
                    }

                    HorizontalDivider()
                    Text("Key Recommendations (${activeMl.prioritizedRecommendations.size})", style = MaterialTheme.typography.titleSmall)
                    activeMl.prioritizedRecommendations.forEach { rec ->
                        Text("• $rec", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            SectorBarChartCard(
                title = "Report Sector Visual",
                subtitle = "Included as progress bars in generated A4 PDF",
                agricultureAvg = activeMl.scores.agricultureScore,
                healthcareAvg = activeMl.scores.healthcareScore,
                educationAvg = activeMl.scores.educationScore,
                infrastructureAvg = activeMl.scores.infrastructureScore
            )
        }
    }
}
