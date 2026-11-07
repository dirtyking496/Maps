# XIO GPS — édition Web installable

Cette version est une PWA : elle peut être ajoutée à l'écran d'accueil Android depuis Chrome. Elle offre une carte interactive, la géolocalisation lorsque la page est ouverte, le calcul d'itinéraire automobile en ligne et les annonces vocales du navigateur.

## Déploiement Netlify
Publier le dossier `web` comme dossier de site (il contient `index.html`, `manifest.webmanifest`, `sw.js` et `icon.svg`). Aucun build n'est requis.

## Limites importantes
- Cette édition Web requiert Internet pour charger les cartes OSM, rechercher les adresses et calculer les routes via OSRM.
- Le service worker conserve la coquille de l'application; il ne transforme pas le moteur de routage en moteur hors ligne.
- La géolocalisation Web fonctionne lorsque le navigateur autorise l'accès et que la page est active; le suivi permanent écran verrouillé n'est pas garanti.
- Le moteur Valhalla natif et le paquet PMTiles/Valhalla du Québec restent dans l'application Android native. Le PWA n'importe pas ce paquet.
- Les services publics OpenStreetMap, Nominatim et OSRM sont fournis selon leurs politiques et sans garantie de disponibilité.

Attribution cartographique : © OpenStreetMap contributors.
