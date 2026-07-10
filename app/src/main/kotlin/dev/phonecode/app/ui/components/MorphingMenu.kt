package dev.phonecode.app.ui.components

import androidx.compose.animation.core.MutableTransitionState
import androidx.compose.animation.core.animateDp
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.rememberTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntRect
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupPositionProvider
import androidx.compose.ui.window.PopupProperties
import dev.phonecode.app.ui.theme.PhoneEasings

@Composable
fun MorphingMenu(
    expanded: Boolean,
    onDismiss: () -> Unit,
    above: Boolean,
    alignEnd: Boolean = false,
    anchorSize: Dp = 40.dp,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val density = LocalDensity.current
    val margin = with(density) { 8.dp.roundToPx() }
    val anchorPixels = with(density) { anchorSize.toPx() }
    val positionProvider = remember(above, alignEnd, margin) {
        object : PopupPositionProvider {
            override fun calculatePosition(
                anchorBounds: IntRect,
                windowSize: IntSize,
                layoutDirection: LayoutDirection,
                popupContentSize: IntSize,
            ): IntOffset {
                val rawX = if (alignEnd) anchorBounds.right - popupContentSize.width else anchorBounds.left
                val maxX = (windowSize.width - popupContentSize.width - margin).coerceAtLeast(margin)
                val rawY = if (above) anchorBounds.bottom - popupContentSize.height else anchorBounds.top
                val maxY = (windowSize.height - popupContentSize.height - margin).coerceAtLeast(margin)
                return IntOffset(rawX.coerceIn(margin, maxX), rawY.coerceIn(margin, maxY))
            }
        }
    }
    val state = remember { MutableTransitionState(false) }
    LaunchedEffect(expanded) { state.targetState = expanded }
    if (expanded || state.currentState || !state.isIdle) {
        val transition = rememberTransition(state, label = "morphingMenu")
        val progress by transition.animateFloat(
            transitionSpec = {
                if (targetState) tween(200, easing = PhoneEasings.iOSStandard)
                else tween(150, easing = PhoneEasings.emphasizedAccelerate)
            },
            label = "menuProgress",
        ) { if (it) 1f else 0f }
        val corner by transition.animateDp(
            transitionSpec = {
                if (targetState) tween(200, easing = PhoneEasings.iOSStandard)
                else tween(150, easing = PhoneEasings.emphasizedAccelerate)
            },
            label = "menuCorner",
        ) { if (it) 28.dp else 160.dp }
        val origin = TransformOrigin(if (alignEnd) 1f else 0f, if (above) 1f else 0f)
        val contentProgress = ((progress - 0.48f) / 0.52f).coerceIn(0f, 1f)
        Popup(
            popupPositionProvider = positionProvider,
            onDismissRequest = onDismiss,
            properties = PopupProperties(focusable = true),
        ) {
            Box(modifier) {
                Surface(
                    modifier = Modifier.matchParentSize().graphicsLayer {
                        transformOrigin = origin
                        scaleX = if (size.width > 0f) anchorPixels / size.width + (1f - anchorPixels / size.width) * progress else 1f
                        scaleY = if (size.height > 0f) anchorPixels / size.height + (1f - anchorPixels / size.height) * progress else 1f
                    },
                    shape = RoundedCornerShape(corner),
                    color = MaterialTheme.colorScheme.surfaceContainerHigh,
                    tonalElevation = 6.dp,
                    shadowElevation = 12.dp,
                ) {}
                Column(
                    Modifier.clip(RoundedCornerShape(28.dp)).graphicsLayer {
                        alpha = contentProgress
                        translationY = (1f - contentProgress) * if (above) 8.dp.toPx() else (-8).dp.toPx()
                    },
                    content = content,
                )
            }
        }
    }
}
