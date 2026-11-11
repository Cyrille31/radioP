package io.github.cyrille31.radiop.ui

import android.app.TimePickerDialog
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import io.github.cyrille31.radiop.Alarm
import io.github.cyrille31.radiop.AlarmScheduler
import io.github.cyrille31.radiop.Prefs
import io.github.cyrille31.radiop.Station
import kotlinx.coroutines.delay
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale

private val DATE_FORMAT = DateTimeFormatter.ofPattern("EEEE d MMMM yyyy", Locale.FRENCH)
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

@Composable
fun ReveilScreen(station: Station) {
    val context = LocalContext.current
    var alarms by remember { mutableStateOf(Prefs.alarms(context)) }
    var editing by remember { mutableStateOf<Alarm?>(null) }   // réveil ouvert dans l'éditeur
    var creating by remember { mutableStateOf(false) }

    var now by remember { mutableStateOf(LocalDateTime.now()) }
    LaunchedEffect(Unit) {
        while (true) {
            now = LocalDateTime.now()
            delay(1_000)
        }
    }

    fun save(updated: List<Alarm>) {
        alarms = updated.sortedBy { it.hour * 60 + it.minute }
        Prefs.saveAlarms(context, alarms)
        AlarmScheduler.rescheduleAll(context)
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())
                .navigationBarsPadding().padding(horizontal = 20.dp).padding(bottom = 96.dp),
        ) {
            Spacer(Modifier.height(if (alarms.isEmpty()) 180.dp else 28.dp))
            Text(now.format(DATE_FORMAT), color = Color(0xFFCFEAFB), fontSize = 19.sp, fontWeight = FontWeight.SemiBold)
            Text(now.format(TIME_FORMAT), color = Color.White, fontSize = 92.sp, fontWeight = FontWeight.Bold)

            if (alarms.isNotEmpty()) Spacer(Modifier.height(16.dp))
            alarms.forEach { alarm ->
                AlarmRow(
                    alarm,
                    onClick = { editing = alarm },
                    onToggle = { on -> save(alarms.map { if (it.id == alarm.id) it.copy(enabled = on) else it }) },
                )
            }
            if (alarms.isNotEmpty()) {
                Text(
                    "Le réveil lance la dernière antenne écoutée (${station.name}). " +
                        "Si la radio est injoignable, la sonnerie du téléphone prend le relais.",
                    color = Color(0xFFCFEAFB), fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, start = 4.dp, end = 4.dp),
                )
            }
        }

        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier.align(Alignment.BottomCenter).navigationBarsPadding().padding(bottom = 18.dp)
                .size(64.dp).shadow(6.dp, CircleShape).clip(CircleShape).background(Palette.Sky)
                .clickable(role = Role.Button, onClickLabel = "Nouveau réveil") { creating = true },
        ) {
            Icon(AppIcons.Add, contentDescription = "Nouveau réveil", tint = Color.White, modifier = Modifier.size(36.dp))
        }
    }

    if (creating || editing != null) {
        AlarmEditor(
            initial = editing,
            onCancel = { creating = false; editing = null },
            onSave = { edited ->
                val alarm = if (edited.id == 0) edited.copy(id = (alarms.maxOfOrNull { it.id } ?: 0) + 1) else edited
                save(alarms.filter { it.id != alarm.id } + alarm)
                creating = false; editing = null
            },
            onDelete = { removed ->
                AlarmScheduler.cancel(context, removed.id)
                save(alarms.filter { it.id != removed.id })
                creating = false; editing = null
            },
        )
    }
}

@Composable
private fun AlarmRow(alarm: Alarm, onClick: () -> Unit, onToggle: (Boolean) -> Unit) {
    val ink = if (alarm.enabled) Color.White else Color.White.copy(alpha = 0.55f)
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(vertical = 5.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.14f)).clickable(onClickLabel = "Modifier le réveil", onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 12.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(alarm.time, color = ink, fontSize = 34.sp, fontWeight = FontWeight.Bold)
            Text(
                listOf(alarm.name, alarm.daysLabel()).filter { it.isNotBlank() }.joinToString(" · "),
                color = ink, fontSize = 14.sp,
            )
        }
        Switch(
            checked = alarm.enabled, onCheckedChange = onToggle,
            colors = SwitchDefaults.colors(
                checkedTrackColor = Palette.Sky, checkedThumbColor = Color.White,
                uncheckedTrackColor = Color.White.copy(alpha = 0.2f), uncheckedThumbColor = Color.White,
                uncheckedBorderColor = Color.Transparent,
            ),
        )
    }
}

