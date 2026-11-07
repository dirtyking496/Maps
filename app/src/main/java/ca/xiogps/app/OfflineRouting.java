package ca.xiogps.app;

import android.content.Context;
import com.valhalla.valhalla.Valhalla;
import com.valhalla.valhalla.config.ValhallaConfigFactory;
import com.valhalla.config.models.ValhallaConfig;
import java.io.File;
import org.json.JSONObject;

/** Local-only automobile routing against an installed Valhalla tile directory. */
public final class OfflineRouting {
    private OfflineRouting() {}

    public static String route(Context context, double fromLat, double fromLon,
                               double toLat, double toLon) throws Exception {
        File tileDir = new File(context.getFilesDir(), "offline/quebec/valhalla");
        if (!containsFiles(tileDir)) {
            throw new IllegalStateException("CARTES ABSENTES: le paquet routier Québec doit être installé dans "
                    + tileDir.getAbsolutePath());
        }

        ValhallaConfig config = ValhallaConfigFactory.INSTANCE.usingTilesDir(tileDir.getAbsolutePath(), null);
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

    private static boolean containsFiles(File directory) {
        if (!directory.isDirectory()) return false;
        File[] children = directory.listFiles();
        if (children == null) return false;
        for (File child : children) {
            if (child.isFile() && child.length() > 0) return true;
            if (child.isDirectory() && containsFiles(child)) return true;
        }
        return false;
    }
}
