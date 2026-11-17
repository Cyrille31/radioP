# radioP

Deux applications pour écouter le direct de Radio Présence :

- **l'appli Android** (dossier `app/`), pour téléphone, tablette, et plus tard Android Auto et Android TV ;
- **l'appli web** (dossier `web/`), pour iPhone, iPad, ordinateur, et Apple TV via AirPlay.

## Appli Android

Application Android personnelle pour écouter le direct de Radio Présence : choix de l'antenne,
titre de l'émission en cours, lecture en arrière-plan avec commandes dans la notification,
minuteur de veille, réveil sur la radio, contact, et les pages du site (à la une, agenda, don).

Application indépendante, ni éditée ni soutenue par la radio. Les flux audio et les contenus
du site appartiennent à Radio Présence.

### Installer l'appli

1. Sur GitHub, onglet **Actions**, ouvrir la dernière fabrication « Fabriquer l'APK » réussie.
2. En bas de la page, télécharger l'artefact **radioP-apk** (un fichier zip) et en extraire `radioP.apk`.
3. Copier `radioP.apk` sur le téléphone et l'ouvrir (autoriser l'installation depuis cette source).

Chaque fabrication porte un numéro de version croissant et la même signature :
un nouvel APK s'installe par-dessus l'ancien, sans perdre les réveils.

### Fabriquer soi-même

Avec le kit de développement Android installé (`ANDROID_HOME`) :

```bash
./gradlew assembleRelease
```

L'APK se trouve dans `app/build/outputs/apk/release/`.

### Où régler quoi

- Antennes et adresses des flux : `app/src/main/kotlin/io/github/cyrille31/radiop/Stations.kt`
- Pages du site ouvertes par le menu : objet `Site` dans `ui/WebScreen.kt`
- Téléphone et courriel de contact : en tête de `ui/RadioScreen.kt`

## Appli web

Adresse : https://cyrille31.github.io/radioP/

Lecteur du direct, choix de l'antenne, titre de l'émission en cours, minuteur de veille, contact.
Les pages du site (à la une, agenda, don) s'ouvrent dans un onglet. Pas de réveil : un navigateur
ne peut pas lancer la radio à heure fixe écran éteint.

- **iPhone, iPad** : ouvrir l'adresse dans Safari, bouton Partager, puis « Sur l'écran d'accueil ».
  La lecture continue écran verrouillé, avec les commandes sur l'écran de verrouillage,
  sur l'Apple Watch et dans CarPlay (écran « À l'écoute ») ; les boutons « précédent » et « suivant »
  changent d'antenne.
- **Apple TV, enceintes** : AirPlay depuis les commandes de lecture de l'iPhone.
- **Android, ordinateur** : menu du navigateur, puis « Installer l'application ».

Mise en ligne : automatique à chaque modification du dossier `web/` arrivée sur la branche `main`
(fichier `.github/workflows/web.yml`). À faire une seule fois avant : dans les réglages du dépôt,
**Settings → Pages → Build and deployment → Source : GitHub Actions**.

Essayer en local : `python3 -m http.server -d web 8000`, puis ouvrir http://localhost:8000.

## Licence

MIT + « BAL 1.0 — Bonne Action License » (voir `LICENSE`).

En échange, une seule chose vous est demandée, sur l'honneur : faire une bonne action
chaque jour. Aider un voisin, sourire à un inconnu, ramasser un papier… c'est vous qui voyez.
