# Green Coin Backend

Spring Boot backend for the Green Coin civic waste reporting platform.

## Tech Stack

- **Framework**: Spring Boot 3.x
- **Database**: PostgreSQL
- **Authentication**: Firebase Admin SDK (JWT verification)
- **Real-time**: WebSocket (STOMP)
- **Security**: Spring Security with role-based access

## Prerequisites

1. **Java 17+**
2. **Maven 3.6+**
3. **PostgreSQL 14+**
4. **Firebase Project** (with service account key)

## Quick Start

### 1. Database Setup

```bash
# Create database
psql -U postgres
CREATE DATABASE greencoin;
\q

# Run schema
psql -U postgres -d greencoin -f ../database/schema.sql
```

### 2. Firebase Configuration

Download your Firebase service account key:
1. Go to Firebase Console → Project Settings → Service Accounts
2. Click "Generate New Private Key"
3. Save as `firebase-admin-key.json` in backend root directory

### 3. Environment Variables

Create `.env` file or set environment variables:

```env
DB_PASSWORD=your_postgres_password
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CREDENTIALS_PATH=./firebase-admin-key.json
ALLOWED_ORIGINS=http://localhost:3000,https://your-vercel-domain.vercel.app
```

### 4. Run Backend

```bash
# Install dependencies
mvn clean install

# Run application
mvn spring-boot:run
```

Backend runs on `http://localhost:8080`

## Project Structure

```
backend/
├── src/main/java/com/greencoin/
│   ├── config/              # Configuration classes
│   │   ├── FirebaseConfig.java
│   │   ├── SecurityConfig.java
│   │   └── WebSocketConfig.java
│   ├── controller/          # REST API endpoints
│   │   ├── UserController.java
│   │   ├── WasteReportController.java
│   │   ├── CoinController.java
│   │   └── HealthController.java
│   ├── dto/                 # Data Transfer Objects
│   │   ├── UserProfileResponse.java
│   │   ├── CreateReportRequest.java
│   │   ├── WasteReportResponse.java
│   │   └── CoinTransactionResponse.java
│   ├── exception/           # Error handling
│   │   └── GlobalExceptionHandler.java
│   ├── model/               # JPA Entities
│   │   ├── User.java
│   │   ├── WasteReport.java
│   │   ├── CoinTransaction.java
│   │   └── CollectorWhitelist.java
│   ├── repository/          # Data access layer
│   │   ├── UserRepository.java
│   │   ├── WasteReportRepository.java
│   │   ├── CoinTransactionRepository.java
│   │   └── CollectorWhitelistRepository.java
│   ├── security/            # Authentication
│   │   └── FirebaseTokenFilter.java
│   ├── service/             # Business logic
│   │   ├── UserService.java
│   │   ├── WasteReportService.java
│   │   ├── CoinService.java
│   │   └── WebSocketService.java
│   └── GreenCoinApplication.java
└── src/main/resources/
    └── application.yml      # Configuration
```

## API Endpoints

### Authentication
- `POST /api/user/register` - Register/sync user after Firebase login
- `GET /api/user/profile` - Get current user profile

### Waste Reports (Citizens)
- `POST /api/reports` - Submit waste report
- `GET /api/reports/my-reports` - View my submitted reports

### Waste Reports (Collectors)
- `GET /api/reports/available` - List pickupable reports
- `GET /api/reports/nearby?lat={lat}&lng={lng}` - Nearby reports
- `GET /api/reports/my-pickups` - My active pickups
- `PATCH /api/reports/{id}/pick` - Mark as PICKING
- `PATCH /api/reports/{id}/collect` - Mark as COLLECTED (awards coins)

### Coins
- `GET /api/coins/balance` - Current balance
- `GET /api/coins/transactions` - Transaction history

### Health
- `GET /health` - Service status

## Authentication Flow

1. **Frontend**: User signs in with Firebase (Google/Email)
2. **Frontend**: Gets Firebase ID token
3. **Frontend**: Calls `/api/user/register` with token in `Authorization: Bearer {token}` header
4. **Backend**: `FirebaseTokenFilter` verifies token
5. **Backend**: Extracts Firebase UID and email
6. **Backend**: Creates user record if first login
7. **Backend**: Assigns role (CITIZEN or COLLECTOR based on whitelist)
8. **Backend**: Returns user profile

## WebSocket Configuration

Connect to: `ws://localhost:8080/ws`

### Subscribe Topics

- `/topic/reports/new` - New waste reports broadcast
- `/topic/reports/{reportId}/status` - Status updates
- `/user/queue/coins` - Personal coin notifications

## Database Schema

See `../database/schema.sql` for complete schema with:
- Users table with role-based access
- Waste reports with GPS coordinates
- Coin transactions audit log
- Collector whitelist
- Marketplace items and redemptions

## Security Features

✅ Firebase JWT token verification  
✅ Spring Security role-based access control  
✅ CORS configured for Vercel frontend  
✅ Stateless session management  
✅ SQL injection prevention (JPA)  
✅ Input validation  
✅ Error response sanitization

## Deployment

### Railway / Render

1. Create new PostgreSQL database
2. Set environment variables in platform
3. Deploy from GitHub repository
4. Update `ALLOWED_ORIGINS` with frontend URL

### Environment Variables (Production)

```
DATABASE_URL=postgresql://user:pass@host:5432/dbname
FIREBASE_PROJECT_ID=your-project-id
FIREBASE_CREDENTIALS_PATH=/app/firebase-admin-key.json
ALLOWED_ORIGINS=https://your-frontend.vercel.app
PORT=8080
```

## Troubleshooting

**Firebase Authentication Error**
- Verify `firebase-admin-key.json` path
- Check `FIREBASE_PROJECT_ID` matches your project

**Database Connection Failed**
- Verify PostgreSQL is running
- Check credentials in `application.yml`
- Ensure database `greencoin` exists

**CORS Issues**
- Add frontend URL to `ALLOWED_ORIGINS`
- Check `SecurityConfig.java` CORS configuration

## Next Steps

- [ ] Set up PostgreSQL database
- [ ] Configure Firebase service account
- [ ] Run database schema
- [ ] Start backend server
- [ ] Test with Postman/frontend
- [ ] Deploy to cloud hosting
