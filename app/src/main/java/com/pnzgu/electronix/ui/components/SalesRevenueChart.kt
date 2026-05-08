package com.pnzgu.electronix.ui.components

import android.graphics.Paint
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pnzgu.electronix.data.dto.SalesReportPointDto
import kotlin.math.ceil
import kotlin.math.floor
import kotlin.math.log10
import kotlin.math.max
import kotlin.math.pow
import java.time.Instant
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

private fun niceCeilY(v: Float): Float {
    if (v <= 0f) return 1f
    val exp = floor(log10(v.toDouble())).toFloat()
    val frac = v / 10f.pow(exp)
    val nf = when {
        frac <= 1f -> 1f
        frac <= 2f -> 2f
        frac <= 5f -> 5f
        else -> 10f
    }
    return nf * 10f.pow(exp)
}

private fun formatBucketLabel(iso: String, granularity: String): String {
    val instant = try {
        Instant.parse(iso)
    } catch (_: Exception) {
        return iso.take(12)
    }
    val z = instant.atZone(ZoneOffset.UTC)
    val fmt = if (granularity.equals("month", ignoreCase = true)) {
        DateTimeFormatter.ofPattern("LLLL yyyy", Locale("ru"))
    } else {
        DateTimeFormatter.ofPattern("d MMM", Locale("ru"))
    }
    return fmt.format(z)
}

/**
 * Line + area chart for revenue series; Y scale adapts to max revenue; X labels thinned when crowded.
 */
@Composable
fun SalesRevenueChart(
    series: List<SalesReportPointDto>,
    granularity: String,
    modifier: Modifier = Modifier,
) {
    val primary = MaterialTheme.colorScheme.primary
    val onBg = MaterialTheme.colorScheme.onSurface
    val muted = MaterialTheme.colorScheme.onSurfaceVariant
    val grid = MaterialTheme.colorScheme.outline.copy(alpha = 0.28f)

    if (series.isEmpty()) {
        Text(
            "—",
            style = MaterialTheme.typography.bodyMedium,
            color = muted,
            modifier = modifier.padding(16.dp),
        )
        return
    }

    val values = remember(series) { series.map { it.revenue.toFloat() } }
    val maxY = remember(values) { niceCeilY(values.maxOrNull() ?: 1f).coerceAtLeast(1f) }
    val yTicks = remember(maxY) {
        val n = 4
        (0..n).map { i -> maxY * i / n }
    }

    val labelIndices = remember(series.size) {
        val maxLabels = 9
        when {
            series.isEmpty() -> emptyList()
            series.size <= maxLabels -> series.indices.toList()
            else -> {
                val step = max(1, ceil(series.size / maxLabels.toDouble()).toInt())
                val idx = buildList {
                    var i = 0
                    while (i < series.size) {
                        add(i)
                        i += step
                    }
                }
                if (idx.last() != series.lastIndex) idx + listOf(series.lastIndex) else idx
            }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(220.dp),
        ) {
            val padL = 44.dp.toPx()
            val padR = 12.dp.toPx()
            val padT = 8.dp.toPx()
            val padB = 6.dp.toPx()
            val w = size.width
            val h = size.height
            val chartW = (w - padL - padR).coerceAtLeast(4f)
            val chartH = (h - padT - padB).coerceAtLeast(4f)

            // Horizontal grid + Y labels
            val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = muted.toArgb()
                textSize = 10.sp.toPx()
            }
            yTicks.forEach { tick ->
                val ty = padT + chartH - (tick / maxY) * chartH
                drawLine(
                    color = grid,
                    start = Offset(padL, ty),
                    end = Offset(w - padR, ty),
                    strokeWidth = 1.dp.toPx(),
                )
                val txt = if (tick >= 1000) {
                    "%.0fk".format(tick / 1000f)
                } else {
                    "%.0f".format(tick)
                }
                drawContext.canvas.nativeCanvas.drawText(txt, 4f, ty + 4f, labelPaint)
            }

            if (series.size < 2) {
                val x = padL + chartW / 2f
                val y0 = padT + chartH
                val y1 = padT + chartH - (values[0] / maxY) * chartH
                drawLine(
                    color = primary.copy(alpha = 0.5f),
                    start = Offset(x, y0),
                    end = Offset(x, y1),
                    strokeWidth = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                )
            } else {
                val pathLine = Path()
                val pathFill = Path()
                val n1 = (series.size - 1).coerceAtLeast(1)
                series.forEachIndexed { i, pt ->
                    val x = padL + (i.toFloat() / n1) * chartW
                    val v = pt.revenue.toFloat().coerceAtLeast(0f)
                    val y = padT + chartH - (v / maxY) * chartH
                    if (i == 0) {
                        pathLine.moveTo(x, y)
                        pathFill.moveTo(x, padT + chartH)
                        pathFill.lineTo(x, y)
                    } else {
                        pathLine.lineTo(x, y)
                        pathFill.lineTo(x, y)
                    }
                }
                pathFill.lineTo(padL + chartW, padT + chartH)
                pathFill.close()

                drawPath(
                    path = pathFill,
                    brush = Brush.verticalGradient(
                        0f to primary.copy(alpha = 0.22f),
                        1f to primary.copy(alpha = 0.02f),
                        startY = padT,
                        endY = padT + chartH,
                    ),
                )
                drawPath(
                    path = pathLine,
                    color = primary,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round),
                )
                series.forEachIndexed { i, pt ->
                    val x = padL + (i.toFloat() / n1) * chartW
                    val v = pt.revenue.toFloat().coerceAtLeast(0f)
                    val y = padT + chartH - (v / maxY) * chartH
                    drawCircle(color = Color.White, radius = 4.dp.toPx(), center = Offset(x, y))
                    drawCircle(color = primary, radius = 3.dp.toPx(), center = Offset(x, y))
                }
            }
        }

        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(top = 6.dp, start = 44.dp, end = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            labelIndices.forEach { idx ->
                Text(
                    formatBucketLabel(series[idx].periodStart, granularity),
                    style = MaterialTheme.typography.labelSmall,
                    color = onBg.copy(alpha = 0.75f),
                )
            }
        }
    }
}
