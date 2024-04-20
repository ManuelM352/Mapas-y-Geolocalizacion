package net.ivanvega.milocationymapascompose.ui.maps

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Text

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.PolylineOptions
import com.google.maps.android.compose.GoogleMap
import com.google.maps.android.compose.MapType
import com.google.maps.android.compose.Marker
import com.google.maps.android.compose.MarkerState
import com.google.maps.android.compose.rememberCameraPositionState
import androidx.compose.runtime.remember
import androidx.compose.runtime.*
import com.google.android.gms.maps.GoogleMap
import android.location.Location
import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Card
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.maps.android.compose.Circle
import com.google.maps.android.compose.Polygon
import com.google.maps.android.compose.Polyline
import com.google.maps.android.ktx.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

@Composable
fun MapWithCameraAndDrawing() {
    var drawnPoints by remember { mutableStateOf(emptyList<LatLng>()) }
    var drawnPolyline by remember { mutableStateOf<List<LatLng>?>(null) }
    var drawnPolygon by remember { mutableStateOf<List<LatLng>?>(null) }
    var drawnCircleCenter by remember { mutableStateOf<LatLng?>(null) }
    var drawnCircleRadius by remember { mutableStateOf<Float?>(null) }
    var isDrawingCircle by remember { mutableStateOf(false) }

    val cameraPositionState = rememberCameraPositionState {
        CameraPosition.Builder()
            .target(LatLng(1.35, 103.87))
            .zoom(10f)
            .build()
    }

    fun startDrawingCircleMode() {
        isDrawingCircle = true
    }

    fun calculateRadius(points: List<LatLng>): Float {
        val firstPoint = points.firstOrNull()
        val lastPoint = points.lastOrNull()

        return if (firstPoint != null && lastPoint != null) {
            val results = FloatArray(1)
            Location.distanceBetween(
                firstPoint.latitude,
                firstPoint.longitude,
                lastPoint.latitude,
                lastPoint.longitude,
                results
            )
            results[0]
        } else {
            0f
        }
    }



    fun moveCameraToPosition(newPosition: LatLng) {
        cameraPositionState.position = CameraPosition.Builder()
            .target(newPosition)
            .zoom(10f)
            .build()
    }

    fun moveCameraToPositionWithZoom(newPosition: LatLng, zoomLevel: Float) {
        cameraPositionState.position = CameraPosition.Builder()
            .target(newPosition)
            .zoom(zoomLevel)
            .build()
    }

    fun moveCameraWithOffset(offset: Float) {
        val currentZoom = cameraPositionState.position.zoom
        cameraPositionState.position = CameraPosition.Builder()
            .target(cameraPositionState.position.target)
            .zoom(currentZoom + offset)
            .build()
    }

    // Función para limpiar los elementos dibujados
    fun clearDrawnElements() {
        drawnPoints = emptyList()
        drawnPolyline = null
        drawnPolygon = null
        drawnCircleCenter = null
        drawnCircleRadius = null
        isDrawingCircle = false
    }

    GoogleMap(
        modifier = Modifier.fillMaxSize(),
        cameraPositionState = cameraPositionState,
        onMapClick = { latLng ->
            drawnPoints = drawnPoints + latLng
            drawnPolyline = drawnPoints.takeLast(2)
            if (drawnPoints.size >= 3) {
                drawnPolygon = drawnPoints
            }
            if (isDrawingCircle) {
                drawnCircleCenter = latLng
                drawnCircleRadius = calculateRadius(drawnPoints)
            }
        }
    ) {
        drawnPolyline?.let { polyline ->
            Polyline(points = polyline, color = Color.Blue, width = 5F)
        }
        drawnPolygon?.let { polygon ->
            com.google.maps.android.compose.Polygon(points = polygon, fillColor = Color.Red.copy(alpha = 0.5f))
        }
        drawnCircleCenter?.let { center ->
            drawnCircleRadius?.let { radius ->
                Circle(center = center, radius = radius.toDouble(), fillColor = Color.Green.copy(alpha = 0.5f))
            }
        }
        drawnPoints.forEach { point ->
            Marker(
                state = MarkerState(position = point)
            )
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        verticalArrangement = Arrangement.Top
    ) {
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { moveCameraToPosition(LatLng(1.35, 103.87)) },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("Mover a Singapore")
            }
            Button(
                onClick = { moveCameraToPositionWithZoom(LatLng(1.0, 103.0), 15f) },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("Mover a una posición con zoom")
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { moveCameraWithOffset(1f) },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("Acercar")
            }
            Button(
                onClick = { moveCameraWithOffset(-1f) },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("Alejar")
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { startDrawingCircleMode() },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("Dibujar circulo")
            }
            Button(
                onClick = { isDrawingCircle = false },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("Salir del modo circulo")
            }
        }
        Row(
            horizontalArrangement = Arrangement.SpaceEvenly,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = { clearDrawnElements() },
                modifier = Modifier.weight(1f).padding(horizontal = 4.dp)
            ) {
                Text("Recomponer Elementos")
            }
        }

    }

    drawnPoints.forEach { point ->
        MarkerInfoWindow(
            title = "Coordenadas",
            snippet = "Lat: ${point.latitude}, Lng: ${point.longitude}"
        )
    }
}

@Composable
fun MarkerInfoWindow(title: String, snippet: String) {
    Box(
        modifier = Modifier
            .width(200.dp)
            .height(100.dp)
            .padding(8.dp)
            .fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Card {
            Column(
                modifier = Modifier.padding(8.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(text = title)
                Text(text = snippet)
            }
        }
    }
}



