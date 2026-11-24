package com.example.ml

import com.example.data.model.AgricultureMetrics
import com.example.data.model.DevelopmentLevel
import com.example.data.model.EducationMetrics
import com.example.data.model.HealthcareMetrics
import com.example.data.model.InfrastructureMetrics
import com.example.data.model.PopulationMetrics
import com.example.data.model.SectorScores
import com.example.data.model.VillageRecord
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

data class BenchmarkSample(
    val id: String,
    val irrigatedLandPct: Double,
    val doctorsPer10k: Double,
    val immunizationPct: Double,
    val literacyRatePct: Double,
    val pavedRoadPct: Double,
    val electricityHours: Double,
    val drinkingWaterPct: Double,
    val sanitationPct: Double,
    val internetPct: Double,
    val targetCompositeScore: Double,
    val targetTier: DevelopmentLevel
)

data class MlEvaluationReport(
    val trainingSampleCount: Int,
    val testHoldoutCount: Int,
    val linearRegressionMae: Double,
    val linearRegressionRmse: Double,
    val linearRegressionR2: Double,
    val knnAccuracyPct: Double,
    val knnCorrectCount: Int,
    val datasetNotice: String
)

data class VillageMlAnalysisResult(
    val scores: SectorScores,
    val predictedRegressionScore: Double,
    val knnClassifiedTier: DevelopmentLevel,
    val knnNeighborsUsed: List<String>,
    val weakSectors: List<String>,
    val healthcareGaps: List<String>,
    val educationGaps: List<String>,
    val infrastructureGaps: List<String>,
    val agricultureGaps: List<String>,
    val prioritizedRecommendations: List<String>,
    val evaluationReport: MlEvaluationReport
)

data class CropRecommendation(
    val cropName: String,
    val suitabilityScore: Int,
    val npkFertilizer: String,
    val irrigationMethod: String,
    val waterSavingTip: String,
    val reason: String
)

data class AgronomicProfile(
    val cropName: String,
    val preferredSoils: Set<String>,
    val minRainMm: Double,
    val maxRainMm: Double,
    val minTempC: Double,
    val maxTempC: Double,
    val preferredSeasons: Set<String>,
    val npkFertilizer: String,
    val irrigationMethod: String,
    val waterSavingTip: String
)

object VillageMlEngine {

