package com.mdksolutions.flowr.ads

import com.mdksolutions.flowr.BuildConfig

object AdUnits {
    private const val TEST_BANNER = "ca-app-pub-3940256099942544/6300978111"
    private const val TEST_REWARDED = "ca-app-pub-3940256099942544/5224354917"

    // TODO: replace with your real Ad Unit IDs
    private const val PROD_BANNER = "ca-app-pub-3774551279310155/7652028149"
    private const val PROD_REWARDED = "ca-app-pub-3774551279310155/2886964235"

    val banner: String get() = if (BuildConfig.DEBUG) TEST_BANNER else PROD_BANNER
    val rewarded: String get() = if (BuildConfig.DEBUG) TEST_REWARDED else PROD_REWARDED
}
