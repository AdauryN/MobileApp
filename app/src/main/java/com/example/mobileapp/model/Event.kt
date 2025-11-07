package com.example.mobileapp.model

import com.google.gson.annotations.SerializedName

data class EventsResponse(
    @SerializedName("events_results")
    val eventsResults: List<EventItem>?
)

data class EventItem(
    val title: String,
    val address: List<String>?,
    val date: EventDate?
)

data class EventDate(
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("when")
    val whenText: String?
)
