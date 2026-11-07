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
import android.speech.tts.TextToSpeech;
import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.Locale;

/** Foreground GPS tracking and French spoken maneuver announcements for a loaded online route. */
public class NavigationLocationService extends Service implements LocationListener {
    public static final String ACTION_START = "ca.xiogps.app.START";
    public static final String ACTION_STOP = "ca.xiogps.app.STOP";
    private static final String CHANNEL = "xio_navigation";
    private static final int NOTIFICATION_ID = 7101;
    private LocationManager locationManager;
    private TextToSpeech voice;
    private android.content.SharedPreferences prefs;

    @Override public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        prefs = getSharedPreferences("xio_gps", MODE_PRIVATE);
        voice = new TextToSpeech(this, result -> { if (result == TextToSpeech.SUCCESS) voice.setLanguage(Locale.CANADA_FRENCH); });
        if (Build.VERSION.SDK_INT >= 26) {
            NotificationChannel c = new NotificationChannel(CHANNEL, "Navigation XIO GPS", NotificationManager.IMPORTANCE_LOW);
            c.setDescription("Suivi GPS et guidage vocal pendant un trajet");
            getSystemService(NotificationManager.class).createNotificationChannel(c);
        }
    }
    @Override public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && ACTION_STOP.equals(intent.getAction())) {
            stopUpdates(); stopForeground(STOP_FOREGROUND_REMOVE); stopSelf(); return START_NOT_STICKY;
        }
        startForeground(NOTIFICATION_ID, new NotificationCompat.Builder(this, CHANNEL)
                .setSmallIcon(android.R.drawable.ic_menu_mylocation).setContentTitle("XIO GPS")
                .setContentText("GPS actif · guidage du trajet").setOngoing(true)
                .setCategory(Notification.CATEGORY_NAVIGATION).build());
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
                && ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            stopSelf(); return START_NOT_STICKY;
        }
        try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1200L, 2f, this, Looper.getMainLooper()); }
        catch (SecurityException ex) { stopSelf(); }
        return START_STICKY;
    }
    @Override public void onLocationChanged(@NonNull Location location) {
        prefs.edit().putString("last_lat", String.valueOf(location.getLatitude()))
                .putString("last_lon", String.valueOf(location.getLongitude())).putLong("last_time", location.getTime()).apply();
        announceManeuver(location);
        Intent update = new Intent("ca.xiogps.app.LOCATION_UPDATE").setPackage(getPackageName());
        update.putExtra("lat", location.getLatitude()); update.putExtra("lon", location.getLongitude()); sendBroadcast(update);
    }
    private void announceManeuver(Location current) {
        try {
            JSONArray steps = new JSONArray(prefs.getString("maneuvers", "[]"));
            int index = prefs.getInt("next_maneuver", 0);
            if (index >= steps.length()) return;
            JSONObject step = steps.getJSONObject(index);
            Location target = new Location("route-step");
            target.setLatitude(step.getDouble("lat")); target.setLongitude(step.getDouble("lon"));
            float meters = current.distanceTo(target);
            String instruction = step.optString("text", "Continuez votre route");
            boolean far = prefs.getBoolean("announced_500", false);
            boolean near = prefs.getBoolean("announced_80", false);
            if (meters <= 500 && !far) {
                speak("Dans 500 mètres, " + instruction);
                prefs.edit().putBoolean("announced_500", true).apply();
            } else if (meters <= 80 && !near) {
                speak("Dans 80 mètres, " + instruction);
                prefs.edit().putBoolean("announced_80", true).apply();
            }
            if (meters <= 30) {
                prefs.edit().putInt("next_maneuver", index + 1)
                        .putBoolean("announced_500", false).putBoolean("announced_80", false).apply();
            }
        } catch (Exception ignored) { }
    }
    private void speak(String message) {
        if (voice != null) voice.speak(message, TextToSpeech.QUEUE_ADD, null, "xio-step-" + System.currentTimeMillis());
    }
    private void stopUpdates() { if (locationManager != null) locationManager.removeUpdates(this); }
    @Override public void onDestroy() { stopUpdates(); if (voice != null) { voice.stop(); voice.shutdown(); } super.onDestroy(); }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onStatusChanged(String p, int s, Bundle b) {}
    @Override public void onProviderEnabled(@NonNull String p) {}
    @Override public void onProviderDisabled(@NonNull String p) {}
}
