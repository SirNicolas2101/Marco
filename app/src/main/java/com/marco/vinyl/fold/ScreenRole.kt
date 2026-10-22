package com.marco.vinyl.fold

import android.app.Activity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration
import androidx.window.layout.FoldingFeature
import androidx.window.layout.WindowInfoTracker

/** Welche Seite des Fold die App gerade zeigt. */
enum class ScreenRole {
    /** Zugeklappt, Außendisplay: Plattenspieler. */
    Cover,

    /** Aufgeklappt, Innendisplay: Plattenschrank. */
    Inner,
}

/**
 * Aufgeklappt meldet Jetpack WindowManager das Scharnier als [FoldingFeature]. Ohne Scharnier
 * (z. B. App im Split-Screen oder auf einem Tablet/Emulator) entscheidet die kleinste Displaybreite:
 * ab 600 dp gilt das Display als „innen“.
 */
@Composable
fun rememberScreenRole(activity: Activity): ScreenRole {
    val tracker = remember(activity) { WindowInfoTracker.getOrCreate(activity) }
    val layoutInfo by remember(activity) { tracker.windowLayoutInfo(activity) }.collectAsState(initial = null)
    val configuration = LocalConfiguration.current

    val hingeVisible = layoutInfo?.displayFeatures?.any { it is FoldingFeature } == true
    return if (hingeVisible || configuration.smallestScreenWidthDp >= 600) ScreenRole.Inner else ScreenRole.Cover
}
