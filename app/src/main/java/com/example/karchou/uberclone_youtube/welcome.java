package com.example.karchou.uberclone_youtube;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.Switch;
import android.widget.Toast;


import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;

import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.maps.model.SquareCap;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class welcome extends FragmentActivity implements OnMapReadyCallback,
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        LocationListener
{

    private GoogleMap mMap;
    private static final int MY_PERMISSION_REQUEST_CODE= 7000;
    private static final int PLAY_SERVICE_RES_REQUEST=7001;
    private LocationRequest mLocationRequest;
    private GoogleApiClient mGoogleAPIclinet;
    private Location mlastlocation;
    private static int UPDATE_INTERVAL=5000;
    private static int FASTES_INTERVAL=3000;
    private static int DISPLACEMENT=10;

    DatabaseReference drivers;
    GeoFire geoFire;
    Marker mCurrent;
    Switch location_switch;
    SupportMapFragment mapFragment;

    private List<LatLng> polyLineList;
    private Marker pickupLocatiionMarker,carMarker;
    private float v;
    private double lat,lng;
    private LatLng startposition,endposition,currentposition;
    private int index,next;
    private Button btnGo;
    private EditText edtPlace;
    private String destination;
    private PolylineOptions polylineOptions,blackpolylineoptions;
    private Polyline blackpolyline,greypolyline;

    private LinearLayout layoutpanel;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        layoutpanel=(LinearLayout) findViewById(R.id.relativelayout);
        location_switch=(Switch)findViewById(R.id.location_switch);
        location_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isOnline) {
               if (isOnline) {
                   starlocationUpdates();
                   displaylocation();
//                   Snackbar.make(layoutpanel.getRootView(),"You are Online",Snackbar.LENGTH_SHORT).show();

                   Toast.makeText(getApplicationContext(),"You are Online",Toast.LENGTH_SHORT).show();
               }
               else {
                   stopLocationUpdates();
                   if (mCurrent!=null) {
                       mCurrent.remove();
//                 Snackbar.make(layoutpanel.getRootView(),"You are Offline",Snackbar.LENGTH_SHORT).show();
                   Toast.makeText(getApplicationContext(), "You are Offline", Toast.LENGTH_SHORT).show();
                   }
                   else {
                       Toast.makeText(getApplicationContext(), "Error: Location not preseent", Toast.LENGTH_SHORT).show();
                   }
               }
            }
        });

        drivers= FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire=new GeoFire(drivers);
        setupLocation();
    }

    private void getDirection() {
        currentposition=new LatLng(mlastlocation.getLatitude(),mlastlocation.getLongitude());
        String requestAPI=null;
    }

    private void setupLocation() {

        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(this,new String[] {
                    Manifest.permission.ACCESS_COARSE_LOCATION,
                    Manifest.permission.ACCESS_FINE_LOCATION
            },MY_PERMISSION_REQUEST_CODE);
        }

        else {
            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
                if (location_switch.isChecked())
                    displaylocation();
            }
        }
    }

    private void createLocationRequest() {
        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTES_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private boolean checkPlayServices() {
        int resultset= GooglePlayServicesUtil.isGooglePlayServicesAvailable(this);

        if (resultset!= ConnectionResult.SUCCESS) {
            if (GooglePlayServicesUtil.isUserRecoverableError(resultset))
                GooglePlayServicesUtil.getErrorDialog(resultset,this,PLAY_SERVICE_RES_REQUEST).show();
            else {
                Toast.makeText(this, "The device is not supported", Toast.LENGTH_SHORT).show();
                finish();
            }
            return false;
        }
        return true;
    }

    private void buildGoogleApiClient() {
        mGoogleAPIclinet=new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleAPIclinet.connect();
    }

    private void stopLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {

            return;
        }
        LocationServices.FusedLocationApi.removeLocationUpdates(mGoogleAPIclinet,this);
    }

    private void displaylocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {

            return;
        }
       mlastlocation=LocationServices.FusedLocationApi.getLastLocation(mGoogleAPIclinet);

        if (mlastlocation!=null) {
            if(location_switch.isChecked()) {
                final double latittude=mlastlocation.getLatitude();
                final double longitude=mlastlocation.getLongitude();

                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latittude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (mCurrent!=null) {
                            mCurrent.remove();
                            mCurrent=mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.car))
                                                                       .position(new LatLng(latittude,longitude))
                                                                       .title("You"));
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latittude,longitude),15.0f));


                        }
                    }
                });
            }
        }

        else {
            Log.d("ERROR","Cant get location");
            Toast.makeText(this,"Cannot get your location",Toast.LENGTH_SHORT).show();
        }
    }

    private void rotateMarker(final Marker mCurrent, final float i, GoogleMap mMap) {
        final Handler handler=new Handler();
        final long start= SystemClock.uptimeMillis();
        final float startRotation=mCurrent.getRotation();
        final long duration=1500;
        final Interpolator interpolator=new LinearInterpolator();
        handler.post(new Runnable() {
            @Override
            public void run() {
                long elapsed=SystemClock.uptimeMillis()-start;
                float t=interpolator.getInterpolation((float)elapsed/duration);
                float rot=t*i+(1-t)*startRotation;
                mCurrent.setRotation(-rot>180?rot/2:rot);

                if (t<1.0) {
                    handler.postDelayed(this,16);
                }
            }
        });
    }

    private void starlocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)!= PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)!= PackageManager.PERMISSION_GRANTED) {

                return;
        }
        LocationServices.FusedLocationApi.requestLocationUpdates(mGoogleAPIclinet,mLocationRequest,this);
    }


    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);
        mMap.setTrafficEnabled(false);
        mMap.setIndoorEnabled(false);
        mMap.setBuildingsEnabled(false);
        mMap.getUiSettings().setZoomControlsEnabled(true);

    }

    @Override
    public void onConnected(@Nullable Bundle bundle) {
       displaylocation();
       starlocationUpdates();
    }

    @Override
    public void onConnectionSuspended(int i) {
      mGoogleAPIclinet.connect();
    }

    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {

    }

    @Override
    public void onLocationChanged(Location location) {
      mlastlocation=location;
      displaylocation();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        switch (requestCode)
        {
            case MY_PERMISSION_REQUEST_CODE:
                if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                    if (checkPlayServices()) {
                        buildGoogleApiClient();
                        createLocationRequest();
                        if (location_switch.isChecked())
                            displaylocation();
                    }
                }
        }
    }
}
