package com.example.tracker_activity;

import static android.content.ContentValues.TAG;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.google.android.gms.location.Geofence;
import com.google.android.gms.location.GeofencingClient;
import com.google.android.gms.location.GeofencingRequest;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.CircleOptions;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.tracker_activity.databinding.ActivityMapsBinding;
import com.google.android.gms.maps.model.Polygon;
import com.google.android.gms.maps.model.PolygonOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;

import java.util.ArrayList;
import java.util.List;

public class MapsActivity extends AppCompatActivity
        implements
        GoogleMap.OnMyLocationButtonClickListener,
        OnMapReadyCallback,
        ActivityCompat.OnRequestPermissionsResultCallback {

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;
    private int BACKGROUND_LOCATION_ACCESS_REQUEST_CODE = 10002;
    private boolean permissionDenied = false;
    private GoogleMap map;
    private ActivityMapsBinding binding;
    private static final int REQUEST_LOCATION = 1;
    LocationManager locationManager;
    String latitude, longitude;
    private String provider_info;
    boolean isGPSEnabled = false;

    boolean isNetworkEnabled = false;
    boolean isGPSTrackingEnabled = false;
    Location location;
    private static final long MIN_DISTANCE_CHANGE_FOR_UPDATES = 10; // 10 meters
    private static final long MIN_TIME_BW_UPDATES = 1000 * 60 * 1; // 1 minute

    List<LatLng> points = new ArrayList<>();
    Handler gpsTrackHandler = new Handler();
    LatLng plots;
    private volatile boolean isRunning = true;

    List<Marker> markerList = new ArrayList<>();

    private static final long GEO_DURATION = 60 * 60 * 1000;
    private static final String GEOFENCE_REQ_ID = "My Geofence";
    private static final float GEOFENCE_RADIUS = 100.0f; // in meters

    GeofencingClient geofencingClient;
    private GeofenceHelper geofenceHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager().findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);

        providercheck();

        points.add(new LatLng(12.953013054035946, 77.5417514266668));
        points.add(new LatLng(12.95428866232216, 77.5438757362066));
        points.add(new LatLng(12.95558517552543, 77.54565672299249));
        points.add(new LatLng(12.956442543452548, 77.54752354046686));
        points.add(new LatLng(12.95675621390793, 77.54919723889215));
        points.add(new LatLng(12.957069883968225, 77.55112842938287));
        points.add(new LatLng(12.957711349517467, 77.55308710458465));
        points.add(new LatLng(12.958464154110917, 77.55514704110809));
        points.add(new LatLng(12.959656090062529, 77.5559409749765));
        points.add(new LatLng(12.960814324441305, 77.5574167738546));
        points.add(new LatLng(12.961253455257907, 77.5592192183126));
        points.add(new LatLng(12.96156308861349, 77.56126500049922));
        points.add(new LatLng(12.961814019848779, 77.56308890262935));
        points.add(new LatLng(12.962775920574138, 77.56592131534907));
        points.add(new LatLng(12.963340512746937, 77.5676379291186));
        points.add(new LatLng(12.96411114676894, 77.56951526792183));
        points.add(new LatLng(12.964382986146292, 77.5717254081501));
        points.add(new LatLng(12.964584624077109, 77.57370388966811));
        points.add(new LatLng(77.57370388966811, 77.57591402989638));
        points.add(new LatLng(12.963795326167373, 77.57798997552734));
        points.add(new LatLng(12.96358621848664, 77.58015720041138));
        points.add(new LatLng(12.963481664580394, 77.58189527185303));

        geofencingClient = LocationServices.getGeofencingClient(this);
        geofenceHelper = new GeofenceHelper(this);
    }

    private void providercheck() {
        try {
            locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
            isGPSEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
            isNetworkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
            if (isGPSEnabled) {
                this.isGPSTrackingEnabled = true;
                provider_info = LocationManager.GPS_PROVIDER;
                getLocation();
            } else if (isNetworkEnabled) {
                this.isGPSTrackingEnabled = true;
                provider_info = LocationManager.NETWORK_PROVIDER;
                getLocation();
            }
            if (!provider_info.isEmpty()) {
                if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(this,
                        Manifest.permission.ACCESS_COARSE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED) {
                    return;
                }
                locationManager.requestLocationUpdates(
                        provider_info,
                        MIN_TIME_BW_UPDATES,
                        MIN_DISTANCE_CHANGE_FOR_UPDATES,
                        (LocationListener) this
                );
                if (locationManager != null) {
                    location = locationManager.getLastKnownLocation(provider_info);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMapReady(@NonNull GoogleMap googleMap) {
        map = googleMap;
        map.setOnMyLocationButtonClickListener(this);
        map.setMapType(2);
        enableMyLocation();
        List<LatLng> latlong = new ArrayList<>(points);
        for (int i = 0; i < latlong.size(); i++) {
            MarkerOptions markerOptions = new MarkerOptions()
                    .draggable(true)
                    .position(latlong.get(i))
                    .title(String.valueOf(latlong.get(i)));
            Marker marker = map.addMarker(markerOptions);
            markerList.add(marker);
            marker.setTag(latlong.get(i));
            map.moveCamera(CameraUpdateFactory.newLatLngZoom(latlong.get(1), 16));

            addCircle(latlong, GEOFENCE_RADIUS);
            addGeofence(latlong, GEOFENCE_RADIUS);
        }
    }

    private void addGeofence(List<LatLng> latlong, float radius) {
        for (int i = 0; i < latlong.size(); i++) {
            Geofence geofence = geofenceHelper.getGeofence(GEOFENCE_REQ_ID, latlong.get(i), radius,
                    Geofence.GEOFENCE_TRANSITION_ENTER |
                            Geofence.GEOFENCE_TRANSITION_DWELL | Geofence.GEOFENCE_TRANSITION_EXIT);
            GeofencingRequest geofencingRequest = geofenceHelper.getGeofencingRequest(geofence);
            PendingIntent pendingIntent = geofenceHelper.getPendingIntent();

            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                    != PackageManager.PERMISSION_GRANTED) {
                return;
            }
            geofencingClient.addGeofences(geofencingRequest, pendingIntent)
                    .addOnSuccessListener(new OnSuccessListener<Void>() {
                        @Override
                        public void onSuccess(Void aVoid) {
                            Log.d(TAG, "onSuccess: Geofence Added...");
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            String errorMessage = geofenceHelper.getErrorString(e);
                            Log.d(TAG, "onFailure: " + errorMessage);
                        }
                    });
        }
    }

    private void addCircle(List<LatLng> latlong, float geofenceRadius) {
        for (int i = 0; i < latlong.size(); i++) {
            CircleOptions circleOptions = new CircleOptions();
            circleOptions.center(latlong.get(i));
            circleOptions.radius(GEOFENCE_RADIUS);
            circleOptions.strokeColor(Color.argb(55, 255, 0, 0));
            circleOptions.fillColor(Color.argb(24, 55, 0, 0));
            circleOptions.strokeWidth(4);
            map.addCircle(circleOptions);
        }
    }

    @SuppressLint("MissingPermission")
    private void enableMyLocation() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED
                || ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            map.setMyLocationEnabled(true);
            return;
        }

        PermissionUtils.requestLocationPermissions(this, LOCATION_PERMISSION_REQUEST_CODE, true);
    }

    @Override
    public boolean onMyLocationButtonClick() {
        Toast.makeText(this, "MyLocation button clicked", Toast.LENGTH_SHORT).show();
        return false;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        if (requestCode != LOCATION_PERMISSION_REQUEST_CODE) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            return;
        }
        if (requestCode == BACKGROUND_LOCATION_ACCESS_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                //We have the permission
                Toast.makeText(this, "You can add geofences...", Toast.LENGTH_SHORT).show();
            } else {
                //We do not have the permission..
                Toast.makeText(this, "Background location access is neccessary for geofences to trigger...", Toast.LENGTH_SHORT).show();
            }
        }
        if (PermissionUtils.isPermissionGranted(permissions, grantResults,
                Manifest.permission.ACCESS_FINE_LOCATION) || PermissionUtils
                .isPermissionGranted(permissions, grantResults,
                        Manifest.permission.ACCESS_COARSE_LOCATION)) {
            // Enable the my location layer if the permission has been granted.
            enableMyLocation();
        } else {
            // Permission was denied. Display an error message
            // Display the missing permission error dialog when the fragments resume.
            permissionDenied = true;
        }
    }

    @Override
    protected void onResumeFragments() {
        super.onResumeFragments();
        if (permissionDenied) {
            // Permission was not granted, display error dialog.
            showMissingPermissionError();
            permissionDenied = false;
        }
    }

    private void showMissingPermissionError() {
        PermissionUtils.PermissionDeniedDialog
                .newInstance(true).show(getSupportFragmentManager(), "dialog");
    }

    private void OnGPS() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Enable GPS").setCancelable(false).setPositiveButton("Yes", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                startActivity(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS));
            }
        }).setNegativeButton("No", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        final AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private void getLocation() {
        if (ActivityCompat.checkSelfPermission(
                MapsActivity.this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                MapsActivity.this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION);
        } else {
            Location locationGPS = locationManager.getLastKnownLocation(provider_info);
            if (locationGPS != null) {
                plots = new LatLng(locationGPS.getLatitude(), locationGPS.getLongitude());
                double lat = locationGPS.getLatitude();
                double longi = locationGPS.getLongitude();
                latitude = String.valueOf(lat);
                longitude = String.valueOf(longi);
                points.add(plots);
                List<LatLng> latlong = new ArrayList<>(points);
                for (int i = 0; i < latlong.size(); i++) {
                    map.addMarker(new MarkerOptions().position(latlong.get(i)));
                    map.addPolyline(new PolylineOptions().addAll(latlong)
                            .width(5)
                            .color(Color.GREEN).geodesic(true));
                    map.moveCamera(CameraUpdateFactory.newLatLngZoom(plots, 16));
                }
                Toast.makeText(this, "Your Location: " + "\n" + "Latitude: " + latitude + "\n" + "Longitude: " + longitude, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "Unable to find location.", Toast.LENGTH_SHORT).show();
            }
        }
    }

}