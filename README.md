# XIO GPS — Android

Application Android native en cours de développement.

## Intégrations présentes dans cette branche
- Écran natif Android et thème sombre néon.
- Acquisition GPS pendant que l'application est ouverte.
- Service Android au premier plan avec notification persistante et mémorisation de la dernière position.
- Recherche de destination en ligne avec Nominatim.
- Calcul d'itinéraire automobile en ligne avec OSRM.
- Extraction des manœuvres et annonces vocales françaises à 500 m et 80 m, exécutées par le service GPS même lorsque l'écran est verrouillé (à valider sur appareil).

## Fonctions non terminées
- Navigation automobile hors ligne : non disponible. Il faut intégrer un véritable graphe routier local avec restrictions de virage, recherche et recalcul hors réseau.
- Cartes téléchargeables et utilisation hors ligne : non activées dans cette version.
- Guidage virage par virage : implémentation initiale en ligne, non encore validée sur route et sans recalcul après déviation.
- GPS en arrière-plan : première intégration; tests de batterie, redémarrage et cycle de vie Android à effectuer.

## Compiler
Le workflow GitHub Actions compile un APK debug sur chaque push dans `main`. Consulter l'onglet Actions et télécharger l'artefact `XIO-GPS-debug-apk` après une compilation réussie.

## Avertissement
Ne pas utiliser cette version comme seul outil de navigation routière. Les services publics Nominatim et OSRM nécessitent Internet et ne garantissent aucun SLA. L'application n'a pas encore été testée en conditions réelles.
