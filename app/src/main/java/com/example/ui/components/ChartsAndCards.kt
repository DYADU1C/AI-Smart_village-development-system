package com.example.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.data.model.DataSourceType
import com.example.data.model.DevelopmentLevel
import com.example.data.model.VillageRecord
import com.example.ui.theme.SectorAgriColor
import com.example.ui.theme.SectorEduColor
import com.example.ui.theme.SectorHealthColor
import com.example.ui.theme.SectorInfraColor
import com.example.ui.theme.StatusCritical
import com.example.ui.theme.StatusGood
import com.example.ui.theme.StatusModerate
import com.example.ui.theme.StatusWarning
import kotlin.math.roundToInt

@Composable
fun DataSourceBadge(
    sourceType: DataSourceType,
    modifier: Modifier = Modifier
) {
    val isVerified = sourceType == DataSourceType.VERIFIED_FIELD
    val containerColor = if (isVerified) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.secondaryContainer
    }
    val contentColor = if (isVerified) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onSecondaryContainer
    }
    Surface(
        color = containerColor,
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = if (isVerified) "VERIFIED FIELD" else "DEMO SAMPLE",
            style = MaterialTheme.typography.labelSmall,
            color = contentColor,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
        )
    }
}

@Composable
fun TierBadge(
    developmentLevel: String,
    modifier: Modifier = Modifier
) {
    val color = when (DevelopmentLevel.fromLabel(developmentLevel)) {
        DevelopmentLevel.MODEL_SMART -> StatusGood
        DevelopmentLevel.EMERGING -> StatusModerate
        DevelopmentLevel.DEVELOPING -> StatusWarning
        DevelopmentLevel.CRITICAL -> StatusCritical
    }
    Surface(
        color = color.copy(alpha = 0.14f),
        shape = RoundedCornerShape(50),
        modifier = modifier
    ) {
        Text(
            text = developmentLevel,
            style = MaterialTheme.typography.labelMedium,
            color = color,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
        )
    }
}

@Composable
fun KpiMetricCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    accentColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = accentColor.copy(alpha = 0.14f),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.size(34.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = icon,
                            contentDescription = title,
                            tint = accentColor,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SectorBarChartCard(
    title: String,
    subtitle: String,
    agricultureAvg: Double,
    healthcareAvg: Double,
    educationAvg: Double,
    infrastructureAvg: Double,
    modifier: Modifier = Modifier
) {
    val entries = listOf(
        Triple("Agriculture", agricultureAvg, SectorAgriColor),
        Triple("Healthcare", healthcareAvg, SectorHealthColor),
        Triple("Education", educationAvg, SectorEduColor),
        Triple("Infrastructure", infrastructureAvg, SectorInfraColor)
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Column {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            entries.forEach { (label, score, color) ->
                val animatedFraction by animateFloatAsState(
                    targetValue = (score / 100.0).toFloat().coerceIn(0f, 1f),
                    animationSpec = tween(durationMillis = 650),
                    label = "bar_$label"
                )
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(9.dp)
                                    .clip(CircleShape)
                                    .background(color)
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                        }
                        Text(
                            text = "${(score * 10).roundToInt() / 10.0}%",
                            style = MaterialTheme.typography.labelLarge,
                            color = color
                        )
                    }
                    val trackColor = MaterialTheme.colorScheme.surfaceVariant
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                    ) {
                        drawRoundRect(
                            color = trackColor,
                            cornerRadius = CornerRadius(12f, 12f),
                            size = size
                        )
                        drawRoundRect(
                            color = color,
                            cornerRadius = CornerRadius(12f, 12f),
                            size = Size(width = size.width * animatedFraction, height = size.height)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun TierDistributionDonutCard(
    villages: List<VillageRecord>,
    modifier: Modifier = Modifier
) {
    val modelCount = villages.count { it.scores.developmentLevel == DevelopmentLevel.MODEL_SMART.label }
    val emergingCount = villages.count { it.scores.developmentLevel == DevelopmentLevel.EMERGING.label }
    val developingCount = villages.count { it.scores.developmentLevel == DevelopmentLevel.DEVELOPING.label }
    val criticalCount = villages.count { it.scores.developmentLevel == DevelopmentLevel.CRITICAL.label }
    val total = villages.size.coerceAtLeast(1)

    val slices = listOf(
        Triple("Model Smart", modelCount, StatusGood),
        Triple("Emerging", emergingCount, StatusModerate),
        Triple("Developing", developingCount, StatusWarning),
        Triple("Critical", criticalCount, StatusCritical)
    )

    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        modifier = modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text("Development Tier Classification", style = MaterialTheme.typography.titleMedium)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(18.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier.size(120.dp)
                ) {
                    val emptyTrack = MaterialTheme.colorScheme.surfaceVariant
                    Canvas(modifier = Modifier.size(110.dp)) {
                        val stroke = 20.dp.toPx()
                        if (villages.isEmpty()) {
                            drawArc(
                                color = emptyTrack,
                                startAngle = 0f,
                                sweepAngle = 360f,
                                useCenter = false,
                                style = Stroke(width = stroke, cap = StrokeCap.Round)
                            )
                        } else {
                            var startAngle = -90f
                            slices.forEach { (_, count, color) ->
                                if (count > 0) {
                                    val sweep = (count.toFloat() / total.toFloat()) * 360f
                                    drawArc(
                                        color = color,
                                        startAngle = startAngle,
                                        sweepAngle = (sweep - 4f).coerceAtLeast(2f),
                                        useCenter = false,
                                        style = Stroke(width = stroke, cap = StrokeCap.Butt),
                                        topLeft = Offset(stroke / 2f, stroke / 2f),
                                        size = Size(size.width - stroke, size.height - stroke)
                                    )
                                    startAngle += sweep
                                }
                            }
                        }
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${villages.size}",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Villages",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Column(
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    slices.forEach { (label, count, color) ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(label, style = MaterialTheme.typography.bodySmall)
                            }
                            Text(
                                text = "$count",
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}
