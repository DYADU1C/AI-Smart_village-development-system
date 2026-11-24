package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.Logout
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.AccountCircle
import androidx.compose.material.icons.rounded.AdminPanelSettings
import androidx.compose.material.icons.rounded.Analytics
import androidx.compose.material.icons.rounded.Dashboard
import androidx.compose.material.icons.rounded.Eco
import androidx.compose.material.icons.rounded.HealthAndSafety
import androidx.compose.material.icons.rounded.LocationCity
import androidx.compose.material.icons.rounded.Menu
import androidx.compose.material.icons.rounded.PictureAsPdf
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PermanentDrawerSheet
import androidx.compose.material3.PermanentNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.core.configureAppCheck
import com.example.data.ai.VillageAiService
import com.example.data.model.UserProfile
import com.example.data.repository.SmartVillageRepository
import com.example.ui.auth.AuthScreen
import com.example.ui.auth.signOut
import com.example.ui.screens.AgricultureScreen
import com.example.ui.screens.AiAnalysisScreen
import com.example.ui.screens.DashboardScreen
import com.example.ui.screens.ReportsScreen
import com.example.ui.screens.SchemesScreen
import com.example.ui.screens.SectorAnalysisScreen
import com.example.ui.screens.VillagesScreen
import com.example.ui.theme.SmartVillageTheme
import com.example.ui.viewmodel.SmartVillageViewModel
import com.example.ui.viewmodel.WorkspaceMode
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.auth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

enum class AppSection(
    val route: String,
    val title: String,
    val shortLabel: String,
    val icon: ImageVector
) {
    DASHBOARD("dashboard", "Admin Dashboard", "Dashboard", Icons.Rounded.Dashboard),
    VILLAGES("villages", "Village Management", "Villages", Icons.Rounded.LocationCity),
    AI_ANALYSIS("ai_analysis", "AI & ML Analysis", "AI Lab", Icons.Rounded.Analytics),
    AGRICULTURE("agriculture", "Smart Agriculture", "Agri", Icons.Rounded.Eco),
    SECTORS("sectors", "Health, Edu & Infra", "Sectors", Icons.Rounded.HealthAndSafety),
    SCHEMES("schemes", "Govt. Schemes", "Schemes", Icons.Rounded.AccountBalance),
    REPORTS("reports", "AI PDF Reports", "Reports", Icons.Rounded.PictureAsPdf)
}

internal fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { auth ->
        trySend(auth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // STEP 1: Configure App Check with launch intent before any UI, ViewModels, or Firebase calls
        configureAppCheck(this, intent)

        // STEP 2: Enable Edge-to-Edge
        enableEdgeToEdge()

        setContent {
            SmartVillageTheme {
                AppNavigation()
            }
        }
    }
}

