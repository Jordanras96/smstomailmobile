# Mise à jour du Web Client ID

## Action Immédiate Requise

Vous devez maintenant créer le **Web Client OAuth** dans Google Cloud Console et mettre à jour l'application Android.

### 1. Créer le Web Client

**Google Cloud Console > APIs & Services > Credentials :**

1. **Create Credentials > OAuth 2.0 Client ID**
2. **Type :** Web application  
3. **Name :** SMS to Mail Web Client
4. **Authorized JavaScript origins :**
   ```
   https://jordanras96.github.io
   ```
5. **Authorized redirect URIs :**
   ```
   https://jordanras96.github.io/smstomail/
   ```

### 2. Copier le Client ID généré

Le format sera : `447857613313-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.apps.googleusercontent.com`

### 3. Commande de mise à jour

Une fois que vous avez le Web Client ID, exécutez cette commande pour mettre à jour l'app :

```bash
# Remplacez YOUR_WEB_CLIENT_ID par votre client ID réel
sed -i 's/447857613313-XXXXXXXXXXXXXXXXXXXXXXXXXXXXXXXX.apps.googleusercontent.com/YOUR_WEB_CLIENT_ID/g' /home/codeslayer/AndroidStudioProjects/smstomail/app/src/main/java/com/example/smstomail/OAuth2Config.kt
```

### 4. Recompiler l'application

```bash
cd /home/codeslayer/AndroidStudioProjects/smstomail
./gradlew assembleDebug
```

## URLs du site web

- **Site principal :** https://jordanras96.github.io/smstomail/
- **Politique de confidentialité :** https://jordanras96.github.io/smstomail/privacy/
- **Conditions d'utilisation :** https://jordanras96.github.io/smstomail/terms/

## Résultat attendu

Après cette configuration, l'erreur OAuth 2.0 sera complètement résolue et votre application fonctionnera parfaitement avec Google Identity Services !