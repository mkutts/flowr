package com.mdksolutions.flowr.util

import android.app.Activity
import android.widget.Toast
import com.google.android.gms.common.ConnectionResult
import com.google.android.gms.common.GoogleApiAvailability

/**
 * Returns true if Google Play services is available & healthy on this device.
 * If not, shows the standard resolvable dialog (install/enable/update) when possible.
 */
fun ensurePlayServices(activity: Activity): Boolean {
    val gaa = GoogleApiAvailability.getInstance()
    val code = gaa.isGooglePlayServicesAvailable(activity)
    return if (code == ConnectionResult.SUCCESS) {
        true
    } else {
        if (gaa.isUserResolvableError(code)) {
            gaa.getErrorDialog(activity, code, /*requestCode*/ 1001)?.show()
        } else {
            Toast.makeText(
                activity,
                "Google Play services not available on this device.",
                Toast.LENGTH_LONG
            ).show()
        }
        false
    }
}