    /**
     * Synthetic Reference Benchmark Dataset (20 profiles: 15 training + 5 holdout test).
     * Strictly labeled as a synthetic benchmark dataset for ML training & out-of-sample evaluation,
     * never presented as real village records.
     */
    private val benchmarkDataset = listOf(
        // Training split (15 records)
        BenchmarkSample("REF-01", 82.0, 4.2, 96.0, 91.0, 94.0, 23.0, 96.0, 98.0, 82.0, 90.2, DevelopmentLevel.MODEL_SMART),
        BenchmarkSample("REF-02", 75.0, 3.5, 92.0, 86.0, 88.0, 21.0, 92.0, 95.0, 72.0, 84.5, DevelopmentLevel.MODEL_SMART),
        BenchmarkSample("REF-03", 68.0, 2.8, 88.0, 80.0, 82.0, 19.0, 86.0, 88.0, 62.0, 77.6, DevelopmentLevel.EMERGING),
        BenchmarkSample("REF-04", 55.0, 2.2, 82.0, 74.0, 75.0, 17.0, 78.0, 80.0, 50.0, 68.9, DevelopmentLevel.EMERGING),
        BenchmarkSample("REF-05", 42.0, 1.4, 72.0, 66.0, 60.0, 14.0, 68.0, 68.0, 35.0, 56.8, DevelopmentLevel.DEVELOPING),
        BenchmarkSample("REF-06", 35.0, 1.0, 65.0, 60.0, 52.0, 12.0, 60.0, 58.0, 25.0, 48.7, DevelopmentLevel.DEVELOPING),
        BenchmarkSample("REF-07", 20.0, 0.4, 48.0, 46.0, 34.0, 8.0, 42.0, 38.0, 12.0, 32.8, DevelopmentLevel.CRITICAL),
        BenchmarkSample("REF-08", 15.0, 0.2, 42.0, 41.0, 28.0, 6.0, 35.0, 30.0, 8.0, 26.9, DevelopmentLevel.CRITICAL),
        BenchmarkSample("REF-09", 78.0, 3.8, 94.0, 88.0, 90.0, 22.0, 94.0, 96.0, 76.0, 87.4, DevelopmentLevel.MODEL_SMART),
        BenchmarkSample("REF-10", 60.0, 2.4, 84.0, 76.0, 72.0, 18.0, 80.0, 82.0, 55.0, 70.8, DevelopmentLevel.EMERGING),
        BenchmarkSample("REF-11", 48.0, 1.6, 75.0, 68.0, 64.0, 15.0, 70.0, 72.0, 38.0, 59.9, DevelopmentLevel.DEVELOPING),
        BenchmarkSample("REF-12", 26.0, 0.6, 54.0, 52.0, 40.0, 9.0, 48.0, 45.0, 15.0, 38.5, DevelopmentLevel.CRITICAL),
        BenchmarkSample("REF-13", 64.0, 2.6, 86.0, 78.0, 79.0, 18.5, 84.0, 85.0, 58.0, 74.2, DevelopmentLevel.EMERGING),
        BenchmarkSample("REF-14", 39.0, 1.2, 70.0, 63.0, 56.0, 13.0, 62.0, 64.0, 30.0, 52.4, DevelopmentLevel.DEVELOPING),
        BenchmarkSample("REF-15", 85.0, 4.5, 97.0, 92.0, 95.0, 23.5, 97.0, 99.0, 84.0, 92.1, DevelopmentLevel.MODEL_SMART),
        // Holdout test split (5 records — strictly excluded from fitting)
        BenchmarkSample("HOLD-01", 80.0, 3.9, 93.0, 89.0, 91.0, 22.0, 93.0, 95.0, 74.0, 87.1, DevelopmentLevel.MODEL_SMART),
        BenchmarkSample("HOLD-02", 62.0, 2.5, 83.0, 75.0, 76.0, 17.5, 81.0, 83.0, 52.0, 71.4, DevelopmentLevel.EMERGING),
        BenchmarkSample("HOLD-03", 45.0, 1.3, 71.0, 65.0, 58.0, 13.5, 65.0, 66.0, 32.0, 54.8, DevelopmentLevel.DEVELOPING),
        BenchmarkSample("HOLD-04", 22.0, 0.5, 50.0, 49.0, 36.0, 8.5, 44.0, 40.0, 14.0, 34.7, DevelopmentLevel.CRITICAL),
        BenchmarkSample("HOLD-05", 71.0, 3.0, 89.0, 82.0, 84.0, 20.0, 88.0, 90.0, 65.0, 79.6, DevelopmentLevel.EMERGING)
    )

    private val trainSplit = benchmarkDataset.take(15)
    private val testSplit = benchmarkDataset.drop(15)

    // Fitted OLS weights (intercept + 9 normalized features) computed lazily from trainSplit
    private val regressionWeights: DoubleArray by lazy { fitRidgeRegression(trainSplit) }

    // Cached real holdout evaluation report computed from actual testSplit predictions
    val cachedEvaluationReport: MlEvaluationReport by lazy { evaluateOnHoldout() }

    private fun extractFeatureVector(s: BenchmarkSample): DoubleArray = doubleArrayOf(
        s.irrigatedLandPct / 100.0,
        (s.doctorsPer10k / 5.0).coerceIn(0.0, 1.0),
        s.immunizationPct / 100.0,
        s.literacyRatePct / 100.0,
        s.pavedRoadPct / 100.0,
        s.electricityHours / 24.0,
        s.drinkingWaterPct / 100.0,
        s.sanitationPct / 100.0,
        s.internetPct / 100.0
    )

