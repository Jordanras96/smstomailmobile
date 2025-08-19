# Configuration Production OAuth 2.0

## Option 1: Mode Testing (Recommandé pour développement)

**Configuration Google Cloud Console :**
```
OAuth consent screen:
- User Type: External
- Publishing status: Testing
- Test users: Votre email + emails des testeurs
- Champs obligatoires seulement:
  * App name: SMS to Mail
  * User support email: votre email
  * Developer contact information: votre email

Laisser VIDES:
- App homepage
- Privacy policy URL  
- Terms of service URL
```

**Avantages :**
- ✅ Fonctionne immédiatement
- ✅ Pas de validation Google requise
- ✅ Jusqu'à 100 utilisateurs de test
- ✅ Pas d'écran d'avertissement pour les utilisateurs de test

## Option 2: Production complète

Si vous voulez publier pour tous les utilisateurs :

### 1. Créer les pages web requises

Vous devez créer un site web avec :

**Page d'accueil (`index.html`) :**
```html
<!DOCTYPE html>
<html>
<head>
    <title>SMS to Mail - Application Android</title>
</head>
<body>
    <h1>SMS to Mail</h1>
    <p>Application Android qui transfère automatiquement vos SMS vers Gmail</p>
    <p>Disponible pour Android 7.0+</p>
</body>
</html>
```

**Politique de confidentialité (`privacy.html`) :**
```html
<!DOCTYPE html>
<html>
<head>
    <title>Politique de confidentialité - SMS to Mail</title>
</head>
<body>
    <h1>Politique de confidentialité</h1>
    <p>Dernière mise à jour: [DATE]</p>
    
    <h2>Données collectées</h2>
    <p>Cette application accède à:</p>
    <ul>
        <li>Vos SMS reçus pour les transférer</li>
        <li>Votre compte Gmail pour l'envoi</li>
        <li>Informations de profil basiques (email)</li>
    </ul>
    
    <h2>Utilisation des données</h2>
    <p>Les données sont uniquement utilisées pour:</p>
    <ul>
        <li>Transférer vos SMS vers votre adresse email</li>
        <li>Authentifier l'accès à Gmail</li>
    </ul>
    
    <h2>Stockage des données</h2>
    <p>Toutes les données restent sur votre appareil. Aucune donnée n'est envoyée à des tiers.</p>
</body>
</html>
```

**Conditions d'utilisation (`terms.html`) :**
```html
<!DOCTYPE html>
<html>
<head>
    <title>Conditions d'utilisation - SMS to Mail</title>
</head>
<body>
    <h1>Conditions d'utilisation</h1>
    <p>En utilisant cette application, vous acceptez ces conditions.</p>
    
    <h2>Utilisation</h2>
    <p>Cette application est fournie "en l'état" sans garantie.</p>
    
    <h2>Responsabilité</h2>
    <p>L'utilisateur est responsable de la configuration et de l'utilisation.</p>
</body>
</html>
```

### 2. Hébergement gratuit

**Options d'hébergement gratuit :**
- GitHub Pages (recommandé)
- Netlify
- Vercel
- Firebase Hosting

**Exemple avec GitHub Pages :**
1. Créez un repository public `smstomail-website`
2. Ajoutez les fichiers HTML
3. Activez GitHub Pages dans Settings
4. Votre site sera disponible à : `https://votreusername.github.io/smstomail-website/`

### 3. Configuration finale

```
OAuth consent screen:
- App homepage: https://votreusername.github.io/smstomail-website/
- Privacy policy: https://votreusername.github.io/smstomail-website/privacy.html
- Terms of service: https://votreusername.github.io/smstomail-website/terms.html
```

## Recommandation

**Pour votre cas d'usage actuel, utilisez le mode Testing :**
- Configuration immédiate
- Ajoutez simplement votre email comme utilisateur de test
- L'application fonctionnera parfaitement

Vous pourrez toujours passer en production plus tard si nécessaire.