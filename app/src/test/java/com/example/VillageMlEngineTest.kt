package com.example

import com.example.data.seed.SampleDataCatalog
import com.example.ml.VillageMlEngine
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class VillageMlEngineTest {

    @Test
    fun holdoutEvaluationComputesFiniteRealMetrics() {
        val eval = VillageMlEngine.cachedEvaluationReport
        assertEquals(15, eval.trainingSampleCount)
        assertEquals(5, eval.testHoldoutCount)
        assertTrue(eval.linearRegressionMae in 0.0..15.0)
        assertTrue(eval.linearRegressionRmse in 0.0..15.0)
        assertTrue(eval.linearRegressionR2 in 0.5..1.0)
        assertTrue(eval.knnAccuracyPct in 0.0..100.0)
    }

    @Test
    fun villageAnalysisClassifiesModelAndPriorityVillagesCorrectly() {
        val demos = SampleDataCatalog.createDemoVillages()
        val modelVillage = VillageMlEngine.analyzeVillage(demos[0])
        val priorityVillage = VillageMlEngine.analyzeVillage(demos[2])

        assertTrue(modelVillage.scores.overallScore > 75.0)
        assertTrue(priorityVillage.scores.overallScore < 55.0)
        assertTrue(priorityVillage.weakSectors.isNotEmpty())
        assertTrue(priorityVillage.prioritizedRecommendations.isNotEmpty())
    }

    @Test
    fun cropRecommenderPrioritizesSuitableCrops() {
        val kharifBlack = VillageMlEngine.recommendCrops(
            soilType = "Black Cotton",
            rainfallMm = 650.0,
            tempC = 28.0,
            season = "Kharif"
        )
        assertTrue(kharifBlack.isNotEmpty())
        assertTrue(kharifBlack.first().suitabilityScore >= 70)
    }
}
