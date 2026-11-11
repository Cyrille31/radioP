# Appli Android « équivalent Radio Présence » — note de passation

Ce fichier résume une conversation préparatoire et les captures d'écran de l'appli d'origine.
Il sert de point de départ : lis-le en entier avant de commencer.

## Objectif

Créer une application Android équivalente à l'appli « Radio Présence » du Play Store
(paquet `com.icreo.radioprsence`, fournie par RadioKing), puis l'enrichir par la suite
(« des choses en plus et en mieux »). Cette première version doit seulement être équivalente.

- Usage : **personnel** (APK installé à la main, pas de publication Play Store pour l'instant).
- Téléphone cible : Samsung Galaxy S23.
- Méthode : dépôt GitHub piloté depuis Claude Code ; l'APK est fabriqué par GitHub Actions.
- Nom et icône **propres à l'appli** : ne pas reprendre le nom, le logo ni les visuels de la radio
  (les mentions légales de l'appli d'origine interdisent toute reproduction sans autorisation).

## Structure de l'appli d'origine (relevée sur captures d'écran)

### Menu latéral gauche (bouton ☰)

1. **Ma Radio** — écran d'accueil, le lecteur du direct.
2. **A la une** — liste d'articles du site (icône RSS).
3. **Réveil**
4. **Eglise actu** — agenda d'événements (icône RSS).
5. **Site internet** — le site de la radio affiché dans l'appli.
6. **Informations** — mentions légales.
7. **Faire un don** — formulaire de don affiché dans l'appli.

### Écran « Ma Radio »

- En-tête : ☰ à gauche, logo au centre, icône « poste de radio » à droite.
- Grande image carrée à coins arrondis, avec un gros bouton lecture/pause rond
  à cheval sur son bord inférieur.
- Dessous, un bandeau : nom de la radio, puis le **titre de l'émission en cours**
  (exemples vus : « Flash d'information national », « Parole du jour »).
  Ce titre vient vraisemblablement des métadonnées du flux.
- Barre du bas : icône **enveloppe** (contact) et icône **lune** (minuteur de veille).

### Choix de l'antenne (icône radio, panneau latéral droit)

Cinq antennes : **Radio Présence** (antenne principale, Toulouse), **Lourdes**, **Figeac**,
**Cahors**, **St Gaudens**.

### Contact (icône enveloppe)

Boîte « Contacter par… » : **Téléphone** (ouvre le composeur avec le 05 62 48 63 00)
ou **Email** (ouvre un nouveau message à contact@radiopresence.com).

### Minuteur de veille (icône lune)

« Arrêter la radio après » : 5, 10, 15, 30, 45 minutes, 1 heure, 2 heures,
plus un bouton « Supprimer le minuteur ».

### Réveil

Affiche la date et l'heure courantes en grand, avec un bouton « + » pour ajouter un réveil.

Écran « Nouv. réveil » (ouvert par « + ») :
- en haut : « Annuler » à gauche, « Enregistrer » à droite ;
- un champ « Nom du réveil » ;
- l'heure en grand, avec une icône crayon pour la modifier (préremplie avec l'heure courante) ;
- la liste des sept jours, de lundi à dimanche : un appui coche ou décoche le jour
  (plusieurs jours possibles, donc réveil répété chaque semaine).

Non capturé : la liste des réveils une fois enregistrés (activation, modification, suppression)
et le choix de l'antenne à jouer. Par défaut : le réveil lance la dernière antenne écoutée,
et chaque réveil peut être activé, modifié ou supprimé.

### A la une / Eglise actu

Listes de cartes : première carte grande avec image, suivantes avec vignette ronde,
date relative (« il y a 1 jour », « dans 3 jours ») et titre.

### Site internet

Le site `radiopresence.com` en vue web. C'est là que se trouvent **Le direct**, **Réécoute**
(podcasts), les rubriques Information / Foi / Culture / Société, « Fréquences » et « Faire un don ».
L'appli d'origine n'a donc **ni podcasts ni grille en natif** : tout passe par le site.

### Faire un don

Formulaire iRaiser de la radio en vue web (don unique ou mensuel, coordonnées, règlement).

## Périmètre de la version 1

À réaliser en natif :
1. **Lecteur du direct** : lecture/pause, titre en cours, lecture en arrière-plan,
   commandes dans la notification.
2. **Choix de l'antenne** parmi les cinq.
3. **Minuteur de veille** (mêmes durées).
4. **Réveil** sur la radio.
5. **Contact** : téléphone et courriel.
6. **Informations** : écran « À propos » propre à cette appli (licence, voir plus bas).

À ne **pas** recoder — décision de Cyrille : se brancher sur les bonnes pages du site,
qui font exactement la même chose :
- **A la une**, **Eglise actu**, **Site internet**, **Faire un don**.
- Les ouvrir dans une vue web intégrée, comme l'appli d'origine. Pour le don (paiement),
  préférer un onglet du navigateur (Custom Tabs) à une WebView.

Podcasts et grille des programmes : en version 1, accessibles par le site (Réécoute),
comme dans l'appli d'origine. Les faire en natif fait partie des améliorations à venir.

## Ce qui est connu des sources

- Site : `https://www.radiopresence.com` (site SPIP).
- Direct : serveur Icecast, en HTTP seulement (le HTTPS redirige vers le HTTP) :
  `http://direct.radiopresence.com/status.xsl` et `http://live.radiopresence.com/`.
  **Les noms des points de montage n'ont pas été relevés** : les lire sur `status.xsl`
  et les faire correspondre aux cinq antennes.
  Flux en HTTP : prévoir l'autorisation du trafic en clair pour ces hôtes.
- Émissions : une page par émission, par exemple
  `https://www.radiopresence.com/emissions/culture/musique/musique-traditionnelle/…`,
  avec un lien « S'abonner à l'émission » (flux RSS probable, **non vérifié**).
- Grilles : publiées en PDF par antenne, par exemple
  `https://www.radiopresence.com/IMG/pdf/grille_2025-2026_figeac.pdf`.

## Ce qui reste à vérifier en premier

- Les points de montage Icecast, leur format, et la présence du titre en cours dans les métadonnées.
- Les adresses exactes des pages du site à ouvrir : à la une, agenda « Eglise actu », don.

## Défauts connus de l'appli d'origine (à ne pas reproduire)

D'après les avis du Play Store : plantages fréquents, et blocage de l'appli si l'on refuse
les autorisations photos ou position. L'appli ne doit demander aucune autorisation inutile
(ni photos, ni position).

## Consignes de travail

- Échanges et interface en français.
- Ne pas afficher le code dans la conversation : livrer des fichiers dans le dépôt.
- Cyrille dépose parfois des fichiers par le navigateur, qui ignore les dossiers cachés :
  le prévenir chaque fois qu'un fichier caché (`.github/…`, `.gitignore`) est créé ou modifié.
- Licence : MIT + « BAL 1.0 — Bonne Action License ». Partout où la licence est mentionnée
  hors du fichier LICENSE (README, écran « À propos »), ajouter la phrase :
  « En échange, une seule chose vous est demandée, sur l'honneur : faire une bonne action
  chaque jour. Aider un voisin, sourire à un inconnu, ramasser un papier… c'est vous qui voyez. »
- Les flux et contenus appartiennent à la radio : en cas de publication ultérieure,
  il faudra son accord.
