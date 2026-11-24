package com.example.data.ai

import com.example.data.model.GovernmentScheme
import com.example.data.model.VillageRecord
import com.example.ml.VillageMlAnalysisResult
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.Schema
import com.google.firebase.ai.type.generationConfig
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.withTimeout
import org.json.JSONObject

data class AiVillageDeepReport(
    val executiveSummary: String,
    val priorityActions: List<String>,
    val schemeActionNotes: List<String>,
    val impactProjection: String
)

class VillageAiService {

    private val structuredSchema = Schema.obj(
        properties = mapOf(
            "executiveSummary" to Schema.string(
                description = "Concise 3-4 sentence diagnostic summary grounded strictly in the provided village metrics."
            ),
            "priorityActions" to Schema.array(
                items = Schema.string(),
                description = "4 to 6 concrete, ordered rural development interventions targeting weak sectors."
            ),
            "schemeActionNotes" to Schema.array(
                items = Schema.string(),
                description = "2 to 4 ways to map the village gaps to eligible government schemes."
            ),
            "impactProjection" to Schema.string(
                description = "Realistic qualitative impact summary if top recommendations are executed over 12-24 months."
            )
        )
    )

    private val structuredModel by lazy {
        Firebase.ai.generativeModel(
            modelName = "gemini-3.8-flash",
            generationConfig = generationConfig {
                responseMimeType = "application/json"
                responseSchema = structuredSchema
            }
        )
    }

    private val textModel by lazy {
        Firebase.ai.generativeModel(modelName = "gemini-3.8-flash")
    }

    suspend fun generateVillageDeepReport(
        village: VillageRecord,
        mlResult: VillageMlAnalysisResult,
        matchedSchemes: List<GovernmentScheme>
    ): AiVillageDeepReport {
        val schemeNames = matchedSchemes.take(5).joinToString(", ") { "${it.name} (${it.sector})" }
        val prompt = """
            You are a rural development systems analyst advising government planners.
            Analyze ONLY the factual metrics below (do NOT invent statistics or claim fake accuracy).
            Data Classification: ${village.sourceEnum.label}
            Village: ${village.name}, District: ${village.district}, State: ${village.state}
            Population: ${village.population.total} (${village.population.households} households)
            Computed Composite Score: ${mlResult.scores.overallScore}/100 (Tier: ${mlResult.scores.developmentLevel})
            k-NN Classifier Tier: ${mlResult.knnClassifiedTier.label}
            Ridge Regression Score: ${mlResult.predictedRegressionScore}/100
            Sector Scores -> Agriculture: ${mlResult.scores.agricultureScore}, Healthcare: ${mlResult.scores.healthcareScore}, Education: ${mlResult.scores.educationScore}, Infrastructure: ${mlResult.scores.infrastructureScore}
            Weakest Sector: ${mlResult.scores.weakestSector}
            Detected Gaps:
            - Healthcare: ${mlResult.healthcareGaps.ifEmpty { listOf("No critical gaps flagged") }.joinToString("; ")}
            - Education: ${mlResult.educationGaps.ifEmpty { listOf("No critical gaps flagged") }.joinToString("; ")}
            - Infrastructure: ${mlResult.infrastructureGaps.ifEmpty { listOf("No critical gaps flagged") }.joinToString("; ")}
            - Agriculture: ${mlResult.agricultureGaps.ifEmpty { listOf("No critical gaps flagged") }.joinToString("; ")}
            Available Verified Schemes: $schemeNames
            Provide a practical, actionable, non-generic plan tailored to these exact gaps.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                structuredModel.generateContent(prompt)
            }
            val rawJson = response.text
                ?: throw IllegalStateException("AI model returned an empty response.")
            val json = JSONObject(rawJson)

            val summary = json.getString("executiveSummary")
            val actionsJson = json.getJSONArray("priorityActions")
            val actions = (0 until actionsJson.length()).map { actionsJson.getString(it) }
            val notesJson = json.getJSONArray("schemeActionNotes")
            val notes = (0 until notesJson.length()).map { notesJson.getString(it) }
            val impact = json.getString("impactProjection")

            AiVillageDeepReport(
                executiveSummary = summary,
                priorityActions = actions,
                schemeActionNotes = notes,
                impactProjection = impact
            )
        } catch (e: CancellationException) {
            throw e
        }
    }

    suspend fun generateAgronomyAdvice(
        soilType: String,
        rainfallMm: Double,
        tempC: Double,
        season: String,
        irrigatedPct: Double,
        topCrops: List<String>
    ): String {
        val prompt = """
            Provide a practical 4-bullet Smart Agriculture & Water-Saving Advisory for a village with:
            - Soil Type: $soilType
            - Annual Rainfall: $rainfallMm mm
            - Average Temperature: $tempC °C
            - Season: $season
            - Irrigated Land: $irrigatedPct%
            - Top Candidate Crops: ${topCrops.joinToString(", ")}
            Include specific fertilizer timing, micro-irrigation layout, and soil moisture retention advice in clear plain text.
        """.trimIndent()

        return try {
            val response = withTimeout(60_000L) {
                textModel.generateContent(prompt)
            }
            response.text?.takeIf { it.isNotBlank() }
                ?: throw IllegalStateException("AI agronomy advisor returned an empty response.")
        } catch (e: CancellationException) {
            throw e
        }
    }
}
