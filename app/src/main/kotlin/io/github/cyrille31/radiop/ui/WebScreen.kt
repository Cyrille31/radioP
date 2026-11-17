package io.github.cyrille31.radiop.ui

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.browser.customtabs.CustomTabsIntent
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView

object Site {
    const val HOME = "https://www.radiopresence.com/"
    const val AGENDA = "https://www.radiopresence.com/emissions/information/regionale/eglise-actualite/"
    const val DON = "https://www.radiopresence.com/soutenir-radio-presence-en-devenant-donateur/"
    const val LEGAL = "https://www.radiopresence.com/article/mentions-legales"
}

/** Ouvre une adresse hors de l'appli : onglet du navigateur pour le web, appli adaptée pour le reste. */
fun Context.openExternal(uri: Uri) {
    if (uri.scheme == "http" || uri.scheme == "https") {
        try {
            CustomTabsIntent.Builder().setShowTitle(true).build().launchUrl(this, uri)
            return
        } catch (e: Exception) {
            // Pas de navigateur compatible : on retombe sur l'ouverture classique.
        }
    }
    launch(Intent(Intent.ACTION_VIEW, uri))
}

/** Une page du site de la radio affichée dans l'appli. Le reste du web, et le don, s'ouvrent dans le navigateur. */
@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WebScreen(url: String) {
    key(url) {
        var progress by remember { mutableIntStateOf(0) }
        var webView by remember { mutableStateOf<WebView?>(null) }
        var canGoBack by remember { mutableStateOf(false) }

        BackHandler(enabled = canGoBack) { webView?.goBack() }

        Box(Modifier.fillMaxSize().navigationBarsPadding()) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
                                val target = request.url
                                val host = target.host.orEmpty()
                                val inSite = (target.scheme == "http" || target.scheme == "https") &&
                                    (host == "radiopresence.com" || host == "www.radiopresence.com") &&
                                    !target.path.orEmpty().startsWith("/soutenir-radio-presence")
                                if (inSite) return false
                                context.openExternal(target)
                                return true
                            }

                            override fun doUpdateVisitedHistory(view: WebView, url: String?, isReload: Boolean) {
                                canGoBack = view.canGoBack()
                            }
                        }
                        webChromeClient = object : WebChromeClient() {
                            override fun onProgressChanged(view: WebView, newProgress: Int) {
                                progress = newProgress
                            }
                        }
                        loadUrl(url)
                        webView = this
                    }
                },
                onRelease = { it.destroy() },
            )
            if (progress in 1..99) {
                LinearProgressIndicator(
                    progress = { progress / 100f },
                    color = Palette.Coral, trackColor = Color.Transparent,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
