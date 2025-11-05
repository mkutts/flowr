package com.mdksolutions.flowr.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.imageResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.mdksolutions.flowr.R
import kotlin.math.max
import kotlin.math.min

@Composable
fun LeafRatingBar(
    rating: Double,
    onRatingChange: (Double) -> Unit,
    @DrawableRes leafResId: Int = R.drawable.cannabis_leaf, // your PNG
    modifier: Modifier = Modifier,
    maxLeaves: Int = 5,
    leafSize: Dp = 32.dp,
    leafSpacing: Dp = 8.dp,
    baseTint: Color = MaterialTheme.colorScheme.outline.copy(alpha = 0.55f),
    fillTint: Color = MaterialTheme.colorScheme.primary,
    readOnly: Boolean = false
) {
    val density = LocalDensity.current
    val leafPx = with(density) { leafSize.toPx() }
    val spacingPx = with(density) { leafSpacing.toPx() }
    val image = ImageBitmap.imageResource(id = leafResId)

    Row(
        modifier = modifier.then(
            if (!readOnly) {
                Modifier.pointerInput(maxLeaves) {
                    detectTapGestures { offset ->
                        val cell = leafPx + spacingPx
                        val idx = (offset.x / cell).toInt().coerceIn(0, maxLeaves - 1)
                        val withinLeafX = (offset.x - idx * cell).coerceIn(0f, leafPx)
                        val half = if (withinLeafX < leafPx / 2f) 0.5 else 1.0
                        onRatingChange(min(maxLeaves.toDouble(), max(0.5, idx + half)))
                    }
                }
            } else Modifier
        ),
        verticalAlignment = Alignment.CenterVertically
    ) {
        repeat(maxLeaves) { i ->
            val leafIndex = i + 1
            val fillFrac = (rating - (leafIndex - 1)).coerceIn(0.0, 1.0).toFloat() // 0..1

            Box(
                modifier = Modifier
                    .size(leafSize)
                    .clipToBounds(),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    // 1) Draw full leaf in base (empty) tint
                    drawImage(
                        image = image,
                        dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                        colorFilter = ColorFilter.tint(baseTint)
                    )

                    if (fillFrac > 0f) {
                        val fillW = size.width * fillFrac
                        val layerRect = Rect(0f, 0f, fillW, size.height)

                        // 2) Start a layer clipped to the fill area
                        val canvas = drawContext.canvas
                        canvas.saveLayer(layerRect, Paint())

                        // 3) Paint the fill color
                        drawRect(
                            color = fillTint,
                            size = Size(fillW, size.height)
                        )

                        // 4) Mask the fill by the leaf alpha using SrcIn
                        drawImage(
                            image = image,
                            dstSize = IntSize(size.width.toInt(), size.height.toInt()),
                            blendMode = BlendMode.SrcIn
                        )

                        // 5) End the layer
                        canvas.restore()
                    }
                }
            }
            if (i != maxLeaves - 1) Spacer(Modifier.width(leafSpacing))
        }
    }
}
