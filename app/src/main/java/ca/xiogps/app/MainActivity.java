package ca.xiogps.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.view.Gravity;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URLEncoder;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** XIO GPS: live GPS, online route-step preparation, French speech, foreground GPS service.
 * Offline automotive routing is intentionally marked unavailable until a local road graph is integrated.
 */
public class MainActivity extends AppCompatActivity implements LocationListener {
    private static final int GPS_PERMISSION = 410;
    private LocationManager locationManager;
    private TextView status;
    private EditText destination;
    private Location lastLocation;
    private TextToSpeech voice;
    private boolean background = false;
    private final ExecutorService network = Executors.newSingleThreadExecutor();

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        voice = new TextToSpeech(this, result -> { if (result == TextToSpeech.SUCCESS) voice.setLanguage(Locale.CANADA_FRENCH); });
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 32, 24, 24);
        root.setBackgroundColor(0xFF050B14);

        TextView title = new TextView(this);
        title.setText("XIO GPS  ·  GLOW NÉON");
        title.setTextColor(0xFF39D5FF); title.setTextSize(25); title.setGravity(Gravity.CENTER);
        root.addView(title);

        destination = new EditText(this);
        destination.setSingleLine(true); destination.setHint("Adresse en ligne ou coordonnées lat,lon hors ligne");
        destination.setTextColor(0xFFFFFFFF); destination.setHintTextColor(0xFF9AAFC2);
        root.addView(destination);

        Button gps = button("ACTIVER LE GPS");
        gps.setOnClickListener(v -> requestGps()); root.addView(gps);

        Button route = button("CALCULER L'ITINÉRAIRE ET ACTIVER LES ÉTAPES VOCALES");
        route.setOnClickListener(v -> calculateOnlineRoute()); root.addView(route);

        Button bg = button("DÉMARRER LE GPS EN ARRIÈRE-PLAN");
        bg.setOnClickListener(v -> {
            if (!background) {
                if (!hasLocationPermission()) { requestGps(); return; }
                Intent i = new Intent(this, NavigationLocationService.class).setAction(NavigationLocationService.ACTION_START);
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
                background = true; bg.setText("ARRÊTER LE GPS EN ARRIÈRE-PLAN");
                status.setText("GPS arrière-plan actif · annonces vocales si un itinéraire est chargé");
            } else {
                startService(new Intent(this, NavigationLocationService.class).setAction(NavigationLocationService.ACTION_STOP));
                background = false; bg.setText("DÉMARRER LE GPS EN ARRIÈRE-PLAN");
                status.setText("GPS arrière-plan arrêté");
            }
        });
        root.addView(bg);

        Button importMap = button("INSTALLER LA CARTE ROUTIÈRE DU QUÉBEC (.TAR)");
        importMap.setOnClickListener(v -> {
            Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            picker.addCategory(Intent.CATEGORY_OPENABLE);
            picker.setType("*/*");
            picker.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"application/x-tar", "application/octet-stream"});
            startActivityForResult(picker, 412);
        });
        root.addView(importMap);

        Button offline = button("NAVIGATION HORS LIGNE");
        offline.setOnClickListener(v -> calculateOfflineRoute());
        root.addView(offline);

        status = new TextView(this);
        status.setTextColor(0xFFFFFFFF); status.setText("État : prêt · active le GPS pour commencer");
        status.setPadding(0, 24, 0, 0); root.addView(status);
        setContentView(root);
    }

    @Override protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode != 412 || resultCode != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        status.setText("Installation de la carte Valhalla… copie locale en cours.");
        network.execute(() -> {
            File directory = new File(getFilesDir(), "offline/quebec");
            if (!directory.exists() && !directory.mkdirs()) {
                runOnUiThread(() -> status.setText("Impossible de créer le dossier des cartes."));
                return;
            }
            File temporary = new File(directory, "valhalla_tiles.tar.part");
            File installed = new File(directory, "valhalla_tiles.tar");
            try (InputStream input = getContentResolver().openInputStream(uri);
                 FileOutputStream output = new FileOutputStream(temporary)) {
                if (input == null) throw new Exception("Fichier inaccessible.");
                byte[] buffer = new byte[1024 * 1024];
                int count;
                while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
                output.getFD().sync();
                if (temporary.length() < 1024 * 1024) throw new Exception("Fichier trop petit : ce n'est probablement pas un paquet de tuiles Valhalla.");
                if (installed.exists() && !installed.delete()) throw new Exception("Impossible de remplacer l'ancienne carte.");
                if (!temporary.renameTo(installed)) throw new Exception("Impossible de finaliser l'installation de la carte.");
                runOnUiThread(() -> status.setText(String.format(Locale.CANADA_FRENCH,
                        "CARTE ROUTIÈRE IMPORTÉE · %.0f Mo · prête pour le calcul hors ligne",
                        installed.length() / 1048576.0)));
            } catch (Exception ex) {
                temporary.delete();
                runOnUiThread(() -> status.setText("Échec d'installation : " + ex.getMessage()));
            }
        });
    }

    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(0xFFFFFFFF);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF102B41)); return b;
    }
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestGps() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, GPS_PERMISSION); return;
        }
        try {
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 2f, this);
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) onLocationChanged(last);
            status.setText("Recherche du signal GPS…");
        } catch (SecurityException ex) { status.setText("Permission GPS manquante."); }
    }

    /** Calculates a route only with the Valhalla tiles already installed on this device. */
    private void calculateOfflineRoute() {
        if (lastLocation == null) {
            status.setText("Active le GPS et attends ta position avant le calcul hors ligne.");
            requestGps();
            return;
        }
        String value = destination.getText().toString().trim();
        String[] parts = value.split(",");
        if (parts.length != 2) {
            status.setText("HORS LIGNE : entre les coordonnées de destination au format latitude,longitude.");
            return;
        }
        final double lat, lon;
        try {
            lat = Double.parseDouble(parts[0].trim());
            lon = Double.parseDouble(parts[1].trim());
            if (lat < -90 || lat > 90 || lon < -180 || lon > 180) throw new NumberFormatException();
        } catch (NumberFormatException ex) {
            status.setText("Coordonnées invalides. Exemple : 48.45,-69.50 (latitude,longitude).");
            return;
        }
        final double fromLat = lastLocation.getLatitude();
        final double fromLon = lastLocation.getLongitude();
        status.setText("Calcul local en cours · aucune connexion Internet utilisée…");
        network.execute(() -> {
            try {
                String raw = OfflineRouting.route(this, fromLat, fromLon, lat, lon);
                JSONObject parsed = OfflineRouteParser.parse(raw);
                JSONObject summary = parsed.getJSONObject("summary");
                double km = summary.optDouble("length", 0);
                int minutes = (int)Math.round(summary.optDouble("time", 0) / 60.0);
                getSharedPreferences("xio_gps", MODE_PRIVATE).edit()
                        .putString("offline_route_json", raw)
                        .putString("maneuvers", parsed.getJSONArray("maneuvers").toString())
                        .putString("route_coordinates", parsed.getJSONArray("route_coordinates").toString())
                        .putString("destination_lat", String.valueOf(lat))
                        .putString("destination_lon", String.valueOf(lon))
                        .putInt("next_maneuver", 0)
                        .putLong("last_reroute_ms", 0L)
                        .putBoolean("announced_500", false).putBoolean("announced_80", false)
                        .putBoolean("offline_route_mode", true)
                        .putBoolean("offline_route_ready", true).apply();
                runOnUiThread(() -> {
                    status.setText(String.format(Locale.CANADA_FRENCH,
                            "ITINÉRAIRE HORS LIGNE CALCULÉ · %.1f km · env. %d min · moteur local Valhalla",
                            km, minutes));
                    speak("Itinéraire hors ligne calculé. " + Math.round(km) + " kilomètres.");
                });
            } catch (Exception ex) {
                runOnUiThread(() -> status.setText("HORS LIGNE : " + ex.getMessage()));
            }
        });
    }

    private void calculateOnlineRoute() {
        if (lastLocation == null) { status.setText("Active d'abord le GPS et attends ta position."); requestGps(); return; }
        String query = destination.getText().toString().trim();
        if (query.isEmpty()) { status.setText("Entre une destination."); return; }
        status.setText("Recherche de la destination et calcul des étapes…");
        network.execute(() -> {
            try {
                String q = URLEncoder.encode(query, "UTF-8");
                JSONObject place = new JSONArray(httpGet("https://nominatim.openstreetmap.org/search?format=jsonv2&limit=1&q=" + q)).getJSONObject(0);
                double destLat = place.getDouble("lat"), destLon = place.getDouble("lon");
                String routeUrl = "https://router.project-osrm.org/route/v1/driving/" +
                        lastLocation.getLongitude() + "," + lastLocation.getLatitude() + ";" + destLon + "," + destLat +
                        "?overview=full&geometries=geojson&steps=true";
                JSONObject routeRoot = new JSONObject(httpGet(routeUrl));
                JSONObject route = routeRoot.getJSONArray("routes").getJSONObject(0);
                JSONArray legs = route.getJSONArray("legs");
                JSONArray steps = legs.getJSONObject(0).getJSONArray("steps");
                JSONArray saved = new JSONArray();
                for (int i = 0; i < steps.length(); i++) {
                    JSONObject step = steps.getJSONObject(i);
                    JSONObject maneuver = step.getJSONObject("maneuver");
                    JSONArray loc = maneuver.getJSONArray("location");
                    String instruction = describe(maneuver.optString("type"), maneuver.optString("modifier"), step.optString("name"));
                    JSONObject item = new JSONObject();
                    item.put("lat", loc.getDouble(1)); item.put("lon", loc.getDouble(0)); item.put("text", instruction);
                    saved.put(item);
                }
                JSONArray routeCoordinates = route.getJSONObject("geometry").getJSONArray("coordinates");
                getSharedPreferences("xio_gps", MODE_PRIVATE).edit()
                        .putString("maneuvers", saved.toString())
                        .putString("route_coordinates", routeCoordinates.toString())
                        .putString("destination_lat", String.valueOf(destLat))
                        .putString("destination_lon", String.valueOf(destLon))
                        .putInt("next_maneuver", 0)
                        .putLong("last_reroute_ms", 0L)
                        .putBoolean("announced_500", false).putBoolean("announced_80", false)
                        .putBoolean("offline_route_mode", false).apply();
                double km = route.optDouble("distance") / 1000.0;
                int minutes = (int)Math.round(route.optDouble("duration") / 60.0);
                runOnUiThread(() -> {
                    status.setText(String.format(Locale.CANADA_FRENCH,
                            "Itinéraire en ligne prêt · %.1f km · env. %d min · %d étapes vocales", km, minutes, saved.length()));
                    speak("Itinéraire calculé. " + Math.round(km) + " kilomètres. Les instructions vocales sont prêtes.");
                    if (!background) {
                        Intent i = new Intent(this, NavigationLocationService.class).setAction(NavigationLocationService.ACTION_START);
                        if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
                        background = true;
                    }
                });
            } catch (Exception ex) {
                runOnUiThread(() -> status.setText("Erreur de calcul en ligne : " + ex.getMessage()));
            }
        });
    }

    private String httpGet(String address) throws Exception {
        HttpURLConnection c = (HttpURLConnection)new URL(address).openConnection();
        c.setConnectTimeout(12000); c.setReadTimeout(18000);
        c.setRequestProperty("User-Agent", "XIO-GPS-Android/0.2");
        try (BufferedReader r = new BufferedReader(new InputStreamReader(c.getInputStream(), StandardCharsets.UTF_8))) {
            StringBuilder b = new StringBuilder(); String line; while ((line = r.readLine()) != null) b.append(line); return b.toString();
        } finally { c.disconnect(); }
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
        if (voice != null) voice.speak(message, TextToSpeech.QUEUE_FLUSH, null, "xio-intro");
    }
    @Override public void onLocationChanged(@NonNull Location location) { lastLocation = location; status.setText(String.format(Locale.CANADA_FRENCH, "GPS : %.5f, %.5f · précision %.0f m", location.getLatitude(), location.getLongitude(), location.getAccuracy())); }
    @Override public void onRequestPermissionsResult(int request, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == GPS_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) requestGps();
    }
    @Override protected void onDestroy() { network.shutdownNow(); if (locationManager != null) locationManager.removeUpdates(this); if (voice != null) { voice.stop(); voice.shutdown(); } super.onDestroy(); }
    @Override public void onStatusChanged(String p, int s, Bundle b) {}
    @Override public void onProviderEnabled(@NonNull String p) {}
    @Override public void onProviderDisabled(@NonNull String p) {}
}
