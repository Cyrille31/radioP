package io.github.cyrille31.radiop

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.time.ZonedDateTime

/** Un réveil. [days] : 1 = lundi … 7 = dimanche ; vide = une seule fois, à la prochaine occurrence. */
data class Alarm(
    val id: Int,
    val name: String,
    val hour: Int,
    val minute: Int,
    val days: Set<Int>,
    val enabled: Boolean,
) {
    val time: String get() = "%02d:%02d".format(hour, minute)

    fun nextTrigger(now: ZonedDateTime): ZonedDateTime {
        for (offset in 0..7) {
            val candidate = now.toLocalDate().plusDays(offset.toLong()).atTime(hour, minute).atZone(now.zone)
            if (candidate.isAfter(now) && (days.isEmpty() || candidate.dayOfWeek.value in days)) return candidate
        }
        return now.plusDays(1)
    }

    fun daysLabel(): String = when {
        days.isEmpty() -> "Une seule fois"
        days.size == 7 -> "Tous les jours"
        days == setOf(1, 2, 3, 4, 5) -> "Du lundi au vendredi"
        days == setOf(6, 7) -> "Le week-end"
        else -> days.sorted().joinToString(" ") { DAYS_SHORT[it - 1] }
    }

    companion object {
        val DAYS = listOf("Lundi", "Mardi", "Mercredi", "Jeudi", "Vendredi", "Samedi", "Dimanche")
        val DAYS_SHORT = listOf("lun.", "mar.", "mer.", "jeu.", "ven.", "sam.", "dim.")
    }
}

object Prefs {
    private fun sp(c: Context) = c.getSharedPreferences("radiop", Context.MODE_PRIVATE)

    fun lastStation(c: Context): Station = Stations.find(sp(c).getString("station", null)) ?: Stations.all[0]

    fun setLastStation(c: Context, station: Station) = sp(c).edit().putString("station", station.id).apply()

    fun notificationsAsked(c: Context): Boolean = sp(c).getBoolean("notif_asked", false)

    fun setNotificationsAsked(c: Context) = sp(c).edit().putBoolean("notif_asked", true).apply()

    fun alarms(c: Context): List<Alarm> = try {
        val array = JSONArray(sp(c).getString("alarms", "[]"))
        (0 until array.length()).map { i ->
            val o = array.getJSONObject(i)
            val days = o.getJSONArray("days")
            Alarm(
                id = o.getInt("id"),
                name = o.optString("name"),
                hour = o.getInt("hour"),
                minute = o.getInt("minute"),
                days = (0 until days.length()).map { days.getInt(it) }.toSet(),
                enabled = o.optBoolean("enabled", true),
            )
        }
    } catch (e: Exception) {
        emptyList()
    }

    fun saveAlarms(c: Context, alarms: List<Alarm>) {
        val array = JSONArray()
        for (a in alarms) {
            array.put(
                JSONObject()
                    .put("id", a.id).put("name", a.name)
                    .put("hour", a.hour).put("minute", a.minute)
                    .put("days", JSONArray(a.days.sorted()))
                    .put("enabled", a.enabled)
            )
        }
        sp(c).edit().putString("alarms", array.toString()).apply()
    }
}
