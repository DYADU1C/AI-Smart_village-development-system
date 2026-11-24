package com.example.ui.screens

import android.content.Intent
import android.net.Uri
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.OpenInNew
import androidx.compose.material.icons.rounded.AccountBalance
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Verified
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.data.model.GovernmentScheme
import com.example.data.model.UiState
import com.example.data.model.VillageRecord
import com.example.ml.VillageMlEngine

@Composable
fun SchemesScreen(
    schemesState: UiState<List<GovernmentScheme>>,
    villagesState: UiState<List<VillageRecord>>,
    selectedVillageId: String?,
    isBusy: Boolean,
    onSeedSchemes: () -> Unit,
    onAddScheme: (GovernmentScheme, () -> Unit) -> Unit,
    onDeleteScheme: (String) -> Unit
) {
    val context = LocalContext.current
    val villages = (villagesState as? UiState.Success)?.data.orEmpty()
    val activeVillage = villages.firstOrNull { it.id == selectedVillageId } ?: villages.firstOrNull()
    val weakSectors = activeVillage?.let { VillageMlEngine.analyzeVillage(it).weakSectors }.orEmpty()

    var sectorFilter by remember { mutableStateOf("All") }
    var showAddDialog by remember { mutableStateOf(false) }

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.testTag("add_scheme_fab")
            ) {
                Icon(Icons.Rounded.Add, contentDescription = "Add Government Scheme")
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Row(
                modifier = Modifier
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("All", "Recommended", "Agriculture", "Healthcare", "Education", "Infrastructure", "Multi-Sector").forEach { f ->
                    FilterChip(
                        selected = sectorFilter == f,
                        onClick = { sectorFilter = f },
                        label = { Text(f) }
                    )
                }
            }

            when (schemesState) {
                is UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
                is UiState.Error -> Box(Modifier.fillMaxSize().padding(16.dp), contentAlignment = Alignment.Center) {
                    Text(schemesState.message, color = MaterialTheme.colorScheme.error)
                }
                is UiState.Success -> {
                    val list = schemesState.data
                    if (list.isEmpty()) {
                        Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(Icons.Rounded.AccountBalance, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(44.dp))
                                Text("Government Schemes Catalog Empty", style = MaterialTheme.typography.titleMedium)
                                Text(
                                    "Load verified Government of India rural development schemes (PMGSY, Jal Jeevan Mission, PMKSY, Samagra Shiksha, Ayushman Bharat) or add custom schemes.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Button(
                                    onClick = onSeedSchemes,
                                    enabled = !isBusy,
                                    modifier = Modifier.testTag("load_govt_schemes_btn")
                                ) {
                                    Text("Load Verified Govt Schemes")
                                }
                            }
                        }
                    } else {
                        val filtered = list.filter { s ->
                            when (sectorFilter) {
                                "All" -> true
                                "Recommended" -> s.sector in weakSectors || s.sector == "Multi-Sector"
                                else -> s.sector == sectorFilter
                            }
                        }

                        LazyColumn(
                            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 90.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            if (activeVillage != null) {
                                item {
                                    Surface(
                                        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                                        shape = RoundedCornerShape(14.dp),
                                        modifier = Modifier.fillMaxWidth()
                                    ) {
                                        Text(
                                            text = "Matching schemes for ${activeVillage.name} weak sectors: ${weakSectors.joinToString(", ")}",
                                            style = MaterialTheme.typography.bodySmall,
                                            modifier = Modifier.padding(12.dp)
                                        )
                                    }
                                }
                            }

                            items(filtered, key = { it.id }) { scheme ->
                                val isMatched = scheme.sector in weakSectors || scheme.sector == "Multi-Sector"
                                Card(
                                    shape = RoundedCornerShape(18.dp),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(scheme.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                                Text(scheme.ministry, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                            }
                                            Surface(
                                                color = if (isMatched) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.secondaryContainer,
                                                shape = RoundedCornerShape(50)
                                            ) {
                                                Text(
                                                    text = if (isMatched && activeVillage != null) "${scheme.sector} • Match" else scheme.sector,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                                )
                                            }
                                        }
                                        Text("Eligibility: ${scheme.eligibility}", style = MaterialTheme.typography.bodySmall)
                                        Text("Benefits: ${scheme.benefits}", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.primary)
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            OutlinedButton(
                                                onClick = {
                                                    val uri = Uri.parse(scheme.officialWebsite)
                                                    context.startActivity(Intent(Intent.ACTION_VIEW, uri))
                                                }
                                            ) {
                                                if (scheme.verifiedOfficial) {
                                                    Icon(Icons.Rounded.Verified, contentDescription = null, modifier = Modifier.size(16.dp))
                                                    Spacer(Modifier.width(4.dp))
                                                }
                                                Text("Official Portal")
                                                Spacer(Modifier.width(4.dp))
                                                Icon(Icons.AutoMirrored.Rounded.OpenInNew, contentDescription = null, modifier = Modifier.size(14.dp))
                                            }
                                            IconButton(onClick = { onDeleteScheme(scheme.id) }) {
                                                Icon(Icons.Rounded.Delete, contentDescription = "Delete Scheme", tint = MaterialTheme.colorScheme.error)
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showAddDialog) {
        AddSchemeDialog(
            isBusy = isBusy,
            onDismiss = { showAddDialog = false },
            onSave = { scheme -> onAddScheme(scheme) { showAddDialog = false } }
        )
    }
}

@Composable
private fun AddSchemeDialog(
    isBusy: Boolean,
    onDismiss: () -> Unit,
    onSave: (GovernmentScheme) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var ministry by remember { mutableStateOf("") }
    var sector by remember { mutableStateOf("Infrastructure") }
    var eligibility by remember { mutableStateOf("") }
    var benefits by remember { mutableStateOf("") }
    var website by remember { mutableStateOf("https://") }
    var err by remember { mutableStateOf<String?>(null) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Add Government Scheme") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Scheme Name*") }, singleLine = true)
                OutlinedTextField(value = ministry, onValueChange = { ministry = it }, label = { Text("Ministry / Dept*") }, singleLine = true)
                Row(modifier = Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    listOf("Agriculture", "Healthcare", "Education", "Infrastructure", "Multi-Sector").forEach { sec ->
                        FilterChip(selected = sector == sec, onClick = { sector = sec }, label = { Text(sec) })
                    }
                }
                OutlinedTextField(value = eligibility, onValueChange = { eligibility = it }, label = { Text("Eligibility*") })
                OutlinedTextField(value = benefits, onValueChange = { benefits = it }, label = { Text("Benefits*") })
                OutlinedTextField(value = website, onValueChange = { website = it }, label = { Text("Official URL*") }, singleLine = true)
                err?.let { Text(it, color = MaterialTheme.colorScheme.error, style = MaterialTheme.typography.bodySmall) }
            }
        },
        confirmButton = {
            Button(
                enabled = !isBusy,
                onClick = {
                    if (name.trim().length < 2 || ministry.trim().length < 2 || eligibility.trim().length < 2 || benefits.trim().length < 2 || website.trim().length < 5) {
                        err = "Please complete all required fields."
                        return@Button
                    }
                    onSave(
                        GovernmentScheme(
                            name = name.trim(),
                            ministry = ministry.trim(),
                            sector = sector,
                            eligibility = eligibility.trim(),
                            benefits = benefits.trim(),
                            officialWebsite = website.trim(),
                            verifiedOfficial = website.contains(".gov.") || website.contains(".nic.")
                        )
                    )
                }
            ) { Text("Save") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
