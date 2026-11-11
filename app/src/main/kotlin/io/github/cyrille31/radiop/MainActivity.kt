package io.github.cyrille31.radiop

import android.Manifest
import android.content.ComponentName
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Text
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.media3.common.Player
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import com.google.common.util.concurrent.ListenableFuture
import io.github.cyrille31.radiop.ui.AboutScreen
import io.github.cyrille31.radiop.ui.AppIcons
import io.github.cyrille31.radiop.ui.Logo
import io.github.cyrille31.radiop.ui.Palette
import io.github.cyrille31.radiop.ui.RadioScreen
import io.github.cyrille31.radiop.ui.ReveilScreen
import io.github.cyrille31.radiop.ui.Site
import io.github.cyrille31.radiop.ui.WebScreen
import io.github.cyrille31.radiop.ui.openExternal
import io.github.cyrille31.radiop.ui.vertical
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private var controllerFuture: ListenableFuture<MediaController>? = null
    private var controller by mutableStateOf<MediaController?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val transparent = android.graphics.Color.TRANSPARENT
        enableEdgeToEdge(SystemBarStyle.dark(transparent), SystemBarStyle.dark(transparent))
        super.onCreate(savedInstanceState)
        AlarmScheduler.rescheduleAll(this)
        setContent {
            MaterialTheme(colorScheme = lightColorScheme(primary = Palette.Blue)) {
                App(controller)
            }
        }
    }

    override fun onStart() {
        super.onStart()
        val token = SessionToken(this, ComponentName(this, PlaybackService::class.java))
        val future = MediaController.Builder(this, token).buildAsync()
        controllerFuture = future
        future.addListener(
            { if (controllerFuture === future && !future.isCancelled) controller = runCatching { future.get() }.getOrNull() },
            ContextCompat.getMainExecutor(this),
        )
    }

    override fun onStop() {
        controller = null
        controllerFuture?.let { MediaController.releaseFuture(it) }
        controllerFuture = null
        super.onStop()
    }
}

enum class Screen(val title: String) {
    RADIO("Ma Radio"), UNE("A la une"), REVEIL("Réveil"), EGLISE("Eglise actu"), SITE("Site internet"), INFOS("Informations")
}

/** Ce que l'interface a besoin de savoir du lecteur. */
private data class PlayerState(
    val playing: Boolean = false,
    val loading: Boolean = false,
    val stationId: String? = null,
    val title: String? = null,
)

private fun Player.snapshot(): PlayerState {
    val wanted = playWhenReady && playbackState != Player.STATE_IDLE && playbackState != Player.STATE_ENDED
    return PlayerState(
        playing = wanted,
        loading = wanted && playbackState == Player.STATE_BUFFERING,
        stationId = currentMediaItem?.mediaId,
        title = mediaMetadata.title?.toString()?.takeIf { it.isNotBlank() && it != Stations.NO_TITLE },
    )
}

@Composable
private fun rememberPlayerState(controller: MediaController?): PlayerState {
    var state by remember(controller) { mutableStateOf(controller?.snapshot() ?: PlayerState()) }
    DisposableEffect(controller) {
        val listener = object : Player.Listener {
            override fun onEvents(player: Player, events: Player.Events) {
                state = player.snapshot()
            }
        }
        controller?.addListener(listener)
        onDispose { controller?.removeListener(listener) }
    }
    return state
}

@Composable
internal fun App(controller: MediaController?) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val drawer = rememberDrawerState(DrawerValue.Closed)
    var screen by rememberSaveable { mutableStateOf(Screen.RADIO) }
    var showStations by remember { mutableStateOf(false) }
    var station by remember { mutableStateOf(Prefs.lastStation(context)) }
    val player = rememberPlayerState(controller)

    // Si la lecture a été lancée ailleurs (réveil, casque), l'écran suit l'antenne réellement jouée.
    Stations.find(player.stationId)?.let { if (player.playing && it != station) station = it }

    val askNotifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { }

    fun play(target: Station) {
        val c = controller ?: return
        // La notification de lecture a besoin d'une autorisation depuis Android 13. Demandée une seule fois ;
        // un refus n'empêche rien : la radio joue quand même.
        if (Build.VERSION.SDK_INT >= 33 && !Prefs.notificationsAsked(context) &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            Prefs.setNotificationsAsked(context)
            askNotifications.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (c.currentMediaItem?.mediaId != target.id) c.setMediaItem(Stations.request(target))
        c.prepare()
        c.play()
    }

    fun toggle() {
        if (player.playing) controller?.pause() else play(station)
    }

    fun choose(target: Station) {
        station = target
        Prefs.setLastStation(context, target)
        showStations = false
        if (player.playing) play(target)
    }

    BackHandler(enabled = showStations || drawer.isOpen || screen != Screen.RADIO) {
        when {
            showStations -> showStations = false
            drawer.isOpen -> scope.launch { drawer.close() }
            else -> screen = Screen.RADIO
        }
    }

    val isWeb = screen == Screen.UNE || screen == Screen.EGLISE || screen == Screen.SITE
    ModalNavigationDrawer(
        drawerState = drawer,
        // Sur les pages web, le glissement latéral appartient à la page.
        gesturesEnabled = drawer.isOpen || !isWeb,
        drawerContent = {
            DrawerMenu(
                current = screen,
                onScreen = {
                    screen = it
                    scope.launch { drawer.close() }
                },
                onDonate = {
                    scope.launch { drawer.close() }
                    context.openExternal(Uri.parse(Site.DON))
                },
            )
        },
    ) {
        Box(Modifier.fillMaxSize().background(Palette.Background)) {
            Column(Modifier.fillMaxSize()) {
                TopBar(
                    screen = screen,
                    onMenu = { scope.launch { drawer.open() } },
                    onStations = { showStations = true },
                )
                when (screen) {
                    Screen.RADIO -> RadioScreen(
                        station = station,
                        playing = player.playing && player.stationId == station.id,
                        loading = player.loading && player.stationId == station.id,
                        playerTitle = player.title.takeIf { player.playing && player.stationId == station.id },
                        onToggle = ::toggle,
                    )
                    Screen.REVEIL -> ReveilScreen(station)
                    Screen.UNE, Screen.SITE -> WebScreen(Site.HOME)
                    Screen.EGLISE -> WebScreen(Site.AGENDA)
                    Screen.INFOS -> AboutScreen()
                }
            }
            StationsPanel(visible = showStations, current = station, onChoose = ::choose, onDismiss = { showStations = false })
        }
    }
}

