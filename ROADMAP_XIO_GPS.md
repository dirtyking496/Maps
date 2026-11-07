# XIO GPS — fonctions prioritaires

## 1. Navigation automobile hors ligne — À DÉVELOPPER
Le moteur de routage automobile local, les données routières régionales, l'index de recherche et le recalcul hors connexion ne sont pas encore intégrés. Le cache de tuiles cartographiques seul ne constitue pas une navigation hors ligne.

## 2. Guidage vocal virage par virage — À DÉVELOPPER
La synthèse vocale française est initialisée dans l'application native, mais les instructions détaillées synchronisées avec les étapes de route et le recalcul après déviation doivent encore être intégrés et testés.

## 3. GPS en arrière-plan — PREMIÈRE INTÉGRATION
Un service Android au premier plan avec notification persistante et stockage de la dernière position est ajouté. Il reste à valider les permissions Android, la continuité après verrouillage, les restrictions de batterie et les essais sur appareil.

## Validation avant publication
- Compilation Android réussie et APK généré.
- Essais sur appareil Android réel.
- Essai hors ligne en mode avion.
- Essais des annonces vocales pendant un trajet réel.
- Essai écran verrouillé et application en arrière-plan.
- Signature de production, politique de confidentialité et conformité cartographique.
