package example.sesame.buttonapp

import android.app.*
import android.content.Context
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import android.content.Intent.FLAG_ACTIVITY_NEW_TASK
import android.media.AudioManager
import android.support.v4.media.session.MediaSessionCompat
import android.support.v4.media.session.PlaybackStateCompat
import androidx.localbroadcastmanager.content.LocalBroadcastManager
import androidx.media.VolumeProviderCompat

class ButtonService : Service(){

    private lateinit var mediaSession :MediaSessionCompat

    companion object {
        const val CHANNEL_ID :String = "channel_buttonapp"
        const val NOTIFICATION_ID :Int = 89899912
        const val FINISH_REQUEST_FROM_NOTIFICATION :String= "buttonapp_finish_request_from_notification"
        const val STATUS_STOPPED: String = "stopped"
        const val STATUS_RUNNING = "running"
    }

    override fun onCreate() {
        super.onCreate()
        val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        // require Build.VERSION_CODES.O
        val mChannel = NotificationChannel(CHANNEL_ID, getString(R.string.app_name), NotificationManager.IMPORTANCE_DEFAULT)
        notificationManager.createNotificationChannel(mChannel)

        mediaSession = setupMediaSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {

        val startedFromNotification = intent?.getBooleanExtra(FINISH_REQUEST_FROM_NOTIFICATION, false)
        if (startedFromNotification != null && startedFromNotification) {
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID,  createNotification("button watch service started"))
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(STATUS_RUNNING))
        return START_STICKY
    }

    override fun onDestroy() {
        mediaSession.release()
        LocalBroadcastManager.getInstance(this).sendBroadcast(Intent(STATUS_STOPPED))
        super.onDestroy()
    }

    override fun onBind(intent: Intent): IBinder? {
        return null
    }

    private fun createNotification(txt: String) : Notification {
        val activityLaunchIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java), 0)
        val servicePendingIntent = PendingIntent.getService(
            this, 0,
            Intent(this, ButtonService::class.java).apply {
                putExtra(FINISH_REQUEST_FROM_NOTIFICATION, true)
            }, PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID).apply {
            addAction(R.drawable.ic_cancel, "Stop button watch service", servicePendingIntent)
            setContentTitle("ButtonApp")
            setContentText(txt)
            setSmallIcon(R.mipmap.ic_launcher)
            setContentIntent(activityLaunchIntent)
        }.build()
    }

    private fun setupMediaSession() : MediaSessionCompat{
        //
        // let's start a fake media session, although we are not a real media player, we just want to receive
        // onAdjustVolume callback
        //
        return MediaSessionCompat(applicationContext, "ButtonAppService").apply {
            setFlags(MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS or MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS)
            setPlaybackState(
                PlaybackStateCompat.Builder()
                    .setState(PlaybackStateCompat.STATE_PLAYING, 0, 0f) //you simulate a player which plays something.
                    .build()
            )
            isActive = true
            //this will only work on Lollipop and up, see https://code.google.com/p/android/issues/detail?id=224134
            setPlaybackToRemote(object : VolumeProviderCompat(VOLUME_CONTROL_RELATIVE, 100, 50)
            {
                override fun onAdjustVolume(direction: Int) {
                    // our button gadget generates volume_up event on the large button
                    if (direction == AudioManager.ADJUST_RAISE) {
                        startActivity(Intent(applicationContext, MainActivity::class.java).apply {
                            addFlags(FLAG_ACTIVITY_NEW_TASK)
                        })
                    }
                }
            })
        }
    }
}