    private fun extractFeatureVector(village: VillageRecord): DoubleArray {
        val pop = village.population.total.coerceAtLeast(1L)
        val doctorsPer10k = (village.healthcare.doctorsCount.toDouble() / pop.toDouble()) * 10_000.0
        return doubleArrayOf(
            (village.agriculture.irrigatedLandPct / 100.0).coerceIn(0.0, 1.0),
            (doctorsPer10k / 5.0).coerceIn(0.0, 1.0),
            (village.healthcare.immunizationPct / 100.0).coerceIn(0.0, 1.0),
            (village.education.literacyRatePct / 100.0).coerceIn(0.0, 1.0),
            (village.infrastructure.pavedRoadPct / 100.0).coerceIn(0.0, 1.0),
            (village.infrastructure.electricityHoursPerDay / 24.0).coerceIn(0.0, 1.0),
            (village.infrastructure.cleanDrinkingWaterPct / 100.0).coerceIn(0.0, 1.0),
            (village.infrastructure.sanitationCoveragePct / 100.0).coerceIn(0.0, 1.0),
            (village.infrastructure.internetCoveragePct / 100.0).coerceIn(0.0, 1.0)
        )
    }

    /**
     * Solves Ridge-regularized Ordinary Least Squares: (X^T X + lambda*I) w = X^T y
     * via Gaussian Elimination with partial pivoting.
     */
    private fun fitRidgeRegression(samples: List<BenchmarkSample>, lambda: Double = 0.05): DoubleArray {
        val n = samples.size
        val numFeatures = 9
        val d = numFeatures + 1 // +1 for bias intercept
        val xtx = Array(d) { DoubleArray(d) }
        val xty = DoubleArray(d)

        for (sample in samples) {
            val feat = extractFeatureVector(sample)
            val row = DoubleArray(d)
            row[0] = 1.0
            for (j in 0 until numFeatures) row[j + 1] = feat[j]
            val y = sample.targetCompositeScore

            for (i in 0 until d) {
                xty[i] += row[i] * y
                for (j in 0 until d) {
                    xtx[i][j] += row[i] * row[j]
                }
            }
        }
        // Ridge penalty on feature weights (not bias)
        for (i in 1 until d) {
            xtx[i][i] += lambda
        }

        // Solve linear system via Gaussian elimination
        val aug = Array(d) { i -> DoubleArray(d + 1) { j -> if (j < d) xtx[i][j] else xty[i] } }
        for (col in 0 until d) {
            var pivot = col
            for (r in col + 1 until d) {
                if (abs(aug[r][col]) > abs(aug[pivot][col])) pivot = r
            }
            val temp = aug[col]
            aug[col] = aug[pivot]
            aug[pivot] = temp

            val diag = aug[col][col].takeIf { abs(it) > 1e-9 } ?: 1e-9
            for (j in col..d) aug[col][j] /= diag
            for (r in 0 until d) {
                if (r != col) {
                    val factor = aug[r][col]
                    for (j in col..d) aug[r][j] -= factor * aug[col][j]
                }
            }
        }
        return DoubleArray(d) { i -> aug[i][d] }
    }

    private fun predictLinearScore(features: DoubleArray): Double {
        var sum = regressionWeights[0]
        for (i in features.indices) {
            sum += regressionWeights[i + 1] * features[i]
        }
        return sum.coerceIn(0.0, 100.0)
    }

    private fun classifyKnn(features: DoubleArray, k: Int = 3): Pair<DevelopmentLevel, List<String>> {
        val scored = trainSplit.map { sample ->
            val sf = extractFeatureVector(sample)
            var distSq = 0.0
            for (i in features.indices) {
                val diff = features[i] - sf[i]
                distSq += diff * diff
            }
            Triple(sample.id, sqrt(distSq), sample.targetTier)
        }.sortedBy { it.second }.take(k)

        val winner = scored.groupingBy { it.third }.eachCount().maxByOrNull { it.value }?.key
            ?: DevelopmentLevel.DEVELOPING
        val neighborLabels = scored.map { "${it.first} (${it.third.label}, d=${((it.second * 100).roundToInt() / 100.0)})" }
        return winner to neighborLabels
    }

