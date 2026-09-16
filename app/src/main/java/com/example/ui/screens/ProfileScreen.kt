package com.example.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.progresso.PainelDoProgresso
import com.example.ui.components.CartaoDaSerie
import com.example.ui.components.CartaoDePontos
import com.example.ui.components.CartaoDoRanking
import com.example.ui.components.TabelaDoQuantoNadou
import com.example.ui.components.CartaoDeConquistas
import com.example.ui.components.CartaoDeEsforco
import com.example.ui.components.CartaoDeTotais
import com.example.ui.components.CartaoDoHistorico
import com.example.ui.components.GraficoDoMes
import com.example.ui.components.MensagemDeTela
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextPrimary

/** Perfil: a conta (no [cabecalho]) e o progresso de verdade, vindo dos treinos salvos. */
@Composable
fun ProfileScreen(
    modifier: Modifier = Modifier,
    progresso: PainelDoProgresso? = null,
    carregando: Boolean = false,
    erro: String? = null,
    onTentarDeNovo: () -> Unit = {},
    onAbrirRanking: () -> Unit = {},
    cabecalho: @Composable () -> Unit = {}
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp)
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        // Minha conta (perfil, PAR-Q, sair, excluir)
        cabecalho()

        Spacer(modifier = Modifier.height(24.dp))

        Text("Meu progresso", fontSize = 22.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)

        Spacer(modifier = Modifier.height(12.dp))

        erro?.let {
            MensagemDeTela(texto = it, erro = true)
            TextButton(onClick = onTentarDeNovo) { Text("Tentar de novo") }
        }

        if (progresso == null) {
            if (carregando) {
                Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = AquaPrimary)
                }
            }
        } else {
            CartaoDeTotais(progresso)
            Spacer(modifier = Modifier.height(12.dp))
            CartaoDePontos(progresso)
            Spacer(modifier = Modifier.height(12.dp))
            CartaoDoRanking(onAbrir = onAbrirRanking)
            Spacer(modifier = Modifier.height(12.dp))
            CartaoDaSerie(progresso.serie)
            Spacer(modifier = Modifier.height(12.dp))
            GraficoDoMes(progresso)
            Spacer(modifier = Modifier.height(12.dp))
            TabelaDoQuantoNadou(progresso)
            Spacer(modifier = Modifier.height(12.dp))
            CartaoDeEsforco(progresso)
            Spacer(modifier = Modifier.height(12.dp))
            CartaoDeConquistas(progresso)
            Spacer(modifier = Modifier.height(12.dp))
            CartaoDoHistorico(progresso.atividades)
        }

        // A barra de baixo já desconta a própria altura (innerPadding do Scaffold).
        Spacer(modifier = Modifier.height(24.dp))
    }
}
