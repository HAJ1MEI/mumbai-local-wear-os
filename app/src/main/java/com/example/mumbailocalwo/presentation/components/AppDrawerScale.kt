package com.example.mumbailocalwo.presentation.components

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.findRootCoordinates
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import kotlin.math.abs

/**
 * AppDrawerScale: Dynamic decremental scaling matching Wear OS app drawer.
 * Keeps cards at their slim natural height while smoothly enlarging the center item
 * and scaling down items above and below in a decremental fashion along the circular screen curve.
 */
fun Modifier.appDrawerScale(): Modifier = composed {
    var itemCenterY by remember { mutableFloatStateOf(0f) }
    var rootCenterY by remember { mutableFloatStateOf(0f) }

    this
        .onGloballyPositioned { coordinates ->
            val root = coordinates.findRootCoordinates()
            rootCenterY = root.size.height / 2f
            val posInRoot = coordinates.positionInRoot()
            itemCenterY = posInRoot.y + (coordinates.size.height / 2f)
        }
        .graphicsLayer {
            if (rootCenterY > 0f && itemCenterY > 0f) {
                val dist = abs(itemCenterY - rootCenterY)
                val maxDist = rootCenterY * 0.95f
                val fraction = (dist / maxDist).coerceIn(0f, 1f)
                val scale = 1.0f - (0.18f * fraction * fraction)
                scaleX = scale
                scaleY = scale
                alpha = 1.0f - (0.28f * fraction)
            }
        }
}
