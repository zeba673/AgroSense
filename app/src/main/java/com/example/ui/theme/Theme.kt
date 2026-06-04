package com.example.ui.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext

private val DarkColorScheme =
  darkColorScheme(
    primary = VerdeLuzSecundario,
    secondary = VerdeChacraPrimario,
    tertiary = TierraCafeTerciario,
    background = FondoOscuroNativo,
    surface = SuperficieOscura
  )

private val LightColorScheme =
  lightColorScheme(
    primary = VerdeChacraPrimario,
    secondary = VerdeLuzSecundario,
    tertiary = TierraCafeTerciario,
    background = FondoClaroNativo,
    surface = SuperficieClara
  )

@Composable
fun MyApplicationTheme(
  darkTheme: Boolean = false,
  dynamicColor: Boolean = false,
  content: @Composable () -> Unit,
) {
  val colorScheme = LightColorScheme

  MaterialTheme(colorScheme = colorScheme, typography = Typography, content = content)
}
