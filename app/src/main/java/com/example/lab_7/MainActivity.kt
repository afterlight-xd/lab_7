package com.example.lab_7

import android.Manifest
import android.R.id
import android.app.Activity
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.database.sqlite.SQLiteDatabase
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt


private var coords = ArrayList<Coordinate>()
private lateinit var con: SQLiteDatabase;

class MainActivity : AppCompatActivity() {

    private val MY_PERMISSIONS_REQUEST_LOCATION = 1
    private var PERMISSION_ASKED = false
    private var TOAST_SHOWN = false
    private val RESULT_DELETE = 15
    private var locationManager: LocationManager? = null
    private val  CHANNEL_ID = "first_channel"
    private val notificationId = 101
    private val trackingRadiusMeters = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager

        checkPermissions()

        // Настройка списка
        val listView: ListView = findViewById(R.id.listItems)
        listView.adapter = CoordAdapter(this, coords)
        listView.setOnItemClickListener { adapterView: AdapterView<*>,
                                          view1: View, i: Int, l: Long ->
            val intent = Intent(this, CoordActivity::class.java)
            intent.putExtra("index", i)
            intent.putExtra("coord", coords[i])
            startActivityForResult(intent, 0)
        }

        val db = SQLiteHelper(this);
        con = db.readableDatabase
        getCoords()

