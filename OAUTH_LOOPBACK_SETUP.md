# Configuration OAuth 2.0 avec HTTP Loopback

## ✅ Solution implémentée

L'application utilise maintenant la méthode **HTTP loopback** recommandée par Google pour les applications natives Android, au lieu du custom scheme URI qui causait l'erreur `redirect_uri_mismatch`.

## Configuration requise dans Google Cloud Console

### 1. Type de client OAuth
- **Type :** Application de bureau (Desktop Application)
- **PAS Android** - Important !

### 2. URI de redirection autorisées
Ajoutez exactement cette URI dans les "URI de redirection autorisées" :
```
http://localhost:8080/oauth/callback
```

### 3. Configuration complète
1. **Google Cloud Console > APIs & Services > Credentials**
2. **Créer identifiants > OAuth 2.0 Client ID**
3. **Type d'application :** Application de bureau
4. **URI de redirection autorisées :** `http://localhost:8080/oauth/callback`
5. **Copier le Client ID généré**

## Comment ça fonctionne

### Avant (problématique)
```
❌ Custom scheme: com.example.smstomail://oauth/callback
❌ Nécessitait configuration SHA-1 complexe
❌ Erreur: redirect_uri_mismatch
```

### Maintenant (solution)
```
✅ HTTP loopback: http://localhost:8080/oauth/callback  
✅ Serveur HTTP local temporaire dans l'app
✅ Méthode recommandée par Google
✅ Plus de configuration SHA-1 nécessaire
```

## Flux OAuth 2.0 mis à jour

1. **App lance l'authentification :**
   - Démarre un serveur HTTP local sur le port 8080
   - Ouvre Chrome/navigateur avec l'URL Google OAuth
   
2. **Utilisateur s'authentifie :**
   - Interface Google standard dans le navigateur
   - Pas d'écran d'avertissement si configuré correctement
   
3. **Redirection vers l'app :**
   - Google redirige vers `http://localhost:8080/oauth/callback?code=...`
   - Le serveur HTTP local reçoit le code d'autorisation
   - Affiche une page de confirmation à l'utilisateur
   
4. **Échange des tokens :**
   - App échange le code contre les tokens d'accès
   - Sauvegarde les tokens pour utilisation future

## Configuration actuelle de l'app

- **Client ID :** `447857613313-itn45fqo3jqeh51r2o3lumnt5ihdvqrk.apps.googleusercontent.com`
- **Redirect URI :** `http://localhost:8080/oauth/callback`
- **Scopes :** `email`, `profile`, `https://www.googleapis.com/auth/gmail.send`

## Instructions de test

1. **Mettre à jour Google Cloud Console :**
   - Changer le type de "Android" vers "Application de bureau"
   - Ou créer un nouveau client OAuth "Application de bureau"
   - Ajouter `http://localhost:8080/oauth/callback` comme URI de redirection

2. **Installer l'APK mis à jour :**
   ```bash
   adb install app/build/outputs/apk/debug/app-debug.apk
   ```

3. **Tester l'authentification :**
   - Lancer l'app
   - Appuyer sur "Se connecter à Gmail"
   - Le navigateur s'ouvre avec l'écran Google OAuth
   - Après authentification, retour automatique à l'app

## Avantages de cette approche

- ✅ **Recommandée par Google** pour les applications natives
- ✅ **Plus simple à configurer** - pas de SHA-1 complexe
- ✅ **Plus sécurisée** - serveur local temporaire
- ✅ **Meilleure UX** - interface OAuth native dans le navigateur
- ✅ **Compatible** avec toutes les versions Android