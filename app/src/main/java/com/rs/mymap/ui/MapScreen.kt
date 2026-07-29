package com.rs.mymap.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsBike
import androidx.compose.material.icons.filled.DirectionsCar
import androidx.compose.material.icons.filled.Place
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.ListItem
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.PolyUtil
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
import com.rs.mymap.data.api.RetrofitClient
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MapScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val malang = remember { LatLng(-7.9666, 112.6326) }
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(malang, 12f)
    }

    var originText by remember { mutableStateOf("") }
    var destinationText by remember { mutableStateOf("") }
    
    // Multiple routes from API
    var allRoutes by remember { mutableStateOf<List<com.rs.mymap.data.model.Route>>(emptyList()) }
    var selectedRouteIndex by remember { mutableIntStateOf(0) }
    
    var routePoints by remember { mutableStateOf<List<LatLng>>(emptyList()) }
    var distance by remember { mutableStateOf("") }
    var duration by remember { mutableStateOf("") }
    
    // BottomSheet state
    val sheetState = rememberModalBottomSheetState()
    var showBottomSheet by remember { mutableStateOf(false) }

    var selectedMode by remember { mutableStateOf("driving") }
    
    val apiKey = "AIzaSyAMxzfxQjAg9Jr-WE5EtBpAE7xCXwz2B1Q"

    fun updateSelectedRoute(index: Int) {
        if (allRoutes.isNotEmpty() && index in allRoutes.indices) {
            selectedRouteIndex = index
            val route = allRoutes[index]
            routePoints = PolyUtil.decode(route.overviewPolyline.points)
            if (route.legs.isNotEmpty()) {
                distance = route.legs[0].distance.text
                duration = route.legs[0].duration.text
            }
        }
    }

    suspend fun fetchRoute() {
        if (originText.isEmpty() || destinationText.isEmpty()) return
        try {
            val apiAvoid = if (selectedMode == "bicycling") "tolls" else null
            
            val response = RetrofitClient.getDirectionsApiService(context).getDirections(
                origin = originText,
                destination = destinationText,
                mode = "driving",
                avoid = apiAvoid,
                alternatives = true,
                apiKey = apiKey
            )
            
            if (response.routes.isNotEmpty()) {
                allRoutes = response.routes
                updateSelectedRoute(0)
                showBottomSheet = true // Auto show when routes found
                
                // Adjust camera to start
                if (routePoints.isNotEmpty()) {
                    cameraPositionState.position = CameraPosition.fromLatLngZoom(routePoints[0], 14f)
                }
            } else {
                allRoutes = emptyList()
                routePoints = emptyList()
                distance = ""
                duration = ""
            }
        } catch (e: Exception) {
            android.util.Log.e("MapScreen", "Error fetching directions", e)
        }
    }

    // Trigger re-fetch when mode changes
    LaunchedEffect(selectedMode) {
        if (routePoints.isNotEmpty()) {
            fetchRoute()
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        // Input Area
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            OutlinedTextField(
                value = originText,
                onValueChange = { originText = it },
                label = { Text("Asal") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = destinationText,
                onValueChange = { destinationText = it },
                label = { Text("Tujuan") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))

            // Transport Mode Selection
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = selectedMode == "driving",
                    onClick = { selectedMode = "driving" },
                    label = { Text("Mobil") },
                    leadingIcon = { Icon(Icons.Default.DirectionsCar, contentDescription = null) }
                )
                FilterChip(
                    selected = selectedMode == "bicycling", // Using driving + avoid=tolls for Motorcycle
                    onClick = { selectedMode = "bicycling" },
                    label = { Text("Motor") },
                    leadingIcon = { Icon(Icons.AutoMirrored.Filled.DirectionsBike, contentDescription = null) }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = {
                        coroutineScope.launch {
                            fetchRoute()
                        }
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Cari Rute")
                }
                
                OutlinedButton(
                    onClick = {
                        originText = ""
                        destinationText = ""
                        allRoutes = emptyList()
                        routePoints = emptyList()
                        distance = ""
                        duration = ""
                        selectedMode = "driving"
                        showBottomSheet = false
                        cameraPositionState.position = CameraPosition.fromLatLngZoom(malang, 12f)
                    },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Reset")
                }
            }
            
            if (distance.isNotEmpty() && duration.isNotEmpty()) {
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBottomSheet = true }, // Re-open on click
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Place, contentDescription = "Distance", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = distance)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Timer, contentDescription = "Duration", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = duration)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val modeIcon = if (selectedMode == "driving") Icons.Default.DirectionsCar else Icons.AutoMirrored.Filled.DirectionsBike
                        val modeText = if (selectedMode == "driving") "Mobil" else "Motor"
                        Icon(modeIcon, contentDescription = "Transport Mode", tint = Color.Gray)
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(text = modeText)
                    }
                }
            }
        }

        // Map Area
        GoogleMap(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            cameraPositionState = cameraPositionState
        ) {
            if (routePoints.isNotEmpty()) {
                val startState = remember(routePoints) { MarkerState(position = routePoints.first()) }
                val endState = remember(routePoints) { MarkerState(position = routePoints.last()) }

                Marker(
                    state = startState,
                    title = "Asal: $originText"
                )
                
                Marker(
                    state = endState,
                    title = "Tujuan: $destinationText"
                )

                Polyline(
                    points = routePoints,
                    color = Color.Red,
                    width = 10f
                )
            }
        }

        if (showBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBottomSheet = false },
                sheetState = sheetState
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = "Pilihan Rute", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyColumn {
                        itemsIndexed(allRoutes) { index, route ->
                            val routeDistance = if (route.legs.isNotEmpty()) route.legs[0].distance.text else ""
                            val routeDuration = if (route.legs.isNotEmpty()) route.legs[0].duration.text else ""
                            val viaText = route.summary
                            val isSelected = selectedRouteIndex == index

                            ListItem(
                                headlineContent = { Text("Rute ${index + 1}: via $viaText") },
                                supportingContent = {
                                    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Timer, contentDescription = null, modifier = Modifier.height(16.dp))
                                            Text(text = routeDuration)
                                        }
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Place, contentDescription = null, modifier = Modifier.height(16.dp))
                                            Text(text = routeDistance)
                                        }
                                    }
                                },
                                leadingContent = {
                                    val modeIcon = if (selectedMode == "driving") Icons.Default.DirectionsCar else Icons.AutoMirrored.Filled.DirectionsBike
                                    Icon(modeIcon, contentDescription = null)
                                },
                                trailingContent = {
                                    if (isSelected) {
                                        Text("Terpilih", color = Color.Blue)
                                    }
                                },
                                modifier = Modifier.clickable {
                                    updateSelectedRoute(index)
                                    coroutineScope.launch { sheetState.hide() }.invokeOnCompletion {
                                        if (!sheetState.isVisible) {
                                            showBottomSheet = false
                                        }
                                    }
                                }
                            )
                            HorizontalDivider()
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun MapScreenPreview() {
    MapScreen()
}
