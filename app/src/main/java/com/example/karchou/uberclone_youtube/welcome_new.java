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
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.FragmentActivity;
import android.os.Bundle;

import com.example.karchou.uberclone_youtube.Common.Common;
import com.example.karchou.uberclone_youtube.Remote.iGoogleAPI;
import com.firebase.geofire.GeoFire;
import com.firebase.geofire.GeoLocation;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.CameraPosition;
import com.google.android.gms.maps.model.JointType;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import android.location.LocationListener;
import android.location.LocationManager;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.View;
import android.view.animation.Interpolator;
import android.view.animation.LinearInterpolator;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Switch;
import android.widget.Toast;


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
    private Marker carMarker;
    private float v;
    private double lat,lng;
    private Handler handler;
    private int index,next;
    private Button btnGo;
    private EditText edtplace;
    private String destination;
    private PolylineOptions polylineOptions, blackPolylineOptions;
    private Polyline blackPolyline, greyPolyline;
    private iGoogleAPI mService;


    Runnable drawPathRunnable=new Runnable() {
        @Override
        public void run() {
           if(index<polylinelist.size()-1) {
               index++;
               next=index+1;
           }
           if (index<polylinelist.size()-1) {
               startposition=polylinelist.get(index);
               endposition=polylinelist.get(next);
           }

           final ValueAnimator valueAnimator=ValueAnimator.ofFloat(0,1);
           valueAnimator.setDuration(3000);
           valueAnimator.setInterpolator(new LinearInterpolator());
           valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
               @Override
               public void onAnimationUpdate(ValueAnimator animation) {
                   v=valueAnimator.getAnimatedFraction();
                   lng=v*endposition.longitude+(1-v)*startposition.longitude;
                   lat=v*endposition.latitude +(1-v)*startposition.latitude;
                   LatLng newPos=new LatLng(lat,lng);
                   carMarker.setPosition(newPos);
                   carMarker.setAnchor(0.5f,0.5f);
                   carMarker.setRotation(getBearing(startposition,newPos));
                   mMap.moveCamera(CameraUpdateFactory.newCameraPosition(
                           new CameraPosition.Builder()
                           .target(newPos)
                           .zoom(15.5f)
                           .build()
                   ));
               }
           });
           valueAnimator.start();
           handler.postDelayed(this,3000);
        }
    };

    private float getBearing(LatLng startposition, LatLng endPosition) {
        double lat=Math.abs(startposition.latitude-endPosition.latitude);
        double lon=Math.abs(startposition.longitude-endPosition.longitude);

        if (startposition.latitude<endPosition.latitude && startposition.longitude<endPosition.longitude)
            return (float) (Math.toDegrees(Math.atan(lon/lat)));
        else if (startposition.latitude>=endPosition.latitude && startposition.longitude<endPosition.longitude)
            return (float) ((90-Math.toDegrees(Math.atan(lon/lat)))+90);
        else if (startposition.latitude>=endPosition.latitude && startposition.longitude>=endPosition.longitude)
            return (float) (Math.toDegrees(Math.atan(lon/lat))+180);
        else if (startposition.latitude<endPosition.latitude && startposition.longitude>=endPosition.longitude)
            return (float) ((90-Math.toDegrees(Math.atan(lon/lat)))+270);
        return -1;
    }

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
                   if (mCurrent!=null) {
                       mCurrent.remove();
                   }
