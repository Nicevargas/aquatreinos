package com.example.ui.screens

import androidx.compose.foundation.background
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.offset
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ciclo.DataCivil
import com.example.model.Workout
import com.example.ui.components.MensagemDeTela
import com.example.ui.components.corDoNivel
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.viewmodel.MeusTreinosUiState

@Composable
fun MeusTreinosScreen(
    estado: MeusTreinosUiState,
    onNovo: () -> Unit,
    onEditar: (Workout) -> Unit,
    onUsar: (Workout) -> Unit,
    onExcluir: (Workout) -> Unit,
    onConfirmarExclusao: () -> Unit,
    onCancelarExclusao: () -> Unit,
    onTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier,
    onAbrirCodigo: ((String) -> Unit)? = null,
    abrindoCodigo: Boolean = false
) {
    var pedirCodigo by rememberSaveable { mutableStateOf(false) }
    var codigoDigitado by rememberSaveable { mutableStateOf("") }

    if (pedirCodigo && onAbrirCodigo != null) {
        AlertDialog(
            onDismissRequest = { pedirCodigo = false },
            title = { Text("Abrir treino recebido", fontWeight = FontWeight.Bold) },
            text = {
                Column {
                    Text("Cole o link, a mensagem que você recebeu ou só o código do treino.", color = AquaTextSecondary)
                    OutlinedTextField(
                        value = codigoDigitado,
                        onValueChange = { codigoDigitado = it },
                        label = { Text("Link ou código") },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                            .testTag("campo_codigo_treino")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        onAbrirCodigo(codigoDigitado)
                        pedirCodigo = false
                        codigoDigitado = ""
                    },
                    enabled = codigoDigitado.isNotBlank(),
                    modifier = Modifier.testTag("abrir_codigo_treino")
                ) { Text("Abrir", fontWeight = FontWeight.Bold) }
            },
            dismissButton = { TextButton(onClick = { pedirCodigo = false }) { Text("Cancelar") } }
        )
    }

    estado.paraExcluir?.let { alvo ->
        AlertDialog(
            onDismissRequest = onCancelarExclusao,
            title = { Text("Excluir treino?", fontWeight = FontWeight.Bold) },
            text = {
                Text("\"${alvo.title}\"${dataCurta(alvo)?.let { " de $it" }.orEmpty()} será excluído. Não dá para desfazer.")
            },
            confirmButton = {
                TextButton(
                    onClick = onConfirmarExclusao,
                    enabled = !estado.excluindo,
                    modifier = Modifier.testTag("confirmar_exclusao")
                ) {
                    Text(if (estado.excluindo) "Excluindo…" else "Excluir", color = AquaMagenta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = onCancelarExclusao, enabled = !estado.excluindo) {
                    Text("Cancelar")
                }
            }
        )
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        // Embaixo, só um respiro: a barra de navegação já desconta a própria altura.
        contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Meus treinos", fontSize = 24.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)
                    Text(
                        text = when {
                            estado.carregando -> "Carregando…"
                            estado.treinos.size == 1 -> "1 treino salvo"
                            else -> "${estado.treinos.size} treinos salvos"
                        },
                        fontSize = 13.sp,
                        color = AquaTextSecondary
                    )
                }
                Button(
                    onClick = onNovo,
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary),
                    modifier = Modifier.testTag("botao_novo_treino")
                ) {
                    // "+ Novo" curto: com "Novo treino", o título "Meus treinos" quebrava em duas linhas.
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Novo", fontWeight = FontWeight.Bold, softWrap = false)
                }
            }
            if (onAbrirCodigo != null) {
                TextButton(
                    onClick = { pedirCodigo = true },
                    enabled = !abrindoCodigo,
                    modifier = Modifier
                        .offset(x = (-12).dp)
                        .testTag("botao_treino_recebido")
                ) {
                    Text(if (abrindoCodigo) "Abrindo treino…" else "Recebeu um treino? Abrir pelo link ou código")
                }
            }
        }

        estado.erro?.let { erro ->
            item {
                Column {
                    MensagemDeTela(texto = erro, erro = true)
                    TextButton(onClick = onTentarDeNovo) { Text("Tentar de novo") }
                }
            }
        }

        if (estado.carregando && estado.treinos.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AquaPrimary)
                }
            }
        } else if (estado.treinos.isEmpty() && estado.erro == null) {
            item {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(20.dp),
                    color = Color.White,
                    border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("Nenhum treino salvo ainda", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AquaTextPrimary)
                        Text(
                            text = "Crie um treino seu em \"Novo treino\", ou abra a aba Treinos e salve a sugestão do dia.",
                            fontSize = 13.sp,
                            color = AquaTextSecondary,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                    }
                }
            }
        }

        items(estado.treinos, key = { it.id }) { treino ->
            CartaoMeuTreino(treino = treino, onUsar = onUsar, onEditar = onEditar, onExcluir = onExcluir)
        }
    }
}

private fun dataCurta(treino: Workout): String? =
    treino.workoutDate?.let { runCatching { DataCivil.paraBr(DataCivil.deIso(it)) }.getOrNull() }

@Composable
private fun CartaoMeuTreino(
    treino: Workout,
    onUsar: (Workout) -> Unit,
    onEditar: (Workout) -> Unit,
    onExcluir: (Workout) -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("meu_treino_${treino.id}"),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder),
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(corDoNivel(treino.level))
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = listOfNotNull(dataCurta(treino), treino.level.label).joinToString(" · "),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AquaTextSecondary
                )
            }
            Text(
                text = treino.title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextPrimary,
                modifier = Modifier.padding(top = 4.dp)
            )
            Text(
                text = "${treino.totalDistanceMeters}m · ~${treino.estimatedMinutes} min",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = AquaPrimary,
                modifier = Modifier.padding(top = 2.dp)
            )
            treino.phases.forEach { fase ->
                Text(
                    text = "${fase.title}: ${fase.summary}",
                    fontSize = 12.sp,
                    color = AquaTextSecondary,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 8.dp),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = { onExcluir(treino) }, modifier = Modifier.testTag("excluir_${treino.id}")) {
                    Text("Excluir", color = AquaMagenta)
                }
                TextButton(onClick = { onEditar(treino) }, modifier = Modifier.testTag("editar_${treino.id}")) {
                    Text("Editar")
                }
                Button(
                    onClick = { onUsar(treino) },
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
                ) {
                    Text("Usar hoje", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}
