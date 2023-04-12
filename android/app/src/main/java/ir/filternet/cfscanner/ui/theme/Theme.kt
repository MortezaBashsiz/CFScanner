package ir.filternet.cfscanner.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material.MaterialTheme
import androidx.compose.material.darkColors
import androidx.compose.material.lightColors
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorPalette = darkColors(
    primary = OrangeFlare1,
    primaryVariant = OrangeFlare4,
    secondary = Teal200,
    onBackground = Color.White,
    onSecondary = Color.White,
    background = Carbon_dark,
    onSurface = Carbon_light
)

private val LightColorPalette = lightColors(
    primary = OrangeFlare1,
    primaryVariant = OrangeFlare4,
    secondary = Teal200,
    onBackground = Carbon,
    onSecondary = Carbon,
    onSurface = White_Milk,
    /* Other default colors to override
    background = Color.White,
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black,
    */
)

@Composable
fun CFScannerTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) {
        DarkColorPalette
    } else {
        LightColorPalette
    }

    MaterialTheme(
        colors = colors,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}