        createNotificationChannel()
    }

    private fun createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            val name = "Notification Title"
            val descriptionText = "Not"
            val importance = NotificationManager.IMPORTANCE_DEFAULT
            val channel = NotificationChannel(CHANNEL_ID, name, importance).apply {  description = descriptionText}
            val notificationManager: NotificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun sendNotification(place: String){
        val builder = NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.ic_done)
                .setContentText("Ты близко к $place")
                .setContentTitle("title")
                .setPriority(NotificationCompat.PRIORITY_DEFAULT)
        with(NotificationManagerCompat.from(this)){
            notify(notificationId, builder.build())
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int,
                                  data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK)
        {
            val index: Int = data?.getIntExtra("index", -1) ?: -1
            val coord: Coordinate = data?.getParcelableExtra("coord") ?: Coordinate()

            val cv = ContentValues()
            cv.put("name", coord.name)
            cv.put("lon", coord.lon)
            cv.put("lat", coord.lat)
            if (index != -1) {
                coords[index] = coord
                cv.put("id", coord.id)
                con.update("coords", cv, "id=?", arrayOf(coord.id.toString()))
            }
            else {
                //items.add(item)
                con.insert("coords", null, cv)
                coords.clear()
                getCoords()
            }

            val listView: ListView = findViewById(R.id.listItems)
            (listView.adapter as CoordAdapter).notifyDataSetChanged()
        }
        if (resultCode == RESULT_DELETE){
            val coordToDelete = data?.getParcelableExtra("coord to delete") ?: Coordinate()
            con.delete("coords", "id=?", arrayOf(coordToDelete.id.toString()))
            coords.clear()
            getCoords()
            val listView: ListView = findViewById(R.id.listItems)
            (listView.adapter as CoordAdapter).notifyDataSetChanged()
        }
    }

    override fun onResume() {
        super.onResume()
        startTracking()
    }

    fun startTracking(location: Location? = null) {
        // Проверяем есть ли разрешение
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            // Здесь код работы с разрешениями...
            if (!PERMISSION_ASKED)
                checkPermissions()
        }
        else {
            locationManager!!.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER, 1000, 10f, locationListener)
            locationManager!!.requestLocationUpdates(
                    LocationManager.NETWORK_PROVIDER, 1000, 10f, locationListener)
            showInfo()
            if (location != null)
                checkCoords(location.longitude, location.latitude)
        }
    }

    fun checkCoords(userLon: Double, userLat: Double){
        val cursor = con.query("coords",
                arrayOf("id", "name", "lon", "lat"),
                null, null, null, null, null)
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            var lon = cursor.getDouble(2)
            var lat = cursor.getDouble(3)
            if (distanceInMBetweenEarthCoordinates(userLon, userLat, lon, lat) < trackingRadiusMeters)
                sendNotification(cursor.getString(1))
            cursor.moveToNext()
        }
        cursor.close()
    }

    private fun degreesToRadians(degrees: Double):Double {
        return degrees * Math.PI / 180;
    }

    private fun distanceInMBetweenEarthCoordinates(lon1: Double, lat1: Double, lon2: Double, lat2: Double): Double{
        var earthRadiusM = 6371000.0

        var dLat = degreesToRadians(lat2 - lat1)
        var dLon = degreesToRadians(lon2 - lon1)

        var lat1 = degreesToRadians(lat1)
        var lat2 = degreesToRadians(lat2)

        var a = sin(dLat / 2) * sin(dLat / 2) + sin(dLon / 2) *
                sin(dLon / 2) * cos(lat1) * cos(lat2);
        var c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return earthRadiusM * c
    }

    private val locationListener: LocationListener = object : LocationListener {
        override fun onLocationChanged(location: Location) {
            showInfo(location)
            checkCoords(location.longitude, location.latitude)
        }
        override fun onProviderDisabled(provider: String) { showInfo() }
        override fun onProviderEnabled(provider: String) { showInfo() }
        override fun onStatusChanged(provider: String, status: Int,
                                     extras: Bundle) { showInfo() }
    }

    private fun showInfo(location: Location? = null) {
        val isGpsOn = locationManager!!.isProviderEnabled(LocationManager.GPS_PROVIDER)
        val isNetworkOn = locationManager!!.
        isProviderEnabled(LocationManager.NETWORK_PROVIDER)
        findViewById<TextView>(R.id.gps_status).text =
                if (isGpsOn) "GPS ON" else "GPS OFF"
        findViewById<TextView>(R.id.network_status).text =
                if (isNetworkOn) "Network ON" else "Network OFF"
        if (location != null) {
            if (location.provider == LocationManager.GPS_PROVIDER) {
                findViewById<TextView>(R.id.gps_coords).text =
                        "GPS: широта = " + location.latitude.toString() +
                                ", долгота = " + location.longitude.toString()
            }
            if (location.provider == LocationManager.NETWORK_PROVIDER) {
                findViewById<TextView>(R.id.network_coords).text =
                        "Network: широта = " + location.latitude.toString() +
                                ", долгота = " + location.longitude.toString()
            }
        }
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.main_menu, menu)
        return super.onCreateOptionsMenu(menu)
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.action_add) {
            val intent = Intent(this, CoordActivity::class.java)
            startActivityForResult(intent, 0)
        }

        return super.onOptionsItemSelected(item)
    }

    override fun onPause() {
        super.onPause()
        stopTracking()
    }

    fun stopTracking() {
        locationManager!!.removeUpdates(locationListener)
    }

    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (requestCode == MY_PERMISSIONS_REQUEST_LOCATION) {
            // Разрешение есть, заново выполняем требуемое действие
            startTracking()
        }
        else {
            // Разрешения нет...
            stopTracking()
        }
    }

    fun buttonOpenSettings(view: View) {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED)
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
    }

    private fun checkPermissions() {
        if (ContextCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_FINE_LOCATION) !=
                PackageManager.PERMISSION_GRANTED) {
            // Разрешения нет. Нужно ли показать пользователю пояснения?
            if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                            Manifest.permission.ACCESS_FINE_LOCATION)) {
                // Показываем пояснения
                if (!TOAST_SHOWN) {
                    val toast = Toast.makeText(this,
                            "Нужно дать доступ к геолокации",
                            Toast.LENGTH_SHORT)
                    toast.show()
                    TOAST_SHOWN = true
                }
            }
            else
            // Запрашиваем разрешение
            ActivityCompat.requestPermissions(this,
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    MY_PERMISSIONS_REQUEST_LOCATION)
        }
        /*else {
            // Разрешение есть, выполняем требуемое действие
        }*/
    }

    private fun getCoords() {
        val cursor = con.query("coords",
                arrayOf("id", "name", "lon", "lat"),
                null, null, null, null, null)
        cursor.moveToFirst()
        while (!cursor.isAfterLast) {
            val s = Coordinate()
            s.id = cursor.getInt(0)
            s.name = cursor.getString(1)
            s.lon = cursor.getDouble(2)
            s.lat = cursor.getDouble(3)
            coords.add(s)
            cursor.moveToNext()
        }
        cursor.close()
    }

}