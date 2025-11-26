package com.example.mobileapp


import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationListener
import android.location.LocationManager
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.example.mobileapp.model.EventItem
import com.example.mobileapp.model.EventDate
import android.annotation.SuppressLint
import android.location.Geocoder
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import java.util.Locale


class MainActivity : ComponentActivity(), LocationListener {

    private val eventList = mutableStateListOf<EventItem>()

    private val favoriteList = mutableStateListOf<EventItem>()


    // gps
    private lateinit var locationManager: LocationManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        locationManager = getSystemService(LOCATION_SERVICE) as LocationManager
        requestLocationPermission()

        // Search with default city before gps
        //fetchEvents("Dublin")

        setContent {
            var currentScreen by remember { mutableStateOf("home") }

            val favorites = remember { mutableStateListOf<EventItem>() }

            Scaffold(
                bottomBar = {
                    NavigationBar {
                        NavigationBarItem(
                            selected = currentScreen == "home",
                            onClick = { currentScreen = "home" },
                            icon = { Text("🏠") },
                            label = { Text("Home") }
                        )
                        NavigationBarItem(
                            selected = currentScreen == "favorites",
                            onClick = { currentScreen = "favorites" },
                            icon = { Text("⭐") },
                            label = { Text("Favorites") }
                        )
                    }
                }
            ) { innerPadding ->

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                ) {
                    when (currentScreen) {
                        "home" -> HomeScreen(
                            eventList = eventList,
                            fetchEvents = { fetchEvents(it) },
                            favoriteList = favorites
                        )

                        "favorites" -> FavoriteScreen(
                            favorites = favorites
                        )
                    }
                }
            }
        }


    }


    // gps Request permission
    private fun requestLocationPermission() {
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                100
            )
        } else {
            startGPSUpdates()
        }
    }

    // gps update
    @SuppressLint("MissingPermission")
    private fun startGPSUpdates() {
        locationManager.requestLocationUpdates(
            LocationManager.GPS_PROVIDER,3000L,5f,this)
    }

    // gps location change
    override fun onLocationChanged(location: Location) {
        val lat = location.latitude
        val lon = location.longitude

        Log.d("GPS", "New location: $lat, $lon")

        val geocoder = Geocoder(this, Locale.getDefault())
        val result = geocoder.getFromLocation(lat, lon, 1)

        if (result != null && result.isNotEmpty()) {
            val city = result[0].locality
            Log.d("GPS", "Converted city: $city")
            fetchEvents(city)
        } else {
            Log.e("GPS", "Could not determine city from coordinates")
        }
    }



    // Use the API to search events
    private fun fetchEvents(city: String) {
        val apiKey = com.example.mobileapp.BuildConfig.SERPAPI_KEY
        val query = "Events $city"

        val url =
            "https://serpapi.com/search.json?engine=google_events&q=${query.replace(" ", "+")}&hl=en&gl=us&api_key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val body = response.body?.string()
                Log.d("SerpApi", "Response body: $body")


                if (body.isNullOrEmpty()) {
                    Log.e("SerpApi", "Empty response body.")
                    return@launch
                }

                val json = JSONObject(body)
                val results = json.optJSONArray("events_results")

                eventList.clear()
                if (results != null) {
                    for (i in 0 until results.length()) {
                        val obj = results.getJSONObject(i)
                        val title = obj.optString("title", "No title")

                        val addressArr = obj.optJSONArray("address")
                        val addressList = mutableListOf<String>()
                        if (addressArr != null) {
                            for (j in 0 until addressArr.length()) {
                                addressList.add(addressArr.getString(j))
                            }
                        }

                        val dateObj = obj.optJSONObject("date")
                        val whenText = dateObj?.optString("when", "No date") ?: "No date"

                        eventList.add(
                            EventItem(
                                title = title ?: "No title",
                                address = addressList,
                                date = EventDate(startDate = "", whenText = whenText)
                            )
                        )
                    }
                    Log.d("SerpApi", "Loaded ${eventList.size} events for $city.")
                } else {
                    Log.e("SerpApi", "No 'events_results' array found.")
                }

            } catch (e: Exception) {
                Log.e("SerpApi", "Error fetching events: ${e.message}", e)
            }
        }
    }
}

@Composable
fun EventCard(
    event: EventItem,
    onSave: () -> Unit,
    isFavorite: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {

            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = event.title,
                    style = MaterialTheme.typography.titleMedium
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = event.address?.joinToString(", ") ?: "No address",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray
                    )
                )

                Text(
                    text = event.date?.whenText ?: "No date",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color.Gray
                    )
                )
            }

            // Botão de favoritos sem icons — usando unicode
            Text(
                text = if (isFavorite) "★" else "☆",
                modifier = Modifier
                    .padding(start = 8.dp)
                    .clickable { onSave() },
                color = if (isFavorite) Color(0xFFFFD600) else Color.Gray,
                style = MaterialTheme.typography.headlineMedium
            )
        }
    }
}




@Composable
fun HomeScreen(eventList: List<EventItem>, fetchEvents: (String) -> Unit, favoriteList: MutableList<EventItem>) {
    var city by remember { mutableStateOf("") }

    Column(modifier = Modifier.padding(16.dp)) {
        TextField(
            value = city,
            onValueChange = { city = it },
            label = { Text("Search events by city") },
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = {
                if (city.isNotBlank()) fetchEvents(city)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Search")
        }

        Spacer(modifier = Modifier.height(16.dp))

        LazyColumn {
            items(eventList) { event ->
                EventCard(
                    event = event,
                    isFavorite = favoriteList.contains(event),
                    onSave = {
                        if (favoriteList.contains(event)) {
                            favoriteList.remove(event)
                        } else {
                            favoriteList.add(event)
                        }
                    }
                )


            }
        }
    }
}

@Composable
fun FavoriteScreen(favorites: List<EventItem>) {
    Column(modifier = Modifier.padding(16.dp)) {
        Text("Saved Events", style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(favorites) { event ->
                EventCard(
                    event = event,
                    isFavorite = true,
                    onSave = {
                        (favorites as MutableList<EventItem>).remove(event)
                    }
                )


            }
        }

    }
}


