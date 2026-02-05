# Green Coin - Deployment Guide

Complete guide for deploying the Green Coin platform to production.

## Architecture Overview

```
┌─────────────────┐
│  Vercel (CDN)   │  ← Frontend (HTML/CSS/JS)
└────────┬────────┘
         │ HTTPS + JWT
         ▼
┌─────────────────┐
│ Railway/Render  │  ← Backend (Spring Boot)
└────────┬────────┘
         │
         ▼
┌─────────────────┐
│   PostgreSQL    │  ← Database
└─────────────────┘

┌─────────────────┐
│  Firebase Auth  │  ← Authentication
└─────────────────┘
```

---

## Phase 1: Firebase Setup

### 1.1 Create Firebase Project

1. Go to [Firebase Console](https://console.firebase.google.com/)
2. Click "Add Project"
3. Enter project name: `greencoin`
4. Disable Google Analytics (optional)
5. Click "Create Project"

### 1.2 Enable Authentication

1. Go to **Authentication** → **Sign-in method**
2. Enable **Google** provider
3. Enable **Email/Password** provider
4. Add authorized domains:
   - `localhost` (for development)
   - Your Vercel domain (after deployment)

### 1.3 Get Web App Config

1. Go to **Project Settings** → **General**
2. Scroll to "Your apps"
3. Click **Web** icon (</>) to add a web app
4. Register app name: `Green Coin Web`
5. Copy the `firebaseConfig` object

### 1.4 Generate Service Account Key (Backend)

1. Go to **Project Settings** → **Service Accounts**
2. Click **"Generate New Private Key"**
3. Save as `firebase-admin-key.json`
4. **IMPORTANT**: Keep this file secure, never commit to Git

### 1.5 Enable Firebase Storage

1. Go to **Storage** in Firebase Console
2. Click **"Get Started"**
3. Choose **Production mode** security rules
4. Select a location (same region as your backend)

---

## Phase 2: Database Setup (PostgreSQL)

### Option A: Railway (Recommended)

1. Go to [Railway.app](https://railway.app/)
2. Sign up with GitHub
3. Create **New Project** → **Provision PostgreSQL**
4. Click on PostgreSQL service → **Variables** tab
5. Copy these credentials:
   - `DATABASE_URL`
   - `PGHOST`, `PGPORT`, `PGUSER`, `PGPASSWORD`, `PGDATABASE`

### Option B: Local PostgreSQL (Development)

```bash
# Install PostgreSQL
# macOS
brew install postgresql
brew services start postgresql

# Ubuntu/Debian
sudo apt install postgresql
sudo systemctl start postgresql

# Create database
psql -U postgres
CREATE DATABASE greencoin;
\q

# Run schema
psql -U postgres -d greencoin -f database/schema.sql
```

---

## Phase 3: Backend Deployment (Railway)

### 3.1 Prepare Backend

1. Add `firebase-admin-key.json` to `backend/` directory (don't commit!)
2. Create `.gitignore` in `backend/`:
   ```
   firebase-admin-key.json
   target/
   .env
   ```

### 3.2 Deploy to Railway

1. Go to [Railway.app](https://railway.app/)
2. Click **New Project** → **Deploy from GitHub repo**
3. Select your `greencoin` repository
4. Railway will auto-detect Spring Boot
5. Set **Root Directory** to `backend`

### 3.3 Configure Environment Variables

In Railway Dashboard → **Variables**, add:

```
DATABASE_URL=postgresql://user:pass@host:port/db
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CREDENTIALS_PATH=/app/firebase-admin-key.json
ALLOWED_ORIGINS=https://your-app.vercel.app,http://localhost:3000
PORT=8080
```

### 3.4 Upload Service Account Key

Railway doesn't support file uploads directly. Use one of these approaches:

**Option 1: Base64 Environment Variable**
```bash
# Encode file to base64
cat firebase-admin-key.json | base64 > firebase-key-base64.txt
```

Add to Railway variables:
```
FIREBASE_KEY_BASE64=<paste base64 content>
```

Update `FirebaseConfig.java`:
```java
// Decode from environment variable
String base64Key = System.getenv("FIREBASE_KEY_BASE64");
byte[] decodedKey = Base64.getDecoder().decode(base64Key);
InputStream serviceAccount = new ByteArrayInputStream(decodedKey);

FirebaseOptions options = FirebaseOptions.builder()
    .setCredentials(GoogleCredentials.fromStream(serviceAccount))
    .setProjectId(projectId)
    .build();
```

**Option 2: Use Firebase Admin SDK without file**
Set credentials via environment variable in Railway.

### 3.5 Get Backend URL

After deployment, Railway will provide a URL like:
```
https://greencoin-backend-production.up.railway.app
```

Copy this URL for frontend configuration.

---

## Phase 4: Frontend Deployment (Vercel)

### 4.1 Update Frontend Configuration

Edit `frontend/js/config.js`:

```javascript
const firebaseConfig = {
    apiKey: "YOUR_FIREBASE_API_KEY",           // From Firebase Console
    authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_PROJECT_ID.appspot.com",
    messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
    appId: "YOUR_APP_ID"
};

// UPDATE THIS to your Railway backend URL
const API_BASE_URL = "https://greencoin-backend-production.up.railway.app";
const WS_URL = "https://greencoin-backend-production.up.railway.app/ws";
```

### 4.2 Deploy to Vercel

**Option 1: Vercel CLI**
```bash
npm install -g vercel
cd frontend
vercel
```

**Option 2: GitHub Integration**
1. Push code to GitHub
2. Go to [vercel.com](https://vercel.com/)
3. Click **"New Project"**
4. Import your GitHub repository
5. Set **Root Directory** to `frontend`
6. Click **Deploy**

### 4.3 Configure Custom Domain (Optional)

1. In Vercel Dashboard → **Domains**
2. Add your custom domain
3. Update DNS records as instructed
4. Wait for SSL certificate (automatic)

---

## Phase 5: Final Configuration

### 5.1 Update Backend CORS

In `backend/src/main/resources/application.yml`:

```yaml
cors:
  allowed-origins: https://your-app.vercel.app,https://www.your-domain.com
```

Redeploy backend to Railway.

### 5.2 Add Vercel Domain to Firebase

1. Firebase Console → **Authentication** → **Settings**
2. **Authorized domains** → **Add domain**
3. Add your Vercel domain: `your-app.vercel.app`

### 5.3 Add Collectors to Whitelist

Connect to your Railway PostgreSQL database:

```sql
INSERT INTO collector_whitelist (email, added_by) 
VALUES ('collector@example.com', 'admin');
```

---

## Phase 6: Testing

### 6.1 Test Citizen Flow

1. Visit your Vercel URL
2. Click **"Get Started"**
3. Sign in with Google
4. Submit a waste report with photo
5. Verify it appears on the map

### 6.2 Test Collector Flow

1. Add collector email to whitelist
2. Sign in with email/password
3. View available reports on map
4. Pick up a report
5. Mark as collected

### 6.3 Test Real-Time Updates

1. Open citizen dashboard in one browser
2. Open collector dashboard in another
3. Submit a report as citizen
4. Verify collector sees it in real-time
5. Mark as collected
6. Verify citizen receives coin notification

---

## Environment Variables Summary

### Backend (Railway)
```
DATABASE_URL=postgresql://...
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_KEY_BASE64=<base64-encoded-service-account-key>
ALLOWED_ORIGINS=https://your-app.vercel.app
PORT=8080
```

### Frontend (Vercel)
No environment variables needed (config in `config.js`)

---

## Post-Deployment Checklist

- [ ] Backend health check works: `https://backend-url/health`
- [ ] Frontend loads correctly on Vercel
- [ ] Google OAuth works
- [ ] Email/Password login works for collectors
- [ ] Citizens can submit reports
- [ ] Images upload to Firebase Storage
- [ ] Reports appear on map
- [ ] Collectors can pick up reports
- [ ] Coins are awarded correctly
- [ ] WebSocket notifications work
- [ ] Mobile responsiveness verified
- [ ] HTTPS enabled (automatic on Vercel)
- [ ] Custom domain configured (if using)

---

## Monitoring & Logs

### Backend Logs (Railway)
```bash
# View logs in Railway Dashboard
# Or use CLI
railway logs
```

### Frontend Logs (Vercel)
```bash
# Install Vercel CLI
npm i -g vercel

# View logs
vercel logs
```

### Database Access (Railway)
```bash
# Connect via psql
psql $DATABASE_URL
```

---

## Troubleshooting

**Backend won't start on Railway**
- Check logs for errors
- Verify `PORT` env variable is set
- Ensure PostgreSQL is running

**Firebase Authentication Error**
- Verify API keys in `config.js`
- Check authorized domains in Firebase Console
- Clear browser cache and cookies

**CORS Error**
- Add Vercel domain to `ALLOWED_ORIGINS`
- Redeploy backend after changes
- Check browser console for exact error

**Images not uploading**
- Verify Firebase Storage is enabled
- Check Firebase Storage rules
- Test image size (max 5MB)

**WebSocket connection failed**
- Verify `WS_URL` in `config.js`
- Check backend logs for WebSocket errors
- Ensure Railway allows WebSocket connections

---

## Cost Estimate

| Service | Free Tier | Estimated Monthly |
|---------|-----------|-------------------|
| Vercel | 100 GB bandwidth | $0 |
| Railway | 500 hours, 1GB RAM | $0 - $5 |
| Firebase Auth | 50k MAU | $0 |
| Firebase Storage | 1 GB, 50k downloads | $0 |
| PostgreSQL (Railway) | 1 GB | $0 |
| **Total** | | **$0 - $5** |

---

## Scaling Considerations

When you outgrow free tiers:

1. **Frontend**: Vercel Pro ($20/month)
2. **Backend**: Railway Pro or AWS EC2
3. **Database**: Railway Pro or AWS RDS
4. **Storage**: Firebase Blaze (pay-as-you-go)

---

## Security Best Practices

✅ Never commit `firebase-admin-key.json`  
✅ Use environment variables for secrets  
✅ Enable HTTPS only (automatic on Vercel)  
✅ Validate all inputs on backend  
✅ Rate limit API endpoints  
✅ Regularly update dependencies  
✅ Monitor logs for suspicious activity  

---

## Support & Maintenance

Regular tasks:
- Monitor error logs weekly
- Update dependencies monthly
- Backup database regularly
- Review Firebase usage
- Check Railway/Vercel bills

Happy deploying! 🚀
