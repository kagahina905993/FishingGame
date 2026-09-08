package com.example.fishinggame

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.unit.dp

@Composable
fun StreamDiorama(
    modifier: Modifier = Modifier,
    showBobber: Boolean = false,
    showFish: Boolean = false
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(230.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(Color(0xFF9DD7E8))
    ) {
        Canvas(modifier = Modifier.matchParentSize()) {
            val width = size.width
            val height = size.height

            drawRect(
                color = Color(0xFFBDE5EF),
                size = Size(width, height * 0.38f)
            )

            fun mountain(
                startX: Float,
                peakX: Float,
                endX: Float,
                peakY: Float,
                color: Color
            ) {
                val path = Path().apply {
                    moveTo(startX, height * 0.42f)
                    lineTo(peakX, peakY)
                    lineTo(endX, height * 0.42f)
                    close()
                }
                drawPath(path, color)
            }

            mountain(-width * 0.1f, width * 0.18f, width * 0.5f, 8f,
                Color(0xFF4E805B))
            mountain(width * 0.2f, width * 0.55f, width * 0.9f, 18f,
                Color(0xFF3F704E))
            mountain(width * 0.55f, width * 0.82f, width * 1.1f, 4f,
                Color(0xFF315E43))

            drawRect(
                color = Color(0xFF315E43),
                topLeft = Offset(0f, height * 0.34f),
                size = Size(width, height * 0.18f)
            )

            val waterPath = Path().apply {
                moveTo(width * 0.39f, height * 0.38f)
                lineTo(width * 0.61f, height * 0.38f)
                lineTo(width * 0.88f, height)
                lineTo(width * 0.12f, height)
                close()
            }
            drawPath(waterPath, Color(0xFF43A9B7))

            val leftBank = Path().apply {
                moveTo(0f, height * 0.45f)
                lineTo(width * 0.39f, height * 0.38f)
                lineTo(width * 0.12f, height)
                lineTo(0f, height)
                close()
            }
            drawPath(leftBank, Color(0xFF6C8B47))

            val rightBank = Path().apply {
                moveTo(width, height * 0.45f)
                lineTo(width * 0.61f, height * 0.38f)
                lineTo(width * 0.88f, height)
                lineTo(width, height)
                close()
            }
            drawPath(rightBank, Color(0xFF58783D))

            repeat(7) { index ->
                val y = height * (0.48f + index * 0.075f)
                val halfWidth = width * (0.10f + index * 0.035f)
                drawRect(
                    color = Color.White.copy(alpha = 0.38f),
                    topLeft = Offset(width / 2f - halfWidth, y),
                    size = Size(halfWidth * 2f, 3f)
                )
            }

            val rocks = listOf(
                Offset(width * 0.28f, height * 0.73f),
                Offset(width * 0.70f, height * 0.62f),
                Offset(width * 0.43f, height * 0.88f),
                Offset(width * 0.79f, height * 0.85f)
            )
            rocks.forEachIndexed { index, center ->
                val rockSize = 13f + index * 2f
                drawRect(
                    color = Color(0xFF6B7470),
                    topLeft = Offset(center.x - rockSize, center.y - rockSize),
                    size = Size(rockSize * 2f, rockSize * 1.45f)
                )
                drawRect(
                    color = Color(0xFF9AA39B),
                    topLeft = Offset(center.x - rockSize, center.y - rockSize),
                    size = Size(rockSize * 1.1f, 4f)
                )
            }

            fun tree(x: Float, y: Float, scale: Float) {
                drawRect(
                    color = Color(0xFF59462F),
                    topLeft = Offset(x - 3f * scale, y),
                    size = Size(6f * scale, 25f * scale)
                )
                val foliage = Path().apply {
                    moveTo(x, y - 31f * scale)
                    lineTo(x - 17f * scale, y + 7f * scale)
                    lineTo(x + 17f * scale, y + 7f * scale)
                    close()
                }
                drawPath(foliage, Color(0xFF24583A))
                drawRect(
                    color = Color(0xFF3E7A49),
                    topLeft = Offset(x - 10f * scale, y - 10f * scale),
                    size = Size(12f * scale, 5f * scale)
                )
            }

            tree(width * 0.10f, height * 0.39f, 1.2f)
            tree(width * 0.20f, height * 0.43f, 0.9f)
            tree(width * 0.88f, height * 0.39f, 1.15f)
            tree(width * 0.77f, height * 0.44f, 0.82f)

            drawRect(
                color = Color(0xFF705039),
                topLeft = Offset(width * 0.30f, height * 0.40f),
                size = Size(width * 0.40f, 10f)
            )
            drawRect(
                color = Color(0xFFA47A4E),
                topLeft = Offset(width * 0.32f, height * 0.39f),
                size = Size(width * 0.36f, 4f)
            )

            if (showBobber) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(width * 0.57f, height * 0.64f),
                    size = Size(6f, 13f)
                )
                drawRect(
                    color = Color(0xFFE53935),
                    topLeft = Offset(width * 0.57f, height * 0.61f),
                    size = Size(6f, 7f)
                )
            }

            if (showFish) {
                val fishX = width * 0.52f
                val fishY = height * 0.79f
                drawOval(
                    color = Color(0xFF183D45),
                    topLeft = Offset(fishX - 20f, fishY - 8f),
                    size = Size(40f, 16f)
                )
                val tail = Path().apply {
                    moveTo(fishX + 17f, fishY)
                    lineTo(fishX + 29f, fishY - 10f)
                    lineTo(fishX + 29f, fishY + 10f)
                    close()
                }
                drawPath(tail, Color(0xFF183D45))
            }
        }
    }
}
