package com.example.cobauts.Activities

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.ImageView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.example.cobauts.Models.WeatherModel
import com.example.cobauts.R
import com.example.cobauts.Utilities.ApiUtilities
import com.example.cobauts.databinding.ActivityMainBinding
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.math.RoundingMode
import java.time.Instant
import java.time.OffsetDateTime
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.roundToInt

class MainActivity : AppCompatActivity(), View.OnClickListener {

    lateinit var btnInfo: ImageView
    lateinit var binding: ActivityMainBinding
    private lateinit var currentLocation: Location
    private lateinit var fusedLocationProvider:FusedLocationProviderClient
    private val LOCATION_REQUEST_CODE = 116
    private val apiKey="8956cbfaed74e0346453a0f13398b3bd"


    fun openlayout(view: View) {
        
        val intent = Intent(this, Info::class.java)
        startActivity(intent)
    }

    fun openmain(view: View) {

        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)


        binding = DataBindingUtil.setContentView(this,R.layout.activity_main)
        fusedLocationProvider = LocationServices.getFusedLocationProviderClient(this)
        getCurrentLocation()
        binding.citySearch.setOnEditorActionListener { textView, i, keyEvent ->

            if (i== EditorInfo.IME_ACTION_SEARCH){
                getCityWeather(binding.citySearch.text.toString())
                val view=this.currentFocus
                if (view!=null){
                    val imm: InputMethodManager =getSystemService(INPUT_METHOD_SERVICE)
                            as InputMethodManager

                    imm.hideSoftInputFromWindow(view.windowToken,0)
                    binding.citySearch.clearFocus()
                }
                return@setOnEditorActionListener true
            }
            else{
                return@setOnEditorActionListener false
            }

        }

