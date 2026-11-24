package com.example.data.seed

import com.example.data.model.AgricultureMetrics
import com.example.data.model.DataSourceType
import com.example.data.model.EducationMetrics
import com.example.data.model.GovernmentScheme
import com.example.data.model.HealthcareMetrics
import com.example.data.model.InfrastructureMetrics
import com.example.data.model.PopulationMetrics
import com.example.data.model.VillageRecord
import com.example.ml.VillageMlEngine

object SampleDataCatalog {

    /**
     * Clearly labeled DEMO / SAMPLE REFERENCE village profiles so users can explore
     * system capabilities without mistaking sample data for verified field data.
     */
    fun createDemoVillages(): List<VillageRecord> {
        val v1Pop = PopulationMetrics(
            total = 2480L,
            maleCount = 1260L,
            femaleCount = 1220L,
            households = 540L,
            youthRatioPct = 36.2,
            workingAgePct = 62.5
        )
        val v1Agri = AgricultureMetrics(
            soilType = "Black Cotton",
            annualRainfallMm = 580.0,
            avgTempC = 27.5,
            primarySeason = "Kharif",
            irrigatedLandPct = 81.0,
            cultivableHectares = 740.0,
            majorCrops = "Onion, Sorghum, Pomegranate, Pulses"
        )
        val v1Health = HealthcareMetrics(
            primaryHealthCenters = 1L,
            subCenters = 2L,
            doctorsCount = 2L,
            nursesCount = 5L,
            hospitalBeds = 12L,
            ambulanceAvailable = true,
            avgDistanceToHospitalKm = 4.2,
            immunizationPct = 95.5
        )
        val v1Edu = EducationMetrics(
            primarySchools = 2L,
            secondarySchools = 1L,
            literacyRatePct = 88.4,
            femaleLiteracyPct = 83.2,
            teachersCount = 18L,
            digitalClassrooms = 4L,
            dropoutRatePct = 2.4
        )
        val v1Infra = InfrastructureMetrics(
            pavedRoadPct = 92.0,
            electricityHoursPerDay = 22.5,
            cleanDrinkingWaterPct = 96.0,
            sanitationCoveragePct = 98.0,
            internetCoveragePct = 78.0,
            solarStreetLights = 68L
        )
        val v1Scores = VillageMlEngine.computeSectorScores(v1Pop, v1Agri, v1Health, v1Edu, v1Infra)

        val v2Pop = PopulationMetrics(
            total = 3620L,
            maleCount = 1860L,
            femaleCount = 1760L,
            households = 760L,
            youthRatioPct = 39.0,
            workingAgePct = 58.0
        )
        val v2Agri = AgricultureMetrics(
            soilType = "Alluvial",
            annualRainfallMm = 940.0,
            avgTempC = 25.5,
            primarySeason = "Rabi",
            irrigatedLandPct = 58.0,
            cultivableHectares = 920.0,
            majorCrops = "Wheat, Rice, Mustard, Sugarcane"
        )
        val v2Health = HealthcareMetrics(
            primaryHealthCenters = 1L,
            subCenters = 1L,
            doctorsCount = 1L,
            nursesCount = 3L,
            hospitalBeds = 6L,
            ambulanceAvailable = true,
            avgDistanceToHospitalKm = 11.5,
            immunizationPct = 79.0
        )
        val v2Edu = EducationMetrics(
            primarySchools = 3L,
            secondarySchools = 1L,
            literacyRatePct = 71.5,
            femaleLiteracyPct = 62.8,
            teachersCount = 14L,
            digitalClassrooms = 1L,
            dropoutRatePct = 9.2
        )
        val v2Infra = InfrastructureMetrics(
            pavedRoadPct = 68.0,
            electricityHoursPerDay = 15.0,
            cleanDrinkingWaterPct = 72.0,
            sanitationCoveragePct = 74.0,
            internetCoveragePct = 42.0,
            solarStreetLights = 22L
        )
        val v2Scores = VillageMlEngine.computeSectorScores(v2Pop, v2Agri, v2Health, v2Edu, v2Infra)

        val v3Pop = PopulationMetrics(
            total = 1840L,
            maleCount = 945L,
            femaleCount = 895L,
            households = 390L,
            youthRatioPct = 42.1,
            workingAgePct = 54.0
        )
        val v3Agri = AgricultureMetrics(
            soilType = "Red Loamy",
            annualRainfallMm = 410.0,
            avgTempC = 31.0,
            primarySeason = "Kharif",
            irrigatedLandPct = 24.0,
            cultivableHectares = 510.0,
            majorCrops = "Pearl Millet, Groundnut, Pulses"
        )
        val v3Health = HealthcareMetrics(
            primaryHealthCenters = 0L,
            subCenters = 1L,
            doctorsCount = 0L,
            nursesCount = 1L,
            hospitalBeds = 2L,
            ambulanceAvailable = false,
            avgDistanceToHospitalKm = 22.0,
            immunizationPct = 58.0
        )
        val v3Edu = EducationMetrics(
            primarySchools = 1L,
            secondarySchools = 0L,
            literacyRatePct = 54.0,
            femaleLiteracyPct = 44.5,
            teachersCount = 5L,
            digitalClassrooms = 0L,
            dropoutRatePct = 17.5
        )
        val v3Infra = InfrastructureMetrics(
            pavedRoadPct = 36.0,
            electricityHoursPerDay = 8.5,
            cleanDrinkingWaterPct = 42.0,
            sanitationCoveragePct = 48.0,
            internetCoveragePct = 18.0,
            solarStreetLights = 6L
        )
        val v3Scores = VillageMlEngine.computeSectorScores(v3Pop, v3Agri, v3Health, v3Edu, v3Infra)

        return listOf(
            VillageRecord(
                name = "Surajpur Model (Sample)",
                district = "Ahmednagar",
                state = "Maharashtra",
                latitude = 19.0462,
                longitude = 74.5931,
                areaSqKm = 10.4,
                dataSourceType = DataSourceType.DEMO_SAMPLE.code,
                population = v1Pop,
                agriculture = v1Agri,
                healthcare = v1Health,
                education = v1Edu,
                infrastructure = v1Infra,
                scores = v1Scores
            ),
            VillageRecord(
                name = "Rampur Kalan (Sample)",
                district = "Barabanki",
                state = "Uttar Pradesh",
                latitude = 26.9265,
                longitude = 81.1890,
                areaSqKm = 14.2,
                dataSourceType = DataSourceType.DEMO_SAMPLE.code,
                population = v2Pop,
                agriculture = v2Agri,
                healthcare = v2Health,
                education = v2Edu,
                infrastructure = v2Infra,
                scores = v2Scores
            ),
            VillageRecord(
                name = "Devgarh Pahadi (Sample)",
                district = "Dindori",
                state = "Madhya Pradesh",
                latitude = 22.9430,
                longitude = 81.0785,
                areaSqKm = 16.8,
                dataSourceType = DataSourceType.DEMO_SAMPLE.code,
                population = v3Pop,
                agriculture = v3Agri,
                healthcare = v3Health,
                education = v3Edu,
                infrastructure = v3Infra,
                scores = v3Scores
            )
        )
    }

