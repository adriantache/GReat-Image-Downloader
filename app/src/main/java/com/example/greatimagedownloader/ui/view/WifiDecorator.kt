package com.example.greatimagedownloader.ui.view

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.requiredHeight
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.greatimagedownloader.domain.utils.model.delay
import kotlinx.coroutines.launch

private const val ANIMATION_DURATION = 2400 // Must be a multiple of the content animation duration.
private const val DELAY_BETWEEN_ARCS = 600

@Composable
fun WifiDecorator(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    var isArcStackVisible by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(ANIMATION_DURATION + 2 * DELAY_BETWEEN_ARCS)
            isArcStackVisible = !isArcStackVisible
        }
    }

    Box(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = isArcStackVisible
        ) {
            Row(
                modifier = modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
            ) {
                ArcStack(
                    modifier = Modifier.weight(1f),
                    isReversed = true
                )

                Spacer(Modifier.weight(2f))

                ArcStack(
                    modifier = Modifier.weight(1f),
                )
            }
        }

        content()
    }
}

@Composable
fun ArcStack(
    modifier: Modifier = Modifier,
    isReversed: Boolean = false,
) {
    var isFirstArcVisible by remember { mutableStateOf(false) }
    var isSecondArcVisible by remember { mutableStateOf(false) }
    var isThirdArcVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        launch {
            isFirstArcVisible = true

            delay(DELAY_BETWEEN_ARCS)
            isSecondArcVisible = true

            delay(DELAY_BETWEEN_ARCS)
            isThirdArcVisible = true
        }
    }

    Box(
        modifier = modifier,
        contentAlignment = if (isReversed) Alignment.CenterEnd else Alignment.CenterStart,
    ) {
        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = isFirstArcVisible,
        ) {
            Arc(isReversed)
        }

        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = isSecondArcVisible,
        ) {
            Arc(isReversed)
        }

        AnimatedVisibility(
            enter = fadeIn(),
            exit = fadeOut(),
            visible = isThirdArcVisible,
        ) {
            Arc(isReversed)
        }
    }
}

@Composable
private fun Arc(
    isReversed: Boolean = false,
) {
    fun <T> getAnimationSpec() = tween<T>(
        durationMillis = ANIMATION_DURATION,
        easing = LinearEasing,
    )

    val size = 100.dp
    val offset = if (isReversed) 90.dp else (-90).dp
    val startAngle = -45f + if (isReversed) 180f else 0f

    var heightValue by remember { mutableStateOf(size) }
    val heightAnimation by animateDpAsState(
        targetValue = heightValue,
        animationSpec = getAnimationSpec(),
    )

    var offsetValue by remember { mutableStateOf(offset) }
    val offsetAnimation by animateDpAsState(
        targetValue = offsetValue,
        animationSpec = getAnimationSpec(),
    )

    var opacityValue by remember { mutableFloatStateOf(1f) }
    val opacityAnimation by animateFloatAsState(
        targetValue = opacityValue,
        animationSpec = getAnimationSpec(),
    )

    var startAngleValue by remember { mutableFloatStateOf(startAngle) }
    val startAngleAnimation by animateFloatAsState(
        targetValue = startAngleValue,
        animationSpec = getAnimationSpec(),
    )

    var sweepAngleValue by remember { mutableFloatStateOf(90f) }
    val sweepAngleAnimation by animateFloatAsState(
        targetValue = sweepAngleValue,
        animationSpec = getAnimationSpec(),
    )

    LaunchedEffect(Unit) {
        heightValue = size / 2
        offsetValue = 0.dp
        opacityValue = 0f
        startAngleValue = -30f + if (isReversed) 180f else 0f
        sweepAngleValue = 60f
    }

    Box(
        modifier = Modifier
            .requiredHeight(size)
            .alpha(opacityAnimation),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .height(heightAnimation)
                .aspectRatio(1f)
                .offset { IntOffset(x = offsetAnimation.roundToPx(), y = 0) },
        ) {
            val arcColor = MaterialTheme.colorScheme.primary

            Canvas(
                modifier = Modifier.fillMaxSize()
            ) {
                drawArc(
                    color = arcColor,
                    startAngle = startAngleAnimation,
                    sweepAngle = sweepAngleAnimation,
                    useCenter = false,
                    style = Stroke(
                        width = 10f,
                        cap = StrokeCap.Round,
                    ),
                    topLeft = Offset(
                        x = 0f,
                        y = 0f,
                    ),
                )
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun WifiDecoratorPreview() {
    WifiDecorator(
        modifier = Modifier.fillMaxWidth(),
        content = {
            Box(
                modifier = Modifier
                    .requiredSize(150.dp)
                    .background(MaterialTheme.colorScheme.secondary, CircleShape),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "TEST",
                    color = MaterialTheme.colorScheme.onSecondary,
                )
            }
        }
    )
}
