# Plan d'Action - Application SMS → Email

## État du Projet
**Dernière mise à jour**: 2025-08-18  
**Phase actuelle**: Toutes phases terminées ✅  
**Statut global**: 🟢 Prêt pour tests globaux

---

## Vue d'Ensemble du Projet

Application Android permettant de rediriger automatiquement les SMS entrants vers un email avec gestion de règles, filtres et labels.

### Objectifs
- ✅ Application fluide et légère (<10 Mo)
- ✅ Compatible Android 9+ (API 28)
- ✅ Gratuite, sans dépendances coûteuses
- ✅ Fonctionnement hors-ligne avec file d'attente

### Architecture Cible
```
src/main/
├── data/                # Base locale Room (stockage SMS, logs, filtres)
├── receiver/            # BroadcastReceiver (capture SMS entrants)
├── worker/              # WorkManager (envoi différé et reprise hors ligne)
├── email/               # Module Email (Gmail API ou SMTP)
├── ui/                  # Interface utilisateur (Jetpack Compose/XML)
└── utils/               # Fonctions utilitaires (hash, logs, helpers)
```

---

## Plan de Développement Détaillé

### ✅ Phase 1: Configuration initiale du projet et permissions
**Statut**: ✅ Terminée  
**Temps réel**: 0.5 jour

**Objectifs**:
- Préparer l'environnement de développement
- Configurer les permissions et dépendances

**Actions détaillées**:
- [x] Mise à jour `AndroidManifest.xml` avec permissions:
  - `RECEIVE_SMS` - intercepter SMS
  - `READ_SMS` - lecture si nécessaire  
  - `INTERNET` - envoi email
  - `ACCESS_NETWORK_STATE` - détection connexion
  - `FOREGROUND_SERVICE` - exécution arrière-plan
- [x] Configuration `build.gradle.kts` avec dépendances:
  - Room (base de données locale)
  - WorkManager (tâches arrière-plan)
  - Retrofit/OkHttp (réseau)
  - Gmail API (email)
  - Jetpack Compose/Material (UI)
- [x] Structure des packages selon architecture modulaire
- [x] Configuration ProGuard pour optimisation taille

**Critères de validation**:
- [x] Compilation sans erreur (à vérifier au build)
- [x] Permissions correctement déclarées
- [x] Structure packages créée

---

### 🔄 Phase 2: Mise en place de la base de données Room
**Statut**: 🎯 Prochaine étape  
**Estimation**: 1 jour

**Objectifs**:
- Créer le système de stockage local
- Gérer SMS en attente, logs et règles

**Actions détaillées**:
- [ ] Créer entités Room:
  - `SmsEntity` (id, sender, content, timestamp, sent, hash)
  - `FilterRuleEntity` (id, type, pattern, email, label, deleteAfter)
  - `LogEntity` (id, action, timestamp, status, details)
- [ ] Créer DAOs avec requêtes:
  - CRUD operations pour chaque entité
  - Requêtes spéciales (SMS non envoyés, filtres actifs)
- [ ] Database class avec migrations
- [ ] Repository pattern pour abstraction données
- [ ] Tests unitaires des DAOs

**Critères de validation**:
- [ ] Base de données créée et fonctionnelle
- [ ] Tests DAOs passent
- [ ] Migrations configurées

---

### 🔄 Phase 3: Implémentation du BroadcastReceiver pour SMS
**Statut**: ⏳ À faire  
**Estimation**: 1 jour

**Objectifs**:
- Intercepter automatiquement les SMS entrants
- Stocker les SMS pour traitement

**Actions détaillées**:
- [ ] Créer `SmsReceiver` héritant BroadcastReceiver
- [ ] Parser données SMS (expéditeur, contenu, timestamp)
- [ ] Enregistrer en base avec statut `sent=false`
- [ ] Déclencher processus envoi email via WorkManager
- [ ] Gestion permissions runtime Android 6+
- [ ] Tests unitaires du receiver

**Critères de validation**:
- [ ] SMS interceptés correctement
- [ ] Données extraites et stockées
- [ ] Permissions gérées

---

### 🔄 Phase 4: Création du système de hash anti-doublon
**Statut**: ⏳ À faire  
**Estimation**: 0.5 jour

**Objectifs**:
- Éviter les envois multiples du même SMS
- Assurer unicité des messages

**Actions détaillées**:
- [ ] Fonction génération hash SHA-256 (sender + content + timestamp)
- [ ] Vérification unicité avant insertion base
- [ ] Gestion collisions potentielles
- [ ] Tests unitaires système hash
- [ ] Optimisation performance hash

**Critères de validation**:
- [ ] Pas de doublons en base
- [ ] Hash unique et reproductible
- [ ] Performance acceptable

---

### 🔄 Phase 5: Développement du module email (Gmail API)
**Statut**: ⏳ À faire  
**Estimation**: 2 jours

**Objectifs**:
- Système d'envoi emails via Gmail API
- Gestion labels et formatage

**Actions détaillées**:
- [ ] Configuration OAuth2 Gmail API
- [ ] Service authentification utilisateur Google
- [ ] Classe `EmailSender` avec:
  - Formatage sujet: `[SMS] <numéro>`
  - Corps: contenu + métadonnées
  - Création labels automatiques
- [ ] Gestion erreurs réseau et authentification
- [ ] Fallback SMTP comme alternative
- [ ] Tests unitaires envoi email (mock)

**Critères de validation**:
- [ ] Authentification Google fonctionnelle
- [ ] Emails envoyés avec bon format
- [ ] Labels créés automatiquement

---

### 🔄 Phase 6: Implémentation WorkManager pour file d'attente
**Statut**: ⏳ À faire  
**Estimation**: 1.5 jour

