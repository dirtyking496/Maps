package ca.xiogps.app;

import org.json.JSONArray;
import org.json.JSONObject;

/** Converts a Valhalla route response into the navigation data format used by XIO GPS. */
public final class OfflineRouteParser {
    private OfflineRouteParser() {}

    public static JSONObject parse(String raw) throws Exception {
        JSONObject root = new JSONObject(raw);
        JSONObject trip = root.getJSONObject("trip");
        JSONArray legs = trip.getJSONArray("legs");
        if (legs.length() == 0) throw new Exception("Aucun segment dans l'itinéraire.");
        JSONObject leg = legs.getJSONObject(0);
        JSONArray coordinates = decodePolyline6(leg.getString("shape"));
        JSONArray maneuvers = leg.getJSONArray("maneuvers");
        JSONArray saved = new JSONArray();
        for (int i = 0; i < maneuvers.length(); i++) {
            JSONObject maneuver = maneuvers.getJSONObject(i);
            int index = maneuver.optInt("begin_shape_index", -1);
            if (index < 0 || index >= coordinates.length()) continue;
            JSONArray point = coordinates.getJSONArray(index);
            JSONObject item = new JSONObject();
            item.put("lat", point.getDouble(1));
            item.put("lon", point.getDouble(0));
            item.put("text", maneuver.optString("instruction", "Continuez votre route"));
            saved.put(item);
        }
        JSONObject result = new JSONObject();
        result.put("maneuvers", saved);
        result.put("route_coordinates", coordinates);
        result.put("summary", trip.getJSONObject("summary"));
        return result;
    }

    // Valhalla route shapes use a six-decimal encoded polyline.
    private static JSONArray decodePolyline6(String encoded) throws Exception {
        JSONArray points = new JSONArray();
        int index = 0, lat = 0, lon = 0;
        while (index < encoded.length()) {
            int[] a = readValue(encoded, index); lat += a[0]; index = a[1];
            int[] b = readValue(encoded, index); lon += b[0]; index = b[1];
            points.put(new JSONArray().put(lon / 1000000.0).put(lat / 1000000.0));
        }
        if (points.length() == 0) throw new Exception("Géométrie d'itinéraire hors ligne vide.");
        return points;
    }

    private static int[] readValue(String value, int start) throws Exception {
        int result = 0, shift = 0, index = start, chunk;
        do {
            if (index >= value.length() || shift > 30) throw new Exception("Géométrie Valhalla invalide.");
            chunk = value.charAt(index++) - 63;
            if (chunk < 0 || chunk > 63) throw new Exception("Géométrie Valhalla invalide.");
            result |= (chunk & 0x1f) << shift;
            shift += 5;
        } while (chunk >= 0x20);
        int delta = (result & 1) != 0 ? ~(result >> 1) : (result >> 1);
        return new int[]{delta, index};
    }
}
