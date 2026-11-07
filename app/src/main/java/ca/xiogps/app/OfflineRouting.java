package ca.xiogps.app;

import android.content.Context;
import com.valhalla.valhalla.Valhalla;
import com.valhalla.valhalla.config.ValhallaConfigFactory;
import com.valhalla.config.models.ValhallaConfig;
import com.squareup.moshi.Moshi;
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory;
import java.io.File;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import org.json.JSONObject;

/** Local-only automobile routing against an installed Valhalla tile extract. */
public final class OfflineRouting {
    private OfflineRouting() {}

    public static String route(Context context, double fromLat, double fromLon,
                               double toLat, double toLon) throws Exception {
        File packDir = new File(context.getFilesDir(), "offline/quebec");
        File tileExtract = new File(packDir, "valhalla_tiles.tar");
        File adminDb = new File(packDir, "admin.sqlite");
        File timezoneDb = new File(packDir, "timezones.sqlite");
        if (!tileExtract.isFile() || tileExtract.length() < 1024 * 1024
                || !adminDb.isFile() || adminDb.length() == 0
                || !timezoneDb.isFile() || timezoneDb.length() == 0) {
            throw new IllegalStateException("PAQUET QUÉBEC ABSENT OU INCOMPLET : importe le ZIP de cartes XIO GPS.");
        }

        String configJson;
        try (InputStream input = ValhallaConfigFactory.class.getResourceAsStream(
                "/com/valhalla/valhalla/default.json")) {
            if (input == null) throw new IllegalStateException("Configuration Valhalla introuvable.");
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            byte[] buffer = new byte[8192];
            int count;
            while ((count = input.read(buffer)) != -1) bytes.write(buffer, 0, count);
            configJson = new String(bytes.toByteArray(), StandardCharsets.UTF_8);
        }
        String quote = String.valueOf('"');
        configJson = configJson
                .replace(quote + "/data/valhalla/tiles.tar" + quote,
                        org.json.JSONObject.quote(tileExtract.getAbsolutePath()))
                .replace(quote + "/data/valhalla/admin.sqlite" + quote,
                        org.json.JSONObject.quote(adminDb.getAbsolutePath()))
                .replace(quote + "/data/valhalla/tz_world.sqlite" + quote,
                        org.json.JSONObject.quote(timezoneDb.getAbsolutePath()));
        ValhallaConfig config = ValhallaConfigFactory.INSTANCE.fromJson(configJson,
                new Moshi.Builder().add(new KotlinJsonAdapterFactory()).build());
        try (Valhalla engine = new Valhalla(context, config)) {
            JSONObject request = new JSONObject();
            org.json.JSONArray locations = new org.json.JSONArray();
            locations.put(new JSONObject().put("lat", fromLat).put("lon", fromLon));
            locations.put(new JSONObject().put("lat", toLat).put("lon", toLon));
            request.put("locations", locations);
            request.put("costing", "auto");
            request.put("units", "kilometers");
            request.put("directions_options", new JSONObject()
                    .put("units", "kilometers").put("language", "fr-FR"));
            // routeRaw invokes the native engine and reads only the local tile directory.
            return engine.routeRaw(request.toString());
        }
    }

}
