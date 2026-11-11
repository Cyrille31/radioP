package io.github.cyrille31.radiop

import android.app.PendingIntent
import android.content.Intent
import android.media.RingtoneManager
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata
import androidx.media3.common.Metadata
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultDataSource
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.ExoPlayer
import androidx.media3.exoplayer.source.DefaultMediaSourceFactory
import androidx.media3.extractor.metadata.icy.IcyInfo
import androidx.media3.session.MediaSession
import androidx.media3.session.MediaSessionService
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture

/** Minuteur de veille, partagé entre l'interface (qui le règle) et le service (qui met en pause). */
object SleepTimer {
    /** Échéance, sur l'horloge `SystemClock.elapsedRealtime()` ; `null` si aucun minuteur. */
    var endAt by mutableStateOf<Long?>(null)
        private set
    var onExpire: (() -> Unit)? = null

    private val handler = Handler(Looper.getMainLooper())
    private val expire = Runnable {
        endAt = null
        onExpire?.invoke()
    }

    fun set(minutes: Int) {
        handler.removeCallbacks(expire)
        endAt = SystemClock.elapsedRealtime() + minutes * 60_000L
        handler.postDelayed(expire, minutes * 60_000L)
    }

    fun clear() {
        handler.removeCallbacks(expire)
        endAt = null
    }
}

/** Lecture du direct en arrière-plan, avec la notification et ses commandes (Media3). */
@OptIn(UnstableApi::class)
class PlaybackService : MediaSessionService() {

    private lateinit var player: ExoPlayer
    private var session: MediaSession? = null
    private val handler = Handler(Looper.getMainLooper())

    private var retries = 0
    private var stale = false       // une pause a eu lieu : le tampon n'est plus du direct
    private var alarmMode = false   // lecture lancée par un réveil, pas encore audible

    private val retry = Runnable { if (player.playWhenReady) player.prepare() }
    private val alarmWatchdog = Runnable { if (alarmMode) playRingtone() }

    override fun onCreate() {
        super.onCreate()
        val http = DefaultHttpDataSource.Factory()
            .setUserAgent("radioP/${BuildConfig.VERSION_NAME}")
            .setAllowCrossProtocolRedirects(true)
            .setConnectTimeoutMs(15_000)
            .setReadTimeoutMs(15_000)
        player = ExoPlayer.Builder(this)
            .setMediaSourceFactory(DefaultMediaSourceFactory(DefaultDataSource.Factory(this, http)))
            .setAudioAttributes(
                AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MUSIC).build(),
                /* handleAudioFocus = */ true,
            )
            .setHandleAudioBecomingNoisy(true)
            .setWakeMode(C.WAKE_MODE_NETWORK)
            .build()
        player.addListener(listener)

        val open = PendingIntent.getActivity(
            this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        session = MediaSession.Builder(this, player).setSessionActivity(open).setCallback(callback).build()
        SleepTimer.onExpire = { player.pause() }
    }