    private fun evaluateOnHoldout(): MlEvaluationReport {
        val preds = testSplit.map { s ->
            val f = extractFeatureVector(s)
            predictLinearScore(f) to s.targetCompositeScore
        }
        val n = preds.size.toDouble()
        val mae = preds.sumOf { abs(it.first - it.second) } / n
        val rmse = sqrt(preds.sumOf { (it.first - it.second).pow(2) } / n)
        val meanActual = preds.sumOf { it.second } / n
        val ssRes = preds.sumOf { (it.second - it.first).pow(2) }
        val ssTot = preds.sumOf { (it.second - meanActual).pow(2) }.coerceAtLeast(1e-6)
        val r2 = (1.0 - (ssRes / ssTot)).coerceIn(-1.0, 1.0)

        val knnCorrect = testSplit.count { s ->
            val (predicted, _) = classifyKnn(extractFeatureVector(s))
            predicted == s.targetTier
        }
        val knnAcc = (knnCorrect.toDouble() / testSplit.size.toDouble()) * 100.0

        return MlEvaluationReport(
            trainingSampleCount = trainSplit.size,
            testHoldoutCount = testSplit.size,
            linearRegressionMae = ((mae * 100).roundToInt() / 100.0),
            linearRegressionRmse = ((rmse * 100).roundToInt() / 100.0),
            linearRegressionR2 = ((r2 * 1000).roundToInt() / 1000.0),
            knnAccuracyPct = ((knnAcc * 10).roundToInt() / 10.0),
            knnCorrectCount = knnCorrect,
            datasetNotice = "Metrics computed dynamically on ${testSplit.size} held-out synthetic benchmark samples after fitting Ridge OLS & k-NN (k=3) on ${trainSplit.size} training samples."
        )
    }

