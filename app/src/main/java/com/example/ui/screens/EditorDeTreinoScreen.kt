package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.ui.draw.clip
import com.example.data.treinos.MetodoNC
import com.example.ui.components.corDaZona
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.treinos.MontadorDeTreino
import com.example.data.treinos.SerieDigitada
import com.example.ui.components.MensagemDeTela
import com.example.ui.components.SeletorDeNivel
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.viewmodel.EditorDeTreino

private fun metros(serie: SerieDigitada): Int =
    MontadorDeTreino.lerCabecalho(serie.serie)?.let { it.repeticoes * it.metros } ?: 0

/** Criar ou editar um treino, série por série, no formato do carrossel. */
@Composable
fun EditorDeTreinoScreen(
    editor: EditorDeTreino,
    onAlterar: (com.example.data.treinos.TreinoDigitado) -> Unit,
    onSalvar: () -> Unit,
    onCancelar: () -> Unit,
    modifier: Modifier = Modifier,
    onUsarParaNadar: () -> Unit = {}
) {
    BackHandler(onBack = onCancelar)
    val digitado = editor.digitado
    val total = digitado.fases.values.flatten().sumOf { metros(it) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AquaBackground)
            .systemBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onCancelar, modifier = Modifier.testTag("editor_cancelar")) {
                Icon(Icons.Filled.Close, contentDescription = "Cancelar")
            }
            Text(
                text = when {
                    editor.paraNadar -> "Ajustar treino"
                    editor.id == null -> "Novo treino"
                    else -> "Editar treino"
                },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextPrimary,
                modifier = Modifier.weight(1f)
            )
            TextButton(onClick = if (editor.paraNadar) onUsarParaNadar else onSalvar, enabled = !editor.salvando) {
                Text(if (editor.paraNadar) "Usar" else "Salvar", fontWeight = FontWeight.Bold)
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (editor.erros.isNotEmpty()) {
                MensagemDeTela(texto = editor.erros.joinToString("\n"), erro = true)
            }

            if (editor.paraNadar) {
                Spacer(modifier = Modifier.height(12.dp))
                Surface(modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp), color = AquaBlueBg) {
                    Text(
                        text = "Troque, tire ou acrescente o que quiser. O treino ajustado vai para Meus treinos quando você concluir.",
                        fontSize = 13.sp,
                        color = AquaTextPrimary,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = digitado.titulo,
                onValueChange = { onAlterar(digitado.copy(titulo = it)) },
                label = { Text("Nome do treino") },
                placeholder = { Text("Ex.: Tiros de sábado") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_titulo")
            )

            Spacer(modifier = Modifier.height(12.dp))

            OutlinedTextField(
                value = digitado.data,
                onValueChange = { onAlterar(digitado.copy(data = it)) },
                label = { Text("Data") },
                placeholder = { Text("dd/mm/aaaa") },
                singleLine = true,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("editor_data")
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text("Nível", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AquaTextPrimary)
            Spacer(modifier = Modifier.height(6.dp))
            SeletorDeNivel(
                selecionado = digitado.level,
                onSelecionar = { onAlterar(digitado.copy(level = it)) },
                habilitado = !editor.salvando
            )

            Spacer(modifier = Modifier.height(16.dp))

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                color = AquaBlueBg
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Total: ${total}m", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = AquaPrimary)
                    Text(
                        text = "Escreva cada série com a distância primeiro: \"8x50m Crawl\" ou \"400m Crawl\". Os detalhes explicam a série e não somam metros. Intervalo: #20\" descansa 20 s; @1'30\" sai a cada 1'30\". A zona (A0 a AA) diz a intensidade.",
                        fontSize = 12.sp,
                        color = AquaTextSecondary,
                        modifier = Modifier.padding(top = 2.dp)
                    )
                }
            }

            MontadorDeTreino.FASES.forEach { fase ->
                val series = digitado.fases[fase].orEmpty()
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = "$fase · ${series.sumOf { metros(it) }}m",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = AquaTextPrimary
                )
                series.forEachIndexed { indice, serie ->
                    CartaoDeSerie(
                        fase = fase,
                        indice = indice,
                        serie = serie,
                        onAlterar = { nova ->
                            val lista = series.toMutableList().also { it[indice] = nova }
                            onAlterar(digitado.copy(fases = digitado.fases + (fase to lista)))
                        },
                        onRemover = {
                            val lista = series.filterIndexed { i, _ -> i != indice }
                            onAlterar(digitado.copy(fases = digitado.fases + (fase to lista)))
                        }
                    )
                }
                TextButton(
                    onClick = { onAlterar(digitado.copy(fases = digitado.fases + (fase to series + SerieDigitada()))) },
                    modifier = Modifier.testTag("adicionar_serie_${fase.lowercase()}")
                ) {
                    Icon(Icons.Filled.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Adicionar série")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (editor.paraNadar) {
                Button(
                    onClick = onUsarParaNadar,
                    enabled = !editor.salvando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("editor_usar_para_nadar"),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
                ) {
                    Text("Usar este treino", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(10.dp))
                OutlinedButton(
                    onClick = onSalvar,
                    enabled = !editor.salvando,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("editor_salvar"),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    if (editor.salvando) {
                        CircularProgressIndicator(color = AquaPrimary, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                    } else {
                        Text("Salvar em Meus treinos agora", fontSize = 15.sp, fontWeight = FontWeight.Bold, color = AquaPrimary)
                    }
                }
            } else Button(
                onClick = onSalvar,
                enabled = !editor.salvando,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("editor_salvar"),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
            ) {
                if (editor.salvando) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
                } else {
                    Text(
                        text = if (editor.id == null) "Salvar treino" else "Salvar alterações",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun CartaoDeSerie(
    fase: String,
    indice: Int,
    serie: SerieDigitada,
    onAlterar: (SerieDigitada) -> Unit,
    onRemover: () -> Unit
) {
    val cabecalhoInvalido = serie.serie.isNotBlank() && MontadorDeTreino.lerCabecalho(serie.serie) == null
    val intervaloInvalido = MontadorDeTreino.lerIntervalo(serie.intervalo) == null

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
    ) {
        Column(modifier = Modifier.padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 12.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Série ${indice + 1}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AquaTextSecondary,
                    modifier = Modifier.weight(1f)
                )
                IconButton(onClick = onRemover) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = "Remover série ${indice + 1} de $fase",
                        tint = AquaMagenta
                    )
                }
            }
            Column(modifier = Modifier.padding(end = 8.dp)) {
                OutlinedTextField(
                    value = serie.serie,
                    onValueChange = { onAlterar(serie.copy(serie = it)) },
                    label = { Text("Série") },
                    placeholder = { Text("8x50m Crawl") },
                    singleLine = true,
                    isError = cabecalhoInvalido,
                    supportingText = if (cabecalhoInvalido) {
                        { Text("Comece pela distância: 8x50m ou 400m.") }
                    } else {
                        null
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("serie_${fase.lowercase()}_$indice")
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serie.detalhes,
                    onValueChange = { onAlterar(serie.copy(detalhes = it)) },
                    label = { Text("Detalhes (opcional)") },
                    placeholder = { Text("25m forte; 25m leve") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = serie.intervalo,
                    onValueChange = { onAlterar(serie.copy(intervalo = it)) },
                    label = { Text("Intervalo (opcional)") },
                    placeholder = { Text("#20\" ou @1'30\"") },
                    singleLine = true,
                    isError = intervaloInvalido,
                    supportingText = if (intervaloInvalido) {
                        { Text("Use #20\" (descanso) ou @1'30\" (saída a cada).") }
                    } else {
                        null
                    },
                    modifier = Modifier.fillMaxWidth()
                )
                serie.corretivo?.let { c ->
                    Text(
                        text = "Corretivo: ${c.nome} — “${c.dica}”",
                        fontSize = 12.sp,
                        color = AquaTextSecondary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                Text(
                    text = "Zona (opcional)",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = AquaTextSecondary,
                    modifier = Modifier.padding(top = 10.dp, bottom = 6.dp)
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    (listOf("") + MetodoNC.ZONAS.map { it.sigla }).forEach { sigla ->
                        val ativa = serie.zona.equals(sigla, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(
                                    when {
                                        !ativa -> Color.White
                                        sigla.isEmpty() -> AquaBlueBg
                                        else -> corDaZona(sigla)
                                    }
                                )
                                .border(1.dp, if (ativa) Color.Transparent else AquaBorder, RoundedCornerShape(8.dp))
                                .clickable { onAlterar(serie.copy(zona = sigla)) }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                                .testTag("zona_${indice}_${sigla.ifEmpty { "nenhuma" }}")
                        ) {
                            Text(
                                text = sigla.ifEmpty { "—" },
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (ativa && sigla in setOf("AN", "AA")) Color.White else AquaTextPrimary
                            )
                        }
                    }
                }
            }
        }
    }
}
