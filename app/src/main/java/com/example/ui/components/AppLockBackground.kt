package com.example.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.R
import com.example.model.BackgroundTheme
import com.example.ui.theme.CyberBgDark

@Composable
fun AppLockBackground(
    theme: BackgroundTheme,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier.fillMaxSize()) {
        when (theme) {
            BackgroundTheme.CYBER_WALLPAPER -> {
                Image(
                    painter = painterResource(id = R.drawable.bg_cyber_lock),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark overlay to ensure contrast
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    Color(0xCC090D16),
                                    Color(0xE60D1527),
                                    Color(0xF2090D16)
                                )
                            )
                        )
                )
            }
            BackgroundTheme.DEEP_SPACE -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFF1E1B4B),
                                    Color(0xFF0F172A),
                                    Color(0xFF020617)
                                )
                            )
                        )
                )
            }
            BackgroundTheme.NEON_NIGHT -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    Color(0xFF2E0854),
                                    Color(0xFF091E3A),
                                    Color(0xFF050510)
                                )
                            )
                        )
                )
            }
            BackgroundTheme.MINIMAL_STEALTH -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(CyberBgDark)
                )
            }
        }

        content()
    }
}
