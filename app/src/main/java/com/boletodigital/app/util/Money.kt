package com.boletodigital.app.util

import java.text.NumberFormat
import java.util.Locale

private val br = Locale("pt", "BR")
private val currency = NumberFormat.getCurrencyInstance(br)

fun Double.emReais(): String = currency.format(this)

fun String.paraDoubleOuPadrao(padrao: Double): Double =
    replace("R$", "", ignoreCase = true)
        .replace(".", "")
        .replace(",", ".")
        .trim()
        .toDoubleOrNull() ?: padrao