    override fun onGetSession(controllerInfo: MediaSession.ControllerInfo): MediaSession? = session

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val result = super.onStartCommand(intent, flags, startId)
        if (intent?.action == ACTION_ALARM) startAlarm()
        return result
    }

    /**
     * L'appli est balayée de la liste des applis récentes : on arrête tout pour de bon
     * (son, notification, minuteur). Quitter par le bouton retour ou accueil laisse la radio jouer.
     */
    override fun onTaskRemoved(rootIntent: Intent?) {
        handler.removeCallbacksAndMessages(null)
        alarmMode = false
        SleepTimer.clear()
        pauseAllPlayersAndStopSelf()
    }

    override fun onDestroy() {
        SleepTimer.onExpire = null
        handler.removeCallbacksAndMessages(null)
        session?.release()
        session = null
        player.release()
        super.onDestroy()
    }

    private fun startAlarm() {
        alarmMode = true
        retries = 0
        stale = false
        player.repeatMode = Player.REPEAT_MODE_OFF
        player.setMediaItem(Stations.mediaItem(Prefs.lastStation(this)))
        player.prepare()
        player.play()
        // Si la radio n'est pas audible au bout de 20 s (pas de réseau…), on sonne quand même.
        handler.removeCallbacks(alarmWatchdog)
        handler.postDelayed(alarmWatchdog, 20_000)
    }

    /** Sonnerie de secours du réveil : celle du téléphone, en boucle, pilotable depuis la notification. */
    private fun playRingtone() {
        alarmMode = false
        handler.removeCallbacks(retry)
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        val item = MediaItem.Builder()
            .setMediaId(RINGTONE_ID)
            .setUri(uri)
            .setMediaMetadata(
                MediaMetadata.Builder().setTitle("Réveil").setArtist("Radio indisponible : sonnerie de secours").build()
            )
            .build()
        player.repeatMode = Player.REPEAT_MODE_ONE
        player.setMediaItem(item)
        player.prepare()
        player.play()
    }

    private val listener = object : Player.Listener {

        /** Le titre de l'émission en cours arrive dans les métadonnées ICY du flux. */
        override fun onMetadata(metadata: Metadata) {
            for (i in 0 until metadata.length()) {
                val entry = metadata[i]
                if (entry is IcyInfo) showTitle(Stations.cleanTitle(entry.title))
            }
        }

        override fun onPlayWhenReadyChanged(playWhenReady: Boolean, reason: Int) {
            if (!playWhenReady) {
                stale = true
                alarmMode = false
                handler.removeCallbacks(alarmWatchdog)
                handler.removeCallbacks(retry)
            } else if (stale) {
                // Reprise après une pause : on se rebranche sur le direct au lieu de rejouer le tampon.
                stale = false
                val item = player.currentMediaItem
                val station = Stations.find(item?.mediaId)
                if (station != null && player.playbackState != Player.STATE_IDLE) {
                    player.setMediaItem(Stations.mediaItem(station))
                    player.prepare()
                }
            }
        }

        override fun onPlaybackStateChanged(playbackState: Int) {
            if (playbackState == Player.STATE_READY) {
                retries = 0
                alarmMode = false
                handler.removeCallbacks(alarmWatchdog)
            }
        }

        /** Coupure réseau : quelques nouvelles tentatives avant d'abandonner. */
        override fun onPlayerError(error: PlaybackException) {
            if (player.currentMediaItem?.mediaId == RINGTONE_ID) return
            if (alarmMode && retries >= 2) {
                handler.removeCallbacks(alarmWatchdog)
                playRingtone()
            } else if (retries < MAX_RETRIES) {
                retries++
                handler.removeCallbacks(retry)
                handler.postDelayed(retry, 3_000)
            }
        }
    }

    private fun showTitle(title: String?) {
        val item = player.currentMediaItem ?: return
        val station = Stations.find(item.mediaId) ?: return
        val shown = title ?: Stations.NO_TITLE
        if (item.mediaMetadata.title?.toString() == shown) return
        // Même adresse : le lecteur met à jour les informations affichées sans interrompre le son.
        player.replaceMediaItem(player.currentMediaItemIndex, Stations.mediaItem(station, title))
    }

    private val callback = object : MediaSession.Callback {

        /** L'interface n'envoie que l'identifiant de l'antenne : on y associe l'adresse du flux. */
        override fun onAddMediaItems(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
            mediaItems: MutableList<MediaItem>,
        ): ListenableFuture<MutableList<MediaItem>> {
            alarmMode = false
            retries = 0
            stale = false
            handler.removeCallbacks(alarmWatchdog)
            player.repeatMode = Player.REPEAT_MODE_OFF
            val resolved = mediaItems.map { requested ->
                val station = Stations.find(requested.mediaId) ?: Prefs.lastStation(this@PlaybackService)
                Prefs.setLastStation(this@PlaybackService, station)
                Stations.mediaItem(station)
            }
            return Futures.immediateFuture(resolved.toMutableList())
        }

        /** Bouton lecture d'un casque ou d'une voiture alors que l'appli est fermée : dernière antenne écoutée. */
        override fun onPlaybackResumption(
            mediaSession: MediaSession,
            controller: MediaSession.ControllerInfo,
        ): ListenableFuture<MediaSession.MediaItemsWithStartPosition> =
            Futures.immediateFuture(
                MediaSession.MediaItemsWithStartPosition(
                    listOf(Stations.mediaItem(Prefs.lastStation(this@PlaybackService))), 0, C.TIME_UNSET,
                )
            )
    }

    companion object {
        const val ACTION_ALARM = "io.github.cyrille31.radiop.ALARM"
        private const val RINGTONE_ID = "sonnerie"
        private const val MAX_RETRIES = 6
    }
}
