package com.christian.kotlinuberclone.ui.home

import android.annotation.SuppressLint
import android.content.res.Resources
import android.os.Bundle
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.RelativeLayout
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelProviders
import com.christian.kotlinuberclone.R
import com.christian.kotlinuberclone.common.Common
import com.firebase.geofire.GeoFire
import com.firebase.geofire.GeoLocation
import com.google.android.gms.location.*
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.OnMapReadyCallback
import com.google.android.gms.maps.SupportMapFragment
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.MapStyleOptions
import com.google.android.gms.maps.model.MarkerOptions
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthSettings
import com.google.firebase.database.*
import com.karumi.dexter.Dexter
import com.karumi.dexter.PermissionToken
import com.karumi.dexter.listener.PermissionDeniedResponse
import com.karumi.dexter.listener.PermissionGrantedResponse
import com.karumi.dexter.listener.PermissionRequest
import com.karumi.dexter.listener.single.PermissionListener

class HomeFragment : Fragment(), OnMapReadyCallback {

    private lateinit var mMap: GoogleMap
    private lateinit var homeViewModel: HomeViewModel
    private lateinit var mapFragment : SupportMapFragment
    //location
    private lateinit var fusedLocationProviderClient:FusedLocationProviderClient
    private lateinit var locationRequest:LocationRequest
    private lateinit var locationCallback: LocationCallback

    //onlyne system
    private lateinit var onlineRef:DatabaseReference
    private lateinit var currentUserRef:DatabaseReference
    private lateinit var driversLocationRef:DatabaseReference
    private lateinit var geoFire:GeoFire

    private val onlineValueEventListener = object:ValueEventListener{
        override fun onCancelled(p0: DatabaseError) {
            Snackbar.make(mapFragment.requireView(),p0.message,Snackbar.LENGTH_LONG).show()
        }

        override fun onDataChange(p0: DataSnapshot) {
            if (p0.exists())
                currentUserRef.onDisconnect().removeValue()

        }

    }


    override fun onDestroy() {

             fusedLocationProviderClient.removeLocationUpdates(locationCallback)
             geoFire.removeLocation(FirebaseAuth.getInstance().currentUser!!.uid)
             onlineRef.removeEventListener(onlineValueEventListener)


        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        registerOnlineSystem()
    }

    private fun registerOnlineSystem() {
        onlineRef.addValueEventListener(onlineValueEventListener)

    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        homeViewModel = ViewModelProvider(this).get(HomeViewModel::class.java)
        val root = inflater.inflate(R.layout.fragment_home, container, false)

        init()
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = childFragmentManager!!.findFragmentById(R.id.map) as SupportMapFragment
        mapFragment!!.getMapAsync(this)


        return root
    }

    @SuppressLint("MissingPermission")
    private fun init() {

        onlineRef = FirebaseDatabase.getInstance().getReference().child(".info/connected")
        driversLocationRef = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REF)
        currentUserRef = FirebaseDatabase.getInstance().getReference(Common.DRIVERS_LOCATION_REF).child(
            FirebaseAuth.getInstance().currentUser!!.uid
        )

        geoFire = GeoFire(driversLocationRef)
        registerOnlineSystem()

        //locationRequest
        locationRequest = LocationRequest()
        locationRequest.setSmallestDisplacement(10f)
        locationRequest.interval = 5000
        locationRequest.setFastestInterval(3000)
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
        //locationCallback
        locationCallback = object:LocationCallback(){
            override fun onLocationResult(locationResult: LocationResult?) {
                super.onLocationResult(locationResult)
                val newPosition = LatLng(locationResult!!.lastLocation.latitude,locationResult!!.lastLocation.longitude)
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(newPosition,18f))

                //update location
                geoFire.setLocation(
                    FirebaseAuth.getInstance().currentUser!!.uid,
                    GeoLocation(locationResult.lastLocation.latitude,locationResult.lastLocation.longitude)

                ){key:String?,error:DatabaseError?->
                    if (error != null)
                        Snackbar.make(mapFragment.requireView(),error.message,Snackbar.LENGTH_LONG).show()
                    else
                        Snackbar.make(mapFragment.requireView(),"En linea!",Snackbar.LENGTH_SHORT).show()

                }

            }
        }

        fusedLocationProviderClient = LocationServices.getFusedLocationProviderClient(requireContext())
        fusedLocationProviderClient.requestLocationUpdates(locationRequest,locationCallback,Looper.myLooper())

    }

    override fun onMapReady(googleMap: GoogleMap) {
        mMap = googleMap
        mMap.uiSettings.isZoomControlsEnabled = true

        //SOLICITAR PERMISOS PARA ACCEDER A LA UBICACION
        //PERMISO PARA ACCEDER A LA UBICACION ACTUAL
        Dexter.withContext(requireContext())
            .withPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
            .withListener(object: PermissionListener {
                @SuppressLint("MissingPermission")
                override fun onPermissionGranted(p0: PermissionGrantedResponse?) {
                    mMap.isMyLocationEnabled = true
                    mMap.uiSettings.isMyLocationButtonEnabled = true
                    mMap.setOnMyLocationClickListener{


                        fusedLocationProviderClient.lastLocation
                            .addOnFailureListener {
                                Toast.makeText(context!!,""+it.message, Toast.LENGTH_SHORT).show()
                            }
                            .addOnSuccessListener {
                                    location->
                                val userLatlng=LatLng(location.latitude,location.longitude)
                                mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(userLatlng,18f))

                            }
                        true
                    }
                    //set layout button


                    val locationButton = (mapFragment.requireView()
                        .findViewById<View>("1".toInt())!!
                        .parent!! as View).findViewById<View>("2".toInt())
                    val params = locationButton.layoutParams as RelativeLayout.LayoutParams
                    params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM,RelativeLayout.TRUE)
                    params.bottomMargin = 50





                }

                override fun onPermissionRationaleShouldBeShown(
                    p0: PermissionRequest?,
                    p1: PermissionToken?
                ) {

                }

                override fun onPermissionDenied(p0: PermissionDeniedResponse?) {
                    Toast.makeText(context!!,"Permiso denegado", Toast.LENGTH_SHORT).show()
                }

            }).check()

         try {
             val success:Boolean = mMap.setMapStyle(MapStyleOptions.loadRawResourceStyle(context,R.raw.uber_maps_style))
             if (!success)
                 Log.d("CHRISOFT","Error parsing  style")

         }catch (resource:Resources.NotFoundException){
             Log.d("CHRISOFT",resource.message)
         }


    }
}