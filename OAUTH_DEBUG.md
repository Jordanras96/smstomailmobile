# Debug OAuth 2.0 - Erreur redirect_uri

## Erreur actuelle
```
Erreur 400 : invalid_request
redirect_uri=com.example.smstomail://oauth/callback
```

## Configuration actuelle de l'app
- **Client ID:** 447857613313-itn45fqo3jqeh51r2o3lumnt5ihdvqrk.apps.googleusercontent.com
- **Package name:** com.example.smstomail  
- **Redirect URI:** com.example.smstomail://oauth/callback
- **SHA-1 debug:** 16:4D:8F:C1:0E:62:B8:E9:1D:14:C8:56:93:1E:01:E0:48:EC:63:98

## Actions requises dans Google Cloud Console

### 1. Vérifier la configuration OAuth client
1. Allez sur https://console.cloud.google.com/
2. APIs & Services > Credentials
3. Cliquez sur votre client OAuth: `447857613313-itn45fqo3jqeh51r2o3lumnt5ihdvqrk`

### 2. Configuration exacte requise
```
Type d'application: Android
Nom du package: com.example.smstomail
Empreinte du certificat SHA-1: 16:4D:8F:C1:0E:62:B8:E9:1D:14:C8:56:93:1E:01:E0:48:EC:63:98
```

⚠️ **IMPORTANT:** L'empreinte SHA-1 doit correspondre EXACTEMENT à celle ci-dessus

### 3. Pour les applications Android
- Les applications Android n'utilisent PAS d'URI de redirection web
- Google utilise automatiquement le scheme: `package_name://oauth/callback`
- La validation se fait via l'empreinte SHA-1 + package name

### 4. Vérifications
- ✅ Client ID configuré dans l'app
- ⚠️ SHA-1 à vérifier dans Google Console
- ⚠️ Package name à vérifier dans Google Console

### 5. Si l'erreur persiste
1. Supprimez et recréez le client OAuth Android
2. Attendez 5-10 minutes après la configuration (propagation Google)
3. Vérifiez que l'application est ajoutée aux "Test users" si en mode Testing

## Test après configuration
Une fois la configuration mise à jour dans Google Cloud Console:
1. Désinstallez l'app du téléphone
2. Réinstallez depuis l'APK 
3. Testez l'authentification OAuth