package dev.phonecode.app.ui.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween

object PhoneSprings {
    val standard get() = spring<Float>(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
    val quick get() = spring<Float>(dampingRatio = 1f, stiffness = 600f)

    val drawer get() = spring<Float>(dampingRatio = 1f, stiffness = 280f)

    fun <T> standardSpec() = spring<T>(dampingRatio = 1f, stiffness = Spring.StiffnessMediumLow)
    fun <T> emphasizedSpec() = spring<T>(dampingRatio = 0.92f, stiffness = Spring.StiffnessMediumLow)
    fun <T> quickSpec() = spring<T>(dampingRatio = 1f, stiffness = 600f)
}

object PhoneEasings {
    val iOSStandard = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)
}

object PhoneTweens {
    val popEnter get() = tween<Float>(durationMillis = 220, easing = PhoneEasings.iOSStandard)
    val popExit get() = tween<Float>(durationMillis = 150, easing = PhoneEasings.iOSStandard)
}
