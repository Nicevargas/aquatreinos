package com.example.data.supabase

import android.util.Log
import com.example.data.WorkoutRepository
import com.example.model.CompletedSetRecord
import com.example.model.TrainingLevel
import com.example.model.Workout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object SupabaseRepository {
    private const val TAG = "SupabaseRepository"

    fun getInitialStatus(): SupabaseStatus {
        return if (SupabaseClient.isConfigured) {
            SupabaseStatus.CONNECTING
        } else {
            SupabaseStatus.CONFIG_NEEDED
        }
    }

    suspend fun testConnection(): SupabaseStatus = withContext(Dispatchers.IO) {
        if (!SupabaseClient.isConfigured) {
            return@withContext SupabaseStatus.CONFIG_NEEDED
        }
        val api = SupabaseClient.api ?: return@withContext SupabaseStatus.CONFIG_NEEDED
        try {
            val response = api.pingWorkouts()
            if (response.isSuccessful) {
                Log.i(TAG, "Supabase connection verified successfully! Status code: ${response.code()}")
                SupabaseStatus.CONNECTED
            } else {
                Log.w(TAG, "Supabase returned response code: ${response.code()}")
                SupabaseStatus.OFFLINE_LOCAL
            }
        } catch (e: Exception) {
            Log.e(TAG, "Supabase connection failed, falling back to local mode", e)
            SupabaseStatus.OFFLINE_LOCAL
        }
    }

    suspend fun getWorkouts(level: TrainingLevel): List<Workout> = withContext(Dispatchers.IO) {
        val api = SupabaseClient.api
        if (api == null) {
            return@withContext listOf(WorkoutRepository.getWorkoutForLevel(level))
        }

        try {
            val levelFilter = if (level == TrainingLevel.AVANCADO) "eq.AVANCADO" else "eq.INTERMEDIARIO"
            var response = api.getWorkoutsByLevel(levelFilter = levelFilter)
            if (!response.isSuccessful || response.body().isNullOrEmpty()) {
                response = api.getWorkouts()
            }
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val workouts = response.body()!!.map { it.toDomain() }
                Log.i(TAG, "Loaded ${workouts.size} workouts from Supabase")
                workouts
            } else {
                listOf(WorkoutRepository.getWorkoutForLevel(level))
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error querying Supabase workouts, using local repository", e)
            listOf(WorkoutRepository.getWorkoutForLevel(level))
        }
    }

    suspend fun getSwimSetRecords(): List<CompletedSetRecord> = withContext(Dispatchers.IO) {
        val api = SupabaseClient.api ?: return@withContext emptyList()
        try {
            val response = api.getSwimSetRecords()
            if (response.isSuccessful && !response.body().isNullOrEmpty()) {
                val domainLaps = response.body()!!.map { it.toDomain() }
                Log.i(TAG, "Retrieved ${domainLaps.size} swim set records from Supabase")
                domainLaps
            } else {
                emptyList()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error fetching swim sets from Supabase", e)
            emptyList()
        }
    }

    suspend fun recordSwimSet(record: CompletedSetRecord, workoutId: String?): Boolean = withContext(Dispatchers.IO) {
        val api = SupabaseClient.api ?: return@withContext false
        try {
            val dto = record.toDto(workoutId = workoutId)
            val response = api.insertSwimSetRecord(dto)
            if (response.isSuccessful) {
                Log.i(TAG, "Swim set S${record.setNumber} successfully stored in Supabase!")
                true
            } else {
                Log.w(TAG, "Failed to save swim set in Supabase. HTTP ${response.code()}")
                false
            }
        } catch (e: Exception) {
            Log.e(TAG, "Exception storing swim set in Supabase", e)
            false
        }
    }
}
