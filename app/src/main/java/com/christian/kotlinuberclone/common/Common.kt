package com.christian.kotlinuberclone.common

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import com.christian.kotlinuberclone.R
import com.christian.kotlinuberclone.model.DriverInfoModel
import java.lang.StringBuilder

object Common{
    fun buildWelcomeMessage(): String {
        return StringBuilder("Bienvenido, ").append(currentUser!!.firstName)
            .append(" ")
            .append(currentUser!!.lastName)
            .toString()

    }

    fun showNotification(context: Context, id: Int,
                         title: String?,
                         body: String?,
                         intent: Intent?)
    {
        var pendingIntent:PendingIntent ?= null
        if (intent != null)
            pendingIntent = PendingIntent.getActivity(context,id,intent,PendingIntent.FLAG_UPDATE_CURRENT)
        val NOTIFICATION_CHANNEL_ID = "edmt_dev_uber_remake"
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O)
        {
            val notificationChannel = NotificationChannel(NOTIFICATION_CHANNEL_ID,"UberRemakeWithKotlin",
                NotificationManager.IMPORTANCE_HIGH)

                notificationChannel.description="Uber Remake"
                notificationChannel.enableLights(true)
                notificationChannel.lightColor = Color.RED
                notificationChannel.vibrationPattern = longArrayOf(0,1000,500,1000)
                notificationChannel.enableVibration(true)

              notificationManager.createNotificationChannel(notificationChannel)
        }
        val builder = NotificationCompat.Builder(context,NOTIFICATION_CHANNEL_ID)
        builder.setContentTitle(title)
            .setContentText(body)
            .setAutoCancel(false)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setDefaults(Notification.DEFAULT_VIBRATE)
            .setSmallIcon(R.drawable.ic_movil_car_24)
            .setLargeIcon(BitmapFactory.decodeResource(context.resources,R.drawable.ic_movil_car_24))
        if (pendingIntent != null)
            builder.setContentIntent(pendingIntent!!)
        val notification = builder.build()
        notificationManager.notify(id,notification)


    }

    val TOKEN_REF: String="Token"
    const val NOTI_BODY: String = "body"
    const val NOTI_TITLE: String = "title"
    val DRIVERS_LOCATION_REF: String="DriversLocation"
    const val DRIVER_REF:String = "DriverInfo"
    var currentUser:DriverInfoModel?=null
}