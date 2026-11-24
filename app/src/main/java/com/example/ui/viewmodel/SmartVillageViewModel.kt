package com.example.ui.viewmodel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.ai.AiVillageDeepReport
import com.example.data.ai.VillageAiService
import com.example.data.model.AiTaskState
import com.example.data.model.DevelopmentAnalysis
import com.example.data.model.GovernmentScheme
import com.example.data.model.UiState
import com.example.data.model.UserProfile
import com.example.data.model.VillageRecord
import com.example.data.repository.SmartVillageRepository
import com.example.ml.CropRecommendation
import com.example.ml.VillageMlAnalysisResult
import com.example.ml.VillageMlEngine
import com.google.firebase.firestore.FirebaseFirestoreException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

enum class WorkspaceMode(val label: String) {
    ADMIN("Admin Console"),
    PLANNER("Village Planner")
}

class SmartVillageViewModel(
    private val repository: SmartVillageRepository,
    private val aiService: VillageAiService,
    private val currentUserId: String,
    private val userDisplayName: String,
    private val userEmail: String,
    val isVerifiedAdminEmail: Boolean
) : ViewModel() {

    val userProfile: StateFlow<UserProfile?> = repository.observeUserProfile(currentUserId)
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            Log.w(TAG, "Error observing user profile", fsErr ?: error)
            emit(null)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = null
        )

    val villagesState: StateFlow<UiState<List<VillageRecord>>> = repository.observeVillages(currentUserId)
        .map<List<VillageRecord>, UiState<List<VillageRecord>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing villages", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load villages."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val analysesState: StateFlow<UiState<List<DevelopmentAnalysis>>> = repository.observeAnalyses(currentUserId)
        .map<List<DevelopmentAnalysis>, UiState<List<DevelopmentAnalysis>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing analyses", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load analyses."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    val schemesState: StateFlow<UiState<List<GovernmentScheme>>> = repository.observeSchemes(currentUserId)
        .map<List<GovernmentScheme>, UiState<List<GovernmentScheme>>> { UiState.Success(it) }
        .catch { error ->
            val fsErr = generateSequence(error) { it.cause }
                .filterIsInstance<FirebaseFirestoreException>()
                .firstOrNull()
            if (fsErr == null && error is CancellationException) throw error
            val root = fsErr ?: error
            Log.w(TAG, "Error observing schemes", root)
            emit(UiState.Error(root.localizedMessage ?: "Failed to load government schemes."))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = UiState.Loading
        )

    private val _selectedVillageId = MutableStateFlow<String?>(null)
    val selectedVillageId: StateFlow<String?> = _selectedVillageId.asStateFlow()

    private val _workspaceMode = MutableStateFlow(
        if (isVerifiedAdminEmail) WorkspaceMode.ADMIN else WorkspaceMode.PLANNER
    )
    val workspaceMode: StateFlow<WorkspaceMode> = _workspaceMode.asStateFlow()

    private val _aiReportState = MutableStateFlow<AiTaskState<AiVillageDeepReport>>(AiTaskState.Idle)
    val aiReportState: StateFlow<AiTaskState<AiVillageDeepReport>> = _aiReportState.asStateFlow()

    private val _aiAgronomyState = MutableStateFlow<AiTaskState<String>>(AiTaskState.Idle)
    val aiAgronomyState: StateFlow<AiTaskState<String>> = _aiAgronomyState.asStateFlow()

    private val _snackbarMessage = MutableStateFlow<String?>(null)
    val snackbarMessage: StateFlow<String?> = _snackbarMessage.asStateFlow()

    private val _isBusy = MutableStateFlow(false)
    val isBusy: StateFlow<Boolean> = _isBusy.asStateFlow()

    val mlEvaluationReport = VillageMlEngine.cachedEvaluationReport

    init {
        viewModelScope.launch {
            repository.ensureUserProfile(
                displayName = userDisplayName,
                email = userEmail,
                isAdminEmail = isVerifiedAdminEmail
            )
        }
    }

    fun selectVillage(id: String?) {
        _selectedVillageId.value = id
        _aiReportState.value = AiTaskState.Idle
    }

    fun toggleWorkspaceMode(mode: WorkspaceMode) {
        _workspaceMode.value = mode
    }

    fun clearSnackbar() {
        _snackbarMessage.value = null
    }

    fun showMessage(message: String) {
        _snackbarMessage.value = message
    }

    fun addOrUpdateVillage(village: VillageRecord, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val scored = village.copy(
                scores = VillageMlEngine.computeSectorScores(
                    village.population,
                    village.agriculture,
                    village.healthcare,
                    village.education,
                    village.infrastructure
                )
            )
            val result = if (scored.id.isBlank()) {
                repository.addVillage(scored).map { newId ->
                    _selectedVillageId.value = newId
                }
            } else {
                repository.updateVillage(scored)
            }
            _isBusy.value = false
            result.onSuccess {
                _snackbarMessage.value = if (village.id.isBlank()) {
                    "Village '${village.name}' registered & scored."
                } else {
                    "Village '${village.name}' updated."
                }
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to save village."
            }
        }
    }

    fun deleteVillage(villageId: String, villageName: String) {
        viewModelScope.launch {
            _isBusy.value = true
            val result = repository.deleteVillage(villageId)
            _isBusy.value = false
            result.onSuccess {
                if (_selectedVillageId.value == villageId) _selectedVillageId.value = null
                _snackbarMessage.value = "Deleted '$villageName'."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to delete village."
            }
        }
    }

    fun loadDemoVillages() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.seedDemoVillages()
            _isBusy.value = false
            res.onSuccess {
                _snackbarMessage.value = "Loaded 3 Demo Reference Villages (labeled DEMO_SAMPLE)."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not load demo villages."
            }
        }
    }

    fun loadVerifiableSchemes() {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.seedVerifiableSchemes()
            _isBusy.value = false
            res.onSuccess {
                _snackbarMessage.value = "Loaded 8 verified official Government Schemes."
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Could not load schemes."
            }
        }
    }

    fun addScheme(scheme: GovernmentScheme, onSuccess: () -> Unit = {}) {
        viewModelScope.launch {
            _isBusy.value = true
            val res = repository.addScheme(scheme)
            _isBusy.value = false
            res.onSuccess {
                _snackbarMessage.value = "Scheme '${scheme.name}' saved."
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to save scheme."
            }
        }
    }

    fun deleteScheme(schemeId: String) {
        viewModelScope.launch {
            val res = repository.deleteScheme(schemeId)
            res.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to delete scheme."
            }
        }
    }

    fun updateProfile(
        displayName: String,
        roleLabel: String,
        district: String,
        department: String,
        onSuccess: () -> Unit = {}
    ) {
        viewModelScope.launch {
            _isBusy.value = true
            val safeRole = if (roleLabel == "Admin" && !isVerifiedAdminEmail) "Village Officer" else roleLabel
            val res = repository.updateUserProfile(displayName, safeRole, district, department)
            _isBusy.value = false
            res.onSuccess {
                _snackbarMessage.value = "Profile updated."
                onSuccess()
            }.onFailure { err ->
                _snackbarMessage.value = err.localizedMessage ?: "Failed to update profile."
            }
        }
    }

    fun analyzeVillageWithAi(
        village: VillageRecord,
        mlResult: VillageMlAnalysisResult,
        matchedSchemes: List<GovernmentScheme>,
        saveToHistory: Boolean = true
    ) {
        viewModelScope.launch {
            _aiReportState.value = AiTaskState.Loading
            try {
                val report = aiService.generateVillageDeepReport(village, mlResult, matchedSchemes)
                _aiReportState.value = AiTaskState.Success(report)

                if (saveToHistory) {
                    val mlSummaryText = buildString {
                        append("Ridge Regression: ${mlResult.predictedRegressionScore}/100 | ")
                        append("k-NN (k=3): ${mlResult.knnClassifiedTier.label} | ")
                        append("Holdout MAE=${mlResult.evaluationReport.linearRegressionMae}, ")
                        append("R²=${mlResult.evaluationReport.linearRegressionR2}, ")
                        append("k-NN Acc=${mlResult.evaluationReport.knnAccuracyPct}%")
                    }
                    repository.saveAnalysis(
                        DevelopmentAnalysis(
                            villageId = village.id,
                            villageName = village.name,
                            dataSourceType = village.dataSourceType,
                            overallScore = mlResult.scores.overallScore,
                            developmentLevel = mlResult.scores.developmentLevel,
                            weakSectors = mlResult.weakSectors,
                            mlModelSummary = mlSummaryText,
                            aiExecutiveSummary = "${report.executiveSummary}\n\nProjected Impact: ${report.impactProjection}",
                            recommendations = mlResult.prioritizedRecommendations,
                            priorityActions = report.priorityActions
                        )
                    )
                }
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI village analysis timed out", e)
                _aiReportState.value = AiTaskState.Error("AI analysis timed out after 60 seconds. Please retry.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI village analysis failed", e)
                _aiReportState.value = AiTaskState.Error(e.localizedMessage ?: "AI analysis failed.")
            }
        }
    }

    fun generateAgronomyAiAdvice(
        soilType: String,
        rainfallMm: Double,
        tempC: Double,
        season: String,
        irrigatedPct: Double,
        crops: List<CropRecommendation>
    ) {
        viewModelScope.launch {
            _aiAgronomyState.value = AiTaskState.Loading
            try {
                val advice = aiService.generateAgronomyAdvice(
                    soilType = soilType,
                    rainfallMm = rainfallMm,
                    tempC = tempC,
                    season = season,
                    irrigatedPct = irrigatedPct,
                    topCrops = crops.map { it.cropName }
                )
                _aiAgronomyState.value = AiTaskState.Success(advice)
            } catch (e: TimeoutCancellationException) {
                Log.e(TAG, "AI agronomy request timed out", e)
                _aiAgronomyState.value = AiTaskState.Error("Agronomy advisory timed out after 60 seconds.")
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.e(TAG, "AI agronomy request failed", e)
                _aiAgronomyState.value = AiTaskState.Error(e.localizedMessage ?: "Could not generate AI agronomy advice.")
            }
        }
    }

    private companion object {
        const val TAG = "SmartVillageVM"
    }
}
