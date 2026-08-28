package com.monster.literaryflow.ui2

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// ==========================================
// Bento 2.0 Color Palette (Emerald / Mint / Slate / Sky / Amber / Rose)
// ==========================================

// Primary Emerald Palette
val LfEmerald50 = Color(0xFFECFDF5)
val LfEmerald100 = Color(0xFFD1FAE5)
val LfEmerald200 = Color(0xFFA7F3D0)
val LfEmerald300 = Color(0xFF6EE7B7)
val LfEmerald400 = Color(0xFF34D399)
val LfEmerald500 = Color(0xFF10B981) // Main Emerald Accent
val LfEmerald600 = Color(0xFF059669)
val LfEmerald700 = Color(0xFF047857)
val LfEmerald800 = Color(0xFF065F46)
val LfEmerald900 = Color(0xFF064E3B)

// Slate Neutral Palette
val LfSlate50 = Color(0xFFF8FAFC)
val LfSlate100 = Color(0xFFF1F5F9)
val LfSlate150 = Color(0xFFE8EEF5)
val LfSlate200 = Color(0xFFE2E8F0) // Border
val LfSlate300 = Color(0xFFCBD5E1)
val LfSlate400 = Color(0xFF94A3B8)
val LfSlate500 = Color(0xFF64748B)
val LfSlate600 = Color(0xFF475569)
val LfSlate700 = Color(0xFF334155)
val LfSlate800 = Color(0xFF1E293B)
val LfSlate900 = Color(0xFF0F172A)

// Sky Blue Perception
val LfSky50 = Color(0xFFF0F9FF)
val LfSky100 = Color(0xFFE0F2FE)
val LfSky200 = Color(0xFFBAE6FD)
val LfSky400 = Color(0xFF38BDF8)
val LfSky500 = Color(0xFF0EA5E9)
val LfSky600 = Color(0xFF0284C7)

// Amber Warning / Action
val LfAmber50 = Color(0xFFFFFBEB)
val LfAmber100 = Color(0xFFFEF3C7)
val LfAmber200 = Color(0xFFFDE68A)
val LfAmber400 = Color(0xFFFBBF24)
val LfAmber500 = Color(0xFFF59E0B)
val LfAmber700 = Color(0xFFB45309)
val LfAmber800 = Color(0xFF92400E)
val LfAmber900 = Color(0xFF78350F)

// Teal — “等待文字出现” 类步骤的强调色
val LfTeal50 = Color(0xFFF0FDFA)
val LfTeal100 = Color(0xFFCCFBF1)
val LfTeal200 = Color(0xFF99F6E4)
val LfTeal500 = Color(0xFF14B8A6)
val LfTeal600 = Color(0xFF0D9488)
val LfTeal700 = Color(0xFF0F766E)
val LfTeal900 = Color(0xFF134E4A)

// Indigo — “等待指定页面”多条件判断的强调色
val LfIndigo50 = Color(0xFFEEF2FF)
val LfIndigo100 = Color(0xFFE0E7FF)
val LfIndigo200 = Color(0xFFC7D2FE)
val LfIndigo600 = Color(0xFF4F46E5)
val LfIndigo700 = Color(0xFF4338CA)
val LfIndigo950 = Color(0xFF1E1B4B)

// Rose Danger / Error
val LfRose50 = Color(0xFFFFF1F2)
val LfRose100 = Color(0xFFFFE4E6)
val LfRose200 = Color(0xFFFECDD3)
val LfRose500 = Color(0xFFF43F5E)
val LfRose600 = Color(0xFFE11D48)
val LfRose700 = Color(0xFFBE123C)
val LfRose900 = Color(0xFF881337)

// Semantic Names
val LfBackground = Color(0xFFF1F9F6) // Light mint background from prototype
val LfCardBg = Color.White
val LfPrimary = LfEmerald500
val LfPrimarySoft = LfEmerald50
val LfPrimaryDark = LfEmerald800
val LfBorder = LfSlate200
val LfTextPrimary = LfSlate900
val LfTextSecondary = LfSlate500
val LfTextMuted = LfSlate400
val LfDanger = LfRose500
val LfDangerSoft = LfRose50
val LfWarning = LfAmber500
val LfWarningSoft = LfAmber100
val LfSkySoft = LfSky100
val LfDark = LfSlate800
val LfInk = LfSlate900
val LfMuted = LfSlate500

// ==========================================
// Bento 2.0 Typography
// ==========================================
val LfTypography = Typography(
    headlineLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Black,
        fontSize = 26.sp,
        lineHeight = 32.sp,
        color = LfTextPrimary
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        color = LfTextPrimary
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        color = LfTextPrimary
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        color = LfTextPrimary
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        color = LfTextPrimary
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        color = LfTextSecondary
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Medium,
        fontSize = 11.sp,
        lineHeight = 15.sp,
        color = LfTextMuted
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.SansSerif,
        fontWeight = FontWeight.Bold,
        fontSize = 10.sp,
        lineHeight = 14.sp,
        color = LfEmerald600
    )
)

// ==========================================
// Bento 2.0 Shapes (Pills and Super-Ellipses)
// ==========================================
val LfShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

private val LiteraryFlow2Colors = lightColorScheme(
    primary = LfEmerald500,
    onPrimary = Color.White,
    primaryContainer = LfEmerald100,
    onPrimaryContainer = LfEmerald900,
    secondary = LfSky500,
    onSecondary = Color.White,
    secondaryContainer = LfSky100,
    onSecondaryContainer = LfSlate900,
    background = LfBackground,
    onBackground = LfTextPrimary,
    surface = LfCardBg,
    onSurface = LfTextPrimary,
    surfaceVariant = LfSlate50,
    onSurfaceVariant = LfSlate600,
    outline = LfBorder,
    error = LfRose500,
    onError = Color.White,
    errorContainer = LfRose100,
    onErrorContainer = LfRose900
)

@Composable
fun LiteraryFlow2Theme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LiteraryFlow2Colors,
        typography = LfTypography,
        shapes = LfShapes,
        content = content
    )
}
