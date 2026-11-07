# XIO GPS — intégration du moteur hors ligne

Cette branche prépare une première intégration du moteur Android Atlas dans le projet XIO GPS.

## Ce que fait le workflow

- Récupère le moteur Android Atlas depuis son dépôt public.
- Remplace le nom affiché « Atlas » par « XIO GPS ».
- Compile un APK Android de débogage.
- Publie l'APK comme artefact GitHub Actions.

## Limites à connaître

Ce workflow compile le moteur d'application Atlas sous le nom XIO GPS. Il ne fusionne pas encore les anciens écrans Java de XIO GPS avec le nouveau moteur Kotlin/Compose. Il ne télécharge pas non plus automatiquement les cartes du Québec et du Nouveau-Brunswick.

Atlas fournit le rendu cartographique hors ligne, la recherche locale, le routage automobile BeeRouter, le guidage vocal et le recalcul d'itinéraire. Les archives PMTiles régionales doivent être préparées puis importées dans l'application. Le graphe routier est ensuite préparé par région ou installé avec les données de routage préconstruites.

## Obtenir l'APK

1. Ouvrir l'onglet **Actions** du dépôt GitHub.
2. Sélectionner **Build XIO GPS offline Android app**.
3. Appuyer sur **Run workflow** sur la branche `integration-atlas-offline`.
4. Lorsque le travail est terminé, ouvrir le résultat et télécharger l'artefact **XIO-GPS-offline-debug**.

L'APK est un build de débogage non signé pour publication Play Store. Il doit être installé et testé sur un téléphone avant toute utilisation en conduite.

## Attribution et licences

Le moteur Atlas est sous licence MIT. Il intègre notamment BeeRouter (MPL-2.0), MapLibre, OpenMapTiles et des données OpenStreetMap. Conserver les mentions d'attribution affichées dans l'application et respecter les licences des données cartographiques.
