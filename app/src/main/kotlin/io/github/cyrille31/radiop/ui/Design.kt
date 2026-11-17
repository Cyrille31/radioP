package io.github.cyrille31.radiop.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object Palette {
    val Night = Color(0xFF003A63)
    val Blue = Color(0xFF0074B8)
    val Band = Color(0xFF1E90FF)
    val Coral = Color(0xFFFF8A65)
    val Cyan = Color(0xFF56F0F8)
    val Sky = Color(0xFF9ED8F8)
    val Badge = Color(0xFF0B8FD9)
    val BadgeEdge = Color(0xFF0567AB)
    val Ink = Color(0xFF1B1B1F)
    val Paper = Color(0xFFF1F1F6)
    val Background = Brush.verticalGradient(0f to Night, 0.16f to Blue, 1f to Blue)
}

/** Icônes de l'appli (tracés Material, licence Apache 2.0). */
object AppIcons {
    private fun icon(vararg paths: String): ImageVector {
        val builder = ImageVector.Builder(
            defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f,
        )
        for (d in paths) builder.addPath(pathData = addPathNodes(d), fill = SolidColor(Color.Black))
        return builder.build()
    }

    val Menu = icon("M3,18h18v-2H3v2zm0-5h18v-2H3v2zm0-7v2h18V6H3z")
    val Play = icon("M8,5v14l11-7z")
    val Pause = icon("M6,19h4V5H6v14zm8-14v14h4V5h-4z")
    val Mail = icon("M20,4H4c-1.1,0-1.99,0.9-1.99,2L2,18c0,1.1 0.9,2 2,2h16c1.1,0 2-0.9 2-2V6c0-1.1-0.9-2-2-2zm0,4l-8,5-8-5V6l8,5 8-5v2z")
    val Close = icon("M19,6.41L17.59,5 12,10.59 6.41,5 5,6.41 10.59,12 5,17.59 6.41,19 12,13.41 17.59,19 19,17.59 13.41,12z")
    val Add = icon("M19,13h-6v6h-2v-6H5v-2h6V5h2v6h6v2z")
    val Check = icon("M9,16.17L4.83,12l-1.42,1.41L9,19 21,7l-1.41-1.41z")
    val Edit = icon("M3,17.25V21h3.75L17.81,9.94l-3.75-3.75L3,17.25zM20.71,7.04c0.39-0.39 0.39-1.02 0-1.41l-2.34-2.34c-0.39-0.39-1.02-0.39-1.41,0l-1.83,1.83 3.75,3.75 1.83-1.83z")
    val Delete = icon("M6,19c0,1.1 0.9,2 2,2h8c1.1,0 2-0.9 2-2V7H6v12zM19,4h-3.5l-1-1h-5l-1,1H5v2h14V4z")
    val Info = icon("M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10-4.48 10-10S17.52,2 12,2zm1,15h-2v-6h2v6zm0-8h-2V7h2v2z")
    val Heart = icon("M12,21.35l-1.45-1.32C5.4,15.36 2,12.28 2,8.5 2,5.42 4.42,3 7.5,3c1.74,0 3.41,0.81 4.5,2.09C13.09,3.81 14.76,3 16.5,3 19.58,3 22,5.42 22,8.5c0,3.78-3.4,6.86-8.55,11.54L12,21.35z")
    val Volume = icon("M3,9v6h4l5,5V4L7,9H3zm13.5,3c0-1.77-1.02-3.29-2.5-4.03v8.05c1.48-0.73 2.5-2.25 2.5-4.02zM14,3.23v2.06c2.89,0.86 5,3.54 5,6.71s-2.11,5.85-5,6.71v2.06c4.01-0.91 7-4.49 7-8.77s-2.99-7.86-7-8.77z")
    val Feed = icon(
        "M6.18,17.82m-2.18,0a2.18,2.18 0 1,0 4.36,0a2.18,2.18 0 1,0 -4.36,0",
        "M4,4.44v2.83c7.03,0 12.73,5.7 12.73,12.73h2.83c0-8.59-6.97-15.56-15.56-15.56zm0,5.66v2.83c3.9,0 7.07,3.17 7.07,7.07h2.83c0-5.47-4.43-9.9-9.9-9.9z",
    )
    val Clock = icon("M11.99,2C6.47,2 2,6.48 2,12s4.47,10 9.99,10C17.52,22 22,17.52 22,12S17.52,2 11.99,2zM12,20c-4.42,0-8-3.58-8-8s3.58-8 8-8 8,3.58 8,8-3.58,8-8,8zm0.5-13H11v6l5.25,3.15 0.75-1.23-4.5-2.67z")
    val Globe = icon("M12,2C6.48,2 2,6.48 2,12s4.48,10 10,10 10-4.48 10-10S17.52,2 12,2zm-1,17.93c-3.95-0.49-7-3.85-7-7.93 0-0.62 0.08-1.21 0.21-1.79L9,15v1c0,1.1 0.9,2 2,2v1.93zm6.9-2.54c-0.26-0.81-1-1.39-1.9-1.39h-1v-3c0-0.55-0.45-1-1-1H8v-2h2c0.55,0 1-0.45 1-1V7h2c1.1,0 2-0.9 2-2v-0.41c2.93,1.19 5,4.06 5,7.41 0,2.08-0.8,3.97-2.1,5.39z")
    val Radio = icon("M3.24,6.15C2.51,6.43 2,7.17 2,8v12c0,1.1 0.89,2 2,2h16c1.11,0 2-0.9 2-2V8c0-1.11-0.89-2-2-2H8.3l8.26-3.34L15.88,1 3.24,6.15zM7,20c-1.66,0-3-1.34-3-3s1.34-3 3-3 3,1.34 3,3-1.34,3-3,3zm13-8h-2v-2h-2v2H4V8h16v4z")
    val Moon = icon("M9,2c-1.05,0-2.05,0.16-3,0.46 4.06,1.27 7,5.06 7,9.54 0,4.48-2.94,8.27-7,9.54 0.95,0.3 1.95,0.46 3,0.46 5.52,0 10-4.48 10-10S14.52,2 9,2z")
}

