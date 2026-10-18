# GEVINI Restaurant OS — Architecture & Production Guide
**Location**: Malir 15, Main National Hwy, Karachi, Pakistan  
**Stack**: Native Android (Kotlin + Jetpack Compose + Material 3), Web Client (HTML5 / Tailwind / Modular Firebase SDK), Centralized Cloud Firestore Backend.

---

## 1. System Overview & Unified Architecture

GEVINI Restaurant OS centralizes all dining and ordering operations across Malir 15 under a single, real-time Cloud Firestore database:
- **POS Terminal (Android)**: Touchscreen-optimized cashier station with category tabs, modifier dialogs (crusts, extra cheese, toppings), decimal PKR arithmetic, bill splitting, cash drawer change calculator, and receipt generator.
- **Kitchen Display System (KDS)**: Live kitchen ticket rail with station routing (Pizza Oven, Grill, Fryer, Beverage), real-time elapsed timer, and status transitions (`pending` → `preparing` → `ready` → `completed`).
- **Owner & Analytics Dashboard**: Gross and net sales, average order value (AOV), sales by channel (Dine-in, Takeaway, Delivery, Online), and ingredient inventory alerts.
- **Table & Floor Management**: Real-time table occupancy map (Available, Occupied, Reserved, Cleaning) with order linking.
- **Customer Online Ordering (Android & Web)**: Real-time menu browsing, cart checkout with Malir/Karachi delivery details, and live order status tracker.

---

## 2. Cloud Firestore Database Models & Paths

Named Database ID: `ai-studio-android-gevinire-57524d97-a127-49eb-b1c8-d7ae66681638`

### Collections:
1. `/users/{userId}`: User profiles with roles (`super_admin`, `manager`, `cashier`, `kitchen`, `waiter`, `delivery`, `customer`).
2. `/menuCategories/{categoryId}`: Menu sections (`Pizzas`, `Burgers & Fast Food`, `Appetizers & Sides`, `Drinks & Beverages`).
3. `/products/{productId}`: Food items with base PKR price, sizes (Small, Regular, Large, Jumbo), modifiers (Extra cheese, Stuffed crust, Olives, Dips), and kitchen station routing.
4. `/orders/{orderId}`: Multi-channel orders containing order items, subtotal, tax, delivery fee, total, order type (`dine_in`, `takeaway`, `delivery`, `online`), status (`pending`, `preparing`, `ready`, `completed`, `cancelled`), and payment records.
5. `/tables/{tableId}`: Dine-in table status (`available`, `occupied`, `reserved`, `cleaning`) and active order ID.
6. `/ingredients/{ingredientId}`: Raw material stock tracking in kg, liters, or units with minimum safety stock triggers.
7. `/cashShifts/{shiftId}`: Till opening and closing reconciliation records.

---

## 3. Security Rules & Access Control

Configured in `firestore.rules` and deployed to the live backend:
- Public read access for menu categories and products so customer web and mobile clients can view items without friction.
- Authenticated write access for creating orders, validating structure and decimal positive numbers.
- Zero-trust field-level validation (`isValidOrder`, `isValidProduct`, `isValidUser`, `isValidTable`).
- Orders cannot be deleted; cancellations update status to `cancelled` for audit logs and historical accounting.

---

## 4. Setup & Running Instructions

### Android App:
1. Open the project in Android Studio (Ladybug or newer).
2. Ensure JDK 17 or JDK 21 is selected in Gradle settings.
3. Build the project:
   ```bash
   gradle assembleDebug
   ```
4. Run tests:
   ```bash
   gradle :app:testDebugUnitTest
   ```
5. Install and launch on device or emulator:
   ```bash
   gradle installDebug
   ```

### Web Customer Ordering Portal:
1. Open `/web/index.html` in any modern browser or host via Firebase Hosting, Vercel, or Netlify:
   ```bash
   # Quick local testing
   npx serve web
   ```
2. The web portal connects directly to the same Firestore database and pushes online delivery orders directly to the kitchen KDS and POS!

---

## 5. Phase Implementation Checklist

- [x] **Phase 1: Foundation & Omnichannel Ordering**
  - [x] Native Kotlin Jetpack Compose application with MVVM architecture
  - [x] Provisioned Cloud Firestore database and verified SHA-1/SHA-256 fingerprints
  - [x] Zero-trust `firestore.rules` deployed to backend
  - [x] Custom adaptive branding icon and theme (Charcoal, Cream, Deep Burgundy `#8B1E1E`, Gold)
  - [x] Authentic GEVINI Malir 15 menu seeding (Crown Crust Pizza, Malir Special, Zinger Burgers, Pakola)
  - [x] POS Terminal with customization dialog, cart, and change calculator
  - [x] Kitchen Display System (KDS) with live elapsed timer and station routing
  - [x] Owner Dashboard with PKR revenue analytics and channel breakdown
  - [x] Floor & Dining Table Management (T-1 to T-8)
  - [x] Customer Online Ordering module (Android)
  - [x] Customer Web Ordering portal (`/web/index.html`) with real-time Firestore sync
  - [x] Automated unit test suite passing in Gradle
- [ ] **Phase 2: Advanced Operations**
  - [ ] Automatic BOM ingredient deduction upon order completion
  - [ ] Multi-branch stock transfers
  - [ ] Cashier drawer physical cash reconciliation receipts
- [ ] **Phase 3: Financial Systems**
  - [ ] Double-entry general ledger journal entries
  - [ ] Trial balance, P&L, and balance sheet exports
