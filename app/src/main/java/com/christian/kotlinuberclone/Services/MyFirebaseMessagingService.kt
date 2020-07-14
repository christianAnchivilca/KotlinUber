package com.christian.kotlinuberclone.Services

import com.christian.kotlinuberclone.Utils.UserUtils
import com.christian.kotlinuberclone.common.Common
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlin.random.Random


class MyFirebaseMessagingService :FirebaseMessagingService(){

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        if(FirebaseAuth.getInstance().currentUser != null)
        {
            UserUtils.updateToken(this,token)

        }
    }

    override fun onMessageReceived(p0: RemoteMessage) {
        super.onMessageReceived(p0)
        //code for show notification
        val data = p0.data
        if(data != null)
        {
            Common.showNotification(this, Random.nextInt(),
                data[Common.NOTI_TITLE],
                data[Common.NOTI_BODY],
                null)

        }
    }

}