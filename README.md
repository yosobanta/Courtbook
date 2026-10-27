# COURTBOOK — Full-Stack Sports Court Discovery & Booking System
### MongoDB / NoSQL Course Project & Production Architecture

CourtBook is a complete sports-court discovery and booking platform engineered for casual players and facility administrators. Unlike typical CRUD projects, CourtBook explicitly implements and showcases advanced MongoDB and distributed systems concepts:
* **MongoDB Document-Oriented Data Modeling**: Embedded documents vs Normalized collections with explicit 16MB BSON analysis.
* **Compound Unique Indexing**: `{ courtId: 1, date: 1, startTime: 1 }` guaranteeing zero duplicate slots at the database level.
* **Geospatial Indexing & Aggregation**: GeoJSON `Point` and `2dsphere` index with `$geoNear` aggregation stage.
* **Multi-Stage Aggregation Pipelines**: Real `$group`, `$lookup`, `$unwind`, and `$project` pipelines for Revenue by Venue and Popular Sports.
* **Two-Phase Logical Slot Locking**: Soft reservation hold (`status: HELD`, `holdExpiresAt: now + 5 min`) with lazy expiration reaping.
* **Multi-Document ACID Transactions**: Native sessions with `readConcern: "snapshot"` and `writeConcern: { w: "majority", j: true }`.
* **Atomic Conditional Concurrency Control**: Prevention of double booking under simultaneous race conditions.
* **Modern Android Jetpack Compose App**: Clean MVVM architecture, Material 3 sporty theme, Coroutines, StateFlow, and Retrofit 2.

---

## 1. Quick Start Guide

### Prerequisites
- Node.js (v18+)
- MongoDB running locally (default: `mongodb://127.0.0.1:27017/courtbook`) or MongoDB Atlas URI
- Android Studio (Koala / Ladybug or later) with Android SDK 35

### Running the Backend
```bash
cd scratch/courtbook/server

# Install dependencies (already installed)
npm install

# Seed the database with demo venues, courts, 768 slots, coupons & accounts
npm run seed

# Start development server
npm run dev
# Or production build
npm run build && npm start
```
The server will start at `http://localhost:5000/api`.
Health check: `http://localhost:5000/api/health`

### Running the Automated Viva Tests
CourtBook includes automated concurrency and atomicity suites designed for live demonstration:

1. **100-User Concurrent Double-Booking Race Test**:
   ```bash
   npm run test:concurrency
   ```
   *Result*: Spawns 100 simultaneous requests for the exact same slot. Exactly 1 succeeds (HTTP 201), 99 are rejected (HTTP 409 Conflict), and exactly 1 booking exists in the database.

2. **Multi-Slot All-or-Nothing Atomicity Rollback Test**:
   ```bash
   npm run test:atomicity
   ```
   *Result*: Attempts to book 3 consecutive slots where the 3rd is pre-booked. The entire transaction rolls back, leaving slots 1 & 2 available and 0 partial bookings.

### Pre-Seeded Demo Accounts
| Role | Email | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Player** | `alex@example.com` | `Player@123` | Court discovery, slot hold, booking, cancellation |
| **Player** | `sarah@example.com` | `Player@123` | Court discovery, slot hold, booking |
| **Admin** | `admin@courtbook.com` | `Admin@123` | Dashboard KPIs, slot blocking/unblocking, aggregation analytics |

---

## 2. Android Jetpack Compose Application

The Android app resides in `scratch/courtbook/android` and can be opened directly in **Android Studio**.

### Architecture Overview
```
UI (Compose Screens)
    ▲
    │ (observes StateFlow)
ViewModels (HomeViewModel, CourtViewModel, BookingViewModel, AdminViewModel)
    ▲
    │ (calls suspend functions)
Repositories (CourtRepository, BookingRepository, AuthRepository, AdminRepository)
    ▲
    │ (HTTP REST JSON with Bearer JWT)
Retrofit 2 + OkHttp Client
    ▲
Node.js Express Server
```

