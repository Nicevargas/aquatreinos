package com.example.data.supabase

import okhttp3.ResponseBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Query

interface SupabaseApi {

    @GET("rest/v1/workouts")
    suspend fun pingWorkouts(
        @Query("select") select: String = "id",
        @Query("limit") limit: Int = 1
    ): Response<ResponseBody>

    // Treino do ciclo do carrossel para a data e o nível (função SQL no Supabase).
    @POST("rest/v1/rpc/treinos_sugeridos")
    suspend fun getTreinosSugeridos(
        @Body params: TreinosSugeridosParams
    ): Response<List<WorkoutDto>>

    // ---- Meus treinos. O RLS só deixa ver e mexer nos do usuário logado. ----

    @GET("rest/v1/workouts")
    suspend fun getMyWorkouts(
        @Query("user_id") userFilter: String, // "eq.<uuid>"
        @Query("select") select: String = "*",
        @Query("order") order: String = "workout_date.desc,created_at.desc"
    ): Response<List<WorkoutDto>>

    @POST("rest/v1/workouts")
    @Headers("Prefer: return=representation")
    suspend fun createWorkout(
        @Body workout: WorkoutWriteDto
    ): Response<List<WorkoutDto>>

    @PATCH("rest/v1/workouts")
    @Headers("Prefer: return=representation")
    suspend fun updateWorkout(
        @Query("id") idFilter: String, // "eq.<id>"
        @Body workout: WorkoutWriteDto
    ): Response<List<WorkoutDto>>

    // Com RLS, apagar o que não é seu responde sucesso sem apagar nada;
    // pedindo as linhas de volta, a lista vazia denuncia.
    @DELETE("rest/v1/workouts")
    @Headers("Prefer: return=representation")
    suspend fun deleteWorkout(
        @Query("id") idFilter: String
    ): Response<List<WorkoutDto>>

    // ---- Perfil ----

    @GET("rest/v1/profiles")
    suspend fun getProfile(
        @Query("id") idFilter: String,
        @Query("select") select: String = "*"
    ): Response<List<ProfileDto>>

    @POST("rest/v1/profiles")
    @Headers("Prefer: resolution=merge-duplicates,return=representation")
    suspend fun upsertProfile(
        @Body profile: ProfileWriteDto
    ): Response<List<ProfileDto>>

    @POST("rest/v1/rpc/excluir_minha_conta")
    suspend fun deleteMyAccount(
        @Body vazio: Map<String, String> = emptyMap()
    ): Response<ResponseBody>

    // ---- Treinos realizados ("Concluir treino") ----

    @POST("rest/v1/treinos_realizados")
    @Headers("Prefer: return=representation")
    suspend fun registrarTreino(
        @Body registro: com.example.data.execucao.TreinoRealizadoDto
    ): Response<List<com.example.data.execucao.TreinoRealizadoDto>>

    // ---- Séries cronometradas ----

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
}
