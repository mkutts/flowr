package com.mdksolutions.flowr

import android.app.Application
import com.google.android.gms.ads.MobileAds

class FlowrApp : Application() {
    override fun onCreate() {
        super.onCreate()
        MobileAds.initialize(this)
    }
}