### Key Screens Implemented
1. **Splash Screen**: Animated branding ("Find. Book. Play.") with automatic session token verification.
2. **Login & Register Screens**: Inputs, validation, and **One-Click Demo Fill buttons** for quick evaluator login.
3. **Home Screen**: Personalized greeting, sports categories (Badminton, Tennis, Football, Cricket, Table Tennis), promotional coupon banner, nearby courts feed.
4. **Explore / Search Screen**: Live text search, indoor/outdoor filter, price threshold filters.
5. **Court Details & Slot Selector**: Hero visual, venue details, amenities, 7-day horizontal calendar, interactive `SlotChip` grid with status indicators (Available, Held, Booked), sticky checkout bar.
6. **Booking Summary Screen**: Breakdown of selected slots, promo code validation engine (`WELCOME50` - 50% discount), itemized pricing.
7. **Mock Payment Gateway**: UPI / Card selection, simulated processing, commit with snapshot transaction.
8. **Confirmation Screen**: Celebration screen with unique reference number (e.g. `CB-20260920-F8A2`).
9. **My Bookings Screen**: Booking history cards, status pills, transactional cancellation dialog.
10. **Profile Screen**: User details, role indicator, link to Admin portal, logout.
11. **Admin Dashboard**: KPI overview cards (Users, Courts, Today's Bookings, Today's Revenue, Monthly Revenue) and recent bookings.
12. **Admin Slot Management**: Select court and date, block slots for maintenance, unblock slots.
13. **Admin Aggregation Analytics**: Visual cards for Revenue by Venue and Popular Sports aggregation pipelines.

---

## 3. Comprehensive Viva & Technical Q&A Guide

### Q1: Why not embed `slots` directly inside the `courts` document?
**Answer**:
1. **16MB BSON Document Limit**: A facility with 4 courts operating 16 hours daily produces over 23,000 slots per year. Embedding slots in the court document would exceed MongoDB's 16MB document cap.
2. **Concurrency & Document-Level Write Locking**: In MongoDB, writes lock the document being modified. If slots were embedded in the court document, two different players booking two *completely different slots* on the same court at the same second would compete for write locks on the exact same parent document, creating severe write-write conflicts.
3. **Row-Level Granularity**: Modeling each slot as an individual document allows atomic, single-document conditional updates and enables a compound unique index on `{ courtId: 1, date: 1, startTime: 1 }`.

### Q2: Why does the standard "Check-Then-Insert" approach fail in concurrent bookings?
**Answer**:
In naive code:
```javascript
// STEP 1:
const slot = await Slot.findOne({ _id: slotId, status: "AVAILABLE" });
// STEP 2:
if (slot) {
  await Booking.create({ ... });
}
```
Between Step 1 and Step 2, a race condition exists. If 100 players request the slot concurrently, all 100 requests will observe `status: "AVAILABLE"` in Step 1 before any request executes Step 2. Consequently, all 100 players create bookings, causing catastrophic double booking.

**How CourtBook Solves It**:
We use an **Atomic Conditional Update**:
```typescript
const result = await Slot.updateOne(
  {
    _id: slotId,
    $or: [
      { status: "AVAILABLE" },
      { status: "HELD", holdExpiresAt: { $lt: new Date() } }
    ]
  },
  {
    $set: { status: "HELD", heldBy: userId, holdExpiresAt: expireTime },
    $inc: { version: 1 }
  },
  { session }
);

if (result.matchedCount === 0) {
  throw new ConflictError("Slot was just booked by another player"); // HTTP 409
}
```
MongoDB's wiredTiger storage engine ensures that only ONE thread can successfully match the filter and modify the document. All other 99 threads receive `matchedCount: 0` and are rejected with HTTP 409 Conflict.

### Q3: Does MongoDB support SQL isolation levels like SERIALIZABLE?
**Answer**:
No. MongoDB does not use SQL isolation level keywords like `READ UNCOMMITTED`, `READ COMMITTED`, `REPEATABLE READ`, or `SERIALIZABLE`.
Instead, MongoDB provides isolation through **Read Concerns**, **Write Concerns**, and **Multi-Document Transactions**:
* **Read Concern `snapshot`**:
  Transactions read from a synchronized, point-in-time snapshot of committed data across the replica set. Snapshot isolation prevents dirty reads, non-repeatable reads, and phantom reads.
* **Write Concern `{ w: "majority", j: true }`**:
  Guarantees that write operations are committed to the on-disk journal (`j: true`) of a majority of voting replica set nodes before acknowledging success. This guarantees **Durability**.