//                   handler.removeCallbacks(drawPathRunnable);
                   Toast.makeText(getApplicationContext(), "You are Offline", Toast.LENGTH_SHORT).show();
                }
            }
        });

        polylinelist=new ArrayList<>();
        btnGo=(Button)findViewById(R.id.btnGo);
        edtplace=(EditText)findViewById(R.id.edittxty);


        btnGo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                destination=edtplace.getText().toString();
                destination=destination.replace("","+");
                getDirection();
            }
        });

        drivers=FirebaseDatabase.getInstance().getReference("Drivers");
        geoFire=new GeoFire(drivers);
        mService= Common.getGoogleAPI();
    }

    private void getDirection() {
        currentposition=new LatLng(mlastlocation.getLatitude(),mlastlocation.getLongitude());
        String requestAPI=null;

        try{
             requestAPI="https://maps.googleapis.com/maps/api/directions/json?"+
                         "mode=driving&"+
                         "transit_routing_preference=less_driving&"+
                          "origin="+currentposition.latitude+","+currentposition.longitude+
                          "&destination="+destination+"&"+
                          "key="+getResources().getString(R.string.google_direction_api);
             Log.d("karchouURL",requestAPI);

             mService.getPath(requestAPI)
                     .enqueue(new Callback<String>() {
                         @Override
                         public void onResponse(Call<String> call, Response<String> response) {
                             try {
                                 JSONObject jsonObject=new JSONObject(response.body().toString());

                                 JSONArray jsonArray=jsonObject.getJSONArray("routes");

                                 for (int i=0;i<jsonArray.length();i++) {
                                     JSONObject route=jsonArray.getJSONObject(i);
                                     JSONObject poly=route.getJSONObject("overview_polyline");


                                     String polyline=poly.getString("points");
                                     polylinelist=decodePoly(polyline);

                                 }

                                 LatLngBounds.Builder builder=new LatLngBounds.Builder();
                                 for (LatLng latLng:polylinelist)
                                     builder.include(latLng);
                                 LatLngBounds bounds=builder.build();
                                 CameraUpdate mCameraUpdate=CameraUpdateFactory.newLatLngBounds(bounds,2);

                                 mMap.animateCamera(mCameraUpdate);

                                 polylineOptions=new PolylineOptions();
                                 polylineOptions.color(Color.GRAY);
                                 polylineOptions.width(5);
                                 polylineOptions.startCap(new SquareCap());
                                 polylineOptions.endCap(new SquareCap());

                                 polylineOptions.jointType(JointType.ROUND);
                                 polylineOptions.addAll(polylinelist);

                                 greyPolyline=mMap.addPolyline(polylineOptions);


                                 blackPolylineOptions=new PolylineOptions();
                                 blackPolylineOptions.color(Color.BLACK);
                                 blackPolylineOptions.width(5);
                                 blackPolylineOptions.startCap(new SquareCap());
                                 blackPolylineOptions.endCap(new SquareCap());

                                 blackPolylineOptions.jointType(JointType.ROUND);
                                 blackPolyline=mMap.addPolyline(blackPolylineOptions);

                                 mMap.addMarker(new MarkerOptions()
                                                .position(polylinelist.get(polylinelist.size()-1))
                                                 .title("Pickup Location"));


                                 ValueAnimator polyLineanimator=ValueAnimator.ofInt(0,100);
                                 polyLineanimator.setDuration(2000);
                                 polyLineanimator.setInterpolator(new LinearInterpolator());
                                 polyLineanimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                                     @Override
                                     public void onAnimationUpdate(ValueAnimator animation) {
                                         List<LatLng> points=greyPolyline.getPoints();
                                         int Percentvalue=(int)animation.getAnimatedValue();
                                         int size=points.size();

                                         int newPoints=(int)(size*(Percentvalue/100.0f));

                                         List<LatLng> p=points.subList(0,newPoints);
                                         blackPolyline.setPoints(p);
                                     }
                                 });

                                 polyLineanimator.start();

                                 carMarker=mMap.addMarker(new MarkerOptions().position(currentposition)
                                                .flat(true)
                                                .icon(BitmapDescriptorFactory.fromResource(R.drawable.carertical)));


                                 handler=new Handler();
                                 index=-1;
                                 next=1;
                                 handler.postDelayed(drawPathRunnable,3000);

                             } catch (JSONException e) {
                                 e.printStackTrace();
                             }
                         }

                         @Override
                         public void onFailure(Call<String> call, Throwable t) {
                          Toast.makeText(welcome_new.this,""+t.getMessage(),Toast.LENGTH_SHORT).show();
                         }
                     });
        }
        catch (Exception ex) {
           ex.printStackTrace();
        }
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

                     displaylocation();
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

                        mCurrent = mMap.addMarker(new MarkerOptions()
                                .position(new LatLng(latittude, longitude))
                                .title("Your Location"));
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

    private List decodePoly(String encoded) {

        List poly = new ArrayList();
        int index = 0, len = encoded.length();
        int lat = 0, lng = 0;

        while (index < len) {
            int b, shift = 0, result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlat = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lat += dlat;

            shift = 0;
            result = 0;
            do {
                b = encoded.charAt(index++) - 63;
                result |= (b & 0x1f) << shift;
                shift += 5;
            } while (b >= 0x20);
            int dlng = ((result & 1) != 0 ? ~(result >> 1) : (result >> 1));
            lng += dlng;

            LatLng p = new LatLng((((double) lat / 1E5)),
                    (((double) lng / 1E5)));
            poly.add(p);
        }

        return poly;
    }
}