@Composable
internal fun TopBar(screen: Screen, onMenu: () -> Unit, onStations: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().statusBarsPadding().height(72.dp).padding(horizontal = 12.dp),
    ) {
        BarButton(AppIcons.Menu, "Menu", onMenu)
        if (screen == Screen.RADIO) {
            Box(Modifier.weight(1f), contentAlignment = Alignment.Center) { Logo(height = 46.dp) }
            BarButton(AppIcons.Radio, "Choisir l'antenne", onStations)
        } else {
            Text(
                screen.title, color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f).padding(start = 6.dp),
            )
        }
    }
}

@Composable
private fun BarButton(icon: ImageVector, label: String, onClick: () -> Unit) {
    Icon(
        icon, contentDescription = label, tint = Color.White,
        modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClick).padding(8.dp).size(34.dp),
    )
}

@Composable
internal fun DrawerMenu(current: Screen, onScreen: (Screen) -> Unit, onDonate: () -> Unit) {
    ModalDrawerSheet(drawerContainerColor = Palette.Cyan, modifier = Modifier.fillMaxWidth(0.86f)) {
        Column(Modifier.verticalScroll(rememberScrollState()).padding(horizontal = 28.dp)) {
            Logo(height = 84.dp, modifier = Modifier.padding(top = 28.dp, bottom = 26.dp, start = 12.dp))
            val entries: List<Triple<ImageVector, String, () -> Unit>> = listOf(
                Triple(AppIcons.Volume, Screen.RADIO.title) { onScreen(Screen.RADIO) },
                Triple(AppIcons.Feed, Screen.UNE.title) { onScreen(Screen.UNE) },
                Triple(AppIcons.Clock, Screen.REVEIL.title) { onScreen(Screen.REVEIL) },
                Triple(AppIcons.Feed, Screen.EGLISE.title) { onScreen(Screen.EGLISE) },
                Triple(AppIcons.Globe, Screen.SITE.title) { onScreen(Screen.SITE) },
                Triple(AppIcons.Info, Screen.INFOS.title) { onScreen(Screen.INFOS) },
                Triple(AppIcons.Heart, "Faire un don", onDonate),
            )
            entries.forEachIndexed { index, (icon, label, action) ->
                if (index > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.55f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth().clickable(role = Role.Button, onClick = action).padding(vertical = 18.dp),
                ) {
                    Icon(icon, contentDescription = null, tint = Palette.Ink, modifier = Modifier.size(if (index == 0) 34.dp else 26.dp))
                    Spacer(Modifier.width(18.dp))
                    Text(
                        label, color = Palette.Ink, fontSize = 20.sp,
                        fontWeight = if (label == current.title) FontWeight.SemiBold else FontWeight.Normal,
                    )
                }
            }
        }
    }
}

/** Panneau de droite : le choix de l'antenne. */
@Composable
internal fun StationsPanel(visible: Boolean, current: Station, onChoose: (Station) -> Unit, onDismiss: () -> Unit) {
    AnimatedVisibility(visible, enter = fadeIn(), exit = fadeOut()) {
        Box(
            Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.5f))
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss)
        )
    }
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.CenterEnd) {
        AnimatedVisibility(
            visible,
            enter = slideInHorizontally { it },
            exit = slideOutHorizontally { it },
        ) {
            Column(
                Modifier.fillMaxHeight().fillMaxWidth(0.86f).background(Palette.Cyan)
                    // Un appui dans le panneau ne doit pas le refermer.
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { }
                    .statusBarsPadding().navigationBarsPadding()
                    .verticalScroll(rememberScrollState()).padding(horizontal = 22.dp, vertical = 8.dp),
            ) {
                Stations.all.forEachIndexed { index, station ->
                    if (index > 0) HorizontalDivider(color = Color.White.copy(alpha = 0.55f))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth().height(132.dp)
                            .background(if (station == current) Color.Black.copy(alpha = 0.10f) else Color.Transparent)
                            .clickable(role = Role.Button, onClickLabel = "Écouter ${station.name}") { onChoose(station) }
                            .padding(horizontal = 12.dp),
                    ) {
                        if (station.label.isNotEmpty()) {
                            Text(
                                station.label, color = Color.White, fontSize = 15.sp, maxLines = 1,
                                fontWeight = FontWeight.Bold, fontStyle = FontStyle.Italic,
                                modifier = Modifier.vertical().background(Palette.Badge).padding(horizontal = 8.dp, vertical = 3.dp),
                            )
                            Spacer(Modifier.width(10.dp))
                        }
                        Logo(height = 72.dp)
                    }
                }
            }
        }
    }
}
