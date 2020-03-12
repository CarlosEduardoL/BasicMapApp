package zero.network.reto1

import android.content.Context
import android.location.Location
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng

inline fun <T> T.scoped(scope: T.() -> Unit): Unit = scope()

fun Context.showToast(text: String, duration: Int) = Toast.makeText(this, text, duration).show()

/**
 * 'cast' a [Location] object to [LatLng] object
 */
val Location.latlng: LatLng
    get() = LatLng(latitude, longitude)

/**
 * Distance from one point to another
 */
fun distanceBetween(pointOne: LatLng, pointTwo: LatLng) = floatArrayOf(0f).apply {
    Location.distanceBetween(
        pointOne.latitude,
        pointOne.longitude,
        pointTwo.latitude,
        pointTwo.longitude,
        this
    )
}[0]

/**
 * Distance from one point to another
 */
infix fun LatLng.disTo(pointTwo: LatLng) = distanceBetween(this, pointTwo)