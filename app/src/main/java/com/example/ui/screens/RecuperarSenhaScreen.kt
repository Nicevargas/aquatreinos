package com.example.ui.screens

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.MensagemDeTela
import com.example.ui.theme.AquaBackground
import com.example.ui.theme.AquaPrimary
import com.example.ui.theme.AquaTextSecondary
import com.example.viewmodel.EtapaRecuperacao
import com.example.viewmodel.RecuperacaoUiState
import kotlinx.coroutines.delay

/** Esqueci minha senha: e-mail -> código -> senha nova, tudo dentro do app. */
@Composable
fun RecuperarSenhaScreen(
    estado: RecuperacaoUiState,
    configurado: Boolean,
    onEnviarCodigo: (email: String) -> Unit,
    onRedefinir: (codigo: String, novaSenha: String, confirmacao: String) -> Unit,
    onTrocarEmail: () -> Unit,
    onVoltar: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(enabled = !estado.enviando, onBack = onVoltar)

    var email by rememberSaveable(estado.etapa) { mutableStateOf(estado.email) }
    var codigo by rememberSaveable { mutableStateOf("") }
    var senha by rememberSaveable { mutableStateOf("") }
    var confirmacao by rememberSaveable { mutableStateOf("") }
    var senhaVisivel by rememberSaveable { mutableStateOf(false) }

    // Contagem para liberar o reenvio.
    var agora by androidx.compose.runtime.remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(estado.reenviarLiberadoEm) {
        agora = System.currentTimeMillis()
        while (agora < estado.reenviarLiberadoEm) {
            delay(1_000)
            agora = System.currentTimeMillis()
        }
    }
    val faltamSegundos = ((estado.reenviarLiberadoEm - agora + 999) / 1000).coerceAtLeast(0)

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AquaBackground)
            .systemBarsPadding()
            .imePadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 24.dp)
    ) {
        // offset: a seta alinha com a margem do título, descontando a folga interna do IconButton.
        Row(
            modifier = Modifier
                .padding(top = 8.dp)
                .offset(x = (-12).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onVoltar, enabled = !estado.enviando, modifier = Modifier.testTag("recuperar_voltar")) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar para o login")
            }
            Text("Voltar para o login", fontSize = 14.sp, color = AquaTextSecondary)
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Esqueci minha senha",
            fontSize = 28.sp,
            fontWeight = FontWeight.Black,
            color = AquaPrimary
        )

        if (estado.etapa == EtapaRecuperacao.EMAIL) {
            Text(
                text = "Digite o e-mail da sua conta. Vamos mandar um código para você criar uma senha nova.",
                fontSize = 14.sp,
                color = AquaTextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("E-mail") },
                singleLine = true,
                enabled = !estado.enviando,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email, imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { onEnviarCodigo(email) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recuperar_email")
            )

            estado.erro?.let { MensagemDeTela(texto = it, erro = true) }

            Spacer(modifier = Modifier.height(20.dp))

            BotaoPrincipal(
                texto = "Enviar código",
                carregando = estado.enviando,
                habilitado = configurado,
                testTag = "recuperar_enviar_codigo",
                onClick = { onEnviarCodigo(email) }
            )
        } else {
            Text(
                text = if (estado.codigoValidado) {
                    "Código confirmado. Escolha outra senha nova."
                } else {
                    "Digite o código enviado para ${estado.email} e escolha uma senha nova."
                },
                fontSize = 14.sp,
                color = AquaTextSecondary,
                modifier = Modifier.padding(top = 6.dp)
            )
            TextButton(
                onClick = onTrocarEmail,
                enabled = !estado.enviando,
                modifier = Modifier
                    .offset(x = (-12).dp)
                    .testTag("recuperar_trocar_email")
            ) {
                Text("Usar outro e-mail")
            }

            estado.aviso?.let { MensagemDeTela(texto = it, erro = false) }

            Spacer(modifier = Modifier.height(12.dp))

            if (!estado.codigoValidado) {
                OutlinedTextField(
                    value = codigo,
                    onValueChange = { novo -> codigo = novo.filter { it.isDigit() }.take(10) },
                    label = { Text("Código do e-mail") },
                    singleLine = true,
                    enabled = !estado.enviando,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword, imeAction = ImeAction.Next),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("recuperar_codigo")
                )
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = senha,
                onValueChange = { senha = it },
                label = { Text("Senha nova") },
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
                supportingText = { Text("Pelo menos 6 caracteres.") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Next),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recuperar_senha")
            )

            OutlinedTextField(
                value = confirmacao,
                onValueChange = { confirmacao = it },
                label = { Text("Repita a senha nova") },
                singleLine = true,
                enabled = !estado.enviando,
                visualTransformation = if (senhaVisivel) VisualTransformation.None else PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password, imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onRedefinir(codigo, senha, confirmacao) }),
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("recuperar_confirmacao")
            )

            estado.erro?.let { MensagemDeTela(texto = it, erro = true) }

            Spacer(modifier = Modifier.height(20.dp))

            BotaoPrincipal(
                texto = "Salvar senha nova",
                carregando = estado.enviando,
                habilitado = configurado,
                testTag = "recuperar_salvar",
                onClick = { onRedefinir(codigo, senha, confirmacao) }
            )

            if (!estado.codigoValidado) {
                TextButton(
                    onClick = { onEnviarCodigo(estado.email) },
                    enabled = !estado.enviando && faltamSegundos == 0L,
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .padding(top = 8.dp)
                        .testTag("recuperar_reenviar")
                ) {
                    Text(if (faltamSegundos > 0) "Reenviar código em ${faltamSegundos}s" else "Não chegou? Reenviar código")
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))
    }
}

@Composable
private fun BotaoPrincipal(
    texto: String,
    carregando: Boolean,
    habilitado: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        enabled = habilitado && !carregando,
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp)
            .testTag(testTag),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(containerColor = AquaPrimary)
    ) {
        if (carregando) {
            CircularProgressIndicator(color = Color.White, strokeWidth = 2.dp, modifier = Modifier.size(22.dp))
        } else {
            Text(texto, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        }
    }
}
