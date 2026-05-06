package com.example.computerclub.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val ShapeSmall = RoundedCornerShape(8.dp)   // кнопки, чипы, поля
val ShapeMedium = RoundedCornerShape(12.dp) // диалоги, bottom sheet
val ShapeLarge = RoundedCornerShape(16.dp)  // карточки
val ShapeXL = RoundedCornerShape(20.dp)     // hero-блоки

val AppShapes = Shapes(
    small = ShapeSmall,
    medium = ShapeMedium,
    large = ShapeLarge,
)
