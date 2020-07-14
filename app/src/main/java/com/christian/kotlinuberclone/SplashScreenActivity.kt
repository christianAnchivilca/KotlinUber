package com.christian.kotlinuberclone

import android.app.Activity
import android.app.AlertDialog
import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import com.christian.kotlinuberclone.Utils.UserUtils
import com.christian.kotlinuberclone.common.Common
import com.christian.kotlinuberclone.model.DriverInfoModel
import com.firebase.ui.auth.AuthMethodPickerLayout
import com.firebase.ui.auth.AuthUI
import com.firebase.ui.auth.IdpResponse
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.*
import com.google.firebase.iid.FirebaseInstanceId
import io.reactivex.Completable
import io.reactivex.android.schedulers.AndroidSchedulers
import kotlinx.android.synthetic.main.layout_splash_screen.*
import java.util.*
import java.util.concurrent.TimeUnit

class SplashScreenActivity : AppCompatActivity() {

    companion object{
        private val REQUEST_CODE_LOGIN = 1212
    }

    private lateinit var provedores: List<AuthUI.IdpConfig>
    private lateinit var firebaseAuth:FirebaseAuth
    private lateinit var listener:FirebaseAuth.AuthStateListener

    //database
    private lateinit var database:FirebaseDatabase
    private lateinit var driverInfoRef:DatabaseReference

    override fun onStart() {
        super.onStart()
        delaySplashScreen()
    }

    override fun onStop() {
        if(firebaseAuth != null && listener != null) firebaseAuth.removeAuthStateListener(listener)
        super.onStop()
    }

    private fun delaySplashScreen() {
        Completable.timer(3,TimeUnit.SECONDS,AndroidSchedulers.mainThread())
            .subscribe({
                firebaseAuth.addAuthStateListener(listener)
            })
    }


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.layout_splash_screen)
        init()


    }
    private fun init(){
        //init database
        database = FirebaseDatabase.getInstance()
        driverInfoRef = database.getReference(Common.DRIVER_REF)

        //init providers
        provedores = Arrays.asList(AuthUI.IdpConfig.PhoneBuilder().build(),
        AuthUI.IdpConfig.GoogleBuilder().build())
       //init firebaseAuth
        firebaseAuth = FirebaseAuth.getInstance()

        listener = FirebaseAuth.AuthStateListener {
            myfirebaseAuth->
            val user = myfirebaseAuth.currentUser
            if (user != null)
            {
                //update token
                FirebaseInstanceId.getInstance()
                    .instanceId
                    .addOnFailureListener { e->Toast.makeText(this@SplashScreenActivity,""+e.message,Toast.LENGTH_LONG).show() }
                    .addOnSuccessListener { instanceIdResult->
                        Log.d("TOKEN",instanceIdResult.token)
                        UserUtils.updateToken(this@SplashScreenActivity,instanceIdResult.token)
                    }
                checkUserFromFirebase()
            }

            else
                showLoginLayout()
        }
    }

    private fun checkUserFromFirebase() {

        //verifico si esta en firebase
        driverInfoRef
            .child(FirebaseAuth.getInstance().currentUser!!.uid)
            .addListenerForSingleValueEvent(object:ValueEventListener{
                override fun onCancelled(p0: DatabaseError) {
                    Toast.makeText(this@SplashScreenActivity,""+p0.message,Toast.LENGTH_LONG).show()
                }

                override fun onDataChange(dataSnapshot: DataSnapshot) {
                    if (dataSnapshot.exists()){
                        //Toast.makeText(this@SplashScreenActivity,"Ya estas registrado",Toast.LENGTH_LONG).show()
                        progress_bar.visibility = View.GONE
                        var driverinfoModel = dataSnapshot.getValue<DriverInfoModel>(DriverInfoModel::class.java)
                        goToHomeActivity(driverinfoModel!!)


                    }else{
                        showRegisterLayout()

                    }

                }

            })

    }

    private fun goToHomeActivity(driverinfoModel: DriverInfoModel) {

        Common.currentUser = driverinfoModel
        startActivity(Intent(this@SplashScreenActivity,DriverHomeActivity::class.java))
        finish()

    }

    private fun showRegisterLayout() {

        val builder = AlertDialog.Builder(this@SplashScreenActivity,R.style.DialogTheme)
        val view = LayoutInflater.from(this).inflate(R.layout.layout_register,null)
        val edt_first_name = view.findViewById<View>(R.id.edt_first_name) as TextInputEditText
        val edt_last_name = view.findViewById<View>(R.id.edt_last_name) as TextInputEditText
        val edt_phone_number = view.findViewById<View>(R.id.edt_phone_number) as TextInputEditText

        val btn_continue= view.findViewById<View>(R.id.btn_register) as Button
        //set data
        if (FirebaseAuth.getInstance().currentUser!!.phoneNumber != null &&
            !TextUtils.isDigitsOnly(FirebaseAuth.getInstance().currentUser!!.phoneNumber))
            edt_phone_number.setText(FirebaseAuth.getInstance().currentUser!!.phoneNumber)

        builder.setView(view)
        val dialog = builder.create()
        dialog.show()
        //evetn
        btn_continue.setOnClickListener {
            if (TextUtils.isDigitsOnly(edt_first_name.text.toString())){
                Toast.makeText(this,"Porfavor ingrese su primer nombre",Toast.LENGTH_LONG).show()
                return@setOnClickListener
            }else if(TextUtils.isDigitsOnly(edt_last_name.text.toString())){
                Toast.makeText(this,"Porfavor ingrese su apellido",Toast.LENGTH_LONG).show()
            }else if(TextUtils.isDigitsOnly(edt_phone_number.text.toString())){
                Toast.makeText(this,"Porfavor ingrese su celular",Toast.LENGTH_LONG).show()
            }else{
                val model = DriverInfoModel()
                model.firstName = edt_first_name.text.toString()
                model.lastName = edt_last_name.text.toString()
                model.phoneNumber = edt_phone_number.text.toString()
                model.rating = 0.0


                driverInfoRef.child(FirebaseAuth.getInstance().currentUser!!.uid)
                    .setValue(model)
                    .addOnFailureListener{
                        e->
                        Toast.makeText(this,""+e.message,Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        progress_bar.visibility = View.GONE
                    }
                    .addOnSuccessListener {
                        Toast.makeText(this,"Registro Satisfactorio",Toast.LENGTH_LONG).show()
                        dialog.dismiss()
                        progress_bar.visibility = View.GONE
                        goToHomeActivity(model)
                    }

            }


        }



    }

    private fun showLoginLayout() {

        val authMetodoPickerLayout = AuthMethodPickerLayout.Builder(R.layout.layout_sign_in)
            .setPhoneButtonId(R.id.btn_phone_sign_in)
            .setGoogleButtonId(R.id.btn_google_sign_in)
            .build()

        startActivityForResult(AuthUI.getInstance()
            .createSignInIntentBuilder()
            .setAuthMethodPickerLayout(authMetodoPickerLayout)
            .setAvailableProviders(provedores)
            .setIsSmartLockEnabled(false).build(), REQUEST_CODE_LOGIN)

    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_LOGIN){
            val respuesta = IdpResponse.fromResultIntent(data)
            if (resultCode == Activity.RESULT_OK){
                val user = FirebaseAuth.getInstance().currentUser
            }
            else
                Toast.makeText(this,""+respuesta!!.error!!.message,Toast.LENGTH_LONG).show()
        }
    }


}