### Q4: How does MongoDB's 2dsphere index and geospatial query work?
**Answer**:
CourtBook venues store coordinates using the GeoJSON standard:
```json
{
  "location": {
    "type": "Point",
    "coordinates": [77.6245, 12.9352] // [longitude, latitude]
  }
}
```
A `2dsphere` index indexes geometries on an Earth-like sphere.
We query nearby venues using the `$geoNear` aggregation stage:
```javascript
db.venues.aggregate([
  {
    $geoNear: {
      near: { type: "Point", coordinates: [lng, lat] },
      distanceField: "distanceMeters",
      maxDistance: 15000,
      spherical: true,
      query: { isActive: true }
    }
  },
  { $lookup: { from: "courts", localField: "_id", foreignField: "venueId", as: "courts" } }
]);
```
This calculates geodesic spherical distance and returns nearby venues sorted by proximity.

### Q5: Explain the Revenue by Venue aggregation pipeline used in the Admin Dashboard.
**Answer**:
```javascript
db.bookings.aggregate([
  // Stage 1: Filter only confirmed, paid transactions
  { $match: { status: "CONFIRMED", paymentStatus: "PAID" } },
  
  // Stage 2: Group by venueId and accumulate financial metrics
  {
    $group: {
      _id: "$venueId",
      totalRevenue: { $sum: "$finalAmount" },
      totalBookings: { $sum: 1 },
      averageBookingValue: { $avg: "$finalAmount" }
    }
  },
  
  // Stage 3: Left outer join with venues collection to get venue metadata
  {
    $lookup: {
      from: "venues",
      localField: "_id",
      foreignField: "_id",
      as: "venue"
    }
  },
  
  // Stage 4: Flatten the single-element venue array
  { $unwind: "$venue" },
  
  // Stage 5: Reshape and format final output
  {
    $project: {
      _id: 0,
      venueId: "$_id",
      venueName: "$venue.name",
      city: "$venue.city",
      totalRevenue: 1,
      totalBookings: 1,
      averageBookingValue: { $round: ["$averageBookingValue", 2] }
    }
  },
  
  // Stage 6: Sort descending by total revenue
  { $sort: { totalRevenue: -1 } }
]);
```

---

## 4. REST API Endpoint Directory

### Authentication (`/api/auth`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/auth/register` | Register new player or admin |
| `POST` | `/api/auth/login` | Authenticate user & return JWT |
| `GET` | `/api/auth/me` | Fetch currently authenticated user profile |

### Venues & Courts (`/api/venues`, `/api/courts`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/venues` | List active venues with city/search filters |
| `GET` | `/api/venues/:id` | Get venue details and associated courts |
| `GET` | `/api/courts` | Discover courts with sport, price, indoor filters |
| `GET` | `/api/courts/:id` | Get court details |
| `GET` | `/api/courts/nearby` | **Geospatial query** (`$geoNear`) for nearby venues |
| `GET` | `/api/courts/:id/slots` | Fetch time slots for a court on `?date=YYYY-MM-DD` |
| `POST` | `/api/slots/hold` | Acquire temporary 5-minute logical hold |

### Bookings (`/api/bookings`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `POST` | `/api/bookings` | **ACID Multi-Document Booking Transaction** |
| `GET` | `/api/bookings/my` | Player booking history |
| `GET` | `/api/bookings/:id` | Detailed booking information |
| `POST` | `/api/bookings/:id/cancel` | Transactional cancellation & slot release |

### Offers (`/api/offers`)
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/offers/active` | List active promotional coupons |
| `POST` | `/api/offers/validate` | Validate promo code eligibility and calculate discount |

### Admin & Analytics (`/api/admin`) *(Protected by `role: 'ADMIN'`)*
| Method | Endpoint | Description |
| :--- | :--- | :--- |
| `GET` | `/api/admin/dashboard` | KPI summaries (Users, Venues, Courts, Revenue) |
| `GET` | `/api/admin/bookings` | Filterable list of all platform bookings |
| `POST` | `/api/admin/slots/block` | Manually block slots for maintenance |
| `POST` | `/api/admin/slots/unblock` | Unblock slots back to AVAILABLE |
| `GET` | `/api/admin/analytics/revenue` | **Aggregation Pipeline**: Revenue by Venue |
| `GET` | `/api/admin/analytics/popular-sports` | **Aggregation Pipeline**: Popular Sports breakdown |
| `GET` | `/api/admin/users` | Manage player accounts |
