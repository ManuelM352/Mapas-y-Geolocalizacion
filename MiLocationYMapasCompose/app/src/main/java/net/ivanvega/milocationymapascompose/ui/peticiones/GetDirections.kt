package com.google.maps.android.compose

import android.Manifest
import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.*
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

data class RouteResponse(
    val features: List<RouteFeature>
)

data class RouteFeature(
    val geometry: RouteGeometry,
    val properties: RouteProperties
)

data class RouteGeometry(
    val coordinates: List<List<Double>>
)

data class RouteProperties(
    val summary: RouteSummary
)

data class RouteSummary(
    val distance: Double,
    val duration: Double
)

interface RouteService {
    @GET("/v2/directions/driving-car")
    suspend fun getRoute(
        @Query("api_key") apiKey: String,
        @Query("start") start: String,
        @Query("end") end: String
    ): RouteResponse
}

val retrofit = Retrofit.Builder()
    .baseUrl("https://api.openrouteservice.org")
    .addConverterFactory(MoshiConverterFactory.create())
    .build()

val routeService = retrofit.create(RouteService::class.java)


// Define permisos de ubicación
private val permissions = arrayOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

// Código de solicitud para permisos
private const val LOCATION_PERMISSION_REQUEST_CODE = 1

@Composable
fun MiMapa(activity: ComponentActivity) {
    var origen by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf("") }
    var ruta: RouteResponse? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    val singapore = LatLng(20.126275317533462, -101.18905377998448)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }

    Column {
        TextField(
            value = origen,
            onValueChange = { origen = it },
            label = { Text(text = "Coordenadas de origen (latitud, longitud)") }
        )
        TextField(
            value = destino,
            onValueChange = { destino = it },
            label = { Text("Coordenadas de destino (latitud, longitud)") }
        )
        Button(
            onClick = {
                scope.launch {
                    // Verificar si la caja de texto de destino está vacía
                    if (destino.isNotBlank()) {
                        // Solicitar ubicación actual antes de obtener la ruta
                        obtenerUbicacionActual(activity)?.let { location ->
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            origen = "${currentLatLng.longitude},${currentLatLng.latitude}"
                            ruta = obtenerRuta(origen, destino)
                        }
                    } else {
                        Toast.makeText(activity, "Por favor ingresa un destino", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        ) {
            Text("Obtener Ruta")
        }

        // Mostrar la ruta obtenida
        ruta?.let { route ->
            Text("Distancia: ${route.features.first().properties.summary.distance} m")
            Text("Duración: ${route.features.first().properties.summary.duration} s")

            // Crear una lista de LatLng directamente desde las coordenadas de la ruta
            val coordenadasRuta =
                route.features.first().geometry.coordinates.map { LatLng(it[1], it[0]) }

            GoogleMap(
                cameraPositionState = cameraPositionState,
            ) {
                // Dibujar la polilínea en el mapa
                Polyline(points = coordenadasRuta, color = Color.Red)
            }
        }
    }
}

// Función para obtener la ubicación actual
@SuppressLint("MissingPermission")
suspend fun obtenerUbicacionActual(activity: ComponentActivity): Location? {
    return withContext(Dispatchers.Main) {
        val fusedLocationClient = LocationServices.getFusedLocationProviderClient(activity)
        if (hasLocationPermissions(activity)) {
            fusedLocationClient.lastLocation.await()
        } else {
            requestLocationPermissions(activity)
            null
        }
    }
}

// Verifica si se tienen permisos de ubicación
private fun hasLocationPermissions(activity: ComponentActivity): Boolean {
    return permissions.all {
        ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }
}

// Solicita permisos de ubicación si no se tienen
private fun requestLocationPermissions(activity: ComponentActivity) {
    ActivityCompat.requestPermissions(activity, permissions, LOCATION_PERMISSION_REQUEST_CODE)
}

// Función para obtener la ruta utilizando Retrofit
suspend fun obtenerRuta(origen: String, destino: String): RouteResponse {
    return withContext(Dispatchers.IO) {
        routeService.getRoute(
            apiKey = "5b3ce3597851110001cf624837de89af407a42fba52717dbf74f1cd7",
            start = origen,
            end = destino
        )
    }
}