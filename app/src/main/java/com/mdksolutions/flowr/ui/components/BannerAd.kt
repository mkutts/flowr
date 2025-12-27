package com.mdksolutions.flowr.ui.components

import android.view.ViewGroup.LayoutParams.MATCH_PARENT
import android.view.ViewGroup.LayoutParams.WRAP_CONTENT
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.mdksolutions.flowr.ads.AdUnits

@Composable
fun BannerAd(
    modifier: Modifier = Modifier,
    adUnitId: String = AdUnits.banner,
    adSize: AdSize = AdSize.BANNER
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
