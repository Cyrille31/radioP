package io.github.cyrille31.radiop.ui

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.SystemClock
import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import io.github.cyrille31.radiop.SleepTimer
import io.github.cyrille31.radiop.Station
import io.github.cyrille31.radiop.Stations
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

const val CONTACT_PHONE = "0562486300"
const val CONTACT_EMAIL = "contact@radiopresence.com"

@Composable
fun RadioScreen(
    station: Station,
    playing: Boolean,
    loading: Boolean,
    playerTitle: String?,
    onToggle: () -> Unit,
) {
    var showContact by remember { mutableStateOf(false) }
    var showTimer by remember { mutableStateOf(false) }
    val serverTitle = rememberServerTitle(station)
    val title = playerTitle ?: serverTitle

    Column(Modifier.fillMaxSize()) {
        BoxWithConstraints(
            contentAlignment = Alignment.Center,
            modifier = Modifier.weight(1f).fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        ) {
            val side = min(maxWidth, maxHeight - 38.dp)
            Box(Modifier.size(width = side, height = side + 38.dp)) {
                val shape = RoundedCornerShape(side * 0.12f)
                SkyArt(
                    Modifier.size(side).shadow(8.dp, shape).clip(shape).border(8.dp, Color.White, shape)
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .size(76.dp)
                        .shadow(6.dp, CircleShape)
                        .clip(CircleShape)
                        .background(Palette.Sky)
                        .border(4.dp, Color.White, CircleShape)
                        .clickable(role = Role.Button, onClickLabel = if (playing) "Pause" else "Lecture", onClick = onToggle),
                ) {
                    if (loading) {
                        CircularProgressIndicator(Modifier.size(34.dp), color = Color.White, strokeWidth = 3.dp)
                    } else {
                        Icon(
                            if (playing) AppIcons.Pause else AppIcons.Play,
                            contentDescription = if (playing) "Pause" else "Lecture",
                            tint = Color.White,
                            modifier = Modifier.size(44.dp),
                        )
                    }
                }
            }
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth().background(Palette.Band).padding(horizontal = 20.dp, vertical = 26.dp),
        ) {
            Text(
                station.name, color = Palette.Coral, fontSize = 26.sp, fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
            )
            Text(
                title ?: " ", color = Color.White, fontSize = 21.sp, textAlign = TextAlign.Center,
                maxLines = 2, overflow = TextOverflow.Ellipsis, modifier = Modifier.padding(top = 4.dp),
            )
        }

        Box(Modifier.fillMaxWidth().navigationBarsPadding().height(84.dp).padding(horizontal = 28.dp)) {
            Icon(
                AppIcons.Mail, contentDescription = "Contacter la radio", tint = Color.White,
                modifier = Modifier.align(Alignment.Center).clip(CircleShape)
                    .clickable(role = Role.Button) { showContact = true }.padding(10.dp).size(36.dp),
            )
            SleepButton(Modifier.align(Alignment.CenterEnd)) { showTimer = true }
        }
    }

    if (showContact) ContactDialog { showContact = false }
    if (showTimer) SleepTimerSheet { showTimer = false }
}

/**
 * Titre de l'émission lu sur la page d'état du serveur de flux, pour l'afficher même quand
 * la radio ne joue pas (en lecture, c'est le titre porté par le flux lui-même qui prime).
 */
@Composable
private fun rememberServerTitle(station: Station): String? {
    var title by remember(station.id) { mutableStateOf<String?>(null) }
    LaunchedEffect(station.id) {
        while (true) {
            title = withContext(Dispatchers.IO) { fetchServerTitle(station) }
            delay(30_000)
        }
    }
    return title
}

private fun fetchServerTitle(station: Station): String? = try {
    val connection = URL(Stations.STATUS_URL).openConnection() as HttpURLConnection
    connection.connectTimeout = 8_000
    connection.readTimeout = 8_000
    try {
        val text = connection.inputStream.bufferedReader().use { it.readText() }
        val sources = JSONObject(text).getJSONObject("icestats").optJSONArray("source")
        var found: String? = null
        if (sources != null) {
            for (i in 0 until sources.length()) {
                val source = sources.optJSONObject(i) ?: continue
                if (source.optString("listenurl").endsWith("/" + station.mount)) {
                    found = Stations.cleanTitle(if (source.isNull("title")) null else source.optString("title"))
                }
            }
        }
        found
    } finally {
        connection.disconnect()
    }
} catch (e: Exception) {
    null
}

