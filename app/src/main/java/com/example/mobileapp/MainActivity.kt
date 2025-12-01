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
import android.content.Intent
import android.location.Geocoder
import android.net.Uri
import androidx.compose.foundation.clickable
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.example.mobileapp.model.EventLocationMap
import com.example.mobileapp.model.TicketInfo
import com.example.mobileapp.model.Venue
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

        setContent {

            var currentScreen by remember { mutableStateOf("home") }
            var selectedEvent by remember { mutableStateOf<EventItem?>(null) }

            val favorites = remember { mutableStateListOf<EventItem>() }

            Scaffold(
                bottomBar = {
                    if (currentScreen != "details") {
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
                            favoriteList = favorites,
                            onEventClick = { event ->
                                selectedEvent = event
                                currentScreen = "details"
                            }
                        )

                        "favorites" -> FavoriteScreen(
                            favorites = favorites,
                            onEventClick = { event ->
                                selectedEvent = event
                                currentScreen = "details"
                            }
                        )

                        "details" -> EventDetailsScreen(
                            event = selectedEvent!!,
                            onBack = {
                                currentScreen = "home"
                            }
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

        //Transform coordinates into city
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

                        // Address
                        val addressArr = obj.optJSONArray("address")
                        val addressList = mutableListOf<String>()
                        if (addressArr != null) {
                            for (j in 0 until addressArr.length()) {
                                addressList.add(addressArr.getString(j))
                            }
                        }
                        // Date
                        val dateObj = obj.optJSONObject("date")
                        val whenText = dateObj?.optString("when", "No date") ?: "No date"

                        // Description
                        val description = obj.optString("description", null)

                        // Link
                        val link = obj.optString("link", null)

                        // Venue
                        val venueObj = obj.optJSONObject("venue")
                        val venueName = venueObj?.optString("name")
                        val venueRating = venueObj?.optDouble("rating")
                        val venueReviews = venueObj?.optInt("reviews")
                        val venueLink = venueObj?.optString("link")

                        val venue = if (venueObj != null) {
                            Venue(
                                name = venueName,
                                rating = venueRating,
                                reviews = venueReviews,
                                link = venueLink
                            )
                        } else null

                        // Ticket Info
                        val ticketArray = obj.optJSONArray("ticket_info")
                        val ticketList = mutableListOf<TicketInfo>()
                        if (ticketArray != null) {
                            for (j in 0 until ticketArray.length()) {
                                val t = ticketArray.getJSONObject(j)
                                ticketList.add(
                                    TicketInfo(
                                        source = t.optString("source", null),
                                        link = t.optString("link", null),
                                        linkType = t.optString("link_type", null)
                                    )
                                )
                            }
                        }

                        // Event Location Map
                        val mapObj = obj.optJSONObject("event_location_map")
                        val eventLocationMap = if (mapObj != null) {
                            EventLocationMap(
                                image = mapObj.optString("image", null),
                                link = mapObj.optString("link", null),
                                serpapiLink = mapObj.optString("serpapi_link", null)
                            )
                        } else null

                        // Add the final event
                        eventList.add(
                            EventItem(
                                title = title,
                                address = addressList,
                                date = EventDate(startDate = "", whenText = whenText),
                                description = description,
                                ticketInfo = ticketList,
                                venue = venue,
                                eventLocationMap = eventLocationMap,
                                link = link
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
    isFavorite: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
            .clickable { onClick() },
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

            // Favorite button
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
fun HomeScreen(
    eventList: List<EventItem>,
    fetchEvents: (String) -> Unit,
    favoriteList: MutableList<EventItem>,
    onEventClick: (EventItem) -> Unit
) {
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
                    },

                    // go to details screen
                    onClick = {
                        onEventClick(event)
                    }
                )
            }
        }
    }
}


@Composable
fun FavoriteScreen(
    favorites: List<EventItem>,
    onEventClick: (EventItem) -> Unit
) {
    Column(modifier = Modifier.padding(16.dp)) {

        Text(
            "Saved Events",
            style = MaterialTheme.typography.titleLarge
        )

        Spacer(modifier = Modifier.height(12.dp))

        LazyColumn {
            items(favorites) { event ->
                EventCard(
                    event = event,
                    isFavorite = true,

                    onSave = {
                        (favorites as MutableList<EventItem>).remove(event)
                    },

                    // go to details screen
                    onClick = {
                        onEventClick(event)
                    }
                )
            }
        }
    }
}


@Composable
fun EventDetailsScreen(event: EventItem, onBack: () -> Unit) {

    val context = LocalContext.current

    fun openUrl(url: String?) {
        if (!url.isNullOrEmpty()) {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
            context.startActivity(intent)
        }
    }

    Column(modifier = Modifier.padding(16.dp)) {

        // Back button
        Text(
            text = "← Back",
            modifier = Modifier
                .padding(bottom = 12.dp)
                .clickable { onBack() },
            style = MaterialTheme.typography.titleMedium
        )

        // Title
        Text(event.title, style = MaterialTheme.typography.titleLarge)

        Spacer(modifier = Modifier.height(12.dp))

        // Description
        if (!event.description.isNullOrEmpty()) {
            Text(event.description!!, style = MaterialTheme.typography.bodyMedium)
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Address
        Text(
            text = event.address?.joinToString(", ") ?: "No address",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
        )

        // Date
        Text(
            text = event.date?.whenText ?: "No date",
            style = MaterialTheme.typography.bodyMedium.copy(color = Color.Gray)
        )

        Spacer(modifier = Modifier.height(12.dp))

        // Event main link
        if (!event.link.isNullOrEmpty()) {
            Text(
                text = "Open Event Page",
                color = Color(0xFF1E88E5),
                modifier = Modifier.clickable { openUrl(event.link) }
            )
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Venue
        if (event.venue != null) {
            Text("Venue: ${event.venue.name}")
            Text("Rating: ${event.venue.rating} ⭐ (${event.venue.reviews} reviews)")
            if (!event.venue.link.isNullOrEmpty()) {
                Text(
                    text = "Open Venue Page",
                    color = Color(0xFF1E88E5),
                    modifier = Modifier.clickable { openUrl(event.venue.link) }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Tickets list
        if (!event.ticketInfo.isNullOrEmpty()) {
            Text("Tickets:", style = MaterialTheme.typography.titleMedium)
            Spacer(modifier = Modifier.height(4.dp))

            event.ticketInfo!!.forEach { ticket ->
                Text(
                    "- ${ticket.source} (${ticket.linkType})",
                    color = Color(0xFF1E88E5),
                    modifier = Modifier.clickable {
                        openUrl(ticket.link)
                    }
                )
            }
            Spacer(modifier = Modifier.height(12.dp))
        }

        // Event location map
        if (event.eventLocationMap?.link != null) {
            Text(
                text = "Open Map",
                color = Color(0xFF1E88E5),
                modifier = Modifier.clickable {
                    openUrl(event.eventLocationMap.link)
                }
            )
        }
    }
}





