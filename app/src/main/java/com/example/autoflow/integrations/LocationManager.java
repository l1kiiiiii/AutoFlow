package com.example.autoflow.integrations;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.os.Bundle;
import android.os.Looper;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresPermission;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.OnFailureListener;

/**
 * Enhanced LocationManager for AutoFlow automation app
 * Provides location services with proper permission handling and error management
 */
public class LocationManager {
    private static final String TAG = "AutoFlow-LocationManager";
    private final Context context;
    private final android.location.LocationManager androidLocationManager; // Fully qualified name
    private final FusedLocationProviderClient fusedLocationClient;
    private LocationCallback callback;

    public interface LocationCallback {
        void onLocationReceived(@NonNull Location location);
        void onLocationError(@NonNull String errorMessage);
        void onPermissionDenied();
    }

    public LocationManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        // Use fully qualified name to avoid naming conflict
        this.androidLocationManager = (android.location.LocationManager)
                this.context.getSystemService(Context.LOCATION_SERVICE);
        this.fusedLocationClient = LocationServices.getFusedLocationProviderClient(this.context);

        if (this.androidLocationManager == null) {
            Log.e(TAG, "Android LocationManager service not available on this device");
        }
    }

    /**
     * Check if location services are available
     */
    public boolean isLocationAvailable() {
        return androidLocationManager != null;
    }

    /**
     * Check if required location permissions are granted
     */
    public boolean hasLocationPermissions() {
        return ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION)
                        == PackageManager.PERMISSION_GRANTED;
    }

    /**
     * Get required location permissions array
     */
    @NonNull
    public String[] getRequiredPermissions() {
        return new String[]{
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
        };
    }

    /**
     * Check if location services are enabled
     */
    public boolean isLocationEnabled() {
        if (!isLocationAvailable()) {
            return false;
        }

        try {
            return androidLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER) ||
                    androidLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER);
        } catch (Exception e) {
            Log.e(TAG, "Error checking location enabled status: " + e.getMessage());
            return false;
        }
    }

    /**
     * Get last known location using FusedLocationProvider (preferred method)
     * This is the method your WorkflowViewModel is calling
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    })
    public void getLastLocation(@NonNull LocationCallback callback) {
        this.callback = callback;

        if (!hasLocationPermissions()) {
            callback.onPermissionDenied();
            return;
        }

        if (!isLocationEnabled()) {
            callback.onLocationError("Location services are disabled");
            return;
        }

        try {
            // Try FusedLocationProvider first (more efficient)
            fusedLocationClient.getLastLocation()
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(@Nullable Location location) {
                            if (location != null) {
                                callback.onLocationReceived(location);
                            } else {
                                // Fallback to Android LocationManager
                                getLastKnownLocationFallback(callback);
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            Log.w(TAG, "FusedLocationProvider failed, trying fallback: " + e.getMessage());
                            getLastKnownLocationFallback(callback);
                        }
                    });

        } catch (SecurityException e) {
            callback.onLocationError("Location permission denied: " + e.getMessage());
        } catch (Exception e) {
            callback.onLocationError("Error getting location: " + e.getMessage());
        }
    }

    /**
     * Fallback to Android LocationManager when FusedLocationProvider fails
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    })
    private void getLastKnownLocationFallback(@NonNull LocationCallback callback) {
        if (!isLocationAvailable()) {
            callback.onLocationError("Location services not available");
            return;
        }

        try {
            Location bestLocation = null;

            // Check GPS provider
            if (androidLocationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)) {
                Location gpsLocation = androidLocationManager.getLastKnownLocation(
                        android.location.LocationManager.GPS_PROVIDER);
                if (gpsLocation != null) {
                    bestLocation = gpsLocation;
                }
            }

            // Check Network provider
            if (androidLocationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)) {
                Location networkLocation = androidLocationManager.getLastKnownLocation(
                        android.location.LocationManager.NETWORK_PROVIDER);
                if (networkLocation != null) {
                    if (bestLocation == null ||
                            networkLocation.getTime() > bestLocation.getTime()) {
                        bestLocation = networkLocation;
                    }
                }
            }

            if (bestLocation != null) {
                callback.onLocationReceived(bestLocation);
            } else {
                callback.onLocationError("No cached location available");
            }

        } catch (SecurityException e) {
            callback.onLocationError("Location permission denied: " + e.getMessage());
        } catch (Exception e) {
            callback.onLocationError("Error getting fallback location: " + e.getMessage());
        }
    }

    /**
     * Get current location with fresh reading (more battery intensive)
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    })
    public void getCurrentLocation(@NonNull LocationCallback callback) {
        this.callback = callback;

        if (!hasLocationPermissions()) {
            callback.onPermissionDenied();
            return;
        }

        if (!isLocationEnabled()) {
            callback.onLocationError("Location services are disabled");
            return;
        }

        try {
            // Use FusedLocationProvider's getCurrentLocation for fresh reading
            fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, null)
                    .addOnSuccessListener(new OnSuccessListener<Location>() {
                        @Override
                        public void onSuccess(@Nullable Location location) {
                            if (location != null) {
                                callback.onLocationReceived(location);
                            } else {
                                callback.onLocationError("Unable to get current location");
                            }
                        }
                    })
                    .addOnFailureListener(new OnFailureListener() {
                        @Override
                        public void onFailure(@NonNull Exception e) {
                            callback.onLocationError("Error getting current location: " + e.getMessage());
                        }
                    });

        } catch (SecurityException e) {
            callback.onLocationError("Location permission denied: " + e.getMessage());
        } catch (Exception e) {
            callback.onLocationError("Error requesting current location: " + e.getMessage());
        }
    }

    /**
     * Start location updates (for continuous monitoring)
     */
    @RequiresPermission(anyOf = {
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
    })
    public void startLocationUpdates(@NonNull LocationCallback callback, long intervalMs) {
        this.callback = callback;

        if (!hasLocationPermissions()) {
            callback.onPermissionDenied();
            return;
        }

        if (!isLocationEnabled()) {
            callback.onLocationError("Location services are disabled");
            return;
        }

        try {
            com.google.android.gms.location.LocationRequest locationRequest =
                    com.google.android.gms.location.LocationRequest.create()
                            .setPriority(Priority.PRIORITY_HIGH_ACCURACY)
                            .setInterval(intervalMs)
                            .setFastestInterval(intervalMs / 2);

            com.google.android.gms.location.LocationCallback locationCallback =
                    new com.google.android.gms.location.LocationCallback() {
                        @Override
                        public void onLocationResult(@NonNull com.google.android.gms.location.LocationResult locationResult) {
                            Location location = locationResult.getLastLocation();
                            if (location != null) {
                                callback.onLocationReceived(location);
                            }
                        }
                    };

            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, Looper.getMainLooper());

        } catch (SecurityException e) {
            callback.onLocationError("Location permission denied: " + e.getMessage());
        } catch (Exception e) {
            callback.onLocationError("Error starting location updates: " + e.getMessage());
        }
    }

    /**
     * Stop location updates
     */
    public void stopLocationUpdates() {
        try {
            fusedLocationClient.removeLocationUpdates(
                    new com.google.android.gms.location.LocationCallback() {}
            );
        } catch (Exception e) {
            Log.e(TAG, "Error stopping location updates: " + e.getMessage());
        }
    }

    /**
     * Clean up resources
     */
    public void cleanup() {
        stopLocationUpdates();
        callback = null;
    }

}
