# Green Coin Backend - API Examples

Complete examples for testing the backend with curl or Postman.

## Authentication

All API calls (except `/health`) require Firebase ID token in Authorization header:

```
Authorization: Bearer {FIREBASE_ID_TOKEN}
```

## 1. Health Check (Public)

```bash
curl http://localhost:8080/health
```

**Response:**
```json
{
  "status": "UP",
  "service": "greencoin-backend"
}
```

---

## 2. Register User (First Login)

After Firebase authentication, call this to create user record:

```bash
curl -X POST http://localhost:8080/api/user/register?displayName=John%20Doe \
  -H "Authorization: Bearer YOUR_FIREBASE_ID_TOKEN"
```

**Response:**
```json
{
  "id": 1,
  "firebaseUid": "firebase-uid-123",
  "email": "user@example.com",
  "displayName": "John Doe",
  "role": "CITIZEN",
  "coinBalance": 0,
  "profileImageUrl": null
}
```

---

## 3. Get User Profile

```bash
curl http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer YOUR_FIREBASE_ID_TOKEN"
```

---

## 4. Create Waste Report (Citizen)

```bash
curl -X POST http://localhost:8080/api/reports \
  -H "Authorization: Bearer YOUR_FIREBASE_ID_TOKEN" \
  -H "Content-Type: application/json" \
  -d '{
    "latitude": 12.9716,
    "longitude": 77.5946,
    "imageUrl": "https://storage.googleapis.com/bucket/image.jpg",
    "description": "Broken glass near park entrance"
  }'
```

**Response:**
```json
{
  "id": 1,
  "reporterId": 1,
  "reporterName": "John Doe",
  "latitude": 12.9716,
  "longitude": 77.5946,
  "imageUrl": "https://storage.googleapis.com/bucket/image.jpg",
  "description": "Broken glass near park entrance",
  "status": "OPEN",
  "coinsAwarded": 50,
  "collectorId": null,
  "collectorName": null,
  "reportedAt": "2026-02-05T20:00:00",
  "collectedAt": null
}
```

---

## 5. Get Available Reports (Collector)

```bash
curl http://localhost:8080/api/reports/available \
  -H "Authorization: Bearer COLLECTOR_FIREBASE_ID_TOKEN"
```

---

## 6. Get Nearby Reports

```bash
curl "http://localhost:8080/api/reports/nearby?latitude=12.9716&longitude=77.5946" \
  -H "Authorization: Bearer YOUR_FIREBASE_ID_TOKEN"
```

---

## 7. Mark Report as PICKING (Collector)

```bash
curl -X PATCH http://localhost:8080/api/reports/1/pick \
  -H "Authorization: Bearer COLLECTOR_FIREBASE_ID_TOKEN"
```

**Response:** `200 OK`

---

## 8. Mark Report as COLLECTED (Collector)

This awards coins to the reporter!

```bash
curl -X PATCH http://localhost:8080/api/reports/1/collect \
  -H "Authorization: Bearer COLLECTOR_FIREBASE_ID_TOKEN"
```

**Response:** `200 OK`

**Side Effect:** Reporter receives 50 coins, WebSocket notification sent

---

## 9. Get My Submitted Reports (Citizen)

```bash
curl http://localhost:8080/api/reports/my-reports \
  -H "Authorization: Bearer YOUR_FIREBASE_ID_TOKEN"
```

---

## 10. Get My Pickups (Collector)

```bash
curl http://localhost:8080/api/reports/my-pickups \
  -H "Authorization: Bearer COLLECTOR_FIREBASE_ID_TOKEN"
```

---

## 11. Get Coin Balance

```bash
curl http://localhost:8080/api/coins/balance \
  -H "Authorization: Bearer YOUR_FIREBASE_ID_TOKEN"
```

**Response:**
```json
{
  "balance": 150,
  "userId": 1
}
```

---

## 12. Get Transaction History

```bash
curl http://localhost:8080/api/coins/transactions \
  -H "Authorization: Bearer YOUR_FIREBASE_ID_TOKEN"
```

**Response:**
```json
[
  {
    "id": 1,
    "amount": 50,
    "transactionType": "EARNED",
    "referenceId": 1,
    "referenceType": "waste_report",
    "createdAt": "2026-02-05T20:15:00"
  },
  {
    "id": 2,
    "amount": -30,
    "transactionType": "REDEEMED",
    "referenceId": 5,
    "referenceType": "marketplace_item",
    "createdAt": "2026-02-05T20:30:00"
  }
]
```

---

## WebSocket Testing

### Using JavaScript Client

```javascript
const socket = new SockJS('http://localhost:8080/ws');
const stompClient = Stomp.over(socket);

stompClient.connect({}, function(frame) {
  console.log('Connected: ' + frame);
  
  // Subscribe to new reports
  stompClient.subscribe('/topic/reports/new', function(message) {
    console.log('New report:', JSON.parse(message.body));
  });
  
  // Subscribe to personal coin notifications
  stompClient.subscribe('/user/queue/coins', function(message) {
    console.log('Coin update:', JSON.parse(message.body));
  });
});
```

---

## Error Responses

### Invalid Token
```json
{
  "timestamp": "2026-02-05T20:00:00",
  "status": 401,
  "error": "Unauthorized",
  "message": "Invalid or expired Firebase token"
}
```

### Validation Error
```json
{
  "timestamp": "2026-02-05T20:00:00",
  "status": 400,
  "error": "Validation Failed",
  "validationErrors": {
    "latitude": "Latitude is required",
    "imageUrl": "Image URL is required"
  }
}
```

### Business Logic Error
```json
{
  "timestamp": "2026-02-05T20:00:00",
  "status": 400,
  "error": "Invalid Operation",
  "message": "Only collectors can pick up reports"
}
```
