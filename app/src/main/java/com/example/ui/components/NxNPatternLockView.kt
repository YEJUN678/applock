package com.example.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.ui.theme.NeonCyan
import com.example.ui.theme.NeonPurple
import com.example.ui.theme.NeonRed
import kotlin.math.hypot

@Composable
fun NxNPatternLockView(
    gridSize: Int, // 3 to 10
    isError: Boolean = false,
    enabled: Boolean = true,
    isStealthMode: Boolean = false,
    isVibrationEnabled: Boolean = true,
    onPatternCompleted: (List<Int>) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val vibrator = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
    }

    val selectedNodes = remember { mutableStateListOf<Int>() }
    var currentTouchPosition by remember { mutableStateOf<Offset?>(null) }

    val activeColor = if (isError) NeonRed else NeonCyan
    val glowColor = if (isError) Color(0xFFFF0055) else NeonPurple

    BoxWithConstraints(
        modifier = modifier
            .aspectRatio(1f)
            .padding(16.dp)
            .pointerInput(gridSize, enabled) {
                if (!enabled) return@pointerInput

                detectDragGestures(
                    onDragStart = { offset ->
                        selectedNodes.clear()
                        currentTouchPosition = offset
                        val node = findHitNode(offset, size.width.toFloat(), size.height.toFloat(), gridSize)
                        if (node != null) {
                            selectedNodes.add(node)
                            if (isVibrationEnabled) {
                                vibrateDevice(vibrator)
                            }
                        }
                    },
                    onDrag = { change, _ ->
                        change.consume()
                        val pos = change.position
                        currentTouchPosition = pos
                        val node = findHitNode(pos, size.width.toFloat(), size.height.toFloat(), gridSize)
                        if (node != null && !selectedNodes.contains(node)) {
                            selectedNodes.add(node)
                            if (isVibrationEnabled) {
                                vibrateDevice(vibrator)
                            }
                        }
                    },
                    onDragEnd = {
                        currentTouchPosition = null
                        if (selectedNodes.isNotEmpty()) {
                            onPatternCompleted(selectedNodes.toList())
                        }
                    },
                    onDragCancel = {
                        currentTouchPosition = null
                        selectedNodes.clear()
                    }
                )
            }
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val width = size.width
            val height = size.height
            val cellWidth = width / gridSize
            val cellHeight = height / gridSize
            val hitRadius = minOf(cellWidth, cellHeight) * 0.45f
            val baseDotRadius = minOf(cellWidth, cellHeight) * (if (gridSize <= 4) 0.12f else if (gridSize <= 6) 0.10f else 0.08f)

            // Draw connecting lines between selected nodes (unless stealth mode is active)
            if (!isStealthMode) {
                if (selectedNodes.size > 1) {
                    val path = Path()
                    for (i in 0 until selectedNodes.size) {
                        val node = selectedNodes[i]
                        val row = node / gridSize
                        val col = node % gridSize
                        val center = Offset(
                            x = col * cellWidth + cellWidth / 2,
                            y = row * cellHeight + cellHeight / 2
                        )
                        if (i == 0) {
                            path.moveTo(center.x, center.y)
                        } else {
                            path.lineTo(center.x, center.y)
                        }
                    }

                    // Line glow
                    drawPath(
                        path = path,
                        color = glowColor.copy(alpha = 0.35f),
                        style = Stroke(
                            width = minOf(cellWidth, cellHeight) * 0.25f,
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )

                    // Crisp active line
                    drawPath(
                        path = path,
                        brush = Brush.linearGradient(
                            colors = listOf(activeColor, glowColor)
                        ),
                        style = Stroke(
                            width = (minOf(cellWidth, cellHeight) * 0.08f).coerceAtLeast(3f),
                            cap = StrokeCap.Round,
                            join = StrokeJoin.Round
                        )
                    )
                }

                // Draw line to current touch point
                if (selectedNodes.isNotEmpty() && currentTouchPosition != null) {
                    val lastNode = selectedNodes.last()
                    val row = lastNode / gridSize
                    val col = lastNode % gridSize
                    val lastCenter = Offset(
                        x = col * cellWidth + cellWidth / 2,
                        y = row * cellHeight + cellHeight / 2
                    )
                    drawLine(
                        color = activeColor.copy(alpha = 0.7f),
                        start = lastCenter,
                        end = currentTouchPosition!!,
                        strokeWidth = (minOf(cellWidth, cellHeight) * 0.06f).coerceAtLeast(2.5f),
                        cap = StrokeCap.Round
                    )
                }
            }

            // Draw all grid nodes
            for (r in 0 until gridSize) {
                for (c in 0 until gridSize) {
                    val index = r * gridSize + c
                    val center = Offset(
                        x = c * cellWidth + cellWidth / 2,
                        y = r * cellHeight + cellHeight / 2
                    )
                    val isSelected = selectedNodes.contains(index)

                    if (isSelected) {
                        // Outer halo
                        drawCircle(
                            color = glowColor.copy(alpha = 0.25f),
                            radius = hitRadius * 0.9f,
                            center = center
                        )
                        // Middle ring
                        drawCircle(
                            color = activeColor,
                            radius = hitRadius * 0.65f,
                            center = center,
                            style = Stroke(width = (baseDotRadius * 0.4f).coerceAtLeast(2f))
                        )
                        // Core center point
                        drawCircle(
                            color = Color.White,
                            radius = baseDotRadius * 1.1f,
                            center = center
                        )
                    } else {
                        // Subtle grid placeholder dot
                        drawCircle(
                            color = Color(0x3394A3B8),
                            radius = hitRadius * 0.4f,
                            center = center,
                            style = Stroke(width = 1.5f)
                        )
                        drawCircle(
                            color = Color(0x8894A3B8),
                            radius = baseDotRadius * 0.6f,
                            center = center
                        )
                    }
                }
            }
        }
    }
}

private fun findHitNode(touch: Offset, width: Float, height: Float, gridSize: Int): Int? {
    val cellWidth = width / gridSize
    val cellHeight = height / gridSize
    val hitRadius = minOf(cellWidth, cellHeight) * 0.45f

    for (r in 0 until gridSize) {
        for (c in 0 until gridSize) {
            val centerX = c * cellWidth + cellWidth / 2
            val centerY = r * cellHeight + cellHeight / 2
            val dist = hypot(touch.x - centerX, touch.y - centerY)
            if (dist <= hitRadius) {
                return r * gridSize + c
            }
        }
    }
    return null
}

private fun vibrateDevice(vibrator: Vibrator) {
    try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(20, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(20)
        }
    } catch (_: Exception) {
    }
}
