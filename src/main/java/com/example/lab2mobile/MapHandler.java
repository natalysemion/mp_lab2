package com.example.lab2mobile;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Looper;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.maps.DirectionsApi;
import com.google.maps.GeoApiContext;
import com.google.maps.PendingResult;
import com.google.maps.model.DirectionsResult;
import com.google.maps.model.DirectionsRoute;
import com.google.maps.model.TravelMode;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

public class MapHandler implements OnMapReadyCallback {

    private GoogleMap mMap;
    private Context context;
    private FusedLocationProviderClient fusedLocationClient;
    private boolean isLocationProcessed = false;

    public MapHandler(SupportMapFragment mapFragment, Context context) {
        this.context = context;
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(context);
        mapFragment.getMapAsync(this);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        this.mMap = googleMap;
    }

    public void showRoute(String destinationName) {
        if (mMap != null) {
            if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
                getCurrentLocation(destinationName);
            } else {
                ActivityCompat.requestPermissions((Activity) context, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 1);
            }
        }
    }

    private void getCurrentLocation(String destinationName) {
        isLocationProcessed = false;
        LocationRequest locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000);
        locationRequest.setFastestInterval(5000);

        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            fusedLocationClient.requestLocationUpdates(locationRequest, new LocationCallback() {
                @Override
                public void onLocationResult(LocationResult locationResult) {
                    if (locationResult == null || isLocationProcessed) {
                        return;
                    }

                    Location userLocation = locationResult.getLastLocation();

                    if (userLocation != null) {
                        // Mark location as processed
                        isLocationProcessed = true;

                        // Stop location updates
                        fusedLocationClient.removeLocationUpdates(this);

                        LatLng userLatLng = new LatLng(userLocation.getLatitude(), userLocation.getLongitude());

                        // Find the destination coordinates
                        Geocoder geocoder = new Geocoder(context, Locale.getDefault());
                        List<Address> addresses = null;
                        try {
                            addresses = geocoder.getFromLocationName(destinationName, 1);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }

                        if (addresses != null && !addresses.isEmpty()) {
                            Address address = addresses.get(0);
                            LatLng destinationLatLng = new LatLng(address.getLatitude(), address.getLongitude());

                            // Add markers to the map
                            mMap.clear();
                            mMap.addMarker(new MarkerOptions().position(userLatLng).title("Your Location"));
                            mMap.addMarker(new MarkerOptions().position(destinationLatLng).title(destinationName));
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(userLatLng, 10));

                            drawRoute(userLatLng, destinationLatLng);  // Call drawRoute only once
                        }
                    }
                }
            }, Looper.getMainLooper());
        }
    }

    private void drawRoute(LatLng origin, LatLng destination) {
        // Create GeoApiContext with your API key
        GeoApiContext geoApiContext = new GeoApiContext.Builder()
                .apiKey("AIzaSyBZ-Rnfszks3zWHJGvViKK5U3NCGVzK-Cc")  // Insert your API key here
                .build();

        // Convert Android LatLng to Google Maps API LatLng (com.google.maps.model.LatLng)
        com.google.maps.model.LatLng originLatLng = new com.google.maps.model.LatLng(origin.latitude, origin.longitude);
        com.google.maps.model.LatLng destinationLatLng = new com.google.maps.model.LatLng(destination.latitude, destination.longitude);

        // Build the request to the Directions API
        DirectionsApi.newRequest(geoApiContext)
                .origin(originLatLng)  // Set the origin
                .destination(destinationLatLng)  // Set the destination
                .alternatives(false)  // Get only one route
                .mode(TravelMode.DRIVING)
                .setCallback(new PendingResult.Callback<DirectionsResult>() {
                    @Override
                    public void onResult(DirectionsResult result) {
                        DirectionsRoute route = result.routes[0];
                        List<com.google.maps.model.LatLng> path = route.overviewPolyline.decodePath();

                        PolylineOptions polylineOptions = new PolylineOptions();
                        for (com.google.maps.model.LatLng point : path) {
                            polylineOptions.add(new LatLng(point.lat, point.lng));
                        }

                        ((Activity) context).runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                // Add the polyline to the map
                                mMap.addPolyline(polylineOptions);
                            }
                        });
                    }

                    @Override
                    public void onFailure(Throwable e) {
                        // Log the error
                        e.printStackTrace();
                    }
                });
    }

}