@Composable
private fun SleepButton(modifier: Modifier, onClick: () -> Unit) {
    val remaining = sleepMinutesLeft()
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(60.dp).clip(CircleShape).background(Color.White.copy(alpha = 0.22f))
            .clickable(role = Role.Button, onClickLabel = "Minuteur de veille", onClick = onClick),
    ) {
        if (remaining == null) {
            Icon(AppIcons.Moon, contentDescription = "Minuteur de veille", tint = Color.White, modifier = Modifier.size(34.dp))
        } else {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(AppIcons.Moon, contentDescription = "Minuteur de veille", tint = Color.White, modifier = Modifier.size(22.dp))
                Text("$remaining min", color = Color.White, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

/** Minutes restantes avant l'arrêt, arrondies au-dessus ; `null` sans minuteur. */
@Composable
private fun sleepMinutesLeft(): Long? {
    val endAt = SleepTimer.endAt ?: return null
    var now by remember { mutableLongStateOf(SystemClock.elapsedRealtime()) }
    LaunchedEffect(endAt) {
        while (true) {
            now = SystemClock.elapsedRealtime()
            delay(5_000)
        }
    }
    return ((endAt - now + 59_999) / 60_000).coerceAtLeast(1)
}

@Composable
internal fun ContactDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    Dialog(onDismissRequest = onDismiss) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(28.dp)).background(Palette.Paper),
        ) {
            Text(
                "Contacter par…", color = Palette.Ink, fontSize = 20.sp, fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(vertical = 20.dp),
            )
            listOf(
                "Téléphone" to Intent(Intent.ACTION_DIAL, Uri.parse("tel:$CONTACT_PHONE")),
                "Email" to Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$CONTACT_EMAIL")),
            ).forEach { (label, intent) ->
                HorizontalDivider(color = Color(0xFFD0D0D8))
                Text(
                    label, color = Palette.Ink, fontSize = 17.sp, fontWeight = FontWeight.Medium, textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) {
                        onDismiss()
                        context.launch(intent)
                    }.padding(vertical = 16.dp),
                )
            }
        }
    }
}

fun Context.launch(intent: Intent) {
    try {
        startActivity(intent)
    } catch (e: ActivityNotFoundException) {
        Toast.makeText(this, "Aucune application pour ouvrir ce lien", Toast.LENGTH_SHORT).show()
    }
}

private val SLEEP_CHOICES = listOf(
    5 to "5 minutes", 10 to "10 minutes", 15 to "15 minutes", 30 to "30 minutes",
    45 to "45 minutes", 60 to "1 heure", 120 to "2 heures",
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SleepTimerSheet(onDismiss: () -> Unit) {
    val active = SleepTimer.endAt != null
    val remaining = sleepMinutesLeft()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = Color.White,
    ) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 24.dp).padding(bottom = 16.dp)) {
            Text("Minuteur de veille", color = Color(0xFF444444), fontSize = 24.sp, fontWeight = FontWeight.SemiBold)
            Text(
                if (remaining != null) "Arrêt de la radio dans $remaining min" else "Arrêter la radio après",
                color = Color(0xFF8A8A8A), fontSize = 16.sp, modifier = Modifier.padding(top = 6.dp, bottom = 14.dp),
            )
            HorizontalDivider(color = Color(0xFFE6E6EA))
            Spacer(Modifier.height(6.dp))
            SLEEP_CHOICES.forEach { (minutes, label) ->
                Text(
                    label, color = Palette.Ink, fontSize = 17.sp, textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 5.dp).fillMaxWidth().clip(CircleShape)
                        .border(1.dp, Color(0xFFE6E6EA), CircleShape)
                        .clickable(role = Role.Button) {
                            SleepTimer.set(minutes)
                            onDismiss()
                        }.padding(vertical = 13.dp),
                )
            }
            Spacer(Modifier.height(6.dp))
            HorizontalDivider(color = Color(0xFFE6E6EA))
            Text(
                "Supprimer le minuteur", color = Color.White, fontSize = 17.sp, textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 14.dp).fillMaxWidth().clip(CircleShape)
                    .background(if (active) Color(0xFFE5484D) else Color(0xFFF7A8AB))
                    .clickable(enabled = active, role = Role.Button) {
                        SleepTimer.clear()
                        onDismiss()
                    }.padding(vertical = 14.dp),
            )
        }
    }
}
