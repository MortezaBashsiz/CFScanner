package ir.filternet.cfscanner.ui.theme

import androidx.compose.material.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.intl.Locale
import ir.filternet.cfscanner.R


private val VazirFontFamily = FontFamily(
    Font(R.font.vazir_regular),
    Font(R.font.vazir_black, FontWeight.Black),
    Font(R.font.vazir_bold, FontWeight.Bold),
    Font(R.font.vazir_light, FontWeight.Light),
    Font(R.font.vazir_medium, FontWeight.Medium),
    Font(R.font.vazir_thin, FontWeight.Thin),
)

private val VazirNumberFontFamily = FontFamily(
    Font(R.font.vazir_variable)
)

val Typography = Typography(
    defaultFontFamily = if (Locale.current.language == "fa") VazirFontFamily else FontFamily.Default,
)