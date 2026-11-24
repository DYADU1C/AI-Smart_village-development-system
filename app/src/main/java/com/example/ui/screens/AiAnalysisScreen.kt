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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.AutoAwesome
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.ModelTraining
import androidx.compose.material.icons.rounded.Psychology
import androidx.compose.material.icons.rounded.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.ai.AiVillageDeepReport
import com.example.data.model.AiTaskState
import com.example.data.model.DevelopmentAnalysis
import com.example.data.model.GovernmentScheme
import com.example.data.model.UiState
import com.example.data.model.VillageRecord
import com.example.ml.MlEvaluationReport
import com.example.ml.VillageMlEngine
import com.example.ui.components.DataSourceBadge
import com.example.ui.components.SectorBarChartCard
import com.example.ui.components.TierBadge

@Composable
fun AiAnalysisScreen(
    villagesState: UiState<List<VillageRecord>>,
    selectedVillageId: String?,
    schemesState: UiState<List<GovernmentScheme>>,
    analysesState: UiState<List<DevelopmentAnalysis>>,
    mlEvalReport: MlEvaluationReport,
    aiReportState: AiTaskState<AiVillageDeepReport>,
    onSelectVillage: (String) -> Unit,
    onRunAiAnalysis: (VillageRecord, com.example.ml.VillageMlAnalysisResult, List<GovernmentScheme>) -> Unit
) {
    val villages = (villagesState as? UiState.Success)?.data.orEmpty()
    val schemes = (schemesState as? UiState.Success)?.data.orEmpty()
    val analyses = (analysesState as? UiState.Success)?.data.orEmpty()
    val selectedVillage = villages.firstOrNull { it.id == selectedVillageId } ?: villages.firstOrNull()

    LazyColumn(
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
        modifier = Modifier.fillMaxSize()
    ) {
        // Model Evaluation Transparency Card
        item {
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Rounded.ModelTraining,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.secondary
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            "ML Engine & Holdout Evaluation",
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                    Text(
                        text = mlEvalReport.datasetNotice,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        EvalChip("MAE", "${mlEvalReport.linearRegressionMae}", Modifier.weight(1f))
                        EvalChip("RMSE", "${mlEvalReport.linearRegressionRmse}", Modifier.weight(1f))
                        EvalChip("R²", "${mlEvalReport.linearRegressionR2}", Modifier.weight(1f))
                        EvalChip("k-NN Acc", "${mlEvalReport.knnAccuracyPct}%", Modifier.weight(1f))
                    }
                }
            }
        }

        if (villages.isEmpty()) {
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Register a village or load Demo Reference Data on the Dashboard/Villages screen to run ML & Gemini AI development analysis.",
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(18.dp)
                    )
                }
            }
        } else {
            // Village Selector Chips
            item {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("Select Village to Analyze", style = MaterialTheme.typography.titleSmall)
                    Row(
                        modifier = Modifier.horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        villages.forEach { v ->
                            FilterChip(
                                selected = v.id == selectedVillage?.id,
                                onClick = { onSelectVillage(v.id) },
                                label = { Text(v.name) }
                            )
                        }
                    }
                }
            }

            selectedVillage?.let { village ->
                val mlResult = VillageMlEngine.analyzeVillage(village)
                val matchedSchemes = schemes.filter {
                    it.sector in mlResult.weakSectors || it.sector == "Multi-Sector"
                }

                // ML Result Summary Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(village.name, style = MaterialTheme.typography.titleLarge)
                                    Text(
                                        "${village.district}, ${village.state}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                DataSourceBadge(village.sourceEnum)
                            }

                            Row(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TierBadge(mlResult.scores.developmentLevel)
                                Text(
                                    text = "Composite: ${mlResult.scores.overallScore}%",
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            HorizontalDivider()

                            Text(
                                text = "Ridge OLS Prediction: ${mlResult.predictedRegressionScore}/100  •  k-NN (k=3) Class: ${mlResult.knnClassifiedTier.label}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium
                            )
                            Text(
                                text = "Nearest benchmark neighbors: ${mlResult.knnNeighborsUsed.joinToString(", ")}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "Weak Sectors Identified: ${mlResult.weakSectors.joinToString(", ")}",
                                style = MaterialTheme.typography.titleSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }

                item {
                    SectorBarChartCard(
                        title = "${village.name} Sector Breakdown",
                        subtitle = "Composite index across 4 development pillars",
                        agricultureAvg = mlResult.scores.agricultureScore,
                        healthcareAvg = mlResult.scores.healthcareScore,
                        educationAvg = mlResult.scores.educationScore,
                        infrastructureAvg = mlResult.scores.infrastructureScore
                    )
                }

                // Rule-based ML Recommendations
                item {
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text("Diagnostic Gap Recommendations", style = MaterialTheme.typography.titleMedium)
                            mlResult.prioritizedRecommendations.forEach { rec ->
                                Row(verticalAlignment = Alignment.Top) {
                                    Icon(
                                        Icons.Rounded.Warning,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.secondary,
                                        modifier = Modifier.size(18.dp).padding(top = 2.dp)
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text(rec, style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                    }
                }

                // Gemini AI Deep Analysis Card
                item {
                    Card(
                        shape = RoundedCornerShape(20.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    Icons.Rounded.Psychology,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    "Gemini AI Personalized Action Plan",
                                    style = MaterialTheme.typography.titleMedium
                                )
                            }

                            Button(
                                onClick = { onRunAiAnalysis(village, mlResult, matchedSchemes) },
                                enabled = aiReportState !is AiTaskState.Loading,
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("run_gemini_analysis_btn")
                            ) {
                                if (aiReportState is AiTaskState.Loading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(18.dp),
                                        color = MaterialTheme.colorScheme.onPrimary,
                                        strokeWidth = 2.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Text("Analyzing with Gemini 3.8 Flash...")
                                } else {
                                    Icon(Icons.Rounded.AutoAwesome, contentDescription = null, modifier = Modifier.size(18.dp))
                                    Spacer(Modifier.width(8.dp))
                                    Text("Generate AI Recommendations & Save")
                                }
                            }

                            when (aiReportState) {
                                is AiTaskState.Idle -> {
                                    Text(
                                        "Tap above to run structured Gemini AI analysis grounded in ${village.name}'s verified metrics and save the snapshot to Firestore.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                is AiTaskState.Loading -> {}
                                is AiTaskState.Error -> {
                                    Text(
                                        text = aiReportState.message,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.error
                                    )
                                }
                                is AiTaskState.Success -> {
                                    val report = aiReportState.data
                                    Text(report.executiveSummary, style = MaterialTheme.typography.bodyMedium)
                                    Text("Priority Interventions:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
                                    report.priorityActions.forEach { action ->
                                        Row(verticalAlignment = Alignment.Top) {
                                            Icon(
                                                Icons.Rounded.CheckCircle,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(16.dp).padding(top = 2.dp)
                                            )
                                            Spacer(Modifier.width(6.dp))
                                            Text(action, style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    if (report.schemeActionNotes.isNotEmpty()) {
                                        Text("Scheme Convergence:", style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.secondary)
                                        report.schemeActionNotes.forEach { note ->
                                            Text("• $note", style = MaterialTheme.typography.bodySmall)
                                        }
                                    }
                                    Text(
                                        text = "Projected Impact: ${report.impactProjection}",
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium
                                    )
                                }
                            }
                        }
                    }
                }

                // Saved Analysis History
                if (analyses.isNotEmpty()) {
                    item {
                        Text("Recent Saved Analyses (${analyses.size})", style = MaterialTheme.typography.titleMedium)
                    }
                    items(analyses.take(4).size) { idx ->
                        val item = analyses[idx]
                        Card(
                            shape = RoundedCornerShape(14.dp),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(item.villageName, style = MaterialTheme.typography.titleSmall)
                                    Text("${item.overallScore}% (${item.developmentLevel})", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                                }
                                Text(item.mlModelSummary, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EvalChip(label: String, value: String, modifier: Modifier = Modifier) {
    Surface(
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp),
        modifier = modifier
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(vertical = 8.dp, horizontal = 4.dp)
        ) {
            Text(value, style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
            Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