/** Écran « Nouv. réveil » : nom, heure, jours de répétition. `initial == null` pour une création. */
@Composable
internal fun AlarmEditor(initial: Alarm?, onCancel: () -> Unit, onSave: (Alarm) -> Unit, onDelete: (Alarm) -> Unit) {
    val context = LocalContext.current
    val start = remember { LocalDateTime.now() }
    var name by remember { mutableStateOf(initial?.name ?: "") }
    var hour by remember { mutableIntStateOf(initial?.hour ?: start.hour) }
    var minute by remember { mutableIntStateOf(initial?.minute ?: start.minute) }
    var days by remember { mutableStateOf(initial?.days ?: emptySet()) }
    val accent = Color(0xFF2E9BDA)

    Dialog(onDismissRequest = onCancel, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Column(
            Modifier.fillMaxSize().background(Palette.Paper).statusBarsPadding().navigationBarsPadding()
                .verticalScroll(rememberScrollState()).padding(horizontal = 20.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 14.dp)) {
                Text(
                    "Annuler", color = accent, fontSize = 18.sp,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onCancel).padding(8.dp),
                )
                Text(
                    if (initial == null) "Nouv. réveil" else "Modifier",
                    color = Palette.Ink, fontSize = 18.sp, fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                Text(
                    "Enregistrer", color = accent, fontSize = 18.sp,
                    modifier = Modifier.clip(CircleShape).clickable(role = Role.Button) {
                        onSave(Alarm(initial?.id ?: 0, name.trim(), hour, minute, days, enabled = true))
                    }.padding(8.dp),
                )
            }

            Box(
                contentAlignment = Alignment.CenterStart,
                modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White)
                    .padding(horizontal = 20.dp, vertical = 18.dp),
            ) {
                if (name.isEmpty()) Text("Nom du réveil", color = Color(0xFFB8B8C0), fontSize = 18.sp)
                BasicTextField(
                    value = name, onValueChange = { name = it.take(40) }, singleLine = true,
                    textStyle = TextStyle(color = Palette.Ink, fontSize = 18.sp),
                    keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Sentences),
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterHorizontally).padding(vertical = 18.dp).clip(RoundedCornerShape(16.dp))
                    .clickable(role = Role.Button, onClickLabel = "Modifier l'heure") {
                        TimePickerDialog(context, { _, h, m -> hour = h; minute = m }, hour, minute, true).show()
                    }.padding(horizontal = 12.dp),
            ) {
                Text("%02d:%02d".format(hour, minute), color = Color(0xFF333333), fontSize = 56.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.width(6.dp))
                Icon(AppIcons.Edit, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(26.dp))
            }

            Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Color.White).padding(horizontal = 20.dp)) {
                Alarm.DAYS.forEachIndexed { index, label ->
                    val day = index + 1
                    if (index > 0) HorizontalDivider(color = Color(0xFFE6E6EA))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().clickable(role = Role.Checkbox) {
                            days = if (day in days) days - day else days + day
                        }.padding(vertical = 15.dp),
                    ) {
                        Text(label, color = Color(0xFF333333), fontSize = 18.sp, modifier = Modifier.weight(1f))
                        if (day in days) {
                            Icon(AppIcons.Check, contentDescription = "coché", tint = Palette.Sky, modifier = Modifier.size(26.dp))
                        } else {
                            Spacer(Modifier.size(26.dp))
                        }
                    }
                }
            }
            Text(
                if (days.isEmpty()) "Aucun jour coché : le réveil sonnera une seule fois." else "Le réveil sonnera chaque semaine, les jours cochés.",
                color = Color(0xFF8A8A8A), fontSize = 13.sp, modifier = Modifier.padding(top = 10.dp, start = 4.dp),
            )

            if (initial != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(top = 20.dp).align(Alignment.CenterHorizontally).clip(CircleShape)
                        .clickable(role = Role.Button) { onDelete(initial) }.padding(horizontal = 16.dp, vertical = 10.dp),
                ) {
                    Icon(AppIcons.Delete, contentDescription = null, tint = Color(0xFFE5484D), modifier = Modifier.size(22.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Supprimer ce réveil", color = Color(0xFFE5484D), fontSize = 17.sp)
                }
            }
            Spacer(Modifier.height(24.dp))
        }
    }
}
