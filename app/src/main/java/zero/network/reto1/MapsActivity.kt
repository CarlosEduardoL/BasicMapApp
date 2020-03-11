package zero.network.reto1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import android.widget.Toast.LENGTH_LONG
import android.widget.Toast.LENGTH_SHORT
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory.newLatLng
import com.google.android.gms.maps.CameraUpdateFactory.newLatLngZoom
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap


    private lateinit var user: Marker
    private var firstTime = true // in the firs time in the app, the zoom is fixed
    private var isAddMode = false // when is true, the user can add new markers

    /**
     * Information box
     */
    private lateinit var box: TextView

    /**
     * list of actual markers
     */
    private val markers = mutableListOf<Marker>()

    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_maps)

        // Request Permission
        ActivityCompat.requestPermissions(
            this,
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
            REQUEST_CODE
        )

    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE && grantResults[0] == PERMISSION_GRANTED)
            initMap() // if permission is granted init the app
        else if (requestCode == REQUEST_CODE && grantResults[0] != PERMISSION_GRANTED)
            finish() // else finish the app
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(GPS_PROVIDER, 1000, 1f, this)

        val bitmap = (resources.getDrawable(R.drawable.you_are_here, null) as BitmapDrawable).bitmap
        val smallMarker = Bitmap.createScaledBitmap(bitmap, 125, 125, false)

        user = MarkerOptions()
            .position(LatLng(3.4, -76.5))
            .title("You")
            .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
            .let { mMap.addMarker(it) }

        mMap.setOnMapClickListener {
            if (isAddMode) {
                isAddMode = false
                // Create Dialog to add a new marker
                newMarkerDialog(EditText(this), it).show()
            }
        }

        mMap.animateCamera(
            newLatLngZoom(
                locationManager.getLastKnownLocation(GPS_PROVIDER)?.latlng ?: LatLng(3.4, -76.5), 14f
            )
        )
    }

    private fun newMarkerDialog(input: EditText, position: LatLng) = AlertDialog.Builder(this)
        .setTitle("Insert the marker name")
        .setView(input)
        .setPositiveButton("OK") { _, _ -> createNewMarker(position, input) }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        .setOnDismissListener { showToast("Creation Mode Off", LENGTH_LONG) }


    private fun createNewMarker(position: LatLng, input: EditText) {
        markers += mMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(input.text.toString().ifEmpty { "empty" })
        ).apply { snippet = "Distance to te location is ${distanceBetween(user.position, position)} mts" }
        updateNear()
    }

    private fun distanceBetween(pointOne: LatLng, pointTwo: LatLng) = floatArrayOf(0f).apply {
        Location.distanceBetween(
            pointOne.latitude,
            pointOne.longitude,
            pointTwo.latitude,
            pointTwo.longitude,
            this
        )
    }[0]

    private fun initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        box = textBox
        addButton.setOnClickListener {
            isAddMode = true
            Toast.makeText(this, "Creation Mode ON\nPress any site in the map", LENGTH_LONG)
                .show()
        }
    }

    override fun onLocationChanged(location: Location) = location.latlng.scoped {
            // Debug for small position changes
            showToast("Updating...", LENGTH_SHORT)
            // update user position
            user.position = LatLng(latitude, longitude)
            // Put the near direction in the user marker
            user.snippet = getDirection()
            // Find the near marker to the new position and put it in the box
            updateNear()
            centerCamera()
        }

    private fun LatLng.getDirection(): String = Geocoder(this@MapsActivity, Locale.getDefault())
        .getFromLocation(latitude, longitude, 1)[0]
        .getAddressLine(0) ?: "Where are you?"

    private fun centerCamera() = when {
        firstTime -> {
            firstTime = false
            mMap.animateCamera(newLatLngZoom(user.position, 16.0f)) // fix zoom
        }
        else -> {
            mMap.animateCamera(newLatLng(user.position))
        }
    }

    private fun updateNear() {
        if (markers.isNotEmpty()) {
            val nearest = markers.minBy {
                it.snippet =
                    "Distance to te location is ${distanceBetween(user.position, it.position)} mts"
                distanceBetween(user.position, it.position)
            } ?: markers[0]
            box.text = if (distanceBetween(user.position, nearest.position) < 100)
                "You are in \n${nearest.title}" else "You are near to \n${nearest.title}"
        }
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String?) {}

    override fun onProviderDisabled(provider: String?) {}

    companion object {
        private const val REQUEST_CODE = 42
    }
}
