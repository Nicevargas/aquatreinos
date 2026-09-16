package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.compartilhar.TextosDoTreino
import com.example.data.progresso.Pontuacao
import com.example.data.ranking.LinhaDoRanking
import com.example.data.ranking.OpcoesDoRanking
import com.example.data.ranking.PeriodoDoRanking
import com.example.ui.components.MensagemDeTela
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaSurfaceContainerLow
import com.example.ui.theme.AquaTextMuted
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.viewmodel.FiltrosDoRanking
import com.example.viewmodel.FormularioDoRanking
import com.example.viewmodel.RankingUiState

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun RankingScreen(
    estado: RankingUiState,
    onVoltar: () -> Unit,
    onFiltrar: ((FiltrosDoRanking) -> FiltrosDoRanking) -> Unit,
    onParticipar: () -> Unit,
    onAlterarFormulario: ((FormularioDoRanking) -> FormularioDoRanking) -> Unit,
    onSalvarFormulario: () -> Unit,
    onCancelarFormulario: () -> Unit,
    onSairDoRanking: () -> Unit,
    onTentarDeNovo: () -> Unit,
    modifier: Modifier = Modifier
) {
    val formulario = estado.formulario
    BackHandler(onBack = if (formulario != null) onCancelarFormulario else onVoltar)

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
                .padding(horizontal = 4.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = if (formulario != null) onCancelarFormulario else onVoltar) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
            }
            Text(
                text = if (formulario != null) "Participar do ranking" else "Ranking",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = AquaTextPrimary
            )
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 16.dp)
        ) {
            if (formulario != null) {
                FormularioDeParticipacao(formulario, onAlterarFormulario, onSalvarFormulario, onCancelarFormulario)
            } else {
                val participa = estado.participacao?.publico == true
                Cartao {
                    if (participa) {
                        Text("Você está no ranking como", fontSize = 13.sp, color = AquaTextSecondary)
                        Text(estado.participacao?.nome.orEmpty(), fontSize = 18.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)
                        // FlowRow: em tela estreita "Sair do ranking" desce inteiro em vez de quebrar letra por letra.
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(top = 8.dp)
                        ) {
                            OutlinedButton(onClick = onParticipar, shape = RoundedCornerShape(12.dp)) { Text("Editar meus dados") }
                            TextButton(onClick = onSairDoRanking, modifier = Modifier.testTag("sair_do_ranking")) {
                                Text("Sair do ranking", color = AquaMagenta)
                            }
                        }
                    } else {
                        Text("Entre no ranking", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary)
                        Text(
                            text = "Só aparece quem aceita. Os outros nadadores veem o nome que você escolher, seus pontos, metros e treinos. E-mail, idade exata, esforço e saúde nunca aparecem.",
                            fontSize = 13.sp,
                            color = AquaTextSecondary,
                            lineHeight = 19.sp,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        Button(
                            onClick = onParticipar,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary),
                            modifier = Modifier
                                .padding(top = 10.dp)
                                .testTag("participar_do_ranking")
                        ) { Text("Quero participar", fontWeight = FontWeight.Bold) }
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))
                Filtros(estado, onFiltrar)

                Text(
                    text = "Pontos: 1 a cada ${Pontuacao.METROS_POR_PONTO} m · +${Pontuacao.BONUS_TREINO_COMPLETO} por treino completo · +${Pontuacao.BONUS_SEMANA_COM_TREINO} por semana com treino",
                    fontSize = 12.sp,
                    color = AquaTextMuted,
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(top = 10.dp, bottom = 10.dp)
                )

                estado.erro?.let {
                    MensagemDeTela(texto = it, erro = true)
                    TextButton(onClick = onTentarDeNovo) { Text("Tentar de novo") }
                }

                if (estado.carregando && estado.linhas.isEmpty()) {
                    Box(Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = AquaPrimary)
                    }
                } else if (estado.linhas.isEmpty() && estado.erro == null) {
                    Cartao {
                        Text(
                            text = "Ninguém com treinos nesse filtro ainda. Que tal ser o primeiro?",
                            fontSize = 14.sp,
                            color = AquaTextSecondary
                        )
                    }
                } else if (estado.linhas.isNotEmpty()) {
                    Cartao(padding = 0) {
                        estado.linhas.forEachIndexed { i, linha ->
                            if (i > 0) HorizontalDivider(color = AquaBorder)
                            LinhaDoRankingItem(linha)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun Filtros(estado: RankingUiState, onFiltrar: ((FiltrosDoRanking) -> FiltrosDoRanking) -> Unit) {
    val f = estado.filtros
    val p = estado.participacao
    LinhaDeChips("Período") {
        PeriodoDoRanking.entries.forEach { periodo ->
            Chip(periodo.rotulo, f.periodo == periodo) { onFiltrar { it.copy(periodo = periodo) } }
        }
    }
    LinhaDeChips("Idade") {
        Chip("Todas", f.faixa == null) { onFiltrar { it.copy(faixa = null) } }
        OpcoesDoRanking.FAIXAS.forEach { (chave, rotulo) ->
            Chip(rotulo, f.faixa == chave) { onFiltrar { it.copy(faixa = chave) } }
        }
    }
    LinhaDeChips("Sexo") {
        Chip("Todos", f.sexo == null) { onFiltrar { it.copy(sexo = null) } }
        OpcoesDoRanking.SEXOS.forEach { (chave, rotulo) ->
            Chip(rotulo, f.sexo == chave) { onFiltrar { it.copy(sexo = chave) } }
        }
    }
    LinhaDeChips("Horário") {
        Chip("Todos", f.horario == null) { onFiltrar { it.copy(horario = null) } }
        OpcoesDoRanking.HORARIOS.forEach { (chave, rotulo) ->
            Chip(rotulo, f.horario == chave) { onFiltrar { it.copy(horario = chave) } }
        }
    }
    val temCidade = !p?.cidade.isNullOrBlank()
    val temLocal = !p?.local.isNullOrBlank()
    if (temCidade || temLocal) {
        LinhaDeChips("Local") {
            Chip("Todos", !f.soMinhaCidade && !f.soMeuLocal) { onFiltrar { it.copy(soMinhaCidade = false, soMeuLocal = false) } }
            if (temCidade) Chip(p?.cidade.orEmpty(), f.soMinhaCidade) { onFiltrar { it.copy(soMinhaCidade = true, soMeuLocal = false) } }
            if (temLocal) Chip(p?.local.orEmpty(), f.soMeuLocal) { onFiltrar { it.copy(soMeuLocal = true, soMinhaCidade = false) } }
        }
    }
}

@Composable
private fun LinhaDeChips(titulo: String, chips: @Composable () -> Unit) {
    Text(titulo, fontSize = 12.sp, fontWeight = FontWeight.Bold, color = AquaTextSecondary, modifier = Modifier.padding(top = 8.dp, bottom = 4.dp))
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) { chips() }
}

@Composable
private fun Chip(texto: String, ativo: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .clip(RoundedCornerShape(50.dp))
            .clickable(onClick = onClick)
            .testTag("filtro_$texto"),
        shape = RoundedCornerShape(50.dp),
        color = if (ativo) AquaPrimary else Color.White,
        border = BorderStroke(1.dp, if (ativo) AquaPrimary else AquaBorder)
    ) {
        Text(
            text = texto,
            fontSize = 13.sp,
            fontWeight = if (ativo) FontWeight.Bold else FontWeight.Medium,
            color = if (ativo) Color.White else AquaTextPrimary,
            softWrap = false,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
        )
    }
}

@Composable
private fun LinhaDoRankingItem(linha: LinhaDoRanking) {
    val medalha = when (linha.posicao) {
        1 -> Color(0xFFE5B100)
        2 -> Color(0xFF9AA5B1)
        3 -> Color(0xFFC77B30)
        else -> null
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (linha.souEu) AquaBlueBg else Color.Transparent)
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(medalha ?: AquaSurfaceContainerLow),
            contentAlignment = Alignment.Center
        ) {
            Text("${linha.posicao}", fontSize = 15.sp, fontWeight = FontWeight.Black, color = if (medalha != null) Color.White else AquaTextSecondary, softWrap = false)
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp)
        ) {
            Text(
                text = if (linha.souEu) "${linha.nome} (você)" else linha.nome,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = if (linha.souEu) AquaPrimary else AquaTextPrimary
            )
            Text(
                text = "${TextosDoTreino.metros(linha.metros)} · ${linha.treinos} ${if (linha.treinos == 1) "treino" else "treinos"} · ${Pontuacao.nivel(linha.pontos).nome}",
                fontSize = 12.sp,
                color = AquaTextSecondary
            )
        }
        Column(horizontalAlignment = Alignment.End) {
            Text("${linha.pontos}", fontSize = 18.sp, fontWeight = FontWeight.Black, color = AquaTextPrimary, softWrap = false)
            Text("pontos", fontSize = 11.sp, color = AquaTextMuted, softWrap = false)
        }
    }
}

