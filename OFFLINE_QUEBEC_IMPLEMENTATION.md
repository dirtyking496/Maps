# XIO GPS — Quebec offline navigation implementation

## Target
Deliver a standalone Android car-navigation app that continues to display the map, calculate routes, issue French voice instructions, and reroute after a missed turn with mobile data and Wi-Fi disabled.

## Data source and size
- Initial region: Quebec province only.
- Source extract: Geofabrik Quebec OpenStreetMap PBF: https://download.geofabrik.de/north-america/canada/quebec-latest.osm.pbf
- The source extract is approximately 1.1 GB compressed. This is raw OSM source data, NOT an Android-ready map and NOT a routing graph. Do not offer it as a direct in-app install.
- Display-map data and routing data must be generated in a supported offline format and installed as a versioned region package. Show required free storage before download and verify checksum before activation.
- OSM attribution and ODbL notices must be visible in the app.

## Engine decision
The current app uses Nominatim and the public OSRM demo server. Both require Internet. The current route geometry and deviation detector do not provide offline routing.

GraphHopper's current upstream documentation says offline mobile routing is no longer officially supported. BRouter is an Android offline router but its documented Android integration is a separate app/service and its focus is cycling; it is not a suitable drop-in embedded car-navigation engine for this standalone app.

The preferred investigation is OsmAnd's offline map/routing core and its native routing engine. Before embedding, verify:
1. Exact reusable Android artifacts/API and supported version.
2. GPLv3 and any native-core/data licensing obligations for XIO GPS distribution.
3. Car profile support, turn restrictions, one-way roads, roundabouts, toll/avoidance settings.
4. Offline address/POI search coverage and the map package format.
5. APK/AAB size, ABI support, memory use, and route calculation performance on the target Android device.

Do not claim offline routing is implemented until the engine is embedded and route tests pass in airplane mode.

## Implementation milestones
1. **Offline map foundation**: add a real map view; render a locally installed Quebec map package; show download progress, free-space estimate, checksum, installed version, delete/update controls, and an offline-ready state.
2. **Local car router**: integrate the selected routing core and Quebec routing data. All route calculations must execute on-device with network disabled.
3. **Local search**: support destination search from downloaded address/POI indexes and coordinates. Online geocoding may remain an optional fallback only.
4. **Navigation session**: feed local route steps into the existing foreground GPS service; keep French TTS instructions, next-turn distance, remaining distance/time, and destination state.
5. **Offline rerouting**: detect route departure using route matching/corridor distance, throttle recalculation, route from current GPS fix to stored destination locally, replace remaining route, and announce the new route.
6. **Verification**: airplane-mode tests for map display, address search, route calculation, missed turn/recalculation, voice guidance, screen lock, app restart, GPS loss/recovery, and low-storage failure.

## Current repository status
- Android GPS foreground service and an initial online OSRM/Nominatim route flow exist.
- The current MainActivity has no actual map renderer; its offline button only reports that offline routing is unavailable.
- The current route deviation recalculation calls the public OSRM server and therefore fails without Internet.
- This document is a technical implementation plan only. It does not represent completed offline functionality.
