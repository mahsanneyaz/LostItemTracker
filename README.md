<div align="center">
  <img src="app/src/main/res/drawable/lit_logo.png" alt="LiT Logo" width="120"/>
  <h1>Lost Item Tracker (LiT)</h1>
  <p>A mobile-based Android application for centralised lost and found management on college campuses.</p>

  <img src="https://img.shields.io/badge/Platform-Android-brightgreen?style=flat-square&logo=android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-purple?style=flat-square&logo=kotlin" />
  <img src="https://img.shields.io/badge/Min%20SDK-API%2026-blue?style=flat-square" />
  <img src="https://img.shields.io/badge/Version-1.0-orange?style=flat-square" />
  <img src="https://img.shields.io/badge/Backend-Firebase-yellow?style=flat-square&logo=firebase" />
</div>

---

## Download

<div align="center">
  <a href="https://github.com/mahsanneyaz/LostItemTracker/releases/download/v1.0/LostItemTracker_v7.apk">
    <img src="https://img.shields.io/badge/⬇%20Download%20APK-v1.0-2196F3?style=for-the-badge" alt="Download APK" />
  </a>
</div>

> **How to install:**
> 1. Download the APK file on your Android device
> 2. Go to **Settings → Security** (or **Install unknown apps**) and enable installation from unknown sources
> 3. Open the downloaded APK file and tap **Install**
> 4. Requires **Android 8.0 (API 26)** or higher

---

## About the Project

**Lost Item Tracker (LiT)** is a BCA Live Project. It solves a common campus problem — students and staff lose personal belongings with no reliable, centralised way to report or search for them. Traditional methods like notice boards and WhatsApp groups are slow and disorganised.

LiT provides a single platform where:
- Anyone can **report a lost or found item** with photos and details
- An **automated matching engine** compares every report and scores similarity out of 100
- Users are **alerted when a match is found** and can contact each other directly
- **Admins** can moderate reports, verify items, and view campus-wide analytics

---


## Features

| Feature | Description |
|---|---|
| Splash Screen | Animated logo, checks login status, routes by role |
| Login & Register | Firebase Auth, email validation, internet check |
| Forgot Password | Firebase password reset email |
| Dashboard | My Reports list, action buttons, match and search access |
| Report Lost/Found | Category dropdown, date picker, camera + gallery upload |
| Auto Matching Engine | Similarity score 0–100 across category, name, location, date |
| Matches Screen | View all matches, confirm/reject, contact other user |
| Search Screen | Filter by keyword, category, and Lost/Found type |
| Profile | Edit name and phone, logout, delete account |
| Admin Login | Role-verified separate login |
| Admin Dashboard | Analytics cards, all reports tabs, users list |
| Admin Reports | Mark as returned, delete fake/spam reports |
| Push Notifications | FCM token management, foreground notification display |

---

## Matching Engine

The matching engine automatically runs after every new report is submitted. It compares a lost item against all open found items (and vice versa) using four criteria:

| Criteria | Points |
|---|---|
| Category match (exact) | 40 |
| Name similarity (token overlap) | up to 30 |
| Location match (keyword contains) | 20 |
| Date within 3 days | 10 |
| **Total** | **100** |

Any pair scoring **50 or above** is saved as a match in Firestore and shown to both users.

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| IDE | Android Studio |
| Architecture | MVVM (ViewModel + LiveData + Repository) |
| UI | XML Views + Material Design 3 |
| Authentication | Firebase Authentication |
| Database | Firebase Firestore (NoSQL) |
| Image Storage | Cloudinary (free tier, 25 GB) |
| Push Notifications | Firebase Cloud Messaging (FCM) |
| Image Loading | Glide |
| HTTP Networking | OkHttp |
| Async | Kotlin Coroutines |

---

## Project Structure

```
app/src/main/java/com/srm/lostitemtracker/
│
├── SplashActivity.kt              # Launch screen with animations + role routing
├── MainActivity.kt                # Entry point redirect
│
├── LoginActivity.kt               # Login with validation + FCM token save
├── RegisterActivity.kt            # Registration + Firestore user document
├── ForgotPasswordActivity.kt      # Firebase password reset email
│
├── DashboardActivity.kt           # Main user screen + ReportsAdapter
├── DashboardViewModel.kt          # Loads user name and report list
│
├── ReportActivity.kt              # Lost + Found report form (camera/gallery)
├── ReportViewModel.kt             # Calls ItemRepository for submit
│
├── SearchActivity.kt              # Search + filter all campus reports
├── MatchesActivity.kt             # User matches with confirm/reject/contact
│
├── ProfileActivity.kt             # Edit profile, logout, delete account
│
├── AdminLoginActivity.kt          # Role-verified admin login
├── AdminDashboardActivity.kt      # Analytics + ViewPager2 tabs + bottom nav
├── AdminItemsFragment.kt          # Manage all lost/found reports
├── AdminItemsAdapter.kt           # RecyclerView adapter for admin items
├── AdminUsersFragment.kt          # View all registered users
│
├── ItemRepository.kt              # Firestore CRUD + matching engine trigger
├── AuthRepository.kt              # Firebase Auth operations
├── AuthViewModel.kt               # Auth LiveData
│
├── MatchingEngine.kt              # Similarity scoring algorithm
├── CloudinaryManager.kt           # Photo upload via OkHttp
├── MyFirebaseMessagingService.kt  # FCM token refresh + notification display
│
├── LostItem.kt                    # Data class
├── FoundItem.kt                   # Data class
├── Match.kt                       # Data class
```

---

## Firestore Database Structure

```
users/{uid}
  name, email, phone, role, fcmToken, createdAt

lost_items/{docId}
  id, userId, itemName, category, description,
  location, date, photoUrl, status, createdAt

found_items/{docId}
  (same fields as lost_items)

matches/{lostId_foundId}
  id, lostItemId, foundItemId, lostUserId,
  foundUserId, score, status, createdAt
```

---

## Setup (For Developers)

If you want to build and run this project yourself:

1. **Clone the repository**
   ```bash
   git clone https://github.com/mahsanneyaz/LostItemTracker.git
   ```

2. **Create a Firebase project** at [console.firebase.google.com](https://console.firebase.google.com)
   - Enable Authentication (Email/Password)
   - Create a Firestore database
   - Enable Cloud Messaging
   - Download `google-services.json` and place it in `app/`

3. **Create a Cloudinary account** at [cloudinary.com](https://cloudinary.com)
   - Create an unsigned upload preset named `lost_item_tracker`
   - Note your Cloud Name

4. **Update `CloudinaryManager.kt`** with your Cloud Name:
   ```kotlin
   private const val CLOUD_NAME = "your_cloud_name_here"
   ```

5. **Sync Gradle** and run the app

> **Note:** The `google-services.json` file is not included in this repository for security reasons. You must create your own Firebase project.

---

## Team

| Name | Roll Number |
|---|---|
| [Mohammad Ahsan Neyaz](https://github.com/mahsanneyaz) | 42224210112 |
| [Harshit](https://github.com/KAUTS1610) | 42224210105 |
| Tanish | 42224210109 |

**Guide:** Mr. Arul Paul
**Course:** Bachelor of Computer Applications (BCA), Semester 4

---

## License

This project was built as a college live project for academic purposes.

---