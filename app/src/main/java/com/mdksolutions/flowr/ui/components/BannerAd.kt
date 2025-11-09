package com.mdksolutions.flowr.ui.components

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

// Google test banner unit ID. Swap to your real ID for release builds.
private const val TEST_BANNER_ID = "ca-app-pub-3940256099942544/6300978111"

@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = TEST_BANNER_ID,
    adSize: AdSize = AdSize.BANNER // keep simple for now; we can do adaptive next
) {
    AndroidView(
        modifier = modifier,
        factory = { context ->
            AdView(context).apply {
                setAdSize(adSize)
                this.adUnitId = adUnitId
                layoutParams = android.widget.FrameLayout.LayoutParams(MATCH_PARENT, WRAP_CONTENT)
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
