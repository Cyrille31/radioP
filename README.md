# radioP

Application Android personnelle pour écouter le direct de Radio Présence : choix de l'antenne,
titre de l'émission en cours, lecture en arrière-plan avec commandes dans la notification,
minuteur de veille, réveil sur la radio, contact, et les pages du site (à la une, agenda, don).

Application indépendante, ni éditée ni soutenue par la radio. Les flux audio et les contenus
du site appartiennent à Radio Présence.

## Installer l'appli

1. Sur GitHub, onglet **Actions**, ouvrir la dernière fabrication « Fabriquer l'APK » réussie.
2. En bas de la page, télécharger l'artefact **radioP-apk** (un fichier zip) et en extraire `radioP.apk`.
3. Copier `radioP.apk` sur le téléphone et l'ouvrir (autoriser l'installation depuis cette source).

Chaque fabrication porte un numéro de version croissant et la même signature :
un nouvel APK s'installe par-dessus l'ancien, sans perdre les réveils.

## Fabriquer soi-même

Avec le kit de développement Android installé (`ANDROID_HOME`) :

```bash
./gradlew assembleRelease
```

L'APK se trouve dans `app/build/outputs/apk/release/`.

## Où régler quoi

- Antennes et adresses des flux : `app/src/main/kotlin/io/github/cyrille31/radiop/Stations.kt`
- Pages du site ouvertes par le menu : objet `Site` dans `ui/WebScreen.kt`
- Téléphone et courriel de contact : en tête de `ui/RadioScreen.kt`

## Licence

MIT + « BAL 1.0 — Bonne Action License » (voir `LICENSE`).

En échange, une seule chose vous est demandée, sur l'honneur : faire une bonne action
chaque jour. Aider un voisin, sourire à un inconnu, ramasser un papier… c'est vous qui voyez.
