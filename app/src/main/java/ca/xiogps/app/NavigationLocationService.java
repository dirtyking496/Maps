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
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Foreground GPS guidance with route-deviation detection and online automobile rerouting. */
public class NavigationLocationService extends Service implements LocationListener {
    public static final String ACTION_START = "ca.xiogps.app.START";
    public static final String ACTION_STOP = "ca.xiogps.app.STOP";
    private static final String CHANNEL = "xio_navigation";
    private static final int NOTIFICATION_ID = 7101;
    private static final long REROUTE_COOLDOWN_MS = 45000L;
    private static final float OFF_ROUTE_METERS = 120f;
    private LocationManager locationManager;
    private TextToSpeech voice;
    private android.content.SharedPreferences prefs;
    private final ExecutorService routing = Executors.newSingleThreadExecutor();
    private volatile boolean rerouteInProgress = false;

    @Override public void onCreate() {
        super.onCreate();
        locationManager = (LocationManager)getSystemService(Context.LOCATION_SERVICE);
        prefs = getSharedPreferences("xio_gps", MODE_PRIVATE);
        voice = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) voice.setLanguage(Locale.CANADA_FRENCH);
        });
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
                .setContentText("GPS actif · guidage et surveillance de l'itinéraire").setOngoing(true)
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
                .putString("last_lon", String.valueOf(location.getLongitude()))
                .putLong("last_time", location.getTime()).apply();
        announceManeuver(location);
        checkForRouteDeviation(location);
        Intent update = new Intent("ca.xiogps.app.LOCATION_UPDATE").setPackage(getPackageName());
        update.putExtra("lat", location.getLatitude()); update.putExtra("lon", location.getLongitude()); sendBroadcast(update);
    }

    private void checkForRouteDeviation(Location current) {
        if (rerouteInProgress || current.getAccuracy() > 50f) return;
        String raw = prefs.getString("route_coordinates", "");
        if (raw.isEmpty()) return;
        try {
            JSONArray points = new JSONArray(raw);
            if (points.length() == 0) return;
            float[] result = new float[1];
            float nearest = Float.MAX_VALUE;
            // OSRM full GeoJSON geometry contains the road-following route shape.
            for (int i = 0; i < points.length(); i++) {
                JSONArray point = points.getJSONArray(i);
                Location.distanceBetween(current.getLatitude(), current.getLongitude(),
                        point.getDouble(1), point.getDouble(0), result);
                if (result[0] < nearest) nearest = result[0];
                if (nearest < 45f) break;
            }
            long now = System.currentTimeMillis();
            long last = prefs.getLong("last_reroute_ms", 0L);
            if (nearest > OFF_ROUTE_METERS && now - last > REROUTE_COOLDOWN_MS) {
                String destLat = prefs.getString("destination_lat", "");
                String destLon = prefs.getString("destination_lon", "");
                if (destLat.isEmpty() || destLon.isEmpty()) return;
                rerouteInProgress = true;
                prefs.edit().putLong("last_reroute_ms", now).apply();
                speak("Vous avez quitté l'itinéraire. Nouveau calcul en cours.");
                routing.execute(() -> recalculateRoute(current, destLat, destLon));
            }
        } catch (Exception ignored) { }
    }

    private void recalculateRoute(Location current, String destLat, String destLon) {
        if (prefs.getBoolean("offline_route_mode", false)) {
            try {
                String raw = OfflineRouting.route(this, current.getLatitude(), current.getLongitude(),
                        Double.parseDouble(destLat), Double.parseDouble(destLon));
                JSONObject parsed = OfflineRouteParser.parse(raw);
                prefs.edit().putString("offline_route_json", raw)
                        .putString("maneuvers", parsed.getJSONArray("maneuvers").toString())
                        .putString("route_coordinates", parsed.getJSONArray("route_coordinates").toString())
                        .putInt("next_maneuver", 0)
                        .putBoolean("announced_500", false).putBoolean("announced_80", false).apply();
                speak("Nouvel itinéraire hors ligne calculé. Reprenez la route.");
            } catch (Exception ex) {
                speak("Impossible de recalculer hors ligne. Vérifiez que la carte du Québec est installée.");
            } finally {
                rerouteInProgress = false;
            }
            return;
        }
        try {
            String address = "https://router.project-osrm.org/route/v1/driving/"
                    + current.getLongitude() + "," + current.getLatitude() + ";"
                    + destLon + "," + destLat + "?overview=full&geometries=geojson&steps=true";
            JSONObject root = new JSONObject(httpGet(address));
            JSONArray routes = root.optJSONArray("routes");
            if (routes == null || routes.length() == 0) throw new Exception("Aucun itinéraire trouvé");
            JSONObject route = routes.getJSONObject(0);
            JSONArray steps = route.getJSONArray("legs").getJSONObject(0).getJSONArray("steps");
            JSONArray saved = new JSONArray();
            for (int i = 0; i < steps.length(); i++) {
                JSONObject step = steps.getJSONObject(i);
                JSONObject maneuver = step.getJSONObject("maneuver");
                JSONArray loc = maneuver.getJSONArray("location");
                JSONObject item = new JSONObject();
                item.put("lat", loc.getDouble(1));
                item.put("lon", loc.getDouble(0));
                item.put("text", describe(maneuver.optString("type"), maneuver.optString("modifier"), step.optString("name")));
                saved.put(item);
            }
            JSONArray geometry = route.getJSONObject("geometry").getJSONArray("coordinates");
            prefs.edit().putString("maneuvers", saved.toString())
                    .putString("route_coordinates", geometry.toString())
                    .putInt("next_maneuver", 0)
                    .putBoolean("announced_500", false).putBoolean("announced_80", false).apply();
            speak("Nouvel itinéraire calculé. Reprenez la route.");
        } catch (Exception ex) {
            speak("Impossible de recalculer l'itinéraire. Vérifiez votre connexion.");
        } finally {
            rerouteInProgress = false;
        }
    }

    private String httpGet(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(address).openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(18000);
        c.setRequestProperty("User-Agent", "XIO-GPS-Android/0.3");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder b = new StringBuilder(); String line;
            while ((line = r.readLine()) != null) b.append(line);
            return b.toString();
        } finally { c.disconnect(); }
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
            }
            if (meters <= 80 && !near) {
                speak("Dans 80 mètres, " + instruction);
                prefs.edit().putBoolean("announced_80", true).apply();
            }
            if (meters <= 30) {
                prefs.edit().putInt("next_maneuver", index + 1)
                        .putBoolean("announced_500", false).putBoolean("announced_80", false).apply();
            }
        } catch (Exception ignored) { }
    }

    private String describe(String type, String modifier, String road) {
        String action;
        switch (modifier) {
            case "left": action = "tournez à gauche"; break;
            case "slight left": action = "prenez légèrement à gauche"; break;
            case "sharp left": action = "tournez fortement à gauche"; break;
            case "right": action = "tournez à droite"; break;
            case "slight right": action = "prenez légèrement à droite"; break;
            case "sharp right": action = "tournez fortement à droite"; break;
            case "uturn": action = "faites demi-tour"; break;
            case "straight": action = "continuez tout droit"; break;
            default: action = type.equals("roundabout") ? "entrez dans le rond-point" : "continuez";
        }
        if (road == null || road.trim().isEmpty()) return action;
        return action + " sur " + road;
    }

    private void speak(String message) {
        if (voice != null) voice.speak(message, TextToSpeech.QUEUE_ADD, null, "xio-step-" + System.currentTimeMillis());
    }
    private void stopUpdates() { if (locationManager != null) locationManager.removeUpdates(this); }
    @Override public void onDestroy() {
        stopUpdates(); routing.shutdownNow();
        if (voice != null) { voice.stop(); voice.shutdown(); }
        super.onDestroy();
    }
    @Override public IBinder onBind(Intent intent) { return null; }
    @Override public void onStatusChanged(String p, int s, Bundle b) {}
    @Override public void onProviderEnabled(@NonNull String p) {}
    @Override public void onProviderDisabled(@NonNull String p) {}
}
