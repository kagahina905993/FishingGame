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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.roundToInt
import kotlin.math.sin

@Composable
fun StreamDiorama(
    modifier: Modifier = Modifier,
    underwater: Boolean = false,
    showBobber: Boolean = false,
    showFish: Boolean = false,
    showHook: Boolean = false,
    fish: Fish? = null,
    fishApproachProgress: Float = 1f,
    fishNibbleProgress: Float = 0f,
    fishHookedProgress: Float = 0f
) {
    val hookBitmap = if (showHook) {
        ImageBitmap.imageResource(R.drawable.fishing_hook)
    } else {
        null
    }
    val fishBitmap = fish
        ?.let(::fishArtworkResources)
        ?.firstOrNull()
        ?.let { ImageBitmap.imageResource(it) }

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

            if (underwater) {
                drawRect(
                    color = Color(0xFF176D83),
                    size = Size(width, height)
                )
                drawRect(
                    color = Color(0xFF49AFC0),
                    size = Size(width, height * 0.22f)
                )
                drawRect(
                    color = Color(0xFF24899E),
                    topLeft = Offset(0f, height * 0.22f),
                    size = Size(width, height * 0.34f)
                )

                repeat(5) { index ->
                    val startX = width * (0.04f + index * 0.23f)
                    val lightPath = Path().apply {
                        moveTo(startX, 0f)
                        lineTo(startX + width * 0.10f, 0f)
                        lineTo(startX + width * 0.22f, height * 0.72f)
                        lineTo(startX + width * 0.12f, height * 0.72f)
                        close()
                    }
                    drawPath(
                        path = lightPath,
                        color = Color.White.copy(
                            alpha = if (index % 2 == 0) 0.10f else 0.06f
                        )
                    )
                }

                repeat(4) { index ->
                    val waveY = height * (0.08f + index * 0.055f)
                    drawLine(
                        color = Color(0xFFB9F1EE).copy(alpha = 0.42f),
                        start = Offset(width * (0.04f + index * 0.08f), waveY),
                        end = Offset(width * (0.44f + index * 0.12f), waveY),
                        strokeWidth = 2.dp.toPx()
                    )
                }

                val riverbed = Path().apply {
                    moveTo(0f, height * 0.82f)
                    lineTo(width * 0.22f, height * 0.77f)
                    lineTo(width * 0.48f, height * 0.84f)
                    lineTo(width * 0.73f, height * 0.76f)
                    lineTo(width, height * 0.81f)
                    lineTo(width, height)
                    lineTo(0f, height)
                    close()
                }
                drawPath(riverbed, Color(0xFF84755B))
                drawRect(
                    color = Color(0xFFA99A78),
                    topLeft = Offset(0f, height * 0.82f),
                    size = Size(width, 3.dp.toPx())
                )

                val underwaterRocks = listOf(
                    Offset(width * 0.10f, height * 0.86f),
                    Offset(width * 0.31f, height * 0.91f),
                    Offset(width * 0.68f, height * 0.88f),
                    Offset(width * 0.88f, height * 0.93f)
                )
                underwaterRocks.forEachIndexed { index, center ->
                    val rockWidth = (16 + index * 3).dp.toPx()
                    drawOval(
                        color = if (index % 2 == 0) {
                            Color(0xFF526C69)
                        } else {
                            Color(0xFF687A70)
                        },
                        topLeft = Offset(
                            center.x - rockWidth / 2f,
                            center.y - rockWidth * 0.32f
                        ),
                        size = Size(rockWidth, rockWidth * 0.64f)
                    )
                }

                listOf(
                    Offset(width * 0.16f, height * 0.48f),
                    Offset(width * 0.81f, height * 0.35f),
                    Offset(width * 0.89f, height * 0.62f)
                ).forEachIndexed { index, center ->
                    repeat(3) { bubbleIndex ->
                        val radius = (2 + bubbleIndex).dp.toPx()
                        drawCircle(
                            color = Color(0xFFC9F5F2).copy(alpha = 0.55f),
                            radius = radius,
                            center = Offset(
                                center.x + bubbleIndex * 8.dp.toPx(),
                                center.y -
                                    (bubbleIndex * 13 + index * 3).dp.toPx()
                            )
                        )
                    }
                }
            } else {
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
            }

            val hookedProgress = fishHookedProgress.coerceIn(0f, 1f)
            val bobberDrop = 10.dp.toPx() * hookedProgress
            if (showBobber) {
                drawRect(
                    color = Color.White,
                    topLeft = Offset(
                        width * 0.57f,
                        height * 0.64f + bobberDrop
                    ),
                    size = Size(6f, 13f)
                )
                drawRect(
                    color = Color(0xFFE53935),
                    topLeft = Offset(
                        width * 0.57f,
                        height * 0.61f + bobberDrop
                    ),
                    size = Size(6f, 7f)
                )
            }

            hookBitmap?.let { image ->
                val hookWidth = 48.dp.toPx()
                val hookHeight = 32.dp.toPx()
                val hookCenterX = width * 0.575f -
                    52.dp.toPx() * hookedProgress
                val hookTopRatio = if (underwater) 0.52f else 0.69f
                val hookTop = height * hookTopRatio +
                    12.dp.toPx() * hookedProgress
                drawLine(
                    color = Color(0xFF263238),
                    start = if (underwater) {
                        Offset(x = width * 0.575f, y = 0f)
                    } else {
                        Offset(
                            x = width * 0.575f,
                            y = height * 0.67f + bobberDrop
                        )
                    },
                    end = Offset(
                        x = hookCenterX,
                        y = hookTop + 4.dp.toPx()
                    ),
                    strokeWidth = 1.dp.toPx()
                )
                drawImage(
                    image = image,
                    dstOffset = IntOffset(
                        x = (hookCenterX - hookWidth / 2f).roundToInt(),
                        y = hookTop.roundToInt()
                    ),
                    dstSize = IntSize(
                        width = hookWidth.roundToInt(),
                        height = hookHeight.roundToInt()
                    ),
                    filterQuality = FilterQuality.None
                )
            }

            if (showFish && fishBitmap != null) {
                val approachProgress = fishApproachProgress
                    .coerceIn(0f, 1f)
                val fishWidth = 96.dp.toPx()
                val fishHeight = 64.dp.toPx()
                val restingHookX = width * 0.575f
                val startLeft = width + 8.dp.toPx()
                val targetLeft = restingHookX - fishWidth * 0.12f
                val nibbleOffset = sin(
                    fishNibbleProgress.coerceIn(0f, 1f) *
                        PI.toFloat() * 6f
                ) * 4.dp.toPx()
                val fishLeft = startLeft +
                    (targetLeft - startLeft) * approachProgress +
                    nibbleOffset - 52.dp.toPx() * hookedProgress
                val fishTopRatio = if (underwater) 0.52f else 0.69f
                val fishTop = height * fishTopRatio - 12.dp.toPx() +
                    12.dp.toPx() * hookedProgress
                drawImage(
                    image = fishBitmap,
                    dstOffset = IntOffset(
                        x = fishLeft.roundToInt(),
                        y = fishTop.roundToInt()
                    ),
                    dstSize = IntSize(
                        width = fishWidth.roundToInt(),
                        height = fishHeight.roundToInt()
                    ),
                    filterQuality = FilterQuality.None
                )
            } else if (showFish) {
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