@Composable
private fun FormularioDeParticipacao(
    formulario: FormularioDoRanking,
    onAlterar: ((FormularioDoRanking) -> FormularioDoRanking) -> Unit,
    onSalvar: () -> Unit,
    onCancelar: () -> Unit
) {
    val habilitado = !formulario.salvando
    Cartao {
        Text(
            text = "Esses dados servem só para os filtros do ranking. Quem vê o ranking não vê seu ano de nascimento nem seu sexo, só o nome que você escolher.",
            fontSize = 13.sp,
            color = AquaTextSecondary,
            lineHeight = 19.sp
        )
        OutlinedTextField(
            value = formulario.nome,
            onValueChange = { v -> onAlterar { it.copy(nome = v.take(40)) } },
            label = { Text("Nome no ranking") },
            supportingText = { Text("Ex.: Ana S.") },
            singleLine = true,
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .testTag("ranking_nome")
        )
        OutlinedTextField(
            value = formulario.ano,
            onValueChange = { v -> onAlterar { it.copy(ano = v.filter(Char::isDigit).take(4)) } },
            label = { Text("Ano de nascimento (opcional)") },
            singleLine = true,
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
                .testTag("ranking_ano")
        )
        Text("Sexo (opcional)", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AquaTextPrimary, modifier = Modifier.padding(top = 10.dp, bottom = 6.dp))
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Chip("Não informar", formulario.sexo == null) { if (habilitado) onAlterar { it.copy(sexo = null) } }
            OpcoesDoRanking.SEXOS.forEach { (chave, rotulo) ->
                Chip(rotulo, formulario.sexo == chave) { if (habilitado) onAlterar { it.copy(sexo = chave) } }
            }
        }
        OutlinedTextField(
            value = formulario.cidade,
            onValueChange = { v -> onAlterar { it.copy(cidade = v.take(80)) } },
            label = { Text("Cidade (opcional)") },
            singleLine = true,
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
        )
        OutlinedTextField(
            value = formulario.local,
            onValueChange = { v -> onAlterar { it.copy(local = v.take(80)) } },
            label = { Text("Onde você nada (opcional)") },
            supportingText = { Text("Clube, academia ou piscina") },
            singleLine = true,
            enabled = habilitado,
            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp)
        )
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp)
                .clip(RoundedCornerShape(12.dp))
                .toggleable(value = formulario.aceite, enabled = habilitado, role = Role.Checkbox) { v -> onAlterar { it.copy(aceite = v) } }
                .testTag("ranking_aceite"),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = formulario.aceite,
                onCheckedChange = null,
                enabled = habilitado,
                colors = CheckboxDefaults.colors(checkedColor = AquaPrimary),
                modifier = Modifier.padding(10.dp)
            )
            Text(
                text = "Aceito aparecer no ranking do app Natação Criativa com esse nome, meus pontos, metros e treinos. Posso sair quando quiser.",
                fontSize = 13.sp,
                color = AquaTextPrimary,
                lineHeight = 19.sp,
                modifier = Modifier.padding(top = 10.dp, end = 8.dp)
            )
        }
        formulario.erro?.let { MensagemDeTela(texto = it, erro = true) }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp),
            horizontalArrangement = Arrangement.End,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onCancelar, enabled = habilitado) { Text("Cancelar") }
            Spacer(modifier = Modifier.width(8.dp))
            Button(
                onClick = onSalvar,
                enabled = habilitado,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary),
                modifier = Modifier.testTag("ranking_salvar")
            ) {
                if (formulario.salvando) {
                    CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                } else {
                    Text("Salvar", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun Cartao(padding: Int = 16, conteudo: @Composable ColumnScope.() -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AquaBorder)
    ) {
        Column(modifier = Modifier.padding(padding.dp), content = conteudo)
    }
}
