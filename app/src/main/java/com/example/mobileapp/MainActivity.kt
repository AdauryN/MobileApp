package com.example.mobileapp

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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import com.example.mobileapp.model.EventItem
import com.example.mobileapp.model.EventDate

class MainActivity : ComponentActivity() {

    private val eventList = mutableStateListOf<EventItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        //Initial Search later will use GPS location of the user
        fetchEvents("Dublin")

        setContent {
            var city by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                TextField(
                    value = city,
                    onValueChange = { city = it },
                    label = { Text("Search events by city") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        val query = city.trim()
                        if (query.isNotEmpty()) fetchEvents(query)
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Search")
                }

                Spacer(modifier = Modifier.height(16.dp))

                LazyColumn {
                    items(eventList) { event ->
                        EventCard(event)
                    }
                }
            }
        }
    }
//Use the API to search events
    private fun fetchEvents(city: String) {
        val apiKey = com.example.mobileapp.BuildConfig.SERPAPI_KEY
        val query = "Events in $city"

        val url =
            "https://serpapi.com/search.json?engine=google_events&q=${query.replace(" ", "+")}&hl=en&gl=us&api_key=$apiKey"

        val client = OkHttpClient()
        val request = Request.Builder().url(url).build()

        lifecycleScope.launch {
            try {
                val response = withContext(Dispatchers.IO) { client.newCall(request).execute() }
                val body = response.body?.string()

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
//Each event card
@Composable
fun EventCard(event: EventItem) {
    androidx.compose.material3.Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        elevation = androidx.compose.material3.CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            //Name
            Text(
                text = event.title ?: "No title",
                style = androidx.compose.material3.MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(4.dp))

            //Local
            Text(
                text = event.address?.joinToString(", ") ?: "No address",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            )

            //Date
            Text(
                text = event.date?.whenText ?: "No date",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall.copy(
                    color = androidx.compose.ui.graphics.Color.Gray
                )
            )
        }
    }
}

