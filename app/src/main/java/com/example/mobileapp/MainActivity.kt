package com.example.mobileapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.TextField
import androidx.compose.runtime.mutableStateOf
import com.google.firebase.ai.type.GoogleSearch


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            Column {
                TextField(value = search.value, onValueChange = {search.value = it})
                val parameter: MutableMap<String?, String?> = HashMap<String?, String?>()

                parameter.put("engine", "google_events")
                parameter.put("q", "Events in Austin")
                parameter.put("hl", "en")
                parameter.put("gl", "us")
                parameter.put("api_key", "secret_api_key")

                val search: GoogleSearch = GoogleSearch()

                try {
                    val results: JsonObject? = search.getJson()
                } catch (ex: SerpApiSearchException) {
                    println("Exception:")
                    System.out.println(ex.toString())
                }
            }
        }
    }



    var search = mutableStateOf("Search place")
}