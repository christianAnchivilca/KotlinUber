package com.christian.kotlinuberclone.Utils

import android.content.Context
import android.view.View
import android.widget.Toast
import com.christian.kotlinuberclone.Services.MyFirebaseMessagingService
import com.christian.kotlinuberclone.common.Common
import com.christian.kotlinuberclone.model.TokenModel
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

object UserUtils {
    fun updateUser(view: View?,updateData:Map<String,Any>)
    {
        FirebaseDatabase.getInstance().getReference(Common.DRIVER_REF)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .updateChildren(updateData)
            .addOnFailureListener {  e->
                Snackbar.make(view!!,e.message!!, Snackbar.LENGTH_LONG).show()
            }
            .addOnSuccessListener {
                Snackbar.make(view!!,"Actualizado correctamente", Snackbar.LENGTH_LONG).show()
            }

    }

    //ACTUALIZAR NUETRO TOKEN
    fun updateToken(context: Context, token: String) {
        val tokenModel = TokenModel()
        tokenModel.token = token
        FirebaseDatabase.getInstance()
            .getReference(Common.TOKEN_REF)
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .setValue(token)
            .addOnFailureListener {  e->Toast.makeText(context!!,"tokenError: ",Toast.LENGTH_LONG).show()}
            .addOnSuccessListener {
            }



    }


}