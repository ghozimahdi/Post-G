package com.blank.mydiary.service

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.PRIORITY_MAX
import androidx.core.content.ContextCompat
import com.blank.mydiary.R
import com.blank.mydiary.ui.home.HomeActivity
import com.blank.mydiary.utils.AnalyticFirebase
import java.util.*

class AlarmReceiver : BroadcastReceiver() {
    companion object {
        const val EXTRA_MESSAGE = "message"
        private const val ID_REPEATING = 101
    }

    private var analytics: AnalyticFirebase? = null

    override fun onReceive(context: Context, intent: Intent) {
        analytics = AnalyticFirebase(context)
        val title = context.getString(R.string.reminder_title_notif)
        val msg = intent.getStringExtra(EXTRA_MESSAGE) ?: ""
        analytics?.isBroadcastActive()
        showAlarmNotif(context, title, msg)
    }

    fun setRepeatingAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val message = context.getString(R.string.reminder_msg_notif)
        val intent = Intent(context, AlarmReceiver::class.java)
            .putExtra(EXTRA_MESSAGE, message)

        val calendar = Calendar.getInstance()
            .apply {
                timeInMillis = System.currentTimeMillis()
                set(Calendar.HOUR_OF_DAY, 20)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
            }

        val pendingIntent = PendingIntent.getBroadcast(context, ID_REPEATING, intent, 0)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                pendingIntent
            )
        }else{
            alarmManager.setInexactRepeating(
                AlarmManager.RTC_WAKEUP,
                calendar.timeInMillis,
                AlarmManager.INTERVAL_DAY,
                pendingIntent
            )
        }
    }

    fun cancelAlarm(context: Context) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val intent = Intent(context, AlarmReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(context, ID_REPEATING, intent, 0)
        pendingIntent.cancel()
        alarmManager.cancel(pendingIntent)
    }

    fun isAlarmOn(context: Context): Boolean {
        val intent = Intent(context, AlarmReceiver::class.java)
        return PendingIntent.getBroadcast(
            context,
            ID_REPEATING,
            intent,
            PendingIntent.FLAG_NO_CREATE
        ) != null
    }

    private fun showAlarmNotif(
        context: Context,
        title: String,
        message: String
    ) {
        val channelId = "Channel_1"
        val channelName = "AlarmManager Channel"

        val notificationManagerCompat =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_icon_app)
            .setContentTitle(title)
            .setContentText(message)
            .setColor(ContextCompat.getColor(context, android.R.color.transparent))
            .setVibrate(longArrayOf(1000, 1000, 1000, 1000, 1000))
            .setSound(alarmSound)
            .setPriority(PRIORITY_MAX)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_DEFAULT
            )

            channel.enableVibration(true)
            channel.vibrationPattern = longArrayOf(1000, 1000, 1000, 1000, 1000)
            builder.setChannelId(channelId)
            notificationManagerCompat.createNotificationChannel(channel)
        }

        val notificationIntent = Intent(context, HomeActivity::class.java)
        notificationIntent.flags = (Intent.FLAG_ACTIVITY_CLEAR_TOP
                or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        notificationIntent.putExtra("notif", true)

        val intent = PendingIntent.getActivity(
            context, 0,
            notificationIntent, 0
        )
        builder.setAutoCancel(true)
        builder.setContentIntent(intent)

        val notification = builder.build()
        notificationManagerCompat.notify(ID_REPEATING, notification)

        analytics?.isNotifShow(true)
    }
}