/** Logo de l'appli : « radio » suivi d'un P dont la boucle émet des ondes. La taille ne suit pas le réglage de police. */
@Composable
fun Logo(height: Dp, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(height * 0.3f)
    val fontSize = with(LocalDensity.current) { (height * 0.42f).toSp() }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .rotate(-3f)
            .height(height)
            .background(Palette.Badge, shape)
            .border(height * 0.06f, Palette.BadgeEdge, shape)
            .padding(horizontal = height * 0.26f),
    ) {
        Text(
            "radio",
            color = Color(0xFFBFE6FF),
            fontSize = fontSize,
            fontStyle = FontStyle.Italic,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
        )
        Spacer(Modifier.width(height * 0.04f))
        Canvas(Modifier.size(width = height * 0.72f, height = height * 0.6f)) {
            val u = size.height / 64f
            val stroke = 13f * u
            drawLine(Color.White, Offset(stroke / 2, stroke / 2), Offset(stroke / 2, size.height - stroke / 2), stroke, StrokeCap.Round)
            val center = Offset(26f * u, 23.5f * u)
            drawCircle(Color.White, radius = 17f * u, center = center, style = Stroke(stroke))
            listOf(31f to 1f, 43f to 0.8f, 55f to 0.55f).forEach { (r, alpha) ->
                drawArc(
                    color = Palette.Coral.copy(alpha = alpha),
                    startAngle = -30f, sweepAngle = 60f, useCenter = false,
                    topLeft = Offset(center.x - r * u, center.y - r * u),
                    size = Size(2 * r * u, 2 * r * u),
                    style = Stroke(5.5f * u, cap = StrokeCap.Round),
                )
            }
        }
    }
}

/** Illustration de l'écran d'accueil : un ciel dessiné, propre à l'appli. */
@Composable
fun SkyArt(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val w = size.width
        val h = size.height
        drawRect(Brush.linearGradient(listOf(Color(0xFF8FC8F2), Color(0xFF3F8FD6), Color(0xFF2A6FB8)), Offset.Zero, Offset(w, h)))
        val sun = Offset(w * 0.22f, h * 0.30f)
        // Rayons.
        listOf(
            Offset(1f, 0.33f) to Offset(1f, 0.47f), Offset(1f, 0.62f) to Offset(1f, 0.78f),
            Offset(0.80f, 1f) to Offset(0.64f, 1f), Offset(0.42f, 1f) to Offset(0.30f, 1f),
            Offset(0.13f, 1f) to Offset(0.03f, 1f), Offset(1f, 0.06f) to Offset(1f, 0.16f),
            Offset(0.70f, 0f) to Offset(0.84f, 0f),
        ).forEach { (a, b) ->
            val ray = Path().apply {
                moveTo(sun.x, sun.y); lineTo(a.x * w, a.y * h); lineTo(b.x * w, b.y * h); close()
            }
            drawPath(ray, Color.White.copy(alpha = 0.16f))
        }
        // Soleil.
        drawCircle(
            Brush.radialGradient(
                0f to Color.White, 0.22f to Color.White.copy(alpha = 0.95f),
                0.6f to Color(0xFFDFF1FF).copy(alpha = 0.4f), 1f to Color.Transparent,
                center = sun, radius = w * 0.5f,
            ),
            radius = w * 0.5f, center = sun,
        )
        // Nuages : des boules floues.
        fun puff(x: Float, y: Float, r: Float, alpha: Float = 0.95f) {
            val c = Offset(x * w, y * h)
            drawCircle(
                Brush.radialGradient(
                    0f to Color.White.copy(alpha = alpha), 0.55f to Color.White.copy(alpha = alpha * 0.8f),
                    1f to Color.Transparent, center = c, radius = r * w,
                ),
                radius = r * w, center = c,
            )
        }
        puff(0.86f, 0.18f, 0.11f, 0.8f); puff(0.96f, 0.21f, 0.12f, 0.8f); puff(0.77f, 0.22f, 0.07f, 0.7f)
        puff(0.55f, 0.06f, 0.09f, 0.7f); puff(0.66f, 0.09f, 0.08f, 0.7f)
        puff(0.94f, 0.67f, 0.08f, 0.75f)
        puff(0.06f, 0.88f, 0.14f); puff(0.20f, 0.95f, 0.13f); puff(0.36f, 0.99f, 0.12f); puff(0.02f, 0.72f, 0.09f, 0.8f)
        puff(0.78f, 0.95f, 0.16f); puff(0.94f, 0.90f, 0.17f); puff(0.62f, 1.0f, 0.12f); puff(0.87f, 0.79f, 0.10f, 0.85f)
        puff(0.50f, 1.03f, 0.11f)
    }
}

/** Fait pivoter un contenu d'un quart de tour en adaptant la place qu'il occupe (texte vertical). */
fun Modifier.vertical(): Modifier =
    layout { measurable, constraints ->
        val placeable = measurable.measure(
            constraints.copy(
                minWidth = constraints.minHeight, maxWidth = constraints.maxHeight,
                minHeight = constraints.minWidth, maxHeight = constraints.maxWidth,
            )
        )
        layout(placeable.height, placeable.width) {
            placeable.place(-(placeable.width - placeable.height) / 2, (placeable.width - placeable.height) / 2)
        }
    }.rotate(-90f)
