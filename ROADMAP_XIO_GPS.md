# XIO GPS — Prochaines fonctions prioritaires

Les trois fonctions suivantes sont officiellement inscrites à la feuille de route. Elles ne doivent pas être présentées comme terminées tant qu'elles ne sont pas développées, compilées et testées sur un téléphone Android réel.

## Statut : À DÉVELOPPER

### 1. Navigation automobile hors ligne
- Cartes routières téléchargeables et utilisables sans connexion.
- Moteur de calcul d'itinéraire automobile local.
- Prise en charge des routes secondaires, sens uniques, restrictions et interdictions de virage.
- Recherche locale des adresses et destinations.
- Recalcul de l'itinéraire sans Internet.

### 2. Guidage vocal virage par virage
- Instructions vocales en français canadien.
- Annonces des virages, sorties, intersections et changements de direction.
- Distances restantes avant chaque manœuvre.
- Recalcul vocal après une sortie de route.
- Annonces fiables synchronisées avec la progression GPS.

### 3. GPS en arrière-plan
- Service Android au premier plan avec notification persistante.
- Suivi GPS lorsque l'écran est verrouillé ou qu'une autre application est ouverte.
- Continuité des instructions vocales et du suivi de trajet.
- Gestion des permissions Android et des restrictions de batterie.
- Arrêt propre du suivi à la fin du trajet.

## Critères avant de déclarer XIO GPS prêt
- Compilation Android réussie et APK généré.
- Essais réels sur appareil Android.
- Essai de navigation en mode avion pour la fonction hors ligne.
- Essai des annonces vocales sur un trajet réel.
- Essai écran verrouillé / application en arrière-plan.
- Aucune fonction annoncée comme opérationnelle avant validation.

## État actuel
La base Android existe, mais ces trois fonctions restent en développement. Les cartes hors ligne seules ne suffisent pas : il faut aussi intégrer un moteur de routage automobile local et ses données routières.
