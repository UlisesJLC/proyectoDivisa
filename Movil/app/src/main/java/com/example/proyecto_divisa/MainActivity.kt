package com.example.proyecto_divisa

import android.content.Context
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.annotation.RequiresApi
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.work.Constraints
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.work.workDataOf

import com.example.proyecto_divisa.data.RetrofitClient
import com.example.proyecto_divisa.room.AppDatabase
import com.example.proyecto_divisa.room.ExchangeRate

import com.example.proyecto_divisa.ui.theme.Proyecto_DivisaTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path
import java.io.IOException
import java.util.concurrent.TimeUnit
import org.json.JSONObject
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter


class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED) // Opcional: Ejecutar solo cuando haya conexión a internet
            .build()

        val periodicWorkRequest = PeriodicWorkRequestBuilder<MyHourlyTaskWorker>(15, TimeUnit.MINUTES)
            .setConstraints(constraints)
            .build()

        WorkManager.getInstance(applicationContext).enqueueUniquePeriodicWork(
            "uniqueWorkName",
            ExistingPeriodicWorkPolicy.KEEP,
            periodicWorkRequest
        )
        setContent {
            Proyecto_DivisaTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    Greeting(
                        name = "Android",
                        modifier = Modifier.padding(innerPadding)
                    )
                }
            }
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
    Proyecto_DivisaTheme {
        Greeting("Android")
    }
}



class MyHourlyTaskWorker(appContext: Context, workerParams: WorkerParameters) : CoroutineWorker(appContext, workerParams) {

    @RequiresApi(Build.VERSION_CODES.O)
    override suspend fun doWork(): Result {
        return withContext(Dispatchers.IO) {
            try {
                // Obtener la clave API
                val apiKey = "5dec4225b0c3705176f46382"

                // Obtener una instancia del servicio Retrofit
                val exchangeRateService = RetrofitClient.instance

                // Realizar la solicitud a la API (usando la función suspendida)
                val exchangeRateResponse = exchangeRateService.getLatestRates(apiKey, "MXN")

                // Verificar si la solicitud fue exitosa
                if (exchangeRateResponse.result == "success") {
                    // Obtener una instancia de la base de datos
                    val db = AppDatabase.getInstance(applicationContext)

                    // Obtener la fecha actual
                    val fechaHoraActual = LocalDateTime.now()
                    val formatter2 = DateTimeFormatter.ofPattern("yyyy-MM-dd")
                    val fechaFormateada2 = fechaHoraActual.toLocalDate().format(formatter2)

                    // Insertar cada tasa de cambio en la base de datos
                    val response = exchangeRateResponse.conversion_rates
                    for ((clave, valor) in response) {
                        val exchangeDetail = ExchangeRate(
                            nombre = clave,
                            cantidad = valor,
                            fecha = fechaFormateada2
                        )
                        db.exchangeRateDAO().insertar(exchangeDetail)
                    }

                    Log.i("MyHourlyTaskWorker", "Tasas de cambio obtenidas y guardadas en la base de datos")
                    Result.success()
                } else {
                    Log.e("MyHourlyTaskWorker", "Error al obtener tasas de cambio: ${exchangeRateResponse.result}")
                    Result.failure()
                }
            } catch (e: IOException) {
                Log.e("MyHourlyTaskWorker", "Error de red: ${e.message}", e)
                Result.failure()
            } catch (e: HttpException) {
                Log.e("MyHourlyTaskWorker", "Error de servidor: ${e.code()}", e)
                Result.failure()
            } catch (e: Exception) {
                Log.e("MyHourlyTaskWorker", "Error inesperado: ${e.message}", e)
                Result.failure()
            }
        }
    }
}
