package example.sesame.buttonapp

import kotlinx.coroutines.*
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import timber.log.Timber
import java.util.*

interface SESAMEAPIV3 {
    @POST("sesame/{device_id}")
    @Headers("Content-Type: application/json")
    fun sesameControl(@Header("Authorization") apikey: String, @Path("device_id") id: String, @Body cmd: String): Call<ControlResult>

    @GET("sesame/{device_id}")
    fun sesameStatus(@Header("Authorization") apikey: String, @Path("device_id") id: String): Call<StatusResult>
}

data class StatusResult(
    var locked: Boolean,
    var battery: Int,
    var responsive: Boolean
)

data class ControlResult (
    var task_id: String
)

class SesameAPI (prop: Properties, testmode:Boolean = false)
{
    private val fTestmode = testmode
    private val token :String
    private val IDs : Array<String>
    private val CMD_LOCK_JSON = "{\"command\":\"lock\"}"
    private val CMD_UNLOCK_JSON = "{\"command\":\"unlock\"}"

    private var api: SESAMEAPIV3

    init {
        val retrofit = Retrofit.Builder()
            .baseUrl("https://api.candyhouse.co/public/")
            .addConverterFactory(GsonConverterFactory.create())
            .build()
        api = retrofit.create(SESAMEAPIV3::class.java)
        token = prop.getProperty("sesame_apikey")
        IDs = arrayOf(prop.getProperty("sesame_device1"), prop.getProperty("sesame_device2"))
    }

    private fun control(id: String, cmd: String) : String {
        if (fTestmode) {
            return String.format("fTestmode %s %s", id, cmd)
        }

        Timber.d("control(id=%s cmd=%s)", id, cmd)
        return  try {
            val response : Response<ControlResult> = api.sesameControl(token, id, cmd).execute()
            if (response.isSuccessful) {
                (response.body() as ControlResult).task_id
            } else {
                Timber.d("ERROR: %s", response.message())
                response.message().substring(0,40)
            }
        } catch (e: Exception) {
            e.message.toString().substring(0,40)
        }
    }

    private fun status(id:String) :String {
        return try {
            val response: Response<StatusResult> = api.sesameStatus(token, id).execute()
            if (response.isSuccessful) {
                val s = response.body() as StatusResult
                String.format("%s %d%%", if (s.locked) "locked" else "unlocked", s.battery)
            } else {
                response.message().toString().substring(0,40)
            }
        } catch (e: Exception) {
            e.message.toString().substring(0,40)
        }
    }

    fun unlockAsync(index: Int) : Deferred<String> {
        var result :String
        return GlobalScope.async(Dispatchers.IO) {
            result = control(IDs[index], CMD_UNLOCK_JSON)
            return@async result
        }
    }

    fun lockAsync(index: Int) : Deferred<String> {
        var result :String
        return GlobalScope.async(Dispatchers.IO) {
            result = control(IDs[index], CMD_LOCK_JSON)
            return@async result
        }
    }

    fun statusAsync(index: Int) : Deferred<String> {
        var result:String
        return GlobalScope.async(Dispatchers.IO) {
            result = status(IDs[index])
            return@async result
        }
    }
}
