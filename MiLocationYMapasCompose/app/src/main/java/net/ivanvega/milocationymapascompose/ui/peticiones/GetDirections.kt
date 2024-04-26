import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.Polyline
import com.google.maps.android.compose.rememberCameraPositionState
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

// Permisos de ubicación
private val permissions = arrayOf(
    Manifest.permission.ACCESS_FINE_LOCATION,
    Manifest.permission.ACCESS_COARSE_LOCATION
)

// Permisos de ubicación
private fun hasLocationPermissions(activity: ComponentActivity): Boolean {
    return permissions.all {
        ActivityCompat.checkSelfPermission(activity, it) == PackageManager.PERMISSION_GRANTED
    }
}

// Solicitar permisos de ubicación
private fun requestLocationPermissions(activity: ComponentActivity) {
    ActivityCompat.requestPermissions(activity, permissions, LOCATION_PERMISSION_REQUEST_CODE)
}

private const val LOCATION_PERMISSION_REQUEST_CODE = 1
private const val PREF_KEY_ORIGIN = "origin_coordinates"

@Composable
fun MiMapa(activity: ComponentActivity) {
    var origen by remember { mutableStateOf("") }
    var destino by remember { mutableStateOf("") }
    var ruta: RouteResponse? by remember { mutableStateOf(null) }
    var manualDestinationMode by remember { mutableStateOf(false) }
    var manualDestinationCoordinate by remember { mutableStateOf(LatLng(0.0, 0.0)) }
    var markerDestino: LatLng? by remember { mutableStateOf(null) }
    val scope = rememberCoroutineScope()
    val singapore = LatLng(20.126275317533462, -101.18905377998448)
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(singapore, 10f)
    }

    // Obtener la coordenada de origen de SharedPreferences la primera vez que se abre la aplicación
    val sharedPreferences = activity.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
    val casaCoordinate = sharedPreferences.getString(PREF_KEY_ORIGIN, null)
    LaunchedEffect(Unit) {
        val sharedPreferences = activity.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
        val casaCoordinate = sharedPreferences.getString(PREF_KEY_ORIGIN, null)
        if (casaCoordinate == null) {
            // Guardar la ubicación actual como la coordenada de origen "Casa"
            obtenerUbicacionActual(activity)?.let { location ->
                val currentLatLng = LatLng(location.latitude, location.longitude)
                val casaCoords = "${currentLatLng.longitude},${currentLatLng.latitude}"
                sharedPreferences.edit().putString(PREF_KEY_ORIGIN, casaCoords).apply()
                showToast(activity, "Coordenadas de Casa establecidas con éxito: $casaCoords")
            }
        }
    }
    Column {
        casaCoordinate?.let {
            Text("Origen (Casa): $it")
        }
        TextField(
            value = destino,
            onValueChange = { destino = it },
            modifier = Modifier.fillMaxWidth(),
            label = { Text("Destino: longitud, latitud") }
        )

        Row {
            Button(
                onClick = {
                    scope.launch {
                        obtenerUbicacionActual(activity)?.let { location ->
                            val currentLatLng = LatLng(location.latitude, location.longitude)
                            destino = "${currentLatLng.longitude},${currentLatLng.latitude}"
                            markerDestino = currentLatLng
                        }
                    }
                }
            ) {
                Icon(Icons.Default.LocationOn, contentDescription = "Ubicación Actual")
                Text("Ubicación Actual (Destino)")
            }

            Button(
                onClick = { manualDestinationMode = true }
            ) {
                Icon(Icons.Default.Star, contentDescription = "Seleccionar Destino")
                Text("Seleccionar Destino")
            }
        }
        Row {
            Button(
                onClick = {
                    scope.launch {
                        val sharedPreferences =
                            activity.getSharedPreferences("MyPrefs", Context.MODE_PRIVATE)
                        val casaCoordinate = sharedPreferences.getString(PREF_KEY_ORIGIN, null)
                        if (casaCoordinate != null && destino.isNotEmpty()) {
                            ruta = obtenerRuta(casaCoordinate, destino)
                        } else {
                            showToast(
                                activity,
                                "Por favor, complete los campos de origen y destino"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Search, contentDescription = "Trazar Ruta")
                Text("Trazar Ruta")
            }
        }
        GoogleMap(
            cameraPositionState = cameraPositionState,
            onMapClick = { latLng ->
                if (manualDestinationMode) {
                    manualDestinationCoordinate = latLng
                    destino = "${latLng.longitude},${latLng.latitude}"
                    markerDestino = latLng
                    manualDestinationMode = false
                }
            }
        ) {
            casaCoordinate?.let {
                val casaLatLng = LatLng(it.split(",")[1].toDouble(), it.split(",")[0].toDouble())
                Marker(state = MarkerState(position = casaLatLng), title = "Origen (Casa)")
            }

            markerDestino?.let {
                Marker(state = MarkerState(position = it), title = "Destino Manual")
            }

            ruta?.let { route ->
                val coordenadasRuta =
                    route.features.first().geometry.coordinates.map { LatLng(it[1], it[0]) }
                Polyline(points = coordenadasRuta, color = Color.Red)
            }

            if (manualDestinationMode) {
                Marker(
                    state = MarkerState(position = manualDestinationCoordinate),
                    title = "Destino Manual"
                )
            }
        }
    }
}

// Obtener la ubicación actual
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

fun showToast(context: Context, message: String) {
    Toast.makeText(context, message, Toast.LENGTH_SHORT).show()
}
