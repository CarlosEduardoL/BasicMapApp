package zero.network.reto1

import android.location.LocationListener
import android.os.Bundle

/**
 * Interface to remove unused methods
 */
interface LocListener: LocationListener {
    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}
    override fun onProviderEnabled(provider: String?) {}
    override fun onProviderDisabled(provider: String?) {}
}