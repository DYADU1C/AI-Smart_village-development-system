package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

enum class DataSourceType(val code: String, val label: String) {
    VERIFIED_FIELD("VERIFIED_FIELD", "Verified Field Data"),
    DEMO_SAMPLE("DEMO_SAMPLE", "Demo Reference Data");

    companion object {
        fun fromCode(raw: String?): DataSourceType =
            entries.firstOrNull { it.code == raw } ?: VERIFIED_FIELD
    }
}

enum class DevelopmentLevel(val label: String) {
    MODEL_SMART("Model Smart Village"),
    EMERGING("Emerging Smart"),
    DEVELOPING("Developing"),
    CRITICAL("Critical Priority");

    companion object {
        fun fromLabel(raw: String?): DevelopmentLevel =
            entries.firstOrNull { it.label == raw } ?: DEVELOPING
    }
}

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val email: String = "",
    val roleLabel: String = "Village Officer",
    val district: String = "",
    val department: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class PopulationMetrics(
    val total: Long = 1000L,
    val maleCount: Long = 510L,
    val femaleCount: Long = 490L,
    val households: Long = 220L,
    val youthRatioPct: Double = 35.0,
    val workingAgePct: Double = 58.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "total" to total,
        "maleCount" to maleCount,
        "femaleCount" to femaleCount,
        "households" to households,
        "youthRatioPct" to youthRatioPct,
        "workingAgePct" to workingAgePct
    )
}

@Keep
@IgnoreExtraProperties
data class AgricultureMetrics(
    val soilType: String = "Alluvial",
    val annualRainfallMm: Double = 850.0,
    val avgTempC: Double = 27.0,
    val primarySeason: String = "Kharif",
    val irrigatedLandPct: Double = 50.0,
    val cultivableHectares: Double = 450.0,
    val majorCrops: String = "Wheat, Rice, Pulses"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "soilType" to soilType,
        "annualRainfallMm" to annualRainfallMm,
        "avgTempC" to avgTempC,
        "primarySeason" to primarySeason,
        "irrigatedLandPct" to irrigatedLandPct,
        "cultivableHectares" to cultivableHectares,
        "majorCrops" to majorCrops
    )
}

@Keep
@IgnoreExtraProperties
data class HealthcareMetrics(
    val primaryHealthCenters: Long = 1L,
    val subCenters: Long = 1L,
    val doctorsCount: Long = 1L,
    val nursesCount: Long = 3L,
    val hospitalBeds: Long = 6L,
    val ambulanceAvailable: Boolean = true,
    val avgDistanceToHospitalKm: Double = 8.0,
    val immunizationPct: Double = 82.0
) {
    fun toMap(): Map<String, Any> = mapOf(
        "primaryHealthCenters" to primaryHealthCenters,
        "subCenters" to subCenters,
        "doctorsCount" to doctorsCount,
        "nursesCount" to nursesCount,
        "hospitalBeds" to hospitalBeds,
        "ambulanceAvailable" to ambulanceAvailable,
        "avgDistanceToHospitalKm" to avgDistanceToHospitalKm,
        "immunizationPct" to immunizationPct
    )
}

@Keep
@IgnoreExtraProperties
data class EducationMetrics(
    val primarySchools: Long = 2L,
    val secondarySchools: Long = 1L,
    val literacyRatePct: Double = 72.0,
    val femaleLiteracyPct: Double = 65.0,
    val teachersCount: Long = 10L,
    val digitalClassrooms: Long = 2L,
    val dropoutRatePct: Double = 8.5
) {
    fun toMap(): Map<String, Any> = mapOf(
        "primarySchools" to primarySchools,
        "secondarySchools" to secondarySchools,
        "literacyRatePct" to literacyRatePct,
        "femaleLiteracyPct" to femaleLiteracyPct,
        "teachersCount" to teachersCount,
        "digitalClassrooms" to digitalClassrooms,
        "dropoutRatePct" to dropoutRatePct
    )
}

@Keep
@IgnoreExtraProperties
data class InfrastructureMetrics(
    val pavedRoadPct: Double = 65.0,
    val electricityHoursPerDay: Double = 16.0,
    val cleanDrinkingWaterPct: Double = 70.0,
    val sanitationCoveragePct: Double = 75.0,
    val internetCoveragePct: Double = 45.0,
    val solarStreetLights: Long = 24L
) {
    fun toMap(): Map<String, Any> = mapOf(
        "pavedRoadPct" to pavedRoadPct,
        "electricityHoursPerDay" to electricityHoursPerDay,
        "cleanDrinkingWaterPct" to cleanDrinkingWaterPct,
        "sanitationCoveragePct" to sanitationCoveragePct,
        "internetCoveragePct" to internetCoveragePct,
        "solarStreetLights" to solarStreetLights
    )
}

