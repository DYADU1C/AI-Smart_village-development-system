package com.example.data.repository

import android.content.Context
import com.example.R
import com.example.core.OperationType
import com.example.core.handleFirestoreError
import com.example.data.model.DevelopmentAnalysis
import com.example.data.model.GovernmentScheme
import com.example.data.model.UserProfile
import com.example.data.model.VillageRecord
import com.example.data.seed.SampleDataCatalog
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

class SmartVillageRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        )
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("User must be signed in with Google before accessing Firestore.")

    // --- USER PROFILE ---
    fun observeUserProfile(userId: String): Flow<UserProfile?> = flow {
        val path = "users/$userId"
        emitAll(
            db.collection("users").document(userId)
                .snapshots()
                .map { snap ->
                    if (snap.exists()) {
                        snap.toObject(UserProfile::class.java, DocumentSnapshot.ServerTimestampBehavior.ESTIMATE)
                    } else null
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.GET, path)
                    }
                    throw error
                }
        )
    }

    suspend fun ensureUserProfile(
        displayName: String,
        email: String,
        isAdminEmail: Boolean
    ): Result<Unit> {
        val uid = auth.currentUser?.uid ?: return Result.failure(IllegalStateException("Not authenticated"))
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snap = docRef.get().await()
            if (!snap.exists()) {
                val safeName = displayName.trim().ifBlank { "Rural Planner" }.take(80)
                val roleLabel = if (isAdminEmail) "Admin" else "Village Officer"
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to safeName,
                    "email" to email.trim().take(120),
                    "roleLabel" to roleLabel,
                    "district" to "",
                    "department" to "Rural Development",
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    suspend fun updateUserProfile(
        displayName: String,
        roleLabel: String,
        district: String,
        department: String
    ): Result<Unit> {
        var path = "users/unknown"
        return try {
            val uid = requireUserId()
            path = "users/$uid"
            require(displayName.trim().isNotEmpty()) { "Display name is required." }
            db.collection("users").document(uid).update(
                mapOf(
                    "displayName" to displayName.trim().take(80),
                    "roleLabel" to roleLabel,
                    "district" to district.trim().take(80),
                    "department" to department.trim().take(80),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
            ).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    // --- VILLAGES ---
    fun observeVillages(userId: String): Flow<List<VillageRecord>> = flow {
        val path = "users/$userId/villages"
        emitAll(
            db.collection("users").document(userId).collection("villages")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        VillageRecord::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.updatedAt?.seconds ?: it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addVillage(village: VillageRecord): Result<String> {
        var path = "users/unauthenticated/villages"
        return try {
            val uid = requireUserId()
            require(village.name.trim().length >= 2) { "Village name must be at least 2 characters." }
            require(village.district.trim().length >= 2) { "District name must be at least 2 characters." }
            require(village.state.trim().length >= 2) { "State name must be at least 2 characters." }

            val docRef = db.collection("users").document(uid).collection("villages").document()
            path = docRef.path
            docRef.set(village.toCreateMap(uid)).await()
            Result.success(docRef.id)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun updateVillage(village: VillageRecord): Result<Unit> {
        var path = "users/unauthenticated/villages/${village.id}"
        return try {
            val uid = requireUserId()
            require(village.id.isNotBlank()) { "Village ID cannot be blank." }
            require(village.name.trim().length >= 2) { "Village name must be at least 2 characters." }

            val docRef = db.collection("users").document(uid).collection("villages").document(village.id)
            path = docRef.path
            docRef.update(village.toUpdateMap()).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.UPDATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteVillage(villageId: String): Result<Unit> {
        var path = "users/unauthenticated/villages/$villageId"
        return try {
            val uid = requireUserId()
            require(villageId.isNotBlank()) { "Village ID cannot be blank." }
            val docRef = db.collection("users").document(uid).collection("villages").document(villageId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedDemoVillages(): Result<Unit> {
        var path = "users/unauthenticated/villages"
        return try {
            val uid = requireUserId()
            val col = db.collection("users").document(uid).collection("villages")
            path = col.path
            val batch = db.batch()
            SampleDataCatalog.createDemoVillages().forEach { demo ->
                val ref = col.document()
                batch.set(ref, demo.toCreateMap(uid))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }

    // --- ANALYSES ---
    fun observeAnalyses(userId: String): Flow<List<DevelopmentAnalysis>> = flow {
        val path = "users/$userId/analyses"
        emitAll(
            db.collection("users").document(userId).collection("analyses")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        DevelopmentAnalysis::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedByDescending { it.createdAt?.seconds ?: 0L }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun saveAnalysis(analysis: DevelopmentAnalysis): Result<Unit> {
        var path = "users/unauthenticated/analyses"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("analyses").document()
            path = docRef.path
            docRef.set(analysis.toCreateMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    // --- GOVERNMENT SCHEMES ---
    fun observeSchemes(userId: String): Flow<List<GovernmentScheme>> = flow {
        val path = "users/$userId/schemes"
        emitAll(
            db.collection("users").document(userId).collection("schemes")
                .snapshots()
                .map { snapshot ->
                    snapshot.toObjects(
                        GovernmentScheme::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    ).sortedBy { it.sector }
                }
                .catch { error ->
                    val fsErr = generateSequence(error) { it.cause }
                        .filterIsInstance<FirebaseFirestoreException>()
                        .firstOrNull()
                    if (fsErr == null && error is CancellationException) throw error
                    val toLog = fsErr ?: (error as? Exception)
                    if (toLog != null && auth.currentUser?.uid == userId) {
                        handleFirestoreError(toLog, OperationType.LIST, path)
                    }
                    throw error
                }
        )
    }

    suspend fun addScheme(scheme: GovernmentScheme): Result<Unit> {
        var path = "users/unauthenticated/schemes"
        return try {
            val uid = requireUserId()
            require(scheme.name.trim().length >= 2) { "Scheme name required." }
            val docRef = db.collection("users").document(uid).collection("schemes").document()
            path = docRef.path
            docRef.set(scheme.toCreateMap(uid)).await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.CREATE, path)
            Result.failure(e)
        }
    }

    suspend fun deleteScheme(schemeId: String): Result<Unit> {
        var path = "users/unauthenticated/schemes/$schemeId"
        return try {
            val uid = requireUserId()
            val docRef = db.collection("users").document(uid).collection("schemes").document(schemeId)
            path = docRef.path
            docRef.delete().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.DELETE, path)
            Result.failure(e)
        }
    }

    suspend fun seedVerifiableSchemes(): Result<Unit> {
        var path = "users/unauthenticated/schemes"
        return try {
            val uid = requireUserId()
            val col = db.collection("users").document(uid).collection("schemes")
            path = col.path
            val batch = db.batch()
            SampleDataCatalog.createVerifiableGovtSchemes().forEach { scheme ->
                val ref = col.document()
                batch.set(ref, scheme.toCreateMap(uid))
            }
            batch.commit().await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            handleFirestoreError(e, OperationType.WRITE, path)
            Result.failure(e)
        }
    }
}
