package example.sesame.buttonapp

import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.media.RingtoneManager
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.KeyEvent
import android.view.KeyEvent.KEYCODE_BACK
import android.view.View
import android.view.animation.AlphaAnimation
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import java.util.*

class MainActivity : Activity() {

    private val abshutter = ABShutterUtil()
    private lateinit var sesame: SesameAPI
    private lateinit var buttonAppProperties :Properties
    private val buttonClickAnimation = AlphaAnimation(1f, 0.2f).apply {
        duration = 150
    }

    private val localBroadCastReceiver:BroadcastReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.run {
                if (action == ButtonService.STATUS_STOPPED)  {
                    check_button_app_service.isChecked = false
                } else if (action == ButtonService.STATUS_RUNNING)  {
                    check_button_app_service.isChecked = true
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        check_button_app_service.setOnClickListener {
            if (check_button_app_service.isChecked) {
                startForegroundService(Intent(this, ButtonService::class.java))
            } else {
                stopService(Intent(this, ButtonService::class.java))
            }
        }

        button_finish.setOnClickListener {
            viewEffect(it, 200L)
            exitApp()
        }

        button_unlock.setOnClickListener {
            unlockAll()
            viewEffect(it, 500L, true)
        }

        button_lock.setOnClickListener {
            lockAll()
            viewEffect(it, 500L, true)
        }

        status_frame.setOnClickListener {
            status1.text = "..."
            status2.text = "..."
            retrieveStatus()
        }

        //
        // local broadcast receiver that receives messages from the LocationUpdatesService
        //
        LocalBroadcastManager.getInstance(this).run {
            registerReceiver(localBroadCastReceiver, IntentFilter().apply {
                addAction(ButtonService.STATUS_RUNNING)
                addAction(ButtonService.STATUS_STOPPED)
            })
        }

        buttonAppProperties = loadProperty()
        sesame = SesameAPI(buttonAppProperties, BuildConfig.sesame_api_testmode)
        startForegroundService(Intent(this, ButtonService::class.java))
    }

    override fun onResume() {
        Timber.d("## onResume")
        super.onResume()
        retrieveStatus()
    }

    override fun onDestroy() {
        super.onDestroy()
        LocalBroadcastManager.getInstance(this).unregisterReceiver(localBroadCastReceiver)
    }

    override fun onBackPressed() {
        exitApp()
    }

    private fun exitApp() {
        val intent = Intent(this, ExitActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS or
                    Intent.FLAG_ACTIVITY_NO_ANIMATION)
        }
        startActivity(intent)
        finish()
    }


    private fun viewEffect(view: View, vibeduration:Long, fRing:Boolean = false) {
        view.startAnimation(buttonClickAnimation)
        if (vibeduration > 0L) {
            (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).run {
                vibrate(VibrationEffect.createOneShot(vibeduration, VibrationEffect.DEFAULT_AMPLITUDE))
            }
        }
        if (fRing) {
            val notification = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            RingtoneManager.getRingtone(applicationContext, notification).play()
        }
    }

    private fun retrieveStatus() {
        GlobalScope.launch(Dispatchers.Main) {
            val d1 = sesame.statusAsync(1)
            val d2 = sesame.statusAsync(0)
            status1.text = d1.await()
            status2.text = d2.await()
        }
    }

    private fun unlockAll() {
        GlobalScope.launch(Dispatchers.Main) {
            val d1 = sesame.unlockAsync(1)
            val d2 = sesame.unlockAsync(0)
            status1.text = d1.await()
            status2.text = d2.await()
        }
    }

    private fun lockAll() {
        GlobalScope.launch(Dispatchers.Main) {
            val d1 = sesame.lockAsync(1)
            val d2 = sesame.lockAsync(0)
            status1.text = d1.await()
            status2.text = d2.await()
        }
    }

    private fun loadProperty() :Properties {
        val p = Properties()
        try {
            p.load(resources.assets.open("buttonapp.properties"))
            //Timber.d("## buttonAppProperties: $buttonAppProperties")
        } catch (e: IOException) {
            Timber.e("## Failed to open property file")
        }
        return p
    }

    override fun dispatchKeyEvent(event: KeyEvent?): Boolean {
        if (event == null) {
            return false
        }

        if (event.keyCode == KEYCODE_BACK) {
            exitApp()
            return true
        }

        /*
        val dev = event.device
        Timber.d("## name=${dev.name}, vendorId=${dev.vendorId}, productId=${dev.productId}, id=${dev.id}, descriptor=${dev.descriptor}")
        if ( ! buttonAppProperties.containsKey(event.device.descriptor)) {
            Timber.d("## input from unregistered device")
            return false
        }
        */
        return abshutter.handleEvent(event)  {
            Timber.d("## ===> %s", it.toString())
            when (it) {
                ABShutterUtil.ButtonSequence.LARGE_SHORT -> {
                    button_finish.performClick()
                }
                ABShutterUtil.ButtonSequence.LARGE_DOUBLE -> {
                    button_finish.performClick()
                }
                ABShutterUtil.ButtonSequence.LARGE_LONG -> {
                    button_finish.performClick()
                }
                ABShutterUtil.ButtonSequence.SMALL_SHORT -> {
                    // no assignment
                }
                ABShutterUtil.ButtonSequence.SMALL_DOUBLE -> {
                    button_unlock.performClick()
                }
                ABShutterUtil.ButtonSequence.SMALL_LONG-> {
                    button_lock.performClick()
                }
                else -> {
                    /* N/A */
                }
            }
        }
    }
}