# Configuration OAuth 2.0 pour smstomail

## Erreur actuelle
Vous recevez l'erreur "Error 400: invalid_request" car l'application n'est pas correctement configurée dans Google Cloud Console.

## Solution : Configuration complète OAuth 2.0

### 1. Obtenir l'empreinte SHA-1 de votre application

#### Pour la version debug (développement):
```bash
keytool -list -v -keystore ~/.android/debug.keystore -alias androiddebugkey -storepass android -keypass android
```

#### Pour la version release (production):
```bash
keytool -list -v -keystore /path/to/your/keystore.jks -alias your-key-alias
```

Notez la valeur **SHA1** qui ressemble à : `A1:B2:C3:D4:E5:F6...`

### 2. Configuration dans Google Cloud Console

1. **Créer/Sélectionner un projet :**
   - Allez sur https://console.cloud.google.com/
   - Créez un nouveau projet ou sélectionnez un existant

2. **Activer l'API Gmail :**
   - APIs & Services > Library
   - Recherchez "Gmail API"
   - Cliquez sur "Enable"

3. **Créer des identifiants OAuth 2.0 :**
   - APIs & Services > Credentials
   - Cliquez "Create Credentials" > "OAuth 2.0 Client ID"
   - Sélectionnez "Android" comme type d'application
   - **Nom du package :** `com.example.smstomail`
   - **Empreinte du certificat de signature SHA-1 :** Collez votre SHA-1 obtenu à l'étape 1

4. **Configurer l'URI de redirection :**
   - Dans la configuration OAuth, ajoutez :
   - URI de redirection : `com.example.smstomail://oauth/callback`

5. **Configurer l'écran de consentement :**
   - OAuth consent screen > External
   - Remplissez les informations requises (nom de l'app, email de support, etc.)
   - Ajoutez les scopes : `email`, `profile`, `https://www.googleapis.com/auth/gmail.send`

### 3. Mettre à jour le code

Copiez le **Client ID** généré (format: `XXXXX-XXXXX.apps.googleusercontent.com`) et remplacez dans le fichier `OAuth2Config.kt` :

```kotlin
const val CLIENT_ID = "VOTRE_CLIENT_ID_ICI.apps.googleusercontent.com"
```

### 4. Test

1. Compilez et installez l'application
2. Lancez l'authentification OAuth
3. Vous devriez maintenant voir l'écran de consentement Google au lieu de l'erreur

## Sécurité

⚠️ **IMPORTANT :** 
- Ne committez jamais votre client ID dans un dépôt public
- Utilisez des variables d'environnement ou des fichiers de configuration sécurisés pour la production
- Pour la production, créez un keystore dédié et utilisez son SHA-1

## Dépannage

- **Erreur "invalid_client"** : Vérifiez que le client ID est correct
- **Erreur "redirect_uri_mismatch"** : Vérifiez que l'URI de redirection est exactement `com.example.smstomail://oauth/callback`
- **Erreur "unauthorized_client"** : Vérifiez que l'empreinte SHA-1 correspond à votre keystore