@Composable
fun AppNavigation(auth: FirebaseAuth = Firebase.auth) {
    val currentUser by auth.authStateFlow().collectAsStateWithLifecycle(initialValue = auth.currentUser)
    val user = currentUser

    if (user == null) {
        AuthScreen(onAuthSuccess = {})
    } else {
        key(user.uid) {
            SmartVillageRootShell(
                currentUserId = user.uid,
                userDisplayName = user.displayName ?: "Rural Planner",
                userEmail = user.email ?: "",
                isVerifiedAdmin = user.email.equals("yadavdurgesh5492@gmail.com", ignoreCase = true) && user.isEmailVerified
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SmartVillageRootShell(
    currentUserId: String,
    userDisplayName: String,
    userEmail: String,
    isVerifiedAdmin: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    val viewModel: SmartVillageViewModel = viewModel(
        key = currentUserId,
        factory = viewModelFactory {
            initializer {
                val app = checkNotNull(this[APPLICATION_KEY]) {
                    "APPLICATION_KEY missing from CreationExtras"
                }
                val databaseId = app.getString(R.string.firestore_database_id)
                val db = FirebaseFirestore.getInstance(databaseId)
                SmartVillageViewModel(
                    repository = SmartVillageRepository(db),
                    aiService = VillageAiService(),
                    currentUserId = currentUserId,
                    userDisplayName = userDisplayName,
                    userEmail = userEmail,
                    isVerifiedAdminEmail = isVerifiedAdmin
                )
            }
        }
    )

    val userProfile by viewModel.userProfile.collectAsStateWithLifecycle()
    val villagesState by viewModel.villagesState.collectAsStateWithLifecycle()
    val analysesState by viewModel.analysesState.collectAsStateWithLifecycle()
    val schemesState by viewModel.schemesState.collectAsStateWithLifecycle()
    val selectedVillageId by viewModel.selectedVillageId.collectAsStateWithLifecycle()
    val workspaceMode by viewModel.workspaceMode.collectAsStateWithLifecycle()
    val aiReportState by viewModel.aiReportState.collectAsStateWithLifecycle()
    val aiAgronomyState by viewModel.aiAgronomyState.collectAsStateWithLifecycle()
    val snackbarMsg by viewModel.snackbarMessage.collectAsStateWithLifecycle()
    val isBusy by viewModel.isBusy.collectAsStateWithLifecycle()

    var currentSection by remember { mutableStateOf(AppSection.DASHBOARD) }
    var openVillageEditor by remember { mutableStateOf(false) }
    var showProfileDialog by remember { mutableStateOf(false) }

    if (currentSection != AppSection.DASHBOARD) {
        BackHandler { currentSection = AppSection.DASHBOARD }
    }

    val snackbarHostState = remember { SnackbarHostState() }
    LaunchedEffect(snackbarMsg) {
        snackbarMsg?.let { msg ->
            snackbarHostState.showSnackbar(msg)
            viewModel.clearSnackbar()
        }
    }

    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)

    val drawerContent: @Composable () -> Unit = {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Surface(
                color = MaterialTheme.colorScheme.primary,
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Smart Village AI",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onPrimary,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Empowering Villages Through AI",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.85f)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))

            AppSection.entries.forEach { section ->
                NavigationDrawerItem(
                    label = { Text(section.title) },
                    icon = { Icon(section.icon, contentDescription = null) },
                    selected = currentSection == section,
                    onClick = {
                        currentSection = section
                        scope.launch { drawerState.close() }
                    },
                    modifier = Modifier.testTag("drawer_${section.route}")
                )
            }

            Spacer(Modifier.weight(1f))
            HorizontalDivider()
            Spacer(Modifier.height(6.dp))

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            ) {
                Icon(
                    imageVector = if (isVerifiedAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.AccountCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = userProfile?.displayName ?: userDisplayName,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = userProfile?.roleLabel ?: workspaceMode.label,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.secondary
                    )
                }
            }
            OutlinedButton(
                onClick = {
                    scope.launch { drawerState.close() }
                    showProfileDialog = true
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Profile & Role Settings")
            }
        }
    }

    val mainScaffold: @Composable (Boolean) -> Unit = { showHamburger ->
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing,
            snackbarHost = { SnackbarHost(snackbarHostState) },
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = currentSection.title,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "${userProfile?.roleLabel ?: workspaceMode.label} • Smart Village",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    },
                    navigationIcon = {
                        if (showHamburger) {
                            IconButton(
                                onClick = { scope.launch { drawerState.open() } },
                                modifier = Modifier.testTag("menu_drawer_button")
                            ) {
                                Icon(Icons.Rounded.Menu, contentDescription = "Open Navigation Menu")
                            }
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = { showProfileDialog = true },
                            modifier = Modifier.testTag("profile_settings_button")
                        ) {
                            Icon(
                                imageVector = if (isVerifiedAdmin) Icons.Rounded.AdminPanelSettings else Icons.Rounded.AccountCircle,
                                contentDescription = "Profile & Role"
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                )
            },
            bottomBar = {
                if (showHamburger) {
                    val primaryTabs = listOf(
                        AppSection.DASHBOARD,
                        AppSection.VILLAGES,
                        AppSection.AI_ANALYSIS,
                        AppSection.AGRICULTURE,
                        AppSection.REPORTS
                    )
                    NavigationBar {
                        primaryTabs.forEach { tab ->
                            NavigationBarItem(
                                selected = currentSection == tab,
                                onClick = { currentSection = tab },
                                icon = { Icon(tab.icon, contentDescription = tab.shortLabel) },
                                label = { Text(tab.shortLabel) },
                                modifier = Modifier.testTag("nav_${tab.route}")
                            )
                        }
                    }
                }
            }
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(innerPadding)
            ) {
                when (currentSection) {
                    AppSection.DASHBOARD -> DashboardScreen(
                        villagesState = villagesState,
                        isBusy = isBusy,
                        onLoadDemoData = { viewModel.loadDemoVillages() },
                        onAddVillageClick = {
                            openVillageEditor = true
                            currentSection = AppSection.VILLAGES
                        },
                        onOpenVillageAnalysis = { village ->
                            viewModel.selectVillage(village.id)
                            currentSection = AppSection.AI_ANALYSIS
                        },
                        onNavigateVillages = { currentSection = AppSection.VILLAGES }
                    )

                    AppSection.VILLAGES -> VillagesScreen(
                        villagesState = villagesState,
                        isBusy = isBusy,
                        onSaveVillage = { record, done -> viewModel.addOrUpdateVillage(record, done) },
                        onDeleteVillage = { id, name -> viewModel.deleteVillage(id, name) },
                        onLoadDemoVillages = { viewModel.loadDemoVillages() },
                        onAnalyzeVillage = { village ->
                            viewModel.selectVillage(village.id)
                            currentSection = AppSection.AI_ANALYSIS
                        },
                        openEditorTrigger = openVillageEditor,
                        onEditorConsumed = { openVillageEditor = false }
                    )

                    AppSection.AI_ANALYSIS -> AiAnalysisScreen(
                        villagesState = villagesState,
                        selectedVillageId = selectedVillageId,
                        schemesState = schemesState,
                        analysesState = analysesState,
                        mlEvalReport = viewModel.mlEvaluationReport,
                        aiReportState = aiReportState,
                        onSelectVillage = { viewModel.selectVillage(it) },
                        onRunAiAnalysis = { v, ml, schemes ->
                            viewModel.analyzeVillageWithAi(v, ml, schemes)
                        }
                    )

                    AppSection.AGRICULTURE -> AgricultureScreen(
                        villagesState = villagesState,
                        selectedVillageId = selectedVillageId,
                        aiAgronomyState = aiAgronomyState,
                        onSelectVillage = { viewModel.selectVillage(it) },
                        onAskAiAgronomy = { soil, rain, temp, season, irr, crops ->
                            viewModel.generateAgronomyAiAdvice(soil, rain, temp, season, irr, crops)
                        }
                    )

                    AppSection.SECTORS -> SectorAnalysisScreen(
                        villagesState = villagesState,
                        selectedVillageId = selectedVillageId,
                        onSelectVillage = { viewModel.selectVillage(it) }
                    )

                    AppSection.SCHEMES -> SchemesScreen(
                        schemesState = schemesState,
                        villagesState = villagesState,
                        selectedVillageId = selectedVillageId,
                        isBusy = isBusy,
                        onSeedSchemes = { viewModel.loadVerifiableSchemes() },
                        onAddScheme = { scheme, done -> viewModel.addScheme(scheme, done) },
                        onDeleteScheme = { id -> viewModel.deleteScheme(id) }
                    )

                    AppSection.REPORTS -> ReportsScreen(
                        villagesState = villagesState,
                        selectedVillageId = selectedVillageId,
                        schemesState = schemesState,
                        analysesState = analysesState,
                        aiReportState = aiReportState,
                        onSelectVillage = { viewModel.selectVillage(it) },
                        onShowMessage = { viewModel.showMessage(it) }
                    )
                }
            }
        }
    }

    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
        if (maxWidth >= 840.dp) {
            PermanentNavigationDrawer(
                drawerContent = {
                    PermanentDrawerSheet(modifier = Modifier.width(270.dp)) {
                        drawerContent()
                    }
                }
            ) {
                mainScaffold(false)
            }
        } else {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    ModalDrawerSheet(modifier = Modifier.width(285.dp)) {
                        drawerContent()
                    }
                }
            ) {
                mainScaffold(true)
            }
        }
    }

    if (showProfileDialog) {
        ProfileAndRoleDialog(
            profile = userProfile,
            fallbackName = userDisplayName,
            email = userEmail,
            isVerifiedAdmin = isVerifiedAdmin,
            workspaceMode = workspaceMode,
            isBusy = isBusy,
            onDismiss = { showProfileDialog = false },
            onSelectWorkspaceMode = { viewModel.toggleWorkspaceMode(it) },
            onSaveProfile = { name, role, district, dept ->
                viewModel.updateProfile(name, role, district, dept) {
                    showProfileDialog = false
                }
            },
            onSignOut = {
                showProfileDialog = false
                signOut(credentialManager = credentialManager, scope = scope)
            }
        )
    }
}