        binding.currentLocation.setOnClickListener {

            getCurrentLocation()

        }

    }

    private fun getCityWeather(city: String) {

        binding.progressBar.visibility = View.VISIBLE

        ApiUtilities.getApiInterface()?.getCityWeatherData(city, apiKey)?.enqueue(
            object : Callback<WeatherModel> {
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(
                    call: Call<WeatherModel>,
                    response: Response<WeatherModel>
                ) {
                    if (response.isSuccessful) {
                        binding.progressBar.visibility = View.GONE
                        response.body()?.let {
                            setData(it)
                        }
                    } else {
                        Toast.makeText(
                            this@MainActivity, "No City Found",
                            Toast.LENGTH_SHORT
                        ).show()
                        binding.progressBar.visibility = View.GONE
                    }
                }
                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                }
            }
        )

    }

    private fun fetchCurrentLocationWeather(latitude: String, longitude: String) {
        binding.progressBar.visibility = View.VISIBLE

        ApiUtilities.getApiInterface()?.getCurrentWeatherData(latitude,longitude,apiKey)?.enqueue(
            object :Callback<WeatherModel>{
                @RequiresApi(Build.VERSION_CODES.O)
                override fun onResponse(call: Call<WeatherModel>, response: Response<WeatherModel>) {
                    if (response.isSuccessful){
                        binding.progressBar.visibility= View.GONE
                        response.body()?.let {
                            setData(it)
                        }
                    }
                }
                override fun onFailure(call: Call<WeatherModel>, t: Throwable) {
                }

            }
        )
    }

    private fun getCurrentLocation(){

        if (checkPermissions()){

            if (isLocationEnabled()){

                if (ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_FINE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                        this,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    ) != PackageManager.PERMISSION_GRANTED
                ) {
                    requestPermission()
                    return
                }
                fusedLocationProvider.lastLocation.addOnSuccessListener {
                        location ->
                    if (location != null) {
                        currentLocation = location
                        binding.progressBar.visibility = View.VISIBLE
                        fetchCurrentLocationWeather(
                            location.latitude.toString(),
                            location.longitude.toString()
                        )
                    }
                }
            }
            else{
                val intent = Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                startActivity(intent)
            }
        }
        else{
            requestPermission()
        }
    }

    private fun requestPermission() {
        ActivityCompat.requestPermissions(
            this,
            arrayOf( Manifest.permission.ACCESS_COARSE_LOCATION,
                Manifest.permission.ACCESS_FINE_LOCATION),
            LOCATION_REQUEST_CODE
        )
    }

    private fun isLocationEnabled(): Boolean {

        val locationManager: LocationManager =getSystemService(Context.LOCATION_SERVICE)
                as LocationManager

        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
                ||locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    private fun checkPermissions(): Boolean {

        if (ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_COARSE_LOCATION)
            ==PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED){
            return true
        }
        return false
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode==LOCATION_REQUEST_CODE){
            if (grantResults.isNotEmpty() && grantResults[0]==PackageManager.PERMISSION_GRANTED){
                getCurrentLocation()
            }
            else{

            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setData(body:WeatherModel){
        binding.apply {

            val timezoneOffsetInSeconds = body.timezone
            val zoneOffset = ZoneOffset.ofTotalSeconds(timezoneOffsetInSeconds)
            val currentTime = Instant.now().atOffset(zoneOffset)
            val sunriseTime = Instant.ofEpochSecond(body.sys.sunrise.toLong()).atOffset(zoneOffset)
            val sunsetTime = Instant.ofEpochSecond(body.sys.sunset.toLong()).atOffset(zoneOffset)

            val dateTimeFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy hh:mm a", Locale.getDefault())

            dateTime.text=currentTime.format(dateTimeFormatter)

            maxTemp.text="Max "+k2c(body?.main?.temp_max!!)+"°"

            minTemp.text="Min "+k2c(body?.main?.temp_min!!)+"°"

            temp.text="${k2c(body.main.temp)}°"

            pressureValue.text=body.main.pressure.toString()

            humidityValue.text=body.main.humidity.toString()+"%"

            tempFValue.text=""+(k2c(body.main.temp).times(1.8)).plus(32).roundToInt()+"°"

            citySearch.setText(body.name)

            feelsLike.text= ""+k2c(body.main.feels_like)+"°"

            countryValue.text=body.sys.country

            updateUI(body.weather[0].id, sunriseTime, sunsetTime, currentTime)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun ts2td(ts:Long, zoneOffset: ZoneOffset):String{

        val localTime=Instant.ofEpochSecond(ts)
            .atOffset(zoneOffset)
            .toLocalTime()
        return localTime.toString()
    }
    private fun k2c(t:Double):Double{

        var intTemp=t

        intTemp=intTemp.minus(273)

        return intTemp.toBigDecimal().setScale(1, RoundingMode.UP).toDouble()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateUI(id: Int, sunriseTime: OffsetDateTime, sunsetTime: OffsetDateTime, currentTime: OffsetDateTime) {
        binding.apply {
            val isDayTime = currentTime in sunriseTime..sunsetTime
            when (id) {
                //Thunderstorm
                in 200..232 -> {
                    recomend.setImageResource(R.drawable.swt_merah)
                    bajuValue.setText("Jaket")
                    if (isDayTime){
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.thunder_d)
                    } else {
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.thunder_n)
                    }

                }

                //Drizzle
                in 300..321 -> {

                    bajuValue.setText("Payung")
                    recomend.setImageResource(R.drawable.payung)
                    if (isDayTime){
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.rainny_d)
                    } else {
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.rainny_n)
                    }


                }

                //Rain
                in 500..531 -> {

                    recomend.setImageResource(R.drawable.payung)
                    bajuValue.setText("Payung")
                    if (isDayTime){
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.rainny_d)
                    } else {
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.rainny_n)
                    }
                }

                //Snow
                in 600..622 -> {

                    bajuValue.setText("Sweater")
                    if (isDayTime){
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.sunny)
                    } else {
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.clear_n)
                    }
                }

                //Atmosphere
                in 701..781 -> {

                    bajuValue.setText("Sweater")
                    if (isDayTime){
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.cloudy_d)
                    } else {
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.cloudy_n)
                    }
                }

                //Clear
                800 -> {

                    recomend.setImageResource(R.drawable.kaos)
                    bajuValue.setText("T-shirt")

                    if (isDayTime){
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.sunny)
                    } else {
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.clear_n)
                    }
                }

                //Clouds
                in 801..804 -> {

                    recomend.setImageResource(R.drawable.sweater)
                    bajuValue.setText("Sweater")
                    if (isDayTime){
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.cloudy_d)
                    } else {
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.cloudy_n)
                    }
                }

                //unknown
                else->{

                    if (isDayTime){
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.sunny)
                    } else {
                        mainLayout.background= ContextCompat
                            .getDrawable(this@MainActivity, R.drawable.clear_n)
                    }
                }
            }
        }
    }

    override fun onClick(v: View) {
        when (v.id) {
            R.id.btn_info -> {
                val moveIntent = Intent(this@MainActivity, Info::class.java)
                startActivity(moveIntent)
            }
        }
    }

}