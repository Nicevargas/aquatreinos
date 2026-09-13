package com.example.data

sealed interface Resultado<out T> {
    data class Ok<T>(val valor: T) : Resultado<T>
    data class Falha(val mensagem: String) : Resultado<Nothing>
}