    fun computeSectorScores(
        population: PopulationMetrics,
        agriculture: AgricultureMetrics,
        healthcare: HealthcareMetrics,
        education: EducationMetrics,
        infrastructure: InfrastructureMetrics
    ): SectorScores {
        val pop = population.total.coerceAtLeast(1L).toDouble()

        // Agriculture score (0-100)
        val irrigationPart = agriculture.irrigatedLandPct.coerceIn(0.0, 100.0) * 0.65
        val rainBalance = (1.0 - (abs(agriculture.annualRainfallMm - 950.0) / 1400.0)).coerceIn(0.2, 1.0) * 100.0 * 0.20
        val tempBalance = (1.0 - (abs(agriculture.avgTempC - 26.0) / 25.0)).coerceIn(0.2, 1.0) * 100.0 * 0.15
        val agriScore = (irrigationPart + rainBalance + tempBalance).coerceIn(0.0, 100.0)

        // Healthcare score (0-100) based on WHO/IPHS rural norms
        val doctorsPer10k = (healthcare.doctorsCount.toDouble() / pop) * 10_000.0
        val bedsPer10k = (healthcare.hospitalBeds.toDouble() / pop) * 10_000.0
        val doctorScore = ((doctorsPer10k / 4.0) * 100.0).coerceIn(0.0, 100.0)
        val bedScore = ((bedsPer10k / 15.0) * 100.0).coerceIn(0.0, 100.0)
        val distanceScore = (100.0 - (healthcare.avgDistanceToHospitalKm * 3.0)).coerceIn(0.0, 100.0)
        val ambulanceBonus = if (healthcare.ambulanceAvailable) 100.0 else 25.0
        val healthScore = (
            doctorScore * 0.30 +
                bedScore * 0.20 +
                healthcare.immunizationPct.coerceIn(0.0, 100.0) * 0.25 +
                distanceScore * 0.15 +
                ambulanceBonus * 0.10
            ).coerceIn(0.0, 100.0)

        // Education score (0-100)
        val totalSchools = healthcareSafeCount(education.primarySchools + education.secondarySchools)
        val schoolsPer2k = (totalSchools / pop) * 2000.0
        val schoolNorm = ((schoolsPer2k / 2.5) * 100.0).coerceIn(0.0, 100.0)
        val dropoutHealth = (100.0 - (education.dropoutRatePct * 3.5)).coerceIn(0.0, 100.0)
        val digitalScore = ((education.digitalClassrooms.toDouble() / totalSchools) * 50.0).coerceIn(0.0, 100.0)
        val eduScore = (
            education.literacyRatePct.coerceIn(0.0, 100.0) * 0.35 +
                education.femaleLiteracyPct.coerceIn(0.0, 100.0) * 0.25 +
                dropoutHealth * 0.20 +
                schoolNorm * 0.10 +
                digitalScore * 0.10
            ).coerceIn(0.0, 100.0)

        // Infrastructure score (0-100)
        val elecPct = (infrastructure.electricityHoursPerDay / 24.0 * 100.0).coerceIn(0.0, 100.0)
        val infraScore = (
            infrastructure.pavedRoadPct.coerceIn(0.0, 100.0) * 0.22 +
                elecPct * 0.22 +
                infrastructure.cleanDrinkingWaterPct.coerceIn(0.0, 100.0) * 0.24 +
                infrastructure.sanitationCoveragePct.coerceIn(0.0, 100.0) * 0.18 +
                infrastructure.internetCoveragePct.coerceIn(0.0, 100.0) * 0.14
            ).coerceIn(0.0, 100.0)

        val overall = (agriScore * 0.22 + healthScore * 0.26 + eduScore * 0.25 + infraScore * 0.27)
            .coerceIn(0.0, 100.0)

        val sectorPairs = listOf(
            "Agriculture" to agriScore,
            "Healthcare" to healthScore,
            "Education" to eduScore,
            "Infrastructure" to infraScore
        )
        val weakest = sectorPairs.minByOrNull { it.second }?.first ?: "Infrastructure"

        val level = when {
            overall >= 80.0 -> DevelopmentLevel.MODEL_SMART
            overall >= 65.0 -> DevelopmentLevel.EMERGING
            overall >= 45.0 -> DevelopmentLevel.DEVELOPING
            else -> DevelopmentLevel.CRITICAL
        }

        fun round1(v: Double) = (v * 10.0).roundToInt() / 10.0

        return SectorScores(
            overallScore = round1(overall),
            agricultureScore = round1(agriScore),
            healthcareScore = round1(healthScore),
            educationScore = round1(eduScore),
            infrastructureScore = round1(infraScore),
            developmentLevel = level.label,
            weakestSector = weakest
        )
    }

    private fun healthcareSafeCount(count: Long): Double = count.coerceAtLeast(1L).toDouble()

