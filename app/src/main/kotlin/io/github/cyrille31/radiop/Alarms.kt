package io.github.cyrille31.radiop

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import java.time.ZonedDateTime

object AlarmScheduler {
    const val EXTRA_ID = "alarm_id"

    private fun pending(c: Context, id: Int): PendingIntent =
        PendingIntent.getBroadcast(
            c, id,
            Intent(c, AlarmReceiver::class.java).putExtra(EXTRA_ID, id),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

    fun cancel(c: Context, id: Int) {
        c.getSystemService(AlarmManager::class.java).cancel(pending(c, id))
    }

    /** Reprogramme tous les réveils actifs à leur prochaine échéance. */
    fun rescheduleAll(c: Context) {
        val manager = c.getSystemService(AlarmManager::class.java)
        val show = PendingIntent.getActivity(
            c, 0, Intent(c, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val now = ZonedDateTime.now()
        for (alarm in Prefs.alarms(c)) {
            val operation = pending(c, alarm.id)
            manager.cancel(operation)
            if (!alarm.enabled) continue
            val at = alarm.nextTrigger(now).toInstant().toEpochMilli()
            val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()
            try {
                if (exact) manager.setAlarmClock(AlarmManager.AlarmClockInfo(at, show), operation)
                else manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            } catch (e: SecurityException) {
                manager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, operation)
            }
        }
    }
}

/** L'heure d'un réveil est arrivée : on lance la radio. */
class AlarmReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val id = intent.getIntExtra(AlarmScheduler.EXTRA_ID, -1)
        val alarms = Prefs.alarms(context)
        val alarm = alarms.firstOrNull { it.id == id }
        if (alarm != null && alarm.enabled) {
            // Un réveil sans jour coché ne sonne qu'une fois.
            if (alarm.days.isEmpty()) {
                Prefs.saveAlarms(context, alarms.map { if (it.id == id) it.copy(enabled = false) else it })
            }
            try {
                ContextCompat.startForegroundService(
                    context,
                    Intent(context, PlaybackService::class.java).setAction(PlaybackService.ACTION_ALARM),
                )
            } catch (e: Exception) {
                Log.e("radioP", "Impossible de lancer le réveil", e)
            }
        }
        AlarmScheduler.rescheduleAll(context)
    }
}

/** Après un redémarrage, une mise à jour ou un changement d'heure, les réveils sont à reprogrammer. */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        AlarmScheduler.rescheduleAll(context)
    }
}