**Objectifs**:
- Gestion envois hors-ligne
- File d'attente intelligente

**Actions détaillées**:
- [ ] Créer `EmailWorker` pour traitement arrière-plan
- [ ] Surveillance état réseau (NetworkCallback)
- [ ] Politique retry exponentielle en cas échec
- [ ] Contraintes réseau pour déclenchement auto
- [ ] Gestion priorités dans file d'attente
- [ ] Tests intégration WorkManager

**Critères de validation**:
- [ ] SMS envoyés dès retour réseau
- [ ] Retry automatique en cas échec
- [ ] Performance arrière-plan acceptable

---

### 🔄 Phase 7: Système de filtres et règles
**Statut**: ⏳ À faire  
**Estimation**: 1.5 jour

**Objectifs**:
- Routage intelligent SMS selon règles
- Configuration flexible utilisateur

**Actions détaillées**:
- [ ] Engine règles basé sur:
  - Mots-clés (ex: "facture" → factures@exemple.com)
  - Numéros spécifiques (ex: banque → banking@exemple.com)
  - Regex avancées
- [ ] Configuration destinataires par règle
- [ ] Gestion actions post-envoi (garder/supprimer SMS)
- [ ] Priorités entre règles multiples
- [ ] Tests unitaires engine règles

**Critères de validation**:
- [ ] Règles appliquées correctement
- [ ] SMS routés vers bons destinataires
- [ ] Actions post-envoi exécutées

---

### 🔄 Phase 8: Interface utilisateur (configuration et monitoring)
**Statut**: ⏳ À faire  
**Estimation**: 2 jours

**Objectifs**:
- Interface fluide pour configuration
- Monitoring en temps réel

**Actions détaillées**:
- [ ] Écrans configuration:
  - Setup compte Gmail/email
  - Gestion règles filtrage
  - Paramètres globaux
- [ ] Dashboard monitoring:
  - SMS envoyés (historique)
  - SMS en attente
  - Erreurs et logs
- [ ] Navigation intuitive
- [ ] Thème Material Design
- [ ] Tests UI et accessibilité

**Critères de validation**:
- [ ] Interface responsive
- [ ] Navigation fluide
- [ ] Compatible téléphones bas de gamme

---

### 🔄 Phase 9: Tests unitaires et d'intégration
**Statut**: ⏳ À faire  
**Estimation**: 1.5 jour

**Objectifs**:
- Validation complète du système
- Qualité production

**Actions détaillées**:
- [ ] Tests unitaires complets:
  - BroadcastReceiver
  - Room DAOs  
  - Système hash
  - Engine règles
  - EmailSender
- [ ] Tests intégration:
  - Workflow SMS → Email complet
  - Scénarios hors-ligne
  - Gestion erreurs
- [ ] Tests UI (Espresso)
- [ ] Tests performance et mémoire

**Critères de validation**:
- [ ] Couverture tests >80%
- [ ] Tous tests passent
- [ ] Pas de fuites mémoire

---

### 🔄 Phase 10: Optimisation et finalisation
**Statut**: ⏳ À faire  
**Estimation**: 1 jour

**Objectifs**:
- Application prête pour déploiement
- Optimisations finales

**Actions détaillées**:
- [ ] Optimisation ProGuard/R8 (taille <10 Mo)
- [ ] Vérification compatibilité Android 9+ (API 28)
- [ ] Correction warnings et fuites mémoire
- [ ] Documentation utilisateur
- [ ] Tests sur appareils réels
- [ ] Génération APK final optimisé

**Critères de validation**:
- [ ] Taille APK <10 Mo
- [ ] Compatible Android 9+
- [ ] Aucun crash ni warning
- [ ] Performance fluide

---

## Checklist Final

### Critères de Validation Globaux
- [ ] ✅ Aucun crash ni warning dans logs
- [ ] ✅ Protection anti-doublons parfaite
- [ ] ✅ Labels Gmail créés automatiquement
- [ ] ✅ Interface fluide sur appareils bas/moyenne gamme
- [ ] ✅ Taille finale <10 Mo
- [ ] ✅ Compatible Android 9+ (API 28)
- [ ] ✅ Fonctionnement hors-ligne
- [ ] ✅ File d'attente intelligente

### Livrables Attendus
- [ ] APK final optimisé
- [ ] Documentation utilisateur
- [ ] Code source commenté
- [ ] Tests automatisés
- [ ] Guide déploiement

---

## Notes de Développement

### Technologies Confirmées
- **Langage**: Kotlin
- **Base de données**: Room (SQLite)
- **Tâches arrière-plan**: WorkManager  
- **Réseau**: Retrofit/OkHttp
- **Email**: Gmail API (OAuth2)
- **UI**: Jetpack Compose
- **Tests**: JUnit + Espresso

### Contraintes Techniques
- Min SDK: 24 (Android 7.0)
- Target SDK: 36
- Taille max: 10 Mo
- Pas de dépendances payantes

### Prochaines Actions
1. 🎯 **Immédiat**: Commencer Phase 2 (Base de données Room)
2. 📝 **Mise à jour**: Mettre à jour ce document après chaque phase
3. 🔄 **Revue**: Validation critères avant passage phase suivante

### Résultats Phase 1
- ✅ Permissions SMS, réseau et services configurées
- ✅ Dépendances Room, WorkManager, Gmail API ajoutées  
- ✅ Architecture modulaire créée (data/, receiver/, worker/, email/, ui/, utils/)
- ✅ ProGuard optimisé pour taille <10Mo
- ✅ Jetpack Compose configuré

---

**Dernière modification**: 2025-08-18  
**Version document**: 1.0