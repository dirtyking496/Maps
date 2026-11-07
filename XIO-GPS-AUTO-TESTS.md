# XIO GPS — tests automatiques de navigation

Le workflow CI exécute maintenant les tests unitaires du moteur avant de fabriquer l'APK :

```sh
cd atlas
./gradlew --no-daemon :app:testDebugUnitTest
./gradlew --no-daemon :app:assembleDebug
```

Le script `tools/patch-atlas-xio.py` applique à la copie Atlas :
- le nom et l'identifiant Android XIO GPS ;
- les instructions vocales françaises (virages, ronds-points, sorties, distances, arrivée et recalcul) ;
- un scénario JUnit déterministe `XioAutomaticNavigationSessionTest`.

## Scénarios simulés

1. Une séquence de fixes GPS synthétiques fait progresser la navigation jusqu'à destination.
2. Trois fixes consécutifs hors de la route déclenchent la demande de recalcul.
3. Une nouvelle route remplace l'ancienne et la progression reprend.
4. Les phrases vocales françaises sont vérifiées, y compris les distances et les sorties de rond-point.
5. Un ancien identifiant de session ne peut pas écraser une session plus récente.

## Limites du test

Ces tests sont des simulations logicielles : ils ne valident pas le signal GPS réel, les performances thermiques/batterie du téléphone, le son réellement émis par le moteur TTS, ni la couverture routière des cartes régionales. Une validation finale doit être faite sur le Samsung Android avec les cartes Québec et Nouveau-Brunswick importées. Ne pas utiliser un build bêta comme seul outil de navigation en conduite réelle.
