package zero.network.reto1

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager.PERMISSION_GRANTED
import android.graphics.Bitmap.createScaledBitmap
import android.graphics.drawable.BitmapDrawable
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.location.LocationManager.GPS_PROVIDER
import android.os.Bundle
import android.widget.EditText
import android.widget.TextView
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

class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocListener {

    private lateinit var mMap: GoogleMap


    private lateinit var user: Marker
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

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        if (requestCode == REQUEST_CODE && grantResults[0] == PERMISSION_GRANTED)
            initMap() // if permission is granted init the app
        else if (requestCode == REQUEST_CODE && grantResults[0] != PERMISSION_GRANTED) {
            showToast("This App require the location permission to work", LENGTH_LONG)
            finish() // else finish the app
        }
    }

    @SuppressLint("MissingPermission")
    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        locationManager.requestLocationUpdates(GPS_PROVIDER, 1000, 1f, this)

        val bitmap = (resources.getDrawable(R.drawable.you_are_here, null) as BitmapDrawable).bitmap
        val smallMarker = createScaledBitmap(bitmap, ICON_SIZE, ICON_SIZE, false)

        val lastPosition =
            locationManager.getLastKnownLocation(GPS_PROVIDER)?.latlng ?: LatLng(3.4, -76.5)

        user = MarkerOptions()
            .position(lastPosition)
            .title("You")
            .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))
            .let { mMap.addMarker(it) }

        mMap.setOnMapClickListener {
            if (isAddMode) {
                isAddMode = false
                newMarkerDialog(EditText(this), it).show() // Create Dialog to add a new marker
            }
        }

        mMap.animateCamera(newLatLngZoom(lastPosition, INITIAL_ZOOM))
    }

    private fun newMarkerDialog(input: EditText, position: LatLng) = AlertDialog.Builder(this)
        .setTitle("Insert the marker name")
        .setView(input)
        .setPositiveButton("OK") { _, _ -> createNewMarker(position, input.text.toString()) }
        .setNegativeButton("Cancel") { dialog, _ -> dialog.cancel() }
        .setOnDismissListener { showToast("Creation Mode Off", LENGTH_LONG) }


    private fun createNewMarker(position: LatLng, input: String) {
        markers += MarkerOptions()
            .position(position)
            .title(input.ifEmpty { "empty" })
            .let { mMap.addMarker(it) }
            .apply { snippet = "Distance to te location is ${user.position disTo position} mts" }
        updateNear()
    }

    private fun initMap() {
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        val mapFragment = supportFragmentManager
            .findFragmentById(R.id.map) as SupportMapFragment
        mapFragment.getMapAsync(this)
        // instantiate view components
        box = textBox
        addButton.setOnClickListener {
            isAddMode = true
            showToast("Creation Mode ON\nPress any site in the map", LENGTH_LONG)
        }
    }

    override fun onLocationChanged(location: Location) = location.latlng.scoped {
        showToast("Updating...", LENGTH_SHORT) // Debug for small position changes
        user.position = this // update user position
        user.snippet = getDirection() // Put the near direction in the user marker
        updateNear() // Find the near marker to the new position and put it in the box
        mMap.animateCamera(newLatLng(this)) // Center Camera
    }

    private fun LatLng.getDirection(): String = Geocoder(this@MapsActivity, Locale.getDefault())
        .getFromLocation(latitude, longitude, 1)[0]
        .getAddressLine(0) ?: "Where are you?"

    private fun updateNear() {
        if (markers.isNotEmpty()) {
            val nearest = markers.minBy {
                (user.position disTo it.position).apply {
                    it.snippet = "Distance to te location is $this mts"
                }
            } ?: markers.first()
            box.text = when {
                user.position disTo nearest.position < MINIMUM_IN_DISTANCE -> "You are in \n${nearest.title}"
                else -> "You are near to \n${nearest.title}"
            }
        }
    }


    companion object {
        private const val REQUEST_CODE = 42 // Random code
        private const val INITIAL_ZOOM = 16f
        private const val MINIMUM_IN_DISTANCE = 100 // in mts
        private const val ICON_SIZE = 125 // in pxs
    }
}
