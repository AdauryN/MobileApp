package com.example.mobileapp.model

import com.google.gson.annotations.SerializedName

data class EventsResponse(
    @SerializedName("events_results")
    val eventsResults: List<EventItem>?
)

data class EventItem(
    val title: String,
    val address: List<String>?,
    val date: EventDate?,
    val link: String? = null,
    val description: String? = null,
    val thumbnail: String? = null,
    val image: String? = null,

    @SerializedName("event_location_map")
    val eventLocationMap: EventLocationMap? = null,

    @SerializedName("ticket_info")
    val ticketInfo: List<TicketInfo>? = null,

    val venue: Venue? = null
)
data class Venue(
    val name: String?,
    val rating: Double?,
    val reviews: Int?,
    val link: String?
)
data class EventDate(
    @SerializedName("start_date")
    val startDate: String?,
    @SerializedName("when")
    val whenText: String?
)

data class EventLocationMap(
    val image: String?,
    val link: String?,
    @SerializedName("serpapi_link")
    val serpapiLink: String?
)

data class TicketInfo(
    val source: String?,
    val link: String?,
    val linkType: String?
)


