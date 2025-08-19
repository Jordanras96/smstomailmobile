# Configuration Web Client OAuth 2.0

## ✅ Site Web Déployé

Votre site web SMS to Mail est maintenant disponible à :
**https://jordanras96.github.io/smstomail/**

## Configuration Required dans Google Cloud Console

### 1. Activer GitHub Pages dans votre repository

1. **GitHub > Repository Settings > Pages**
2. **Source :** Deploy from a branch
3. **Branch :** main / (root)
4. **Ou** Source : GitHub Actions (recommandé)

### 2. Créer le Web Client OAuth 2.0

**Étapes dans Google Cloud Console :**

1. **Allez sur :** https://console.cloud.google.com/apis/credentials
2. **Sélectionnez votre projet** (même que pour l'app Android)
3. **Create Credentials > OAuth 2.0 Client ID**
4. **Application type :** Web application
5. **Name :** SMS to Mail Web Client

**Configuration des URLs autorisées :**

```
Authorized JavaScript origins:
- https://jordanras96.github.io

Authorized redirect URIs:
- https://jordanras96.github.io/smstomail/
- https://jordanras96.github.io/smstomail/privacy/
- https://jordanras96.github.io/smstomail/terms/
```

6. **Cliquez "Create"**
7. **Copiez le Client ID** (format: `XXXXX-XXXXX.apps.googleusercontent.com`)

### 3. Mettre à jour l'application Android

Dans `OAuth2Config.kt`, remplacez :

```kotlin
// Client ID Web pour server-side auth (nécessaire pour getServerAuthCode)
const val WEB_CLIENT_ID = "VOTRE_CLIENT_WEB_ID_ICI.apps.googleusercontent.com"
```

Par :

```kotlin
// Client ID Web pour server-side auth (nécessaire pour getServerAuthCode)
const val WEB_CLIENT_ID = "447857613313-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.apps.googleusercontent.com"
```

### 4. Configuration complète

**Vous aurez alors :**

- ✅ **Client Android :** `447857613313-itn45fqo3jqeh51r2o3lumnt5ihdvqrk.apps.googleusercontent.com`
- ✅ **Client Web :** `447857613313-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.apps.googleusercontent.com`
- ✅ **Site web :** https://jordanras96.github.io/smstomail/

### 5. OAuth Consent Screen

Maintenant vous pouvez configurer l'écran de consentement avec :

```
App homepage: https://jordanras96.github.io/smstomail/
Privacy policy URL: https://jordanras96.github.io/smstomail/privacy/
Terms of service URL: https://jordanras96.github.io/smstomail/terms/
```

## Test de l'authentification

1. **Compilez l'app Android** avec le nouveau `WEB_CLIENT_ID`
2. **Installez l'APK** sur votre téléphone
3. **Testez l'authentification** - elle devrait maintenant fonctionner !

## Support

- **Site web :** https://jordanras96.github.io/smstomail/
- **Code source :** https://github.com/Jordanras96/smstomail
- **Issues :** https://github.com/Jordanras96/smstomail/issues