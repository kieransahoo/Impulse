package com.impulse.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val Typography = Typography(
    displaySmall = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 40.sp,
        lineHeight = 44.sp,
        letterSpacing = (-1.2).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Serif,
        fontWeight = FontWeight.Medium,
        fontSize = 30.sp,
        lineHeight = 36.sp
    ),
    titleLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp
    ),
    bodyLarge = TextStyle(fontSize = 18.sp, lineHeight = 28.sp),
    bodyMedium = TextStyle(fontSize = 16.sp, lineHeight = 24.sp),
    bodySmall = TextStyle(fontSize = 14.sp, lineHeight = 20.sp),
    labelLarge = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 14.sp,
        lineHeight = 20.sp
    ),
    labelSmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 11.sp,
        lineHeight = 16.sp,
        letterSpacing = 1.1.sp
    )
)
