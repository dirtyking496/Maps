package ca.xiogps.app;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
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
import java.util.Locale;

/** First native XIO GPS screen. Online route engine and offline engine are separate milestones. */
public class MainActivity extends AppCompatActivity implements LocationListener {
    private static final int GPS_PERMISSION = 410;
    private LocationManager locationManager;
    private TextView status;
    private TextToSpeech voice;
    private Location lastLocation;
    private boolean background = false;

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        locationManager = (LocationManager)getSystemService(LOCATION_SERVICE);
        voice = new TextToSpeech(this, result -> {
            if (result == TextToSpeech.SUCCESS) voice.setLanguage(Locale.CANADA_FRENCH);
        });
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(24, 32, 24, 24);
        root.setBackgroundColor(0xFF050B14);
        TextView title = new TextView(this);
        title.setText("XIO GPS  ·  GLOW NÉON");
        title.setTextColor(0xFF39D5FF);
        title.setTextSize(25);
        title.setGravity(Gravity.CENTER);
        root.addView(title);
        EditText destination = new EditText(this);
        destination.setSingleLine(true);
        destination.setHint("Destination");
        destination.setTextColor(0xFFFFFFFF);
        destination.setHintTextColor(0xFF9AAFC2);
        root.addView(destination);
        Button gps = button("ACTIVER LE GPS");
        gps.setOnClickListener(v -> requestGps());
        root.addView(gps);
        Button voiceTest = button("TESTER LA VOIX FRANÇAISE");
        voiceTest.setOnClickListener(v -> speak("XIO GPS est prêt. Bonne route."));
        root.addView(voiceTest);
        Button bg = button("DÉMARRER LE GPS EN ARRIÈRE-PLAN");
        bg.setOnClickListener(v -> {
            if (!background) {
                if (!hasLocationPermission()) { requestGps(); return; }
                Intent i = new Intent(this, NavigationLocationService.class).setAction(NavigationLocationService.ACTION_START);
                if (android.os.Build.VERSION.SDK_INT >= 26) startForegroundService(i); else startService(i);
                background = true;
                bg.setText("ARRÊTER LE GPS EN ARRIÈRE-PLAN");
                status.setText("GPS arrière-plan démarré · notification persistante");
            } else {
                startService(new Intent(this, NavigationLocationService.class).setAction(NavigationLocationService.ACTION_STOP));
                background = false;
                bg.setText("DÉMARRER LE GPS EN ARRIÈRE-PLAN");
                status.setText("GPS arrière-plan arrêté");
            }
        });
        root.addView(bg);
        Button offline = button("NAVIGATION HORS LIGNE");
        offline.setOnClickListener(v -> status.setText("Moteur automobile hors ligne à intégrer : aucun itinéraire hors ligne n'est encore disponible."));
        root.addView(offline);
        status = new TextView(this);
        status.setTextColor(0xFFFFFFFF);
        status.setText("État : prêt · fonctions en développement");
        status.setPadding(0, 24, 0, 0);
        root.addView(status);
        setContentView(root);
    }

    private Button button(String label) {
        Button b = new Button(this); b.setText(label); b.setTextColor(0xFFFFFFFF);
        b.setBackgroundTintList(android.content.res.ColorStateList.valueOf(0xFF102B41));
        return b;
    }
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
            || ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
    }
    private void requestGps() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION}, GPS_PERMISSION);
            return;
        }
        try { locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000L, 2f, this); status.setText("Recherche du signal GPS…"); }
        catch (SecurityException ex) { status.setText("Permission GPS manquante."); }
    }
    @Override public void onLocationChanged(@NonNull Location location) {
        lastLocation = location;
        status.setText(String.format(Locale.CANADA_FRENCH, "GPS : %.5f, %.5f · précision %.0f m", location.getLatitude(), location.getLongitude(), location.getAccuracy()));
    }
    private void speak(String message) {
        if (voice != null) voice.speak(message, TextToSpeech.QUEUE_FLUSH, null, "xio-guidance");
    }
    @Override public void onRequestPermissionsResult(int request, @NonNull String[] permissions, @NonNull int[] results) {
        super.onRequestPermissionsResult(request, permissions, results);
        if (request == GPS_PERMISSION && results.length > 0 && results[0] == PackageManager.PERMISSION_GRANTED) requestGps();
    }
    @Override protected void onDestroy() {
        if (locationManager != null) locationManager.removeUpdates(this);
        if (voice != null) { voice.stop(); voice.shutdown(); }
        super.onDestroy();
    }
    @Override public void onStatusChanged(String p, int s, Bundle b) {}
    @Override public void onProviderEnabled(@NonNull String p) {}
    @Override public void onProviderDisabled(@NonNull String p) {}
}