    fun analyzeVillage(village: VillageRecord): VillageMlAnalysisResult {
        val computedScores = computeSectorScores(
            village.population,
            village.agriculture,
            village.healthcare,
            village.education,
            village.infrastructure
        )
        val features = extractFeatureVector(village)
        val predictedScore = ((predictLinearScore(features) * 10.0).roundToInt() / 10.0)
        val (knnTier, neighbors) = classifyKnn(features)

        val weakSectors = buildList {
            if (computedScores.agricultureScore < 65.0) add("Agriculture")
            if (computedScores.healthcareScore < 65.0) add("Healthcare")
            if (computedScores.educationScore < 65.0) add("Education")
            if (computedScores.infrastructureScore < 65.0) add("Infrastructure")
        }.ifEmpty { listOf(computedScores.weakestSector) }

        val pop = village.population.total.coerceAtLeast(1L).toDouble()
        val doctorsPer10k = (village.healthcare.doctorsCount.toDouble() / pop) * 10_000.0

        val healthGaps = buildList {
            if (village.healthcare.primaryHealthCenters == 0L) add("No Primary Health Center (PHC) inside village limits.")
            if (doctorsPer10k < 2.0) add("Doctor ratio (${((doctorsPer10k * 10).roundToInt() / 10.0)} per 10k) is below rural benchmark (2.0+).")
            if (!village.healthcare.ambulanceAvailable) add("Emergency ambulance dispatch unavailable locally.")
            if (village.healthcare.avgDistanceToHospitalKm > 10.0) add("Nearest referral hospital is ${village.healthcare.avgDistanceToHospitalKm} km away.")
            if (village.healthcare.immunizationPct < 85.0) add("Immunization coverage (${village.healthcare.immunizationPct}%) needs outreach drives.")
        }

        val eduGaps = buildList {
            if (village.education.secondarySchools == 0L) add("No secondary school available; students must commute outside.")
            if (village.education.literacyRatePct < 75.0) add("Overall literacy (${village.education.literacyRatePct}%) trails national smart-village target (85%+).")
            if (village.education.femaleLiteracyPct < 70.0) add("Female literacy gap (${village.education.femaleLiteracyPct}%) requires targeted adult & girl-child programs.")
            if (village.education.dropoutRatePct > 8.0) add("School dropout rate (${village.education.dropoutRatePct}%) is elevated.")
            if (village.education.digitalClassrooms == 0L) add("Zero smart/digital classrooms equipped for STEM & ICT learning.")
        }

        val infraGaps = buildList {
            if (village.infrastructure.pavedRoadPct < 75.0) add("All-weather paved road coverage is only ${village.infrastructure.pavedRoadPct}%.")
            if (village.infrastructure.electricityHoursPerDay < 18.0) add("Power supply averages ${village.infrastructure.electricityHoursPerDay} hrs/day (below 20+ hr target).")
            if (village.infrastructure.cleanDrinkingWaterPct < 85.0) add("Piped clean drinking water reaches ${village.infrastructure.cleanDrinkingWaterPct}% of households.")
            if (village.infrastructure.sanitationCoveragePct < 85.0) add("Sanitation & waste treatment coverage (${village.infrastructure.sanitationCoveragePct}%) has gaps.")
            if (village.infrastructure.internetCoveragePct < 55.0) add("Broadband/internet connectivity (${village.infrastructure.internetCoveragePct}%) limits e-governance & telemedicine.")
        }

        val agriGaps = buildList {
            if (village.agriculture.irrigatedLandPct < 55.0) add("Only ${village.agriculture.irrigatedLandPct}% of cultivable land is irrigated; high monsoon dependency.")
            if (village.agriculture.annualRainfallMm < 500.0) add("Low rainfall (${village.agriculture.annualRainfallMm} mm) warrants drought-resilient crops & drip systems.")
            if (village.agriculture.annualRainfallMm > 1800.0) add("High rainfall (${village.agriculture.annualRainfallMm} mm) requires drainage & flood-tolerant varieties.")
        }

        val recommendations = (healthGaps.take(2).map { "Health: Resolve — $it" } +
            eduGaps.take(2).map { "Education: Resolve — $it" } +
            infraGaps.take(2).map { "Infrastructure: Resolve — $it" } +
            agriGaps.take(2).map { "Agriculture: Resolve — $it" }).ifEmpty {
            listOf(
                "Maintain high-performing services with annual solar, water, and digital literacy audits.",
                "Expand precision agriculture sensors and telemedicine specialist clinics."
            )
        }

        return VillageMlAnalysisResult(
            scores = computedScores,
            predictedRegressionScore = predictedScore,
            knnClassifiedTier = knnTier,
            knnNeighborsUsed = neighbors,
            weakSectors = weakSectors,
            healthcareGaps = healthGaps,
            educationGaps = eduGaps,
            infrastructureGaps = infraGaps,
            agricultureGaps = agriGaps,
            prioritizedRecommendations = recommendations,
            evaluationReport = cachedEvaluationReport
        )
    }

