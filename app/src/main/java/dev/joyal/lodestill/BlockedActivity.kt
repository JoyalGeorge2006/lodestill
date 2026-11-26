package dev.joyal.lodestill

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.joyal.lodestill.core.Engine
import dev.joyal.lodestill.data.HfDb
import dev.joyal.lodestill.data.Prefs
import dev.joyal.lodestill.ui.theme.GeistMono
import dev.joyal.lodestill.ui.theme.GeistSans
import dev.joyal.lodestill.ui.theme.LocalHf
import dev.joyal.lodestill.ui.theme.LodestillTheme
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.text.DateFormat
import java.util.Date

// Shown by the tier-3 accessibility service when an app from the user's own
// blocked list opens during an ACTIVE session. It is part of Lodestill, so it
// follows the same away/grace rules as MainActivity: it clears the away clock
// on start and starts it again only if the user leaves Lodestill from here.
class BlockedActivity : ComponentActivity() {
    private var toSession = false
    private var pkg: String? = null

    private data class Ui(val label: String, val remainingSec: Long, val progress: Float,
                          val endsAt: Long, val place: String, val live: Boolean)

    private fun load(now: Long): Ui {
        val db = HfDb.get(this)
        return runBlocking {
            val s = db.sessions().current()
            val label = pkg?.let { p -> db.blockedApps().all().first().firstOrNull { it.packageName == p }?.label }
                ?: "This app"
            if (s == null || s.status != "ACTIVE") return@runBlocking Ui(label, 0, 0f, now, "Focus", false)
            val rem = Engine.remainingSec(s, now)
            Ui(label, rem, Engine.progress(s, now).coerceIn(0f, 1f), now + rem * 1000,
                db.places().byId(s.placeId)?.name ?: "Focus", rem > 0)
        }
    }

    override fun onCreate(b: Bundle?) {
        super.onCreate(b)
        pkg = intent.getStringExtra(EXTRA_PKG)
        enableEdgeToEdge()
        setContent {
            val theme by remember { mutableStateOf(runBlocking { Prefs.flow(this@BlockedActivity).first().theme }) }
            val dark = when (theme) { "light" -> false; "dark" -> true; else -> isSystemInDarkTheme() }
            var ui by remember { mutableStateOf(load(System.currentTimeMillis())) }
            LaunchedEffect(Unit) {
                while (true) {
                    kotlinx.coroutines.delay(1000)
                    val u = load(System.currentTimeMillis())
                    if (!u.live) { goToSession(); return@LaunchedEffect }
                    ui = u
                }
            }
            LodestillTheme(dark = dark) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    BlockedScreen(ui.label, ui.remainingSec, ui.progress, ui.endsAt, ui.place) { goToSession() }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent); pkg = intent.getStringExtra(EXTRA_PKG)
        recreate()
    }