    /**
     * Verifiable official Government of India rural development schemes with real portals.
     */
    fun createVerifiableGovtSchemes(): List<GovernmentScheme> = listOf(
        GovernmentScheme(
            name = "Jal Jeevan Mission (Har Ghar Jal)",
            ministry = "Ministry of Jal Shakti",
            sector = "Infrastructure",
            eligibility = "All rural households lacking Functional Household Tap Connections (FHTC).",
            benefits = "Provides 55 liters per capita per day potable tap water supply and greywater management.",
            officialWebsite = "https://jaljeevanmission.gov.in",
            verifiedOfficial = true
        ),
        GovernmentScheme(
            name = "Pradhan Mantri Gram Sadak Yojana (PMGSY)",
            ministry = "Ministry of Rural Development",
            sector = "Infrastructure",
            eligibility = "Unconnected rural habitations requiring all-weather paved road connectivity.",
            benefits = "100% centrally funded or shared construction and upgradation of rural all-weather roads.",
            officialWebsite = "https://omms.nic.in",
            verifiedOfficial = true
        ),
        GovernmentScheme(
            name = "Pradhan Mantri Krishi Sinchayee Yojana (PMKSY)",
            ministry = "Ministry of Agriculture & Farmers Welfare",
            sector = "Agriculture",
            eligibility = "Small & marginal farmers adopting drip/sprinkler micro-irrigation and watershed works.",
            benefits = "Up to 55% financial assistance for micro-irrigation ('Per Drop More Crop') and farm ponds.",
            officialWebsite = "https://pmksy.gov.in",
            verifiedOfficial = true
        ),
        GovernmentScheme(
            name = "Soil Health Card & PM-KISAN",
            ministry = "Ministry of Agriculture & Farmers Welfare",
            sector = "Agriculture",
            eligibility = "Cultivable landholding farmer families across rural districts.",
            benefits = "Crop-wise soil nutrient status & fertilizer recommendations plus Rs. 6,000/yr direct support.",
            officialWebsite = "https://soilhealth.dac.gov.in",
            verifiedOfficial = true
        ),
        GovernmentScheme(
            name = "Ayushman Bharat Health & Wellness Centres (AAM)",
            ministry = "Ministry of Health & Family Welfare",
            sector = "Healthcare",
            eligibility = "Rural Sub-Health Centres and Primary Health Centres upgrading to comprehensive care.",
            benefits = "Free essential diagnostics, maternal/child care, telemedicine hubs, and emergency referral support.",
            officialWebsite = "https://ab-hwc.nhp.gov.in",
            verifiedOfficial = true
        ),
        GovernmentScheme(
            name = "Samagra Shiksha Abhiyan & PM SHRI",
            ministry = "Ministry of Education",
            sector = "Education",
            eligibility = "Government primary, upper-primary, and secondary village schools.",
            benefits = "Grants for smart digital classrooms, ICT labs, teacher training, and dropout reduction.",
            officialWebsite = "https://samagra.education.gov.in",
            verifiedOfficial = true
        ),
        GovernmentScheme(
            name = "BharatNet Rural Broadband & Saubhagya Solar",
            ministry = "Ministry of Communications & MNRE",
            sector = "Infrastructure",
            eligibility = "Gram Panchayats and off-grid rural households.",
            benefits = "High-speed optical fiber Wi-Fi hotspots at Gram Panchayats and solar micro-grid street lights.",
            officialWebsite = "https://bbnl.nic.in",
            verifiedOfficial = true
        ),
        GovernmentScheme(
            name = "Swachh Bharat Mission (Gramin) Phase II",
            ministry = "Ministry of Jal Shakti (DDWS)",
            sector = "Multi-Sector",
            eligibility = "Rural Gram Panchayats targeting ODF Plus status.",
            benefits = "Support for household toilets, solid & liquid waste management, and community compost pits.",
            officialWebsite = "https://swachhbharatmission.ddws.gov.in",
            verifiedOfficial = true
        )
    )
}
