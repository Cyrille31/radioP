package io.github.cyrille31.radiop

import androidx.media3.common.MediaItem
import androidx.media3.common.MediaMetadata

/** Une antenne et son point de montage sur le serveur Icecast de la radio. */
data class Station(val id: String, val name: String, val label: String, val mount: String) {
    val url: String get() = "https://direct.radiopresence.com/$mount"
}

object Stations {
    const val STATUS_URL = "https://direct.radiopresence.com/status-json.xsl"
    const val NO_TITLE = "En direct"

    val all = listOf(
        Station("toulouse", "Radio Présence", "", "radio-presence"),
        Station("lourdes", "Radio Présence Lourdes", "LOURDES", "radio-presence-lourdes"),
        Station("figeac", "Radio Présence Figeac", "FIGEAC", "radio-presence-figeac"),
        Station("cahors", "Radio Présence Cahors", "CAHORS", "radio-presence-lot"),
        Station("stgaudens", "Radio Présence St Gaudens", "ST GAUDENS", "radio-presence-pyrenees"),
    )

    fun find(id: String?): Station? = all.firstOrNull { it.id == id }

    /** Élément tel que l'interface l'envoie au lecteur : l'identifiant suffit, le service ajoute l'adresse. */
    fun request(station: Station): MediaItem = MediaItem.Builder().setMediaId(station.id).build()

    /** Élément complet, avec l'adresse du flux et ce qui s'affiche dans la notification. */
    fun mediaItem(station: Station, title: String? = null): MediaItem =
        MediaItem.Builder()
            .setMediaId(station.id)
            .setUri(station.url)
            .setMediaMetadata(
                MediaMetadata.Builder()
                    .setTitle(title ?: NO_TITLE)
                    .setArtist(station.name)
                    .setStation(station.name)
                    .build()
            )
            .build()

    private val suffix = Regex("""(^|\s*-\s*)(www\.radiopresence\.com|radio\s*pr[eé]sence)\s*$""", RegexOption.IGNORE_CASE)

    /**
     * Le flux annonce par exemple « Titre - Radio Présence » ou « Titre - www.radiopresence.com »,
     * parfois seulement le suffixe : on ne garde que le titre.
     */
    fun cleanTitle(raw: String?): String? {
        var title = raw?.trim() ?: return null
        while (true) {
            val shorter = title.replace(suffix, "").trim()
            if (shorter == title) break
            title = shorter
        }
        return title.trim('-').trim().takeIf { it.isNotEmpty() }
    }
}
