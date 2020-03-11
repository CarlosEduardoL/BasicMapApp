package zero.network.reto1

import android.content.Context
import android.location.Location
import android.widget.Toast
import com.google.android.gms.maps.model.LatLng

inline fun <T> T.scoped(scope: T.() -> Unit): Unit = scope()

fun Context.showToast(text: String, duration: Int) = Toast.makeText(this, text, duration).show()

val Location.latlng: LatLng
    get() = LatLng(latitude, longitude)