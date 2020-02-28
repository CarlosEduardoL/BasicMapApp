package zero.network.reto1

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.text.InputType
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.CameraUpdateFactory
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
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        // Subscribe to the location update
        subscribeLocation()
        mapFragment.getMapAsync(this)
        box = textBox
        addButton.setOnClickListener {
            isAddMode = true
            Toast.makeText(this, "Creation Mode ON\nPress any site in the map", Toast.LENGTH_LONG)
                .show()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val bitmap = (resources.getDrawable(R.drawable.you_are_here, null) as BitmapDrawable).bitmap
        val smallMarker = Bitmap.createScaledBitmap(bitmap, 100, 100, false)

        user = mMap.addMarker(
            MarkerOptions()
                .position(LatLng(.0, .0))
                .title("You")
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
        )
        mMap.setOnMapClickListener {
            if (isAddMode) {
                isAddMode = false
                val input = EditText(this).apply { inputType = InputType.TYPE_CLASS_TEXT }
                // Create Dialog to add a new marker
                newMarkerDialog(input, it).show()
            }
        }
    }

    private fun newMarkerDialog(input: EditText, position: LatLng) = AlertDialog.Builder(this)
        .setTitle("Insert the marker name")
        .setView(input)
        .setPositiveButton("OK") { _, _ -> createNewMarker(position, input) }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        .setOnDismissListener { showToast("Creation Mode Off", Toast.LENGTH_LONG) }



    private fun createNewMarker(position: LatLng, input: EditText) {
        markers += mMap.addMarker(
            MarkerOptions()
                .position(position)
                .title(input.text.toString().ifEmpty { "empty" })
        ).apply {
            snippet = "Distance to te location is ${distanceBetween(user.position, position)} mts"
        }
        updateNear(user.position)
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

    private fun subscribeLocation() {
        try {
            locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
            if (checkPermissions())
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 500, 2f, this)
            else
                requestPermission()
        } catch (e: SecurityException) {
            e.printStackTrace()
        }
    }

    private fun requestPermission() {
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
        if (requestCode == REQUEST_CODE && grantResults[0] == PackageManager.PERMISSION_GRANTED)
            subscribeLocation()
        else if (requestCode == REQUEST_CODE && grantResults[0] != PackageManager.PERMISSION_GRANTED)
            finish()
    }

    private fun checkPermissions() = ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    override fun onLocationChanged(location: Location) =
        LatLng(location.latitude, location.longitude).scoped {
            // Debug for small position changes
            showToast("Updating...", Toast.LENGTH_SHORT)
            // update user position
            user.position = LatLng(latitude, longitude)
            // Put the near direction in the user marker
            user.snippet = getDirection()
            // Find the near marker to the new position and put it in the box
            updateNear(this)
            centerCamera(this)
        }

    private fun LatLng.getDirection(): String = Geocoder(this@MapsActivity, Locale.getDefault())
        .getFromLocation(latitude, longitude, 1)[0]
        .getAddressLine(0) ?: "Where are you?"

    private fun centerCamera(position: LatLng) = when {
        firstTime -> {
            firstTime = false
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(position, 15.0f)) // fix zoom
        }
        else -> {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(position))
        }
    }

    private fun updateNear(position: LatLng) {
        if (markers.isNotEmpty()) {
            val nearest = markers.minBy {
                it.snippet = "Distance to te location is ${distanceBetween(user.position, it.position)} mts"
                distanceBetween(position, it.position)
            } ?: markers[0]
            box.text = if (distanceBetween(position, nearest.position) < 100)
                "You are in \n${nearest.title}" else "You are near to \n${nearest.title}"
        }
    }

    private inline fun <T> T.scoped(scope: T.() -> Unit): Unit = scope()

    private fun showToast(text: String, duration: Int) = Toast.makeText(this, text, duration).show()

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {}

    override fun onProviderEnabled(provider: String?) {}

    override fun onProviderDisabled(provider: String?) {}

    companion object {
        private const val REQUEST_CODE = 42
    }
}
