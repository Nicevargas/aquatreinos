package com.example.data.lembrete

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.MainActivity
import com.example.R
import com.example.data.ciclo.DataCivil
import java.util.Calendar
import java.util.TimeZone

/**
 * "Quando você vai voltar a nadar?": a pessoa escolhe o dia depois de salvar o
 * treino e o celular avisa às 7h desse dia. Fica só no aparelho, sem banco.
 */
object LembreteDeTreino {

    const val HORA_DO_AVISO = 7
    private const val ARQUIVO = "lembrete_de_treino"
    private const val CHAVE_DIA = "dia"
    private const val CANAL = "lembretes_de_treino"
    private const val ID_NOTIFICACAO = 7001

    private val DIAS = listOf("segunda", "terça", "quarta", "quinta", "sexta", "sábado", "domingo")

    /** Os 7 dias a partir de amanhã. */
    fun proximosDias(hoje: Long): List<Long> = (1L..7L).map { hoje + it }

    /** "na quarta, 16/09" / "no sábado, 19/09" */
    fun rotulo(dia: Long): String {
        val nome = DIAS[DataCivil.diaDaSemana(dia)]
        val artigo = if (nome == "sábado" || nome == "domingo") "no" else "na"
        return "$artigo $nome, ${DataCivil.paraBr(dia).take(5)}"
    }

    /** Instante do aviso: [hora]h do [dia], no fuso do celular. */
    fun horarioDoAviso(dia: Long, hora: Int = HORA_DO_AVISO, fuso: TimeZone = TimeZone.getDefault()): Long {
        val (ano, mes, d) = DataCivil.civil(dia)
        return Calendar.getInstance(fuso).apply {
            clear()
            set(ano, mes - 1, d, hora, 0, 0)
        }.timeInMillis
    }

    fun agendar(context: Context, dia: Long) {
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit().putLong(CHAVE_DIA, dia).apply()
        programarAlarme(context, dia)
    }

    /** Depois de reiniciar o celular o Android apaga os alarmes: o do dia combinado volta. */
    fun reprogramar(context: Context) {
        val dia = context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).getLong(CHAVE_DIA, Long.MIN_VALUE)
        if (dia != Long.MIN_VALUE && horarioDoAviso(dia) > System.currentTimeMillis()) programarAlarme(context, dia)
    }

    private fun programarAlarme(context: Context, dia: Long) {
        val alarmes = context.getSystemService(AlarmManager::class.java) ?: return
        val quando = horarioDoAviso(dia)
        if (quando <= System.currentTimeMillis()) return
        // Inexato de propósito: não precisa de permissão especial e economiza bateria.
        alarmes.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, quando, intencaoDoAlarme(context))
    }

    private fun intencaoDoAlarme(context: Context): PendingIntent = PendingIntent.getBroadcast(
        context,
        0,
        Intent(context, LembreteReceiver::class.java).setAction(LembreteReceiver.ACAO_AVISAR),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )

    fun avisar(context: Context) {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return

        if (Build.VERSION.SDK_INT >= 26) {
            context.getSystemService(NotificationManager::class.java)?.createNotificationChannel(
                NotificationChannel(CANAL, "Lembretes de treino", NotificationManager.IMPORTANCE_DEFAULT)
            )
        }
        val abrir = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notificacao = NotificationCompat.Builder(context, CANAL)
            .setSmallIcon(R.drawable.ic_notificacao)
            .setContentTitle("Hoje é dia de nadar!")
            .setContentText("Você combinou de voltar hoje. O treino do dia já está no app.")
            .setContentIntent(abrir)
            .setAutoCancel(true)
            .build()
        runCatching { NotificationManagerCompat.from(context).notify(ID_NOTIFICACAO, notificacao) }
        context.getSharedPreferences(ARQUIVO, Context.MODE_PRIVATE).edit().remove(CHAVE_DIA).apply()
    }
}

class LembreteReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            ACAO_AVISAR -> LembreteDeTreino.avisar(context)
            Intent.ACTION_BOOT_COMPLETED -> LembreteDeTreino.reprogramar(context)
        }
    }

    companion object {
        const val ACAO_AVISAR = "com.example.LEMBRETE_DE_TREINO"
    }
}
