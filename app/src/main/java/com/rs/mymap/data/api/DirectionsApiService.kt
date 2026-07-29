package com.rs.mymap.data.api

import com.rs.mymap.data.model.DirectionsResponse
import retrofit2.http.GET
import retrofit2.http.Query

interface DirectionsApiService {
    @GET("maps/api/directions/json")
    suspend fun getDirections(
        @Query("origin") origin: String,
        @Query("destination") destination: String,
        @Query("mode") mode: String = "driving",
        @Query("avoid") avoid: String? = null,
        @Query("alternatives") alternatives: Boolean = true,
        @Query("key") apiKey: String
    ): DirectionsResponse
}
