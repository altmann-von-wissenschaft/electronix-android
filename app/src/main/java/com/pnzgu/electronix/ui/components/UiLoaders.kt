package com.pnzgu.electronix.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.WifiOff
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.foundation.Canvas
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pnzgu.electronix.R

/** Minimal dual-ring loader; uses theme primary / surface so it works in light and dark mode. */
@Composable
fun ElectronixCenteredLoader(modifier: Modifier = Modifier) {
    Box(modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        ElectronixLoaderRing(modifier = Modifier.size(52.dp))
    }
}

@Composable
fun ElectronixLoaderRing(modifier: Modifier = Modifier) {
    val scheme = MaterialTheme.colorScheme
    val transition = rememberInfiniteTransition(label = "electronix_load")
    val rotation by transition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 1100, easing = LinearEasing),
        ),
        label = "spin",
    )
    val breathe by transition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 900, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "breathe",
    )
    Canvas(modifier.rotate(rotation)) {
        val stroke = 3.5.dp.toPx()
        val r = (size.minDimension / 2f - stroke) * breathe
        val c = androidx.compose.ui.geometry.Offset(size.width / 2f, size.height / 2f)
        drawCircle(
            color = scheme.primary.copy(alpha = 0.14f),
            radius = r,
            center = c,
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawArc(
            color = scheme.primary,
            startAngle = -90f,
            sweepAngle = 110f,
            useCenter = false,
            topLeft = androidx.compose.ui.geometry.Offset(c.x - r, c.y - r),
            size = androidx.compose.ui.geometry.Size(r * 2, r * 2),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
    }
}

@Composable
fun ElectronixInlineError(
    message: String,
    onRetry: (() -> Unit)?,
    modifier: Modifier = Modifier,
) {
    val scheme = MaterialTheme.colorScheme
    val localizedMessage = localizeCommonNetworkError(message)
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = scheme.errorContainer.copy(alpha = 0.35f),
        ),
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.WifiOff,
                contentDescription = null,
                modifier = Modifier.size(36.dp),
                tint = scheme.error,
            )
            Text(
                text = localizedMessage,
                style = MaterialTheme.typography.bodyMedium,
                color = scheme.onErrorContainer,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
            )
            if (onRetry != null) {
                Button(onClick = onRetry, shape = RoundedCornerShape(14.dp)) {
                    Text(text = stringResource(R.string.retry))
                }
            }
        }
    }
}

private fun localizeCommonNetworkError(raw: String): String {
    val lower = raw.lowercase()
    return when {
        "timeout" in lower || "timed out" in lower ->
            "Превышено время ожидания ответа сервера. Попробуйте еще раз."
        "unable to resolve host" in lower || "unknownhostexception" in lower || "no address associated with hostname" in lower ->
            "Нет подключения к интернету или недоступен сервер."
        "failed to connect" in lower || "connectexception" in lower ->
            "Не удалось подключиться к серверу. Проверьте соединение."
        "socket closed" in lower || "socketexception" in lower ->
            "Соединение было прервано. Попробуйте снова."
        "sslhandshakeexception" in lower || "certpath" in lower || "certificate" in lower ->
            "Ошибка защищенного соединения (SSL)."
        "http 401" in lower || "unauthorized" in lower ->
            "Требуется повторный вход в аккаунт."
        "http 403" in lower || "forbidden" in lower ->
            "Недостаточно прав для выполнения операции."
        "http 404" in lower || "not found" in lower ->
            "Запрошенные данные не найдены."
        "http 500" in lower || "internal server error" in lower ->
            "Внутренняя ошибка сервера. Попробуйте позже."
        "http 502" in lower || "http 503" in lower || "http 504" in lower || "bad gateway" in lower || "service unavailable" in lower ->
            "Сервер временно недоступен. Попробуйте позже."
        else -> raw
    }
}
