package io.github.cyrille31.radiop.ui

import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.cyrille31.radiop.BuildConfig

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState()).navigationBarsPadding().padding(20.dp),
    ) {
        Logo(height = 64.dp, modifier = Modifier.padding(vertical = 16.dp))
        Text("Version ${BuildConfig.VERSION_NAME}", color = Color(0xFFCFEAFB), fontSize = 14.sp)

        Column(
            Modifier.padding(top = 20.dp).fillMaxWidth().clip(RoundedCornerShape(20.dp)).background(Color.White).padding(20.dp),
        ) {
            Heading("À propos")
            Body(
                "radioP est une application personnelle et indépendante pour écouter le direct de Radio Présence : " +
                    "choix de l'antenne, minuteur de veille, réveil. Elle n'est ni éditée ni soutenue par la radio."
            )
            Body(
                "Les flux audio, les émissions et les pages du site appartiennent à Radio Présence. " +
                    "L'application ne demande aucun accès à vos photos ni à votre position, et ne collecte aucune donnée."
            )
            Text(
                "Mentions légales de la radio", color = Color(0xFF0B74B8), fontSize = 15.sp,
                textDecoration = TextDecoration.Underline,
                modifier = Modifier.padding(top = 4.dp).clickable(role = Role.Button) {
                    context.openExternal(Uri.parse(Site.LEGAL))
                }.padding(vertical = 6.dp),
            )

            Heading("Licence", top = 18)
            Body("Le code de l'application est distribué sous licence MIT + « BAL 1.0 — Bonne Action License ».")
            Body(
                "En échange, une seule chose vous est demandée, sur l'honneur : faire une bonne action chaque jour. " +
                    "Aider un voisin, sourire à un inconnu, ramasser un papier… c'est vous qui voyez."
            )
            Body("Icônes : Material Icons (Google, licence Apache 2.0).")
        }
    }
}

@Composable
private fun Heading(text: String, top: Int = 0) {
    Text(text, color = Palette.Ink, fontSize = 19.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.padding(top = top.dp, bottom = 6.dp))
}

@Composable
private fun Body(text: String) {
    Text(text, color = Color(0xFF3C3C44), fontSize = 15.sp, lineHeight = 22.sp, modifier = Modifier.padding(bottom = 8.dp))
}
