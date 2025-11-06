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
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.QueryMap
import com.example.mobileapp.model.EventsResponse
import com.example.mobileapp.model.EventItem

// Retrofit interface for SerpApi
private interface SerpApiService {
    @GET("search.json")
    suspend fun searchEvents(@QueryMap params: Map<String, String>): retrofit2.Response<EventsResponse>
}

class MainActivity : ComponentActivity() {

    // Lista observável de eventos (visível ao Compose)
    private val eventList = mutableStateListOf<EventItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Busca inicial
        fetchEvents("Dublin")

        setContent {
            var searchText by remember { mutableStateOf("") }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
            ) {
                // Campo de busca simples
                TextField(
                    value = searchText,
                    onValueChange = { searchText = it },
                    label = { Text("Search events by city") },
                    modifier = Modifier.fillMaxWidth()
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Botão que dispara a busca — evita usar IME / KeyboardOptions
                Button(
                    onClick = {
                        val query = searchText.trim()
                        if (query.isNotEmpty()) {
                            fetchEvents(query)
                        }
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = "Search")
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Lista de eventos
                LazyColumn(modifier = Modifier.fillMaxSize()) {
                    items(eventList) { event ->
                        EventCard(event)
                    }
                }
            }
        }
    }

    // Função que faz a chamada à SerpApi (não composable)
    private fun fetchEvents(location: String) {
        lifecycleScope.launch {
            val retrofit = Retrofit.Builder()
                .baseUrl("https://serpapi.com/")
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val service = retrofit.create(SerpApiService::class.java)

            val params = mapOf(
                "engine" to "google_events",
                "q" to "Events in $location",
                "hl" to "en",
                "gl" to "ie",
                "api_key" to com.example.mobileapp.BuildConfig.SERPAPI_KEY
            )

            try {
                val response = service.searchEvents(params)
                if (response.isSuccessful) {
                    val events = response.body()?.eventsResults ?: emptyList()
                    eventList.clear()
                    eventList.addAll(events)
                    Log.d("SerpApi", "Loaded ${events.size} events for $location.")
                } else {
                    Log.e("SerpApi", "Error: ${response.code()} - ${response.errorBody()?.string()}")
                }
            } catch (e: Exception) {
                Log.e("SerpApi", "Exception: ${e.message}", e)
            }
        }
    }
}

// Composable que mostra cada evento (simples)
@Composable
fun EventCard(event: EventItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
    ) {
        //Text(text = event.title)
        Text(text = event.address?.joinToString(", ") ?: "No address")
        Text(text = event.date?.whenText ?: "No date")

    }
}
