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

    private var rewardedAd: RewardedAd? = null

    fun load(activity: Activity, adUnitId: String = AdUnits.rewarded) {
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

    fun show(activity: Activity, onEarned: (reward: RewardItem) -> Unit = {}) {
        val ad = rewardedAd ?: run { load(activity); return }

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewardedAd = null
                load(activity)
            }
            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                rewardedAd = null
                load(activity)
            }
        }

        ad.show(activity) { rewardItem -> onEarned(rewardItem) }
    }
}
