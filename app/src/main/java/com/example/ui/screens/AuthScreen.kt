package com.example.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.painterResource
import com.example.R
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.TrainingLevel
import com.example.ui.components.MensagemDeTela
import com.example.ui.components.SeletorDeNivel
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.AquaBorder
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.viewmodel.ContaUiState

/** Entrar ou criar conta. O app só abre depois daqui. */
@Composable
fun AuthScreen(
    estado: ContaUiState,
    onEntrar: (email: String, senha: String) -> Unit,
    onCadastrar: (nome: String, email: String, senha: String, nivel: TrainingLevel) -> Unit,
    onLimparMensagens: () -> Unit,
    modifier: Modifier = Modifier,
    comecarNoCadastro: Boolean = false,
    onEsqueciSenha: (emailDigitado: String) -> Unit = {}
) {
    var cadastro by rememberSaveable { mutableStateOf(comecarNoCadastro) }
    var nome by rememberSaveable { mutableStateOf("") }
    var email by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var senhaVisivel by rememberSaveable { mutableStateOf(false) }
    var nivel by rememberSaveable { mutableStateOf(TrainingLevel.INTERMEDIARIO) }

    val enviar: () -> Unit = {
        if (cadastro) onCadastrar(nome, email, senha, nivel) else onEntrar(email, senha)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AquaBackground)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        Spacer(modifier = Modifier.height(40.dp))

        Image(
            painter = painterResource(R.drawable.natacao_criativa_logo),
            contentDescription = "Natação Criativa Treinos",
            modifier = Modifier.height(120.dp)
        )
        Text(
            text = if (cadastro) {
                "Crie sua conta e receba um treino de natação por dia, no seu nível."
            } else {
                "Entre para ver o treino do dia e os seus treinos salvos."
            },
            fontSize = 14.sp,
            color = AquaTextSecondary,
            modifier = Modifier.padding(top = 4.dp)
        )

        Spacer(modifier = Modifier.height(28.dp))

        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(50.dp)),
            shape = RoundedCornerShape(50.dp),
            color = Color(0xFFE8F1FC),
            border = androidx.compose.foundation.BorderStroke(1.dp, AquaBorder)
        ) {
            Row(modifier = Modifier.padding(4.dp)) {
                listOf(false to "Entrar", true to "Criar conta").forEach { (modo, rotulo) ->
                    val ativo = cadastro == modo
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(50.dp))
                            .background(if (ativo) Color.White else Color.Transparent)
                            .clickable(enabled = !estado.enviando && !ativo) {
                                cadastro = modo
                                onLimparMensagens()
                            }
                            .padding(vertical = 10.dp)
                            .testTag(if (modo) "aba_cadastro" else "aba_entrar"),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = rotulo,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (ativo) AquaPrimary else AquaTextSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        if (!estado.configurado) {
            MensagemDeTela(
                texto = "Este build não tem SUPABASE_URL e SUPABASE_ANON_KEY. Sem eles não dá para entrar.",
                erro = true
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (cadastro) {
            OutlinedTextField(
                value = nome,
                onValueChange = { nome = it },
                label = { Text("Nome") },
                singleLine = true,
                enabled = !estado.enviando,
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.Words,
                    imeAction = ImeAction.Next
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("campo_nome")
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("E-mail") },
            singleLine = true,
            enabled = !estado.enviando,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Next),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("campo_email")
        )

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedTextField(
            value = senha,
            onValueChange = { senha = it },
            label = { Text("Senha") },
            singleLine = true,
            enabled = !estado.enviando,
            visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
            trailingIcon = {
                IconButton(onClick = { senhaVisivel = !senhaVisivel }) {
                    Icon(
                        imageVector = if (senhaVisivel) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                        contentDescription = if (senhaVisivel) "Esconder senha" else "Mostrar senha"
                    )
                }
            },
            supportingText = if (cadastro) {
                { Text("Pelo menos 6 caracteres.") }
            } else {
                null
            },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
            keyboardActions = KeyboardActions(onDone = { enviar() }),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("campo_senha")
        )

        if (!cadastro) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = { onEsqueciSenha(email) },
                    enabled = !estado.enviando,
                    // O texto termina na borda do campo de senha, descontando a folga interna do TextButton.
                    modifier = Modifier
                        .offset(x = 12.dp)
                        .testTag("esqueci_senha")
                ) {
                    Text("Esqueci minha senha", fontWeight = FontWeight.SemiBold, color = AquaPrimary)
                }
            }
        }

        if (cadastro) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Seu nível na natação",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = AquaTextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            SeletorDeNivel(selecionado = nivel, onSelecionar = { nivel = it }, habilitado = !estado.enviando)
            Text(
                text = "É o nível do treino sugerido todo dia. Dá para mudar depois, no Perfil.",
                fontSize = 12.sp,
                color = AquaTextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
        }

        estado.erro?.let { MensagemDeTela(texto = it, erro = true) }
        estado.aviso?.let { MensagemDeTela(texto = it, erro = false) }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = enviar,
            enabled = estado.configurado && !estado.enviando,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("botao_enviar"),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
        ) {
            if (estado.enviando) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Text(
                    text = if (cadastro) "Criar conta" else "Entrar",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}
