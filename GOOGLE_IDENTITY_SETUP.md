# Configuration Google Identity Services

## ⚠️ Migration d'AppAuth vers Google Identity Services

L'application utilise maintenant **Google Identity Services** au lieu d'AppAuth, conformément aux recommandations officielles d'Android.

## Configuration requise dans Google Cloud Console

### 1. Client ID Android (existant - à conserver)
- **Type :** Android
- **Client ID :** `447857613313-itn45fqo3jqeh51r2o3lumnt5ihdvqrk.apps.googleusercontent.com`  
- **Package name :** `com.example.smstomail`
- **SHA-1 :** `16:4D:8F:C1:0E:62:B8:E9:1D:14:C8:56:93:1E:01:E0:48:EC:63:98`

### 2. Client ID Web (NOUVEAU - requis pour getServerAuthCode)

**Vous devez créer un client Web supplémentaire :**

1. **Google Cloud Console > APIs & Services > Credentials**
2. **Create Credentials > OAuth 2.0 Client ID**
3. **Application type :** Web application
4. **Name :** SMS to Mail Web Client
5. **Authorized redirect URIs :** Laisser vide (pas nécessaire pour server-side auth)
6. **Copier le Client ID généré**

### 3. Mise à jour du code

Remplacez dans `OAuth2Config.kt` :
```kotlin
const val WEB_CLIENT_ID = "VOTRE_CLIENT_WEB_ID_ICI.apps.googleusercontent.com"
```

## Comment ça fonctionne maintenant

### Flow Google Identity Services

1. **Utilisateur clique "Se connecter à Gmail"**
2. **Google Sign-In s'ouvre** (interface Google native)
3. **Utilisateur s'authentifie** et accorde les permissions Gmail
4. **App reçoit serverAuthCode** (code d'autorisation à usage unique)
5. **App échange le code** contre access_token + refresh_token via API REST
6. **Tokens sauvegardés** pour utilisation Gmail API

### Avantages vs AppAuth

- ✅ **Recommandé par Google** pour Android
- ✅ **Interface native Google** (pas de Custom Tabs)
- ✅ **Plus sécurisé** avec server-side auth
- ✅ **Gestion automatique** des scopes et permissions
- ✅ **Meilleure UX** intégrée au système

## Scopes utilisés

- `email` - Adresse email de l'utilisateur
- `profile` - Informations de profil de base  
- `https://www.googleapis.com/auth/gmail.send` - Envoi d'emails via Gmail

## Structure des fichiers

### Fichiers supprimés
- ❌ `OAuth2GmailManager.kt` (AppAuth)
- ❌ `OAuth2CallbackActivity.kt` (Custom scheme)
- ❌ `OAuth2HttpServer.kt` (Loopback server)

### Fichiers ajoutés
- ✅ `GoogleSignInManager.kt` (Google Identity Services)

### Fichiers mis à jour
- ✅ `OAuth2Config.kt` - Configuration pour Google Identity Services
- ✅ `SimpleMainActivity.kt` - Utilise GoogleSignInManager
- ✅ `build.gradle.kts` - Dépendances Google Identity Services

## Test après configuration

1. **Créer le client Web dans Google Cloud Console**
2. **Mettre à jour `WEB_CLIENT_ID` dans OAuth2Config.kt**
3. **Compiler et installer l'APK**
4. **Tester l'authentification :**
   - Interface Google Sign-In native
   - Permissions Gmail automatiquement demandées
   - Tokens obtenus et sauvegardés

## Dépannage

### Configuration manquante
```
Erreur: "Configuration Google manquante - vérifiez OAuth2Config"
```
**Solution :** Créer le client Web et mettre à jour `WEB_CLIENT_ID`

### Client ID invalid
```
Erreur: "Invalid client ID"  
```
**Solution :** Vérifier que les deux clients (Android + Web) utilisent le même projet Google Cloud

### SHA-1 mismatch
```
Erreur: "Package name and SHA-1 mismatch"
```
**Solution :** Vérifier l'empreinte SHA-1 dans le client Android