    private val cropKnowledgeBase = listOf(
        AgronomicProfile(
            cropName = "Paddy (Rice)",
            preferredSoils = setOf("Alluvial", "Clayey", "Loamy"),
            minRainMm = 900.0,
            maxRainMm = 2500.0,
            minTempC = 22.0,
            maxTempC = 36.0,
            preferredSeasons = setOf("Kharif", "All-Season"),
            npkFertilizer = "NPK 100:50:50 kg/ha + Zinc Sulphate (25 kg/ha)",
            irrigationMethod = "Alternate Wetting & Drying (AWD) / System of Rice Intensification (SRI)",
            waterSavingTip = "AWD reduces water usage by 25–30% without sacrificing grain yield."
        ),
        AgronomicProfile(
            cropName = "Wheat",
            preferredSoils = setOf("Alluvial", "Loamy", "Black Cotton"),
            minRainMm = 350.0,
            maxRainMm = 1000.0,
            minTempC = 12.0,
            maxTempC = 26.0,
            preferredSeasons = setOf("Rabi", "All-Season"),
            npkFertilizer = "NPK 120:60:40 kg/ha (split Nitrogen at crown-root initiation)",
            irrigationMethod = "Sprinkler / Laser-Leveled Check Basin",
            waterSavingTip = "Laser land leveling + sprinkler irrigation saves up to 25% water."
        ),
        AgronomicProfile(
            cropName = "Cotton",
            preferredSoils = setOf("Black Cotton", "Alluvial"),
            minRainMm = 500.0,
            maxRainMm = 1100.0,
            minTempC = 21.0,
            maxTempC = 36.0,
            preferredSeasons = setOf("Kharif"),
            npkFertilizer = "NPK 80:40:40 kg/ha + Boron & Magnesium foliar spray",
            irrigationMethod = "Inline Drip Irrigation with Fertigation",
            waterSavingTip = "Drip fertigation saves 45% irrigation water and improves boll retention."
        ),
        AgronomicProfile(
            cropName = "Pearl Millet (Bajra) & Sorghum (Jowar)",
            preferredSoils = setOf("Sandy", "Red Loamy", "Black Cotton"),
            minRainMm = 250.0,
            maxRainMm = 750.0,
            minTempC = 24.0,
            maxTempC = 40.0,
            preferredSeasons = setOf("Kharif", "Zaid"),
            npkFertilizer = "NPK 60:30:20 kg/ha + Azospirillum bio-fertilizer",
            irrigationMethod = "Rainfed + Supplemental Farm-Pond Micro-Sprinkler",
            waterSavingTip = "Mulching with crop residue preserves topsoil moisture in dry spells."
        ),
        AgronomicProfile(
            cropName = "Chickpea (Gram) & Pulses",
            preferredSoils = setOf("Red Loamy", "Black Cotton", "Alluvial"),
            minRainMm = 300.0,
            maxRainMm = 850.0,
            minTempC = 15.0,
            maxTempC = 30.0,
            preferredSeasons = setOf("Rabi", "Kharif"),
            npkFertilizer = "NPK 20:40:20 kg/ha + Rhizobium seed inoculation (fixes atmospheric N)",
            irrigationMethod = "Micro-Sprinkler at Pre-Flowering & Pod-Fill stages",
            waterSavingTip = "Requires only 1–2 light irrigations; ideal for water-stressed villages."
        ),
        AgronomicProfile(
            cropName = "Soybean",
            preferredSoils = setOf("Black Cotton", "Red Loamy", "Alluvial"),
            minRainMm = 550.0,
            maxRainMm = 1200.0,
            minTempC = 20.0,
            maxTempC = 34.0,
            preferredSeasons = setOf("Kharif"),
            npkFertilizer = "NPK 30:60:30 kg/ha + Sulphur (20 kg/ha)",
            irrigationMethod = "Broad Bed Furrow (BBF) with Rainwater Conservation",
            waterSavingTip = "BBF drains excess monsoon water while storing moisture during dry gaps."
        ),
        AgronomicProfile(
            cropName = "Horticulture (Pomegranate / Banana / Vegetables)",
            preferredSoils = setOf("Red Loamy", "Alluvial", "Laterite", "Black Cotton"),
            minRainMm = 400.0,
            maxRainMm = 1600.0,
            minTempC = 18.0,
            maxTempC = 35.0,
            preferredSeasons = setOf("All-Season", "Zaid", "Kharif", "Rabi"),
            npkFertilizer = "Vermicompost (5 t/ha) + Soluble NPK 19:19:19 via Drip",
            irrigationMethod = "Solar-Powered Precision Drip Irrigation",
            waterSavingTip = "Drip emitters + plastic/organic mulching cut evaporation losses by 60%."
        ),
        AgronomicProfile(
            cropName = "Groundnut & Oilseeds",
            preferredSoils = setOf("Sandy", "Red Loamy", "Laterite"),
            minRainMm = 400.0,
            maxRainMm = 1000.0,
            minTempC = 22.0,
            maxTempC = 35.0,
            preferredSeasons = setOf("Kharif", "Zaid"),
            npkFertilizer = "NPK 25:50:30 kg/ha + Gypsum (250 kg/ha at pegging stage)",
            irrigationMethod = "Portable Mini-Sprinkler System",
            waterSavingTip = "Sprinklers prevent soil crusting for better peg penetration while saving 35% water."
        )
    )

    fun recommendCrops(
        soilType: String,
        rainfallMm: Double,
        tempC: Double,
        season: String
    ): List<CropRecommendation> {
        return cropKnowledgeBase.map { crop ->
            var score = 0.0
            val reasons = mutableListOf<String>()

            if (crop.preferredSoils.any { it.equals(soilType, ignoreCase = true) }) {
                score += 35.0
                reasons += "Well-suited for $soilType soil"
            } else {
                score += 12.0
            }

            if (crop.preferredSeasons.any { it.equals(season, ignoreCase = true) }) {
                score += 25.0
                reasons += "Matches $season season"
            } else {
                score += 8.0
            }

            val rainScore = if (rainfallMm in crop.minRainMm..crop.maxRainMm) {
                reasons += "Optimal rainfall range (${crop.minRainMm.toInt()}–${crop.maxRainMm.toInt()} mm)"
                22.0
            } else {
                val dist = if (rainfallMm < crop.minRainMm) crop.minRainMm - rainfallMm else rainfallMm - crop.maxRainMm
                (22.0 - (dist / 50.0)).coerceAtLeast(3.0)
            }
            score += rainScore

            val tempScore = if (tempC in crop.minTempC..crop.maxTempC) {
                reasons += "Ideal temperature (${crop.minTempC.toInt()}–${crop.maxTempC.toInt()} °C)"
                18.0
            } else {
                val dist = if (tempC < crop.minTempC) crop.minTempC - tempC else tempC - crop.maxTempC
                (18.0 - (dist * 1.5)).coerceAtLeast(2.0)
            }
            score += tempScore

            CropRecommendation(
                cropName = crop.cropName,
                suitabilityScore = score.roundToInt().coerceIn(10, 99),
                npkFertilizer = crop.npkFertilizer,
                irrigationMethod = crop.irrigationMethod,
                waterSavingTip = crop.waterSavingTip,
                reason = if (reasons.isNotEmpty()) reasons.joinToString(" • ") else "Moderate agronomic match; soil amendments advised."
            )
        }.sortedByDescending { it.suitabilityScore }.take(4)
    }
}
