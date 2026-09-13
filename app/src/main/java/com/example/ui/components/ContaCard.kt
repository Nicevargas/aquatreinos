package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrainingLevel
import com.example.ui.theme.AquaBlueBg
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaMagenta
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.viewmodel.ContaUiState

/** Minha conta: ver e editar o perfil, sair e excluir a conta. */
@Composable
fun ContaCard(
    estado: ContaUiState,
    onSalvar: (nome: String, piscinaMetros: Int, nivel: TrainingLevel) -> Unit,
    onSair: () -> Unit,
    onExcluirConta: () -> Unit,
    modifier: Modifier = Modifier
) {
    val perfil = estado.perfil
    var editando by rememberSaveable { mutableStateOf(false) }
    var nome by rememberSaveable(perfil) { mutableStateOf(perfil?.nome.orEmpty()) }
    var piscina by rememberSaveable(perfil) { mutableStateOf(perfil?.piscinaMetros ?: 25) }
    var nivel by rememberSaveable(perfil) { mutableStateOf(perfil?.nivel ?: TrainingLevel.INTERMEDIARIO) }
    var confirmarExclusao by remember { mutableStateOf(false) }

    // Perfil novo chegou do banco (salvou): fecha a edição.
    LaunchedEffect(perfil) { editando = false }

    if (confirmarExclusao) {
        AlertDialog(
            onDismissRequest = { if (!estado.excluindoConta) confirmarExclusao = false },
            title = { Text("Excluir sua conta?", fontWeight = FontWeight.Bold) },
            text = {
                Text("Isso apaga sua conta, seu perfil, seus treinos salvos e os tempos gravados. Não dá para desfazer.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        confirmarExclusao = false
                        onExcluirConta()
                    },
                    modifier = Modifier.testTag("confirmar_excluir_conta")
                ) {
                    Text("Excluir conta", color = AquaMagenta, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmarExclusao = false }) { Text("Cancelar") }
            }
        )
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("conta_card"),
        shape = RoundedCornerShape(24.dp),
        color = Color.White,
        border = BorderStroke(1.dp, AquaBorder)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(AquaPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = (perfil?.nome?.trim()?.firstOrNull() ?: estado.sessao?.email?.firstOrNull() ?: '?')
                            .uppercaseChar().toString(),
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = perfil?.nome?.ifBlank { null } ?: "Minha conta",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = AquaTextPrimary
                    )
                    Text(
                        text = perfil?.email ?: estado.sessao?.email.orEmpty(),
                        fontSize = 13.sp,
                        color = AquaTextSecondary
                    )
                }
            }

            if (estado.carregandoPerfil && perfil == null) {
                LinearProgressIndicator(
                    color = AquaPrimary,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp)
                )
            }

            if (editando) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = nome,
                    onValueChange = { nome = it },
                    label = { Text("Nome") },
                    singleLine = true,
                    enabled = !estado.salvandoPerfil,
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Words),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("perfil_nome")
                )

                Spacer(modifier = Modifier.height(12.dp))
                Text("Piscina", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AquaTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    listOf(25, 50).forEach { metros ->
                        val ativo = piscina == metros
                        Surface(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(12.dp))
                                .clickable(enabled = !estado.salvandoPerfil) { piscina = metros }
                                .testTag("piscina_$metros"),
                            shape = RoundedCornerShape(12.dp),
                            color = if (ativo) AquaBlueBg else Color.White,
                            border = BorderStroke(if (ativo) 2.dp else 1.dp, if (ativo) AquaPrimary else AquaBorder)
                        ) {
                            Text(
                                text = "${metros}m",
                                // fillMaxWidth: sem ele o textAlign não centraliza e "25m" ficava no canto.
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 10.dp),
                                fontWeight = FontWeight.Bold,
                                color = if (ativo) AquaPrimary else AquaTextPrimary,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text("Nível", fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AquaTextPrimary)
                Spacer(modifier = Modifier.height(6.dp))
                SeletorDeNivel(selecionado = nivel, onSelecionar = { nivel = it }, habilitado = !estado.salvandoPerfil)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 16.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(
                        onClick = {
                            nome = perfil?.nome.orEmpty()
                            piscina = perfil?.piscinaMetros ?: 25
                            nivel = perfil?.nivel ?: TrainingLevel.INTERMEDIARIO
                            editando = false
                        },
                        enabled = !estado.salvandoPerfil
                    ) { Text("Cancelar") }
                    Button(
                        onClick = { onSalvar(nome, piscina, nivel) },
                        enabled = !estado.salvandoPerfil,
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary),
                        modifier = Modifier.testTag("perfil_salvar")
                    ) {
                        if (estado.salvandoPerfil) {
                            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
                        } else {
                            Text("Salvar", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            } else if (perfil != null) {
                Spacer(modifier = Modifier.height(16.dp))
                LinhaDoPerfil("Piscina", "${perfil.piscinaMetros}m")
                LinhaDoPerfil(
                    "Nível",
                    listOf(perfil.nivel.label, perfil.nivel.carouselLabel).distinct().joinToString(" · ")
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OutlinedButton(
                        onClick = { editando = true },
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("perfil_editar")
                    ) { Text("Editar perfil", color = AquaPrimary, fontWeight = FontWeight.Bold) }
                    TextButton(onClick = onSair, modifier = Modifier.testTag("sair")) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Sair")
                    }
                }
            }

            estado.erro?.let { MensagemDeTela(texto = it, erro = true) }
            estado.aviso?.let { MensagemDeTela(texto = it, erro = false) }

            HorizontalDivider(color = AquaBorder, modifier = Modifier.padding(top = 16.dp))
            TextButton(
                onClick = { confirmarExclusao = true },
                enabled = !estado.excluindoConta,
                // O TextButton tem 12dp de folga interna: puxa para o texto alinhar com a margem do cartão.
                modifier = Modifier
                    .offset(x = (-12).dp)
                    .testTag("excluir_conta")
            ) {
                Text(
                    text = if (estado.excluindoConta) "Excluindo conta…" else "Excluir minha conta",
                    color = AquaMagenta,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
private fun LinhaDoPerfil(rotulo: String, valor: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(rotulo, fontSize = 13.sp, color = AquaTextSecondary, modifier = Modifier.width(72.dp))
        Text(valor, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = AquaTextPrimary)
    }
}
