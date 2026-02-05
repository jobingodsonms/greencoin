# Green Coin Frontend

Static HTML/CSS/JavaScript frontend for the Green Coin civic waste reporting platform.

## Tech Stack

- **HTML5** - Structure
- **CSS3** - Styling with CSS Variables
- **Vanilla JavaScript** - Logic (no frameworks)
- **Firebase SDK** - Authentication (Google OAuth + Email/Password)
- **Leaflet.js** - Interactive maps with OpenStreetMap
- **SockJS + STOMP** - WebSocket real-time updates

## Quick Start

### 1. Configure Firebase

Edit `js/config.js` and replace with your Firebase credentials:

```javascript
const firebaseConfig = {
    apiKey: "YOUR_FIREBASE_API_KEY",
    authDomain: "YOUR_PROJECT_ID.firebaseapp.com",
    projectId: "YOUR_PROJECT_ID",
    storageBucket: "YOUR_PROJECT_ID.appspot.com",
    messagingSenderId: "YOUR_MESSAGING_SENDER_ID",
    appId: "YOUR_APP_ID"
};
```

### 2. Update API URL

In `js/config.js`, set your backend URL:

```javascript
const API_BASE_URL = "https://your-backend.railway.app";  // Production
// const API_BASE_URL = "http://localhost:8080";  // Development
```

### 3. Test Locally

```bash
# Serve with any static server
npx serve frontend

# Or use Python
cd frontend
python -m http.server 8000
```

Visit `http://localhost:8000`

## File Structure

```
frontend/
├── index.html              # Landing page
├── login.html              # Authentication page
├── citizen.html            # Citizen dashboard
├── collector.html          # Collector dashboard
├── authority.html          # Authority dashboard
├── css/
│   └── styles.css          # Main styles
├── js/
│   ├── config.js           # Firebase & API config
│   ├── auth.js             # Authentication logic
│   ├── api.js              # API client
│   ├── websocket.js        # WebSocket client
│   ├── citizen.js          # Citizen dashboard logic
│   └── collector.js        # Collector dashboard logic
└── vercel.json             # Vercel deployment config
```

## Features

### Citizen Dashboard (`citizen.html`)
✅ Submit waste reports with photo + GPS  
✅ View reports on interactive map  
✅ Coin balance display  
✅ Transaction history  
✅ Real-time coin updates via WebSocket  

### Collector Dashboard (`collector.html`)
✅ View all available reports on map  
✅ Nearby reports search  
✅ Pick up reports (mark as PICKING)  
✅ Complete pickups (mark as COLLECTED)  
✅ Real-time new report notifications  

### Authority Dashboard (`authority.html`)
✅ System statistics overview  
✅ Placeholder for management features  

## Deploy to Vercel

### Option 1: Vercel CLI

```bash
# Install Vercel CLI
npm install -g vercel

# Deploy
cd frontend
vercel
```

### Option 2: GitHub Integration

1. Push `frontend/` to GitHub
2. Go to [vercel.com](https://vercel.com)
3. Click "New Project"
4. Import your GitHub repository
5. Set **Root Directory** to `frontend`
6. Deploy!

### Environment Variables in Vercel

Set these in Vercel Dashboard → Project Settings → Environment Variables:

```
FIREBASE_API_KEY=your_api_key
FIREBASE_PROJECT_ID=your_project_id
```

> **Note:** For static sites, you can also hardcode Firebase config in `js/config.js` since it's client-side anyway.

## Configure Backend CORS

Update your backend `application.yml`:

```yaml
cors:
  allowed-origins: https://your-app.vercel.app
```

## Authentication Flow

1. **User visits** `login.html`
2. **Citizen**: Click "Sign in with Google" → Firebase OAuth
3. **Collector/Authority**: Enter email/password → Firebase Auth
4. **Frontend**: Gets Firebase ID token
5. **Frontend**: Calls `/api/user/register` with token
6. **Backend**: Verifies token, creates/updates user, assigns role
7. **Backend**: Returns user profile with role
8. **Frontend**: Redirects to appropriate dashboard:
   - `CITIZEN` → `citizen.html`
   - `COLLECTOR` → `collector.html`
   - `AUTHORITY` → `authority.html`

## Map Integration

Uses **Leaflet.js** with **OpenStreetMap** tiles:

- Color-coded markers for report status
- Interactive popups with report details
- Geolocation support for "Near Me" feature
- Responsive on mobile devices

## WebSocket Real-Time Updates

**Citizens** receive:
- Coin awarded notifications when reports are collected

**Collectors** receive:
- New report notifications (browser push notifications)

## Mobile Responsiveness

✅ Mobile-first CSS design  
✅ Touch-friendly buttons and forms  
✅ GPS location capture on mobile  
✅ Camera integration for photos  
✅ Responsive map layout  

## Browser Compatibility

- Chrome/Edge (recommended)
- Firefox
- Safari
- Mobile browsers (iOS Safari, Chrome Android)

## Security Notes

- Firebase tokens are stored in `localStorage`
- Tokens are sent in `Authorization: Bearer` headers
- Backend validates all Firebase tokens
- Role-based access enforced on both frontend and backend

## Production Checklist

- [ ] Update `js/config.js` with production Firebase config
- [ ] Set `API_BASE_URL` to production backend URL
- [ ] Configure CORS on backend for Vercel domain
- [ ] Enable Firebase Storage for image uploads
- [ ] Add collectors to `collector_whitelist` table
- [ ] Test all authentication flows
- [ ] Test map functionality on mobile
- [ ] Verify WebSocket connections work

## Troubleshooting

**Firebase Authentication Error**
- Verify Firebase config in `js/config.js`
- Check Firebase Console → Authentication is enabled
- Add Vercel domain to Authorized Domains

**Backend Connection Failed**
- Check `API_BASE_URL` in `js/config.js`
- Verify backend CORS configuration
- Check browser console for errors

**Map Not Loading**
- Check internet connection (OSM tiles need network)
- Verify Leaflet.js CDN is accessible

**Location Not Working**
- Enable location services in browser
- Use HTTPS (required for geolocation API)

## Next Steps

- Add marketplace redemption UI
- Implement advanced filtering for collectors
- Add user profile editing
- Create admin panel for authority dashboard
- Add data export features
