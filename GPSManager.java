package com.farmappweb.pests.android.application;

import android.Manifest;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.util.Log;
import android.widget.Toast;

import com.farmappweb.pests.android.interfaces.GPSListener;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationServices;

/**
 * Created by juanlabrador on 30/11/16.
 */

public class GPSManager implements LocationListener {

    private static final String TAG = "GPSManager";
    private static GPSManager instance;
    private static Context context;
    private static GpsLocationReceiver mGpsLocationReceiver;
    private double latitude;
    private double longitude;
    private static GPSListener gpsListener;

    // Google client to interact with Google API
    private static GoogleApiClient mGoogleApiClient;

    // boolean flag to toggle periodic location updates
    private boolean mRequestingLocationUpdates = false;

    private static LocationRequest mLocationRequest;

    // Location updates intervals in sec
    private static int UPDATE_INTERVAL = 10000; // 10 sec
    private static int FASTEST_INTERVAL = 5000; // 5 sec
    private static int DISPLACEMENT = 1; // 10 meters

    private static GoogleApiClient.ConnectionCallbacks connectionCallbacks;
    private static GoogleApiClient.OnConnectionFailedListener onConnectionFailedListener;

    public GPSManager() {
        connectionCallbacks = new GoogleApiClient.ConnectionCallbacks() {
            @Override
            public void onConnected(@Nullable Bundle bundle) {
                //if (mRequestingLocationUpdates) {
                    startLocationUpdates();
                //}
            }

            @Override
            public void onConnectionSuspended(int i) {
                mGoogleApiClient.connect();
            }
        };

        onConnectionFailedListener = new GoogleApiClient.OnConnectionFailedListener() {
            @Override
            public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
                Log.e(TAG, "Connection failed: ConnectionResult.getErrorCode() = "
                        + connectionResult.getErrorCode());
            }
        };
    }

    public static synchronized void initializeInstance(Context context_) {
        if (instance == null) {
            context = context_;
            instance = new GPSManager();

            if (checkPlayServices()) {
                buildGoogleApiClient();
                createLocationRequest();
            }

            mGpsLocationReceiver = new GpsLocationReceiver();
        }

        context.registerReceiver(mGpsLocationReceiver, new IntentFilter(LocationManager.PROVIDERS_CHANGED_ACTION));
        Log.e(TAG, "initialize GPS!");
    }

    /**
     * Creating google api client object
     * */
    protected synchronized static void buildGoogleApiClient() {
        mGoogleApiClient = new GoogleApiClient.Builder(context)
                .addConnectionCallbacks(connectionCallbacks)
                .addOnConnectionFailedListener(onConnectionFailedListener)
                .addApi(LocationServices.API)
                .build();
    }


    /**
     * Method to verify google play services on the device
     * */
    private static boolean checkPlayServices() {
        int resultCode = GooglePlayServicesUtil
                .isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            return false;
        }
        return true;
    }

    public void onResume() {
        // Resuming the periodic location updates
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()/* && mRequestingLocationUpdates*/) {
                startLocationUpdates();
            }
        }
    }

    public void onPause() {
        stopLocationUpdates();
    }

    /**
     * Method to toggle periodic location updates
     * */
    private void togglePeriodicLocationUpdates() {
        if (!mRequestingLocationUpdates) {
            mRequestingLocationUpdates = true;

            // Starting the location updates
            startLocationUpdates();

            Log.d(TAG, "Periodic location updates started!");

        } else {
            mRequestingLocationUpdates = false;

            // Stopping the location updates
            stopLocationUpdates();

            Log.d(TAG, "Periodic location updates stopped!");
        }
    }

    /**
     * Creating location request object
     * */
    public static void createLocationRequest() {
        mLocationRequest = new LocationRequest();
        mLocationRequest.setInterval(UPDATE_INTERVAL);
        mLocationRequest.setFastestInterval(FASTEST_INTERVAL);
        mLocationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        mLocationRequest.setSmallestDisplacement(DISPLACEMENT); // 10 meters
    }

    /**
     * Starting the location updates
     * */
    public void startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            if (mGoogleApiClient != null) {
                if (mGoogleApiClient.isConnected()) {
                    LocationServices.FusedLocationApi.requestLocationUpdates(
                            mGoogleApiClient, mLocationRequest, this);
                }
            }
        }
    }

    /**
     * Stopping location updates
     */
    public void stopLocationUpdates() {
        if (mGoogleApiClient != null) {
            if (mGoogleApiClient.isConnected()) {
                LocationServices.FusedLocationApi.removeLocationUpdates(
                        mGoogleApiClient, this);
            }
        }
    }

    public void onStart() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    public static synchronized GPSManager getInstance() {
        if (instance == null) {
            throw new IllegalStateException(TAG + " is not initialized, call initializeInstance(..) method first.");
        }

        return instance;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void onStop() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }
    }

    public void onDestroy() {
        if (mGoogleApiClient != null) {
            mGoogleApiClient.disconnect();
        }

        latitude = 0;
        longitude = 0;

        gpsListener = null;

        try {
            if (mGpsLocationReceiver != null) {
                context.unregisterReceiver(mGpsLocationReceiver);
            }
        } catch (IllegalArgumentException e) {
            Log.e(TAG, e.getMessage());
        }
    }

    public void onGPSListener(GPSListener gpsListener_) {
        gpsListener = gpsListener_;
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.e(TAG, "Location updated: Lat: " + location.getLatitude() + " Lng: " + location.getLongitude());
        latitude = location.getLatitude();
        longitude = location.getLongitude();

        if (gpsListener != null) {
            gpsListener.onLocationChanged(location.getLatitude(), location.getLongitude());
        }
    }


    public static class GpsLocationReceiver extends BroadcastReceiver {

        public GpsLocationReceiver() {}

        @Override
        public void onReceive(Context context, Intent intent) {
            LocationManager manager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
            if (gpsListener != null) {
                gpsListener.onStatusGPS(manager.isProviderEnabled(LocationManager.GPS_PROVIDER));
            }
        }
    }
}