@Composable
private fun ProfileAndRoleDialog(
    profile: UserProfile?,
    fallbackName: String,
    email: String,
    isVerifiedAdmin: Boolean,
    workspaceMode: WorkspaceMode,
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSelectWorkspaceMode: (WorkspaceMode) -> Unit,
    onSaveProfile: (String, String, String, String) -> Unit,
    onSignOut: () -> Unit
) {
    var displayName by remember(profile) { mutableStateOf(profile?.displayName ?: fallbackName) }
    var district by remember(profile) { mutableStateOf(profile?.district ?: "") }
    var department by remember(profile) { mutableStateOf(profile?.department ?: "Rural Development") }
    val allowedRoles = if (isVerifiedAdmin) {
        listOf("Admin", "Village Officer", "Field Analyst", "Student Researcher")
    } else {
        listOf("Village Officer", "Field Analyst", "Student Researcher")
    }
    var selectedRole by remember(profile) {
        mutableStateOf(
            profile?.roleLabel?.takeIf { it in allowedRoles } ?: allowedRoles.first()
        )
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Account & Role Settings") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = email.ifBlank { "Authenticated Google User" },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.secondary
                )
                OutlinedTextField(
                    value = displayName,
                    onValueChange = { displayName = it },
                    label = { Text("Display Name") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = district,
                    onValueChange = { district = it },
                    label = { Text("Assigned District") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                OutlinedTextField(
                    value = department,
                    onValueChange = { department = it },
                    label = { Text("Department / Institution") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                Text("Profile Role", style = MaterialTheme.typography.labelMedium)
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    allowedRoles.chunked(2).forEach { pair ->
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            pair.forEach { role ->
                                FilterChip(
                                    selected = selectedRole == role,
                                    onClick = { selectedRole = role },
                                    label = { Text(role, style = MaterialTheme.typography.labelSmall) }
                                )
                            }
                        }
                    }
                }

                Text("Dashboard View Mode", style = MaterialTheme.typography.labelMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    WorkspaceMode.entries.forEach { mode ->
                        FilterChip(
                            selected = workspaceMode == mode,
                            onClick = { onSelectWorkspaceMode(mode) },
                            label = { Text(mode.label, style = MaterialTheme.typography.labelSmall) }
                        )
                    }
                }

                HorizontalDivider()
                OutlinedButton(
                    onClick = onSignOut,
                    modifier = Modifier.fillMaxWidth().testTag("sign_out_button")
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Sign Out")
                }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy && displayName.trim().isNotEmpty(),
                onClick = { onSaveProfile(displayName, selectedRole, district, department) }
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