@Keep
@IgnoreExtraProperties
data class SectorScores(
    val overallScore: Double = 0.0,
    val agricultureScore: Double = 0.0,
    val healthcareScore: Double = 0.0,
    val educationScore: Double = 0.0,
    val infrastructureScore: Double = 0.0,
    val developmentLevel: String = DevelopmentLevel.DEVELOPING.label,
    val weakestSector: String = "Infrastructure"
) {
    fun toMap(): Map<String, Any> = mapOf(
        "overallScore" to overallScore,
        "agricultureScore" to agricultureScore,
        "healthcareScore" to healthcareScore,
        "educationScore" to educationScore,
        "infrastructureScore" to infrastructureScore,
        "developmentLevel" to developmentLevel,
        "weakestSector" to weakestSector
    )
}

@Keep
@IgnoreExtraProperties
data class VillageRecord(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val district: String = "",
    val state: String = "",
    val latitude: Double = 0.0,
    val longitude: Double = 0.0,
    val areaSqKm: Double = 5.0,
    val dataSourceType: String = DataSourceType.VERIFIED_FIELD.code,
    val population: PopulationMetrics = PopulationMetrics(),
    val agriculture: AgricultureMetrics = AgricultureMetrics(),
    val healthcare: HealthcareMetrics = HealthcareMetrics(),
    val education: EducationMetrics = EducationMetrics(),
    val infrastructure: InfrastructureMetrics = InfrastructureMetrics(),
    val scores: SectorScores = SectorScores(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    val sourceEnum: DataSourceType get() = DataSourceType.fromCode(dataSourceType)

    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim(),
        "district" to district.trim(),
        "state" to state.trim(),
        "latitude" to latitude,
        "longitude" to longitude,
        "areaSqKm" to areaSqKm,
        "dataSourceType" to dataSourceType,
        "population" to population.toMap(),
        "agriculture" to agriculture.toMap(),
        "healthcare" to healthcare.toMap(),
        "education" to education.toMap(),
        "infrastructure" to infrastructure.toMap(),
        "scores" to scores.toMap(),
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )

    fun toUpdateMap(): Map<String, Any> = mapOf(
        "name" to name.trim(),
        "district" to district.trim(),
        "state" to state.trim(),
        "latitude" to latitude,
        "longitude" to longitude,
        "areaSqKm" to areaSqKm,
        "dataSourceType" to dataSourceType,
        "population" to population.toMap(),
        "agriculture" to agriculture.toMap(),
        "healthcare" to healthcare.toMap(),
        "education" to education.toMap(),
        "infrastructure" to infrastructure.toMap(),
        "scores" to scores.toMap(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

@Keep
@IgnoreExtraProperties
data class DevelopmentAnalysis(
    @DocumentId val id: String = "",
    val userId: String = "",
    val villageId: String = "",
    val villageName: String = "",
    val dataSourceType: String = DataSourceType.VERIFIED_FIELD.code,
    val overallScore: Double = 0.0,
    val developmentLevel: String = DevelopmentLevel.DEVELOPING.label,
    val weakSectors: List<String> = emptyList(),
    val mlModelSummary: String = "",
    val aiExecutiveSummary: String = "",
    val recommendations: List<String> = emptyList(),
    val priorityActions: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> {
        val map = mutableMapOf<String, Any>(
            "userId" to uid,
            "villageId" to villageId,
            "villageName" to villageName.take(80),
            "dataSourceType" to dataSourceType,
            "overallScore" to overallScore,
            "developmentLevel" to developmentLevel.take(40),
            "weakSectors" to weakSectors.take(6),
            "mlModelSummary" to mlModelSummary.take(3000),
            "recommendations" to recommendations.take(15),
            "priorityActions" to priorityActions.take(10),
            "createdAt" to FieldValue.serverTimestamp(),
            "updatedAt" to FieldValue.serverTimestamp()
        )
        if (aiExecutiveSummary.isNotBlank()) {
            map["aiExecutiveSummary"] = aiExecutiveSummary.take(5000)
        }
        return map
    }
}

@Keep
@IgnoreExtraProperties
data class GovernmentScheme(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val ministry: String = "",
    val sector: String = "Multi-Sector",
    val eligibility: String = "",
    val benefits: String = "",
    val officialWebsite: String = "",
    val verifiedOfficial: Boolean = true,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
) {
    fun toCreateMap(uid: String): Map<String, Any> = mapOf(
        "userId" to uid,
        "name" to name.trim().take(140),
        "ministry" to ministry.trim().take(140),
        "sector" to sector,
        "eligibility" to eligibility.trim().take(600),
        "benefits" to benefits.trim().take(600),
        "officialWebsite" to officialWebsite.trim().take(240),
        "verifiedOfficial" to verifiedOfficial,
        "createdAt" to FieldValue.serverTimestamp(),
        "updatedAt" to FieldValue.serverTimestamp()
    )
}

sealed interface UiState<out T> {
    data object Loading : UiState<Nothing>
    data class Success<T>(val data: T) : UiState<T>
    data class Error(val message: String) : UiState<Nothing>
}

sealed interface AiTaskState<out T> {
    data object Idle : AiTaskState<Nothing>
    data object Loading : AiTaskState<Nothing>
    data class Success<T>(val data: T) : AiTaskState<T>
    data class Error(val message: String) : AiTaskState<Nothing>
}
