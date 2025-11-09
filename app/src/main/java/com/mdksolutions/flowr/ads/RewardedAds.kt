package com.mdksolutions.flowr.ads

import android.app.Activity
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback

object RewardedAds {

    // Google TEST rewarded ad unit. Swap to prod for release builds.
    private const val TEST_REWARDED_ID = "ca-app-pub-3940256099942544/5224354917"

    private var rewardedAd: RewardedAd? = null

    fun load(activity: Activity, adUnitId: String = TEST_REWARDED_ID) {
        val request = AdRequest.Builder().build()
        RewardedAd.load(
            activity,
            adUnitId,
            request,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    Log.d("RewardedAds", "Rewarded loaded")
                }
                override fun onAdFailedToLoad(error: com.google.android.gms.ads.LoadAdError) {
                    rewardedAd = null
                    Log.w("RewardedAds", "Failed to load: ${error.message}")
                }
            }
        )
    }

    fun show(
        activity: Activity,
        onEarned: (reward: RewardItem) -> Unit = {}
    ) {
        val ad = rewardedAd ?: run {
            // try to load and bail; user can tap again in a moment
            load(activity)
            return
        }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdShowedFullScreenContent() {
                Log.d("RewardedAds", "Shown")
            }
            override fun onAdDismissedFullScreenContent() {
                Log.d("RewardedAds", "Dismissed; reloading")
                rewardedAd = null
                load(activity) // prepare the next one
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w("RewardedAds", "Failed to show: ${adError.message}")
                rewardedAd = null
                load(activity)
            }
        }

        ad.show(activity) { rewardItem ->
            onEarned(rewardItem)
        }
    }
}
