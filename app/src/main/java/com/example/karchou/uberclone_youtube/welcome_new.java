package com.example.karchou.uberclone_youtube;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;

import android.location.Location;
import android.os.Handler;
import android.os.SystemClock;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;


import com.google.android.gms.maps.model.Polyline;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.ArrayList;
import java.util.List;


public class welcome_new extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    LocationManager locationManager;
    LocationListener locationListener;
    LatLng userlocation;
    DatabaseReference myRef1,myRef2,myRef3;

    public FirebaseDatabase database;
    double currentlat,currentlong;
    Button uberbtn;
    Switch location_switch;
    private LatLng startposition,endposition,currentposition;
    private Location mlastlocation;
    private LocationRequest mLocationRequest;
    DatabaseReference drivers;
    GeoFire geoFire;
    private GoogleApiClient mGoogleAPIclinet;
    Marker mCurrent;
    private static final int MY_PERMISSION_REQUEST_CODE= 7000;
    private static final int PLAY_SERVICE_RES_REQUEST=7001;
    SupportMapFragment mapFragment;
    private static int UPDATE_INTERVAL=5000;
    private static int FASTES_INTERVAL=3000;
    private static int DISPLACEMENT=10;

    private List<LatLng> polylinelist;
    private Marker pickupLocationMarker;
    private float v;
    private double lat,lng;
    private Handler handler;
    private int index,next;
    private Button btnGo;
    private EditText edtplace;
    private String destination;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private Polyline blackPolyline, greyPolyline;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_welcome_new);
        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        uberbtn=(Button)findViewById(R.id.uberbtn);
        location_switch=(Switch)findViewById(R.id.location_switch);


        location_switch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if(isChecked) {
                  startlocationUpdates();
                  displaylocation();

                  Toast.makeText(getApplicationContext(),"You are Online",Toast.LENGTH_SHORT).show();
                }

                else {
                   stoplocationUpdates();
                   mCurrent.remove();
                   Toast.makeText(getApplicationContext(), "You are Offline", Toast.LENGTH_SHORT).show();
                }
            }
        });

        polylinelist=new ArrayList<>();


        drivers=FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire=new GeoFire(drivers);
    }

    private void getDirection() {
        currentposition=new LatLng(mlastlocation.getLatitude(),mlastlocation.getLongitude());
        String requestAPI=null;
    }

    private void startlocationUpdates() {
        locationManager=(LocationManager)this.getSystemService(Context.LOCATION_SERVICE);

        locationListener=new LocationListener() {
            @Override
            public void onLocationChanged(Location location) {
                mlastlocation=location;
                UpdateMap(location);
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {

            }

            @Override
            public void onProviderEnabled(String provider) {

            }

            @Override
            public void onProviderDisabled(String provider) {

            }
        };
    }


    private void displaylocation() {

        if ((ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
           (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {

            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                                                                         Manifest.permission.ACCESS_COARSE_LOCATION},
                                                                         MY_PERMISSION_REQUEST_CODE);
        }
        else
        {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            mlastlocation= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (mlastlocation !=null) {
                if(location_switch.isChecked())
                   UpdateMap(mlastlocation);
           }
        }
    }

    private void stoplocationUpdates() {
        if ((ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {
                            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION},
                                    MY_PERMISSION_REQUEST_CODE);
        }
        else {
            locationManager.removeUpdates(locationListener);
            locationManager=null;
            mlastlocation=null;
            mMap.animateCamera(CameraUpdateFactory.zoomOut());
            mMap.clear();
        }
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
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode==MY_PERMISSION_REQUEST_CODE) {
            if (grantResults.length>0 && grantResults[0]==PackageManager.PERMISSION_GRANTED) {
                if ((ContextCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED) &&
                        (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED)) {

                    locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
                    mlastlocation= locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);

                    if (mlastlocation !=null) {
                        if(location_switch.isChecked()) {
                            UpdateMap(mlastlocation);
                        }
                    }
                }
            }
        }
    }

    private void UpdateMap(Location location) {
        final double latittude = location.getLatitude();
        final double longitude = location.getLongitude();
        userlocation = new LatLng(latittude, longitude);

        if (userlocation!=null) {

            if (location_switch.isChecked()) {
                geoFire.setLocation(FirebaseAuth.getInstance().getCurrentUser().getUid(), new GeoLocation(latittude, longitude), new GeoFire.CompletionListener() {
                    @Override
                    public void onComplete(String key, DatabaseError error) {
                        if (mCurrent != null)
                            mCurrent.remove();

                        mCurrent = mMap.addMarker(new MarkerOptions().icon(BitmapDescriptorFactory.fromResource(R.drawable.carertical))
                                .position(new LatLng(latittude, longitude))
                                .title("You"));
                        mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(latittude, longitude), 15.0f));
                        //rotateMarker(mCurrent, -360, mMap);
                    }
                });
            }
        } else {
            Log.d("ERROR","");
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

    private void createLocationRequest() {
        mLocationRequest=new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTES_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT);
    }

    private void buildGoogleApiClient() {
        mGoogleAPIclinet=new GoogleApiClient.Builder(this)
                .addApi(LocationServices.API)
                .build();
        mGoogleAPIclinet.connect();
    }
}