    private fun goToSession() {
        if (toSession) return
        toSession = true
        startActivity(Intent(this, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        finish()
    }

    @Deprecated("Back on this screen means return to the session, not reveal the blocked app")
    override fun onBackPressed() { goToSession() }

    private fun session() = runBlocking { HfDb.get(this@BlockedActivity).sessions().current() }

    override fun onStart() {
        super.onStart()
        val s = session() ?: return
        val st = runBlocking { Prefs.flow(this@BlockedActivity).first() }
        val r = Engine.onForeground(s, System.currentTimeMillis(), st.graceSec, st.finishWindowH)
        if (r.event != "none") runBlocking { HfDb.get(this).sessions().update(r.s) }
    }

    override fun onStop() {
        super.onStop()
        if (toSession) return
        // The user left Lodestill from the blocked screen (Home, recents): same as leaving the app.
        val s = session()
        if (s != null) runBlocking {
            HfDb.get(this@BlockedActivity).sessions().update(Engine.onBackground(s, System.currentTimeMillis()))
        }
        if (!isChangingConfigurations) finish()
    }

    companion object {
        const val EXTRA_PKG = "pkg"
        fun intent(c: Context, pkg: String): Intent =
            Intent(c, BlockedActivity::class.java).putExtra(EXTRA_PKG, pkg)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
    }
}

@Composable
private fun LodestillMark(modifier: Modifier, arc: Color, dot: Color) {
    Canvas(modifier) {
        // Same geometry as res/drawable/ic_foreground.xml (viewBox x 30..170, y 40..165).
        val k = size.width / 140f
        fun p(x: Float, y: Float) = Offset((x - 30f) * k, (y - 40f) * k)
        val sw = 12f * k
        for (r in listOf(56f, 36f, 16f)) {
            drawArc(arc, 180f, 180f, false, topLeft = p(100f - r, 150f - r),
                size = Size(2 * r * k, 2 * r * k), style = Stroke(sw, cap = StrokeCap.Round))
        }
        drawCircle(dot, 13f * k, p(100f, 58f))
    }
}

@Composable
private fun BlockedScreen(label: String, remainingSec: Long, progress: Float, endsAt: Long,
                          place: String, onBack: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val hf = LocalHf.current
    val dark = cs.background == Color(0xFF000000)
    val ends = DateFormat.getTimeInstance(DateFormat.SHORT).format(Date(endsAt))
    Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 28.dp)) {
        Spacer(Modifier.height(32.dp))
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            LodestillMark(Modifier.size(30.dp, 27.dp), cs.onBackground, if (dark) Color(0xFFFF8A3D) else Color(0xFFFF6A1A))
            Text("Lodestill", fontFamily = GeistSans, fontWeight = FontWeight.SemiBold, fontSize = 16.sp, color = cs.onBackground)
        }
        Spacer(Modifier.height(56.dp))
        Text("BLOCKED APP", fontFamily = GeistMono, fontSize = 11.sp, letterSpacing = 1.3.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(14.dp))
        Text("$label is blocked\nduring your session", fontFamily = GeistSans, fontWeight = FontWeight.SemiBold,
            fontSize = 28.sp, lineHeight = 34.sp, color = cs.onBackground)
        Spacer(Modifier.height(12.dp))
        Text("You chose to block this app while you focus. It opens again when the session ends.",
            fontFamily = GeistSans, fontSize = 14.sp, lineHeight = 21.sp, color = cs.onSurfaceVariant)
        Spacer(Modifier.height(34.dp))
        Column(Modifier.fillMaxWidth().border(1.dp, hf.hair, RoundedCornerShape(12.dp)).padding(20.dp)) {
            Text("TIME REMAINING", fontFamily = GeistMono, fontSize = 11.sp, letterSpacing = 1.3.sp, color = cs.onSurfaceVariant)
            Text(Engine.fmtRemaining(remainingSec), fontFamily = GeistMono, fontWeight = FontWeight.SemiBold,
                fontSize = 60.sp, color = cs.onBackground, modifier = Modifier.padding(top = 8.dp))
            Spacer(Modifier.height(14.dp))
            Box(Modifier.fillMaxWidth().height(3.dp).background(hf.ringTrack)) {
                Box(Modifier.fillMaxWidth(progress).height(3.dp).background(cs.onBackground))
            }
            Text("Ends $ends · building a layer at $place", fontFamily = GeistSans, fontSize = 12.sp,
                color = cs.onSurfaceVariant, modifier = Modifier.padding(top = 12.dp))
        }
        Spacer(Modifier.weight(1f))
        Box(Modifier.fillMaxWidth().height(52.dp).background(cs.onBackground, RoundedCornerShape(10.dp))
            .clickable(onClick = onBack), contentAlignment = Alignment.Center) {
            Text("Back to session", fontFamily = GeistSans, fontWeight = FontWeight.SemiBold, fontSize = 15.sp, color = cs.background)
        }
        Text("Your session keeps running", fontFamily = GeistSans, fontSize = 12.sp, color = cs.onSurfaceVariant,
            textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 24.dp))
    }
}
