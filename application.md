Guide de développement de l’application SMS → Email

Ce document détaille la conception et la mise en œuvre d’une application Android permettant de rediriger automatiquement les SMS entrants vers un email, avec gestion de règles, filtres, labels et conditions réseau.
L’objectif est de produire une application fluide, légère (<10 Mo), gratuite et compatible Android 9+.

Vue d’ensemble

L’application suit une architecture modulaire avec séparation des responsabilités :
src/main/
├── data/                # Base locale Room (stockage SMS en attente, logs, filtres)
├── receiver/            # BroadcastReceiver (capture SMS entrants)
├── worker/              # WorkManager (envoi différé et reprise hors ligne)
├── email/               # Module Email (Gmail API ou SMTP libre)
├── ui/                  # Interface utilisateur (Jetpack Compose ou XML)
└── utils/               # Fonctions utilitaires (hash anti-doublon, logs, helpers)

Fonctionnalités principales

Interception SMS

BroadcastReceiver écoute les SMS entrants.

Chaque SMS est enregistré en DB locale avec état sent=false.

Redirection Email

Utiliser Gmail API OAuth2 (libre et robuste) ou JavaMail SMTP.

Sujet : [SMS] <numéro> ; Corps : contenu + numéro + horodatage.

Ajout d’un libellé SMS et sous-libellés (SMS/Facture, SMS/Banque, etc.).

File d’attente et hors ligne

Si pas de connexion, SMS mis en attente.

WorkManager surveille l’état réseau et déclenche l’envoi dès qu’il y a du trafic data.

Gestion des doublons

Création d’un hash SHA-256 (numéro + contenu + timestamp).

Vérification avant envoi pour éviter les duplications.

Filtres et conditions

Mots-clés → ex. “facture” → destinataire factures@exemple.com.

Numéros spécifiques → ex. 032XXXXXXX → label SMS/Partenaire.

Action après envoi → choix utilisateur : conserver ou supprimer SMS.

Interface utilisateur

Configurer les règles (mot-clé, numéro, destinataire, suppression).

Visualiser les SMS envoyés et ceux en attente.

Paramètre global “Supprimer après envoi”.

Technologies choisies (gratuites et légères)

Langage : Kotlin (léger, optimisé Android).

Base locale : Room (SQLite).

Background tasks : WorkManager.

Réseau : Retrofit (ou HttpURLConnection pour plus de légèreté).

Email : Gmail API (OAuth2 gratuit) ou JavaMail (SMTP libre).

UI : Jetpack Compose ou XML classique (au choix).

Permissions Android

RECEIVE_SMS : intercepter SMS.

READ_SMS : lecture si nécessaire.

INTERNET : envoi email.

ACCESS_NETWORK_STATE : détection de la connexion.

FOREGROUND_SERVICE : exécution fiable en arrière-plan.

Optimisations

Compatibilité Android 9+ (API 28).

Poids final visé : <10 Mo.

Pas de frameworks lourds (Firebase, etc.).

Reprise automatique via WorkManager (pas de services persistants gourmands).

Tests requis

Tests unitaires :

Interception SMS (BroadcastReceiver).

Stockage DB Room.

Hash anti-doublon.

Application des règles (mots-clés, numéros).

Envoi email simulé.

Tests d’intégration :

SMS reçu hors ligne → envoi automatique dès retour réseau.

SMS avec mot-clé spécifique → redirection vers bon destinataire.

Option suppression après envoi → vérification suppression en base + téléphone.

Tests utilisateurs :

Vérifier fluidité de l’UI.

Vérifier absence de crash.

Problèmes à résoudre avant livraison

Aucun crash ni warning dans les logs.

Vérification des doublons parfaitement fonctionnelle.

Labels Gmail créés automatiquement et correctement assignés.

Interface fluide même sur téléphones bas/moyenne gamme.

Étapes finales

Développement complet et modularisé.

Tests unitaires et d’intégration jusqu’à 0 erreur / 0 warning.

Attendre validation utilisateur après test réel.

Génération de l’APK final optimisé pour déploiement.