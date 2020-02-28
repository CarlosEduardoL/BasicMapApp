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
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.google.android.gms.maps.*
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Marker
import com.google.android.gms.maps.model.MarkerOptions
import kotlinx.android.synthetic.main.activity_maps.*
import java.util.*


class MapsActivity : AppCompatActivity(), OnMapReadyCallback, LocationListener {

    private lateinit var mMap: GoogleMap


    private lateinit var you: Marker
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
        box = findViewById(R.id.box)
        addButton.setOnClickListener {
            isAddMode = true
            Toast.makeText(this, "Creation Mode ON\nPress any site in the map", Toast.LENGTH_LONG).show()
        }

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap

        val bitmap =
            resources.getDrawable(R.drawable.you_are_here, null) as BitmapDrawable
        val b = bitmap.bitmap
        val smallMarker = Bitmap.createScaledBitmap(b, 100, 100, false)

        you = mMap.addMarker(
            MarkerOptions()
                .position(LatLng(.0, .0))
                .title("You")
                .icon(BitmapDescriptorFactory.fromBitmap(smallMarker))

        )
        mMap.setOnMapClickListener {
            if (isAddMode) {
                isAddMode = false

                AlertDialog.Builder(this).apply {
                    setTitle("Insert the marker name")
                    val input = EditText(this@MapsActivity).apply {
                        inputType = InputType.TYPE_CLASS_TEXT
                        width = (width.toDouble() * .8).toInt()
                    }

                    setView(input)
                    dialogButtons(input, it)
                    show()
                }
            }
        }
    }

    private fun AlertDialog.Builder.dialogButtons(
        input: EditText,
        it: LatLng
    ) {
        setPositiveButton(
            "OK"
        ) { _, _ ->
            val tittle = input.text.toString()
            val distance = floatArrayOf(0f)
            Location.distanceBetween(
                you.position.latitude,
                you.position.longitude,
                it.latitude,
                it.longitude,
                distance
            )
            markers += mMap.addMarker(
                MarkerOptions()
                    .position(it)
                    .title(if (tittle.isEmpty()) "empty" else tittle)
            ).apply {
                snippet = "Distance to te location is ${distance[0]} mts"
            }
            Toast.makeText(this@MapsActivity, "Creation Mode Off", Toast.LENGTH_LONG).show()
            updateNear(you.position.latitude, you.position.longitude)
        }
        setNegativeButton(
            "Cancel"
        ) { dialog, _ ->
            dialog.cancel()
            Toast.makeText(this@MapsActivity, "Creation Mode Off", Toast.LENGTH_LONG).show()
        }
    }

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
            42
        )
    }

    private fun checkPermissions() = ActivityCompat.checkSelfPermission(
        this,
        Manifest.permission.ACCESS_FINE_LOCATION
    ) == PackageManager.PERMISSION_GRANTED

    override fun onLocationChanged(location: Location) {
        val latitude = location.latitude
        val longitude = location.longitude
        // Debug for small position changes
        Toast.makeText(this, "Upadating...", Toast.LENGTH_SHORT).show()
        if (latitude != you.position.latitude || longitude != you.position.longitude) {
            you.position = LatLng(latitude, longitude)
            // Put the near direction in the user marker
            you.snippet = Geocoder(this, Locale.getDefault())
                .getFromLocation(latitude, longitude, 1)[0]
                .getAddressLine(0)
            // Find the near marker to the new position and put it in the box
            updateNear(latitude, longitude)
            centerCamera(latitude, longitude)
        }
    }

    private fun centerCamera(latitude: Double, longitude: Double) {
        if (firstTime) {
            firstTime = false
            mMap.animateCamera(
                CameraUpdateFactory.newLatLngZoom(
                    LatLng(latitude, longitude),
                    15.0f // fix zoom
                )
            )
        } else {
            mMap.moveCamera(CameraUpdateFactory.newLatLng(LatLng(latitude, longitude)))
        }
    }

    private fun updateNear(latitude: Double, longitude: Double) {
        val distance = floatArrayOf(0f)
        var minStr = "You don't have any mark\nplease press over the + button"
        var min = Float.MAX_VALUE
        markers.forEach {
            Location.distanceBetween(
                latitude,
                longitude,
                it.position.latitude,
                it.position.longitude,
                distance
            )
            it.snippet = "Distance to te location is ${distance[0]} mts"
            if (distance[0] < min) {
                min = distance[0]
                minStr =
                    if (min < 30) "You are in \n${it.title}" else "You are near to \n${it.title}"
            }
        }
        box.text = minStr
    }

    override fun onStatusChanged(provider: String?, status: Int, extras: Bundle?) {
    }

    override fun onProviderEnabled(provider: String?) {
    }

    override fun onProviderDisabled(provider: String?) {
    }
}
