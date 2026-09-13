package com.example.data.supabase

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {

    @GET("rest/v1/workouts")
    suspend fun pingWorkouts(
        @Query("select") select: String = "id",
        @Query("limit") limit: Int = 1
    ): Response<ResponseBody>

    @GET("rest/v1/workouts")
    suspend fun getWorkouts(
        @Query("select") select: String = "*",
        @Query("order") order: String = "workout_date.desc"
    ): Response<List<WorkoutDto>>

    @GET("rest/v1/workouts")
    suspend fun getWorkoutsByLevel(
        @Query("level") levelFilter: String, // e.g. "eq.INTERMEDIARIO"
        @Query("select") select: String = "*"
    ): Response<List<WorkoutDto>>

    // Treino do ciclo do carrossel para a data e o nível (função SQL no Supabase).
    @POST("rest/v1/rpc/treinos_sugeridos")
    suspend fun getTreinosSugeridos(
        @Body params: TreinosSugeridosParams
    ): Response<List<WorkoutDto>>

    @POST("rest/v1/workouts")
    @Headers("Prefer: return=representation")
    suspend fun insertWorkout(
        @Body workout: WorkoutDto
    ): Response<List<WorkoutDto>>

    @GET("rest/v1/swim_set_records")
    suspend fun getSwimSetRecords(
        @Query("select") select: String = "*",
        @Query("order") order: String = "set_number.asc"
    ): Response<List<SwimSetRecordDto>>

    @POST("rest/v1/swim_set_records")
    @Headers("Prefer: return=representation")
    suspend fun insertSwimSetRecord(
        @Body record: SwimSetRecordDto
    ): Response<List<SwimSetRecordDto>>

    @GET("rest/v1/profiles")
    suspend fun getProfiles(
        @Query("select") select: String = "*"
    ): Response<List<ProfileDto>>
}
