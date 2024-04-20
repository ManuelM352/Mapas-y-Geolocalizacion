package net.ivanvega.milocationymapascompose

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.viewinterop.AndroidView
import com.google.android.gms.maps.OnStreetViewPanoramaReadyCallback
import com.google.android.gms.maps.StreetViewPanorama
import com.google.android.gms.maps.StreetViewPanoramaOptions
import com.google.android.gms.maps.StreetViewPanoramaView
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.MiMapa
import net.ivanvega.milocationymapascompose.ui.theme.MiLocationYMapasComposeTheme

class MainActivity : ComponentActivity(), OnStreetViewPanoramaReadyCallback {
    @RequiresApi(Build.VERSION_CODES.Q)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MiLocationYMapasComposeTheme {
                // A surface container using the 'background' color from the theme
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    //Greeting("Android")
                    //LocationPermissionScreen()
                    //CurrentLocationScreen()
                    //LocationUpdatesScreen()
                    //MapWithCameraAndDrawing()
                    //StreetViewPanoramaFragmentContainer()
                    // Crear un objeto RouteResponse con la información de la ruta
                    MiMapa(this)

                }
            }
        }
    }


    override fun onStreetViewPanoramaReady(panorama: StreetViewPanorama) {
        // Configurar la vista de Street View según sea necesario
        val initialPosition = LatLng(40.748817, -73.985428) // Ubicación inicial
        panorama.setPosition(initialPosition)
    }
}

@Composable
fun StreetViewPanoramaFragmentContainer() {
    val context = LocalContext.current
    val svpView = remember {
        StreetViewPanoramaView(context, StreetViewPanoramaOptions().position(LatLng(40.748817, -73.985428)))
    }

    AndroidView(
        factory = { svpView },
        modifier = Modifier.fillMaxSize()
    ) { view ->
        view.onCreate(null)
        view.onResume()
        view.getStreetViewPanoramaAsync { panorama ->
            val initialPosition = LatLng(40.748817, -73.985428)
            panorama.setPosition(initialPosition)
        }
    }
}

@Composable
fun Greeting(name: String, modifier: Modifier = Modifier) {
    Text(
        text = "Hello $name!",
        modifier = modifier
    )
}

@Preview(showBackground = true)
@Composable
fun GreetingPreview() {
    MiLocationYMapasComposeTheme {
        Greeting("Android")
    }
}