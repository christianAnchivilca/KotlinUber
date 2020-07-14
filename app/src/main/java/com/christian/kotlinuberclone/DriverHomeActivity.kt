package com.christian.kotlinuberclone

import android.app.Activity
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.text.TextUtils
import android.view.Menu
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog

import com.google.android.material.navigation.NavigationView
import androidx.navigation.findNavController
import androidx.navigation.ui.AppBarConfiguration
import androidx.navigation.ui.navigateUp
import androidx.navigation.ui.setupActionBarWithNavController
import androidx.navigation.ui.setupWithNavController
import androidx.drawerlayout.widget.DrawerLayout
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.navigation.NavController
import com.bumptech.glide.Glide
import com.christian.kotlinuberclone.Utils.UserUtils
import com.christian.kotlinuberclone.common.Common
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.lang.StringBuilder
import java.util.*
import kotlin.collections.HashMap

class DriverHomeActivity : AppCompatActivity() {

    private lateinit var appBarConfiguration: AppBarConfiguration
    private lateinit var navView: NavigationView
    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navController:NavController
    private lateinit var img_avatar:ImageView
    private var imageUri:Uri?=null
    private lateinit var storageRef:StorageReference
    private lateinit var waitingDialog:AlertDialog

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_driver_home)
        val toolbar: Toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)


        drawerLayout = findViewById(R.id.drawer_layout)
        navView = findViewById(R.id.nav_view)
        navController = findNavController(R.id.nav_host_fragment)
        // Passing each menu ID as a set of Ids because each
        // menu should be considered as top level destinations.
        appBarConfiguration = AppBarConfiguration(
            setOf(
                R.id.nav_home
            ), drawerLayout
        )
        setupActionBarWithNavController(navController, appBarConfiguration)
        navView.setupWithNavController(navController)
        init()
    }

    private fun init() {

        storageRef = FirebaseStorage.getInstance().getReference()
        waitingDialog = AlertDialog.Builder(this@DriverHomeActivity)
            .setTitle("Espere un momento ...")
            .setCancelable(false).create()

        navView.setNavigationItemSelectedListener {
                it->
            if (it.itemId == R.id.nav_sign_out)
            {

                val builder = AlertDialog.Builder(this@DriverHomeActivity)
                builder.setTitle("Cerrar Sesion")
                builder.setMessage("¿Estas seguro de salir?")
                builder.setNegativeButton("CANCELAR"){dialog, which: Int ->  dialog.dismiss()}
                builder.setPositiveButton("ACEPTAR"){dialog, which: Int ->
                    FirebaseAuth.getInstance().signOut()
                    val intent = Intent(this@DriverHomeActivity,SplashScreenActivity::class.java)
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                    startActivity(intent)
                    finish()
                }
                builder.setCancelable(false)
                val dialog = builder.create()
                dialog.setOnShowListener{
                    dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                        .setTextColor(resources.getColor(android.R.color.holo_red_dark))
                    dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                        .setTextColor(resources.getColor(android.R.color.darker_gray))
                }
                dialog.show()

            }
            true
        }

        val headerView = navView.getHeaderView(0)
        val txt_start = headerView.findViewById<View>(R.id.txt_start) as TextView
         img_avatar = headerView.findViewById<View>(R.id.img_avatar) as ImageView
        val txt_name = headerView.findViewById<View>(R.id.txt_name) as TextView
        val txt_phone = headerView.findViewById<View>(R.id.txt_phone) as TextView

        txt_name.setText(Common.buildWelcomeMessage())
        txt_phone.setText(Common.currentUser!!.phoneNumber)
        txt_start.setText(StringBuilder().append(Common.currentUser!!.rating))
        if (Common.currentUser!!.avatar != null && Common.currentUser != null && !TextUtils.isEmpty(Common.currentUser!!.avatar))
        {
            Glide.with(this).load(Common.currentUser!!.avatar)
                .into(img_avatar)

        }
        img_avatar.setOnClickListener {
            val intent = Intent()
            intent.setType("image/*")
            intent.setAction(Intent.ACTION_GET_CONTENT)
            startActivityForResult(Intent.createChooser(intent,"Selecciona una imagen"),REQUEST_CODE_IMAGE)
        }
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode == REQUEST_CODE_IMAGE && resultCode == Activity.RESULT_OK)
        {
            if (data != null && data.data != null){
                imageUri = data.data
                img_avatar.setImageURI(imageUri)
                showDialogUpload()
            }

        }
    }

    private fun showDialogUpload() {
        val builder = AlertDialog.Builder(this@DriverHomeActivity)
        builder.setTitle("Actualizar Avatar")
        builder.setMessage("¿Estas seguro de cambiar tu avatar?")
        builder.setNegativeButton("CANCELAR"){dialog, which: Int ->  dialog.dismiss()}
        builder.setPositiveButton("ACTUALIZAR"){dialog, which: Int ->

            if(imageUri != null)
            {
                waitingDialog.show()
                val avatarFolder = storageRef.child("avatars/"+FirebaseAuth.getInstance().currentUser!!.uid)

                avatarFolder.putFile(imageUri!!)
                    .addOnFailureListener {
                        e->
                        Snackbar.make(drawerLayout,e.message!!,Snackbar.LENGTH_LONG).show()
                        waitingDialog.dismiss()
                    }
                    .addOnCompleteListener{
                        task ->
                        if (task.isSuccessful){
                            avatarFolder.downloadUrl.addOnSuccessListener { uri->
                                val update_data =  HashMap<String,Any>()
                                update_data.put("avatar",uri.toString())
                                UserUtils.updateUser(drawerLayout,update_data)

                            }
                        }

                        waitingDialog.dismiss()

                    }.addOnProgressListener {
                        taskSnapshot ->
                        val progress = (100.0 * taskSnapshot.bytesTransferred / taskSnapshot.totalByteCount)
                        waitingDialog.setMessage(StringBuilder("Actualizando: ").append(progress).append("%"))
                    }


            }

        }
        builder.setCancelable(false)
        val dialog = builder.create()
        dialog.setOnShowListener{
            dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setTextColor(resources.getColor(android.R.color.holo_red_dark))
            dialog.getButton(AlertDialog.BUTTON_NEGATIVE)
                .setTextColor(resources.getColor(android.R.color.darker_gray))
        }
        dialog.show()

    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        // Inflate the menu; this adds items to the action bar if it is present.
        menuInflater.inflate(R.menu.driver_home, menu)
        return true
    }

    override fun onSupportNavigateUp(): Boolean {
        val navController = findNavController(R.id.nav_host_fragment)
        return navController.navigateUp(appBarConfiguration) || super.onSupportNavigateUp()
    }

    companion object{
        val REQUEST_CODE_IMAGE = 1212
    }
}