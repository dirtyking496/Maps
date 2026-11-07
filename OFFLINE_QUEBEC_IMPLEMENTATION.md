# XIO GPS — Quebec offline navigation implementation

## Product decision
XIO GPS remains a proprietary application. Do not adopt or embed GPLv3 OsmAnd routing components unless the owner explicitly changes this licensing decision after legal review.

## Selected technical stack
- **Map display:** MapLibre Native Android (BSD-2-Clause), using a locally stored vector-tile region package and a local style/sprite/glyph bundle. No remote style or tile URL may be required in airplane mode.
- **Automotive routing:** Valhalla (MIT), packaged as a native Android library through the Android NDK/JNI. Valhalla uses tiled hierarchical routing data, supports offline routing and configurable automobile costing. Its upstream project describes cross-compilation for portable devices and low-memory tiled data.
- **Map/routing data:** OpenStreetMap Quebec extract, transformed in a controlled build pipeline into (a) Valhalla routing tiles and (b) MapLibre-compatible vector tiles. These are separate data products generated from the same source snapshot; a raw .osm.pbf is not directly usable as either installed package.
- **Destination lookup:** include an offline address/POI index in the region package. Online Nominatim may remain an optional fallback, never a requirement for offline mode.

Why this stack:
- Valhalla is MIT licensed, so it can be incorporated into a proprietary app while preserving required copyright/license notices.
- MapLibre Native Android is BSD-2-Clause, which permits proprietary distribution subject to its notice conditions.
- OpenStreetMap data has its own ODbL attribution/share-alike requirements. Software licensing and map-data licensing are separate; legal review is still needed for the generated distributed region database and any modifications.

Upstream references:
- Valhalla: https://github.com/valhalla/valhalla and https://valhalla.github.io/valhalla/
- MapLibre Native Android: https://github.com/maplibre/maplibre-native
- MapLibre Android SDK guide: https://maplibre.org/maplibre-native/android/
- Quebec OSM extract: https://download.geofabrik.de/north-america/canada/quebec.html

## Data source and packaging
- Initial region: all of Quebec.
- Source extract: Geofabrik Quebec OpenStreetMap PBF. Its size varies by update; measure the current extract in the build pipeline rather than hard-coding an estimate.
- The raw PBF is source material, NOT an Android-ready map and NOT a routing graph. Never offer it as a direct in-app install.
- Build a versioned Quebec region bundle containing Valhalla tiles, MapLibre vector tiles, local style assets, and an address/POI index. Include metadata: source timestamp, bundle version, geographic coverage, byte size, checksum, and OSM attribution/license notices.
- The app must show required free storage before download, support resumable download, verify checksum, unpack safely, and activate atomically only after all components validate. Keep the previous working package until activation succeeds.
- OSM attribution and applicable ODbL notices must be visible in the app and distributed package.

## Integration constraints
- Valhalla is a C++ engine, not a drop-in Android Java dependency. Build and package compatible native binaries for supported Android ABIs, expose only the needed routing functions via JNI, and verify ABI/runtime dependencies. Do not claim the engine is embedded until an Android build and on-device test succeed.
- MapLibre's offline renderer requires a compatible local vector-tile source and style assets. Merely adding the SDK dependency or showing a blank MapView is not an offline map.
- Do not use public OSRM/Nominatim services for any operation required by offline mode.
- The existing app currently uses Nominatim and the public OSRM demo server. The current route geometry/deviation detector does not provide offline routing.
- GraphHopper upstream says offline mobile routing is no longer officially supported; OsmAnd routing components are not selected because this project is keeping its proprietary licensing model.

## Implementation milestones
1. **Native build proof:** create an isolated Android NDK/JNI build of Valhalla for the target ABI(s); invoke a local route calculation against a small test tileset in an instrumentation/native test.
2. **Map proof:** add MapLibre Android and render a small locally packaged vector-tile test region with no network connection.
3. **Quebec data pipeline:** automate conversion of one consistent Quebec OSM snapshot into Valhalla routing tiles and MapLibre vector tiles; generate bundle metadata, checksum and attribution.
4. **Region manager:** download, resume, verify, install, update, delete and atomically activate the Quebec bundle; show package size and storage requirements.
5. **Local search:** query the installed address/POI index offline, with coordinate entry as a fallback.
6. **Navigation session:** feed Valhalla automobile route legs/steps into the existing foreground GPS service; provide French voice instructions, next-turn distance, remaining distance/time and route line display.
7. **Offline rerouting:** detect departure from the active route, throttle recalculation, route from the latest valid GPS fix to the stored destination using local Valhalla tiles, replace remaining route, and announce the new route.
8. **Verification:** airplane-mode tests for map display, search, route calculation, missed-turn rerouting, voice guidance, screen lock, app restart, GPS loss/recovery, interrupted download, checksum failure and low-storage failure.

## Current repository status
- Android GPS foreground service and an initial online OSRM/Nominatim route flow exist.
- The current MainActivity has no actual map renderer; its offline button only reports that offline routing is unavailable.
- The current route deviation recalculation calls the public OSRM server and therefore fails without Internet.
- This document records the selected architecture and implementation plan only. It does not represent completed offline functionality or a tested APK.
