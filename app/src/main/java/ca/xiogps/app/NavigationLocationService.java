package ca.xiogps.app;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.os.Looper;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

/** Explicitly started foreground GPS service for an active navigation session. */
public class NavigationLocationService extends Service implements LocationListener {
    public static final String ACTION_START = "ca.xiogps.app.START";
    public static final String ACTION_STOP = "ca.xiogps.app.STOP";
    private static final String CHANNEL = "xio_navigation";
    private static final int NOTIFICATION_ID = 7101;
    private LocationManager locationManager;

    @Override public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager) getSystemService(Context.LOCATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Navigation XIO GPS", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Suivi GPS pendant un trajet actif");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }

    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopUpdates();
            stopForeground(STOP_FOREGROUND_REMOVE);
            stopSelf();
            return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation)
                .setContentTitle("XIO GPS")
                .setContentText("GPS actif · suivi du trajet")
                .setOngoing(true).setCategory(Notification.CATEGORY_NAVIGATION).build());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf();
            return START_NOT_STICKY;
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1500L, 2f, this, Looper.getMainLooper());
        } catch (SecurityException ex) { stopSelf(); }
        return START_STICKY;
    }

    @Override public void onLocationChanged(@NonNull Location location) {
        getSharedPreferences("xio_gps", MODE_PRIVATE).edit()
                .putString("lat", String.valueOf(location.getLatitude()))
                .putString("lon", String.valueOf(location.getLongitude()))
                .putLong("time", location.getTime()).apply();
        Intent update = new Intent("ca.xiogps.app.LOCATION_UPDATE").setPackage(getPackageName());
        update.putExtra("lat", location.getLatitude());
        update.putExtra("lon", location.getLongitude());
        sendBroadcast(update);
    }
    private void stopUpdates() { if (locationManager != null) locationManager.removeUpdates(this); }
    @Override public void onDestroy() { stopUpdates(); super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onStatusChanged(String p, int s, Bundle b) {}
    @Override public void onProviderEnabled(@NonNull String p) {}
    @Override public void onProviderDisabled(@NonNull String p) {}
}
