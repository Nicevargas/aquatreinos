package com.example.data.supabase

import android.util.Log
import com.example.BuildConfig
import com.example.data.auth.AuthApi
import com.example.data.auth.RefreshGrantBody
import com.example.data.auth.SessaoStore
import com.example.data.auth.paraSessao
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import okhttp3.Authenticator
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Qual Authorization vai em cada chamada.
 *
 * Login, cadastro, renovação e recuperação de senha vão com a chave pública; o
 * resto vai com o token de quem está logado, que é o que o RLS enxerga como
 * auth.uid(). Se quem chamou já pôs um Authorization (troca de senha com a
 * sessão de recuperação), ele é mantido.
 */
internal fun escolherAuthorization(
    caminho: String,
    explicito: String?,
    tokenDaSessao: String?,
    chaveAnon: String
): String {
    if (explicito != null) return explicito
    val usaSessao = !caminho.contains("/auth/v1/") || caminho.endsWith("/logout")
    return "Bearer ${tokenDaSessao?.takeIf { usaSessao } ?: chaveAnon}"
}

enum class SupabaseStatus {
    CONNECTED,
    CONFIG_NEEDED,
    CONNECTING,
    OFFLINE_LOCAL
}

object SupabaseClient {
    private const val TAG = "SupabaseClient"

    val supabaseUrl: String = BuildConfig.SUPABASE_URL.trim()
    val supabaseAnonKey: String = BuildConfig.SUPABASE_ANON_KEY.trim()

    val isConfigured: Boolean by lazy {
        supabaseUrl.isNotBlank() &&
                !supabaseUrl.contains("placeholder", ignoreCase = true) &&
                supabaseAnonKey.isNotBlank() &&
                !supabaseAnonKey.contains("placeholder", ignoreCase = true)
    }

    /** Preenchida por AuthRepository.init. Sem sessão, as chamadas vão com a chave pública. */
    @Volatile
    var sessaoStore: SessaoStore? = null

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    private val baseUrl: String
        get() = if (supabaseUrl.endsWith("/")) supabaseUrl else "$supabaseUrl/"

    private val authInterceptor = Interceptor { chain ->
        val original = chain.request()
        val requestBuilder = original.newBuilder()
            .header("apikey", supabaseAnonKey)
            .header(
                "Authorization",
                escolherAuthorization(
                    caminho = original.url.encodedPath,
                    explicito = original.header("Authorization"),
                    tokenDaSessao = sessaoStore?.atual()?.accessToken,
                    chaveAnon = supabaseAnonKey
                )
            )
            .header("Content-Type", "application/json")
            .header("Accept", "application/json")
        chain.proceed(requestBuilder.build())
    }

    private val logging = HttpLoggingInterceptor().apply {
        level = HttpLoggingInterceptor.Level.BASIC
    }

    // Cliente sem renovação automática: usado para a própria renovação.
    private val clienteBase: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .addInterceptor(logging)
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(15, TimeUnit.SECONDS)
            .writeTimeout(15, TimeUnit.SECONDS)
            .build()
    }

    /**
     * O token de acesso vence em ~1h. Num 401, troca pelo refresh token e
     * repete a chamada uma vez. Refresh recusado encerra a sessão (o app volta
     * para o login); falta de rede não encerra, só deixa a chamada falhar.
     */
    private val renovador = Authenticator { _, response ->
        val pedido = response.request
        if (pedido.url.encodedPath.contains("/auth/v1/")) return@Authenticator null
        if (response.priorResponse != null) return@Authenticator null
        val store = sessaoStore ?: return@Authenticator null
        val tokenUsado = pedido.header("Authorization")?.removePrefix("Bearer ")

        val novoToken = synchronized(this) {
            val atual = store.atual() ?: return@Authenticator null
            if (atual.accessToken != tokenUsado) {
                atual.accessToken // outra chamada já renovou enquanto esta esperava
            } else {
                val api = authApi ?: return@Authenticator null
                try {
                    val r = api.refresh(RefreshGrantBody(atual.refreshToken)).execute()
                    val nova = r.body()?.paraSessao(System.currentTimeMillis() / 1000)
                    when {
                        r.isSuccessful && nova != null -> {
                            store.salvar(nova)
                            nova.accessToken
                        }
                        r.code() in 400..499 -> {
                            Log.w(TAG, "Refresh token recusado (HTTP ${r.code()}); encerrando a sessão")
                            store.limpar()
                            null
                        }
                        else -> null
                    }
                } catch (e: IOException) {
                    Log.w(TAG, "Sem rede para renovar a sessão", e)
                    null
                }
            }
        } ?: return@Authenticator null

        pedido.newBuilder().header("Authorization", "Bearer $novoToken").build()
    }

    private val okHttpClient: OkHttpClient by lazy {
        clienteBase.newBuilder().authenticator(renovador).build()
    }

    private fun <T> criar(cliente: OkHttpClient, tipo: Class<T>): T? {
        if (!isConfigured) {
            Log.w(TAG, "Supabase credentials are not configured. Falling back to local offline mode.")
            return null
        }
        return try {
            Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(cliente)
                .addConverterFactory(MoshiConverterFactory.create(moshi))
                .build()
                .create(tipo)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Supabase Retrofit client", e)
            null
        }
    }

    val api: SupabaseApi? by lazy { criar(okHttpClient, SupabaseApi::class.java) }

    val authApi: AuthApi? by lazy { criar(clienteBase, AuthApi::class.java) }
}
