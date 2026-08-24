# 💸 SplitMate — Open Source Group Expense Tracker & Bill Splitter App

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/UI-Jetpack%20Compose-4285F4?style=for-the-badge&logo=jetpackcompose&logoColor=white" alt="Jetpack Compose" />
  <img src="https://img.shields.io/badge/Architecture-Clean%20Architecture-FF6F00?style=for-the-badge" alt="Clean Architecture" />
  <img src="https://img.shields.io/badge/Database-Room%20%2B%20Firebase-FFCA28?style=for-the-badge&logo=firebase&logoColor=black" alt="Room + Firebase" />
  <img src="https://img.shields.io/badge/License-MIT-blue.svg?style=for-the-badge" alt="License" />
</p>

<p align="center">
  <b>SplitMate</b> is a powerful, offline-first open-source <b>Android Group Expense Tracker and Bill Splitter app</b> built with <b>Kotlin</b> and <b>Jetpack Compose</b>. Designed as a modern, privacy-focused alternative to Splitwise, SplitMate helps users split group bills, track shared expenses, simplify group debts with optimal settlement algorithms, and analyze spending habits with interactive charts.
</p>

<p align="center">
  <a href="#-key-features">Key Features</a> •
  <a href="#-app-preview">Screenshots</a> •
  <a href="#-tech-stack--architecture">Tech Stack</a> •
  <a href="#-debt-simplification-algorithm">Debt Simplification</a> •
  <a href="#-quick-start--installation">Getting Started</a> •
  <a href="#-seo--repository-optimization">SEO & Topics</a>
</p>

---

## ✨ Key Features

- **👥 Smart Group Bill Splitting**: Create custom groups for Trips 🌴, Housemates 🏠, Couples 💑, Projects 💻, or Events 🎉.
- **⚖️ Multiple Split Methods**: Split bills equally, by percentage (%), by exact amounts, or by custom shares.
- **🧮 Intelligent Debt Simplification**: Built-in greedy debt graph algorithm minimizes total transactions needed to settle up between group members.
- **🌐 Offline-First & Realtime Cloud Sync**: Powered by Room Database for fast local storage with seamless background Firebase Realtime DB synchronization.
- **📲 QR Code Invite & Camera Scanner**: Easily share and join groups instantly using dynamic QR codes generated via ZXing & CameraX.
- **📊 Spending Analytics & Visual Reports**: Track group and personal budget breakdowns with interactive charts and category-wise spending summaries.
- **📂 CSV Export & Data Backup**: Export complete expense logs and settlement histories to CSV files for accounting and record-keeping.
- **🎨 Modern Material 3 UI**: Full support for Dark Mode, smooth Compose micro-animations, dynamic themes, and edge-to-edge layout.

---

## 📸 App Preview

| Dashboard & Summary | Group Expense List | Add Expense | Debt Settlement | Reports & Analytics |
|:---:|:---:|:---:|:---:|:---:|
| <img src="https://raw.githubusercontent.com/AsimNadeem213/expense_tracker/main/docs/screenshots/dashboard.png" width="180" alt="SplitMate Dashboard"/> | <img src="https://raw.githubusercontent.com/AsimNadeem213/expense_tracker/main/docs/screenshots/group_detail.png" width="180" alt="Group Detail Screen"/> | <img src="https://raw.githubusercontent.com/AsimNadeem213/expense_tracker/main/docs/screenshots/add_expense.png" width="180" alt="Add Expense Screen"/> | <img src="https://raw.githubusercontent.com/AsimNadeem213/expense_tracker/main/docs/screenshots/balances.png" width="180" alt="Debt Settlement Screen"/> | <img src="https://raw.githubusercontent.com/AsimNadeem213/expense_tracker/main/docs/screenshots/reports.png" width="180" alt="Spending Reports Screen"/> |

*(Note: Replace preview URLs with actual screenshots after uploading to your repository)*

---

## 🛠️ Tech Stack & Architecture

SplitMate follows **Android Clean Architecture** guidelines (Presentation, Domain, Data) and adheres to modern Android app development best practices.

### Modern Android Tech Stack
- **Language**: [Kotlin](https://kotlinlang.org/) (100% Kotlin codebase)
- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **Architecture**: Clean Architecture (Feature-by-Package) + ViewModel + Unidirectional Data Flow (StateFlow)
- **Dependency Injection**: [Koin](https://insert-koin.io/) for lightweight dependency injection
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite ORM with KSP)
- **Backend & Auth**: [Firebase Auth](https://firebase.google.com/docs/auth) & [Firebase Realtime Database](https://firebase.google.com/docs/database)
- **Asynchronous Processing**: Kotlin Coroutines & Flow
- **Background Jobs**: AndroidX WorkManager
- **Image Loading**: Coil for Compose
- **QR Code Engine**: ZXing Core & AndroidX CameraX

### Clean Architecture Blueprint
```
app/src/main/java/com/asim/splitmate/
├── core/             # Base components, database, navigation, theme & utilities
│   ├── database/     # Room Database configuration
│   ├── firebase/     # Realtime Database data source
│   ├── navigation/   # Jetpack Compose Navigation routes
│   └── utils/        # Debt Simplification graph algorithm & CSV exporter
├── data/             # Data layer: DAOs, entities, & repository implementations
├── domain/           # Domain layer: Entities, models, interfaces, & Use Cases
├── di/               # Koin Dependency Injection modules
└── feature/          # UI Layer: Feature-based Jetpack Compose screens & ViewModels
    ├── auth/         # Login, signup & Firebase Auth state
    ├── balances/     # Settle up & net balance visualization
    ├── dashboard/    # Main activity overview & recent activity
    ├── expenses/     # Add expense, edit, category picker
    ├── groups/       # Group creation, details, member management
    ├── reports/      # Category-wise charts & spending breakdown
    └── qr/           # QR code generation & CameraX scanner
```

---

## 🧮 Debt Simplification Algorithm

One of SplitMate's core algorithmic strengths is its **Greedy Debt Graph Simplifier** (`DebtSimplifier.kt`). 

When multiple members make payments for a shared trip or group, traditional logs result in messy, circular transactions (e.g., A owes B, B owes C, C owes A). SplitMate calculates the **net balance** for every user and applies a max-heap greedy balancing algorithm to resolve debts with the **minimum possible number of transactions**.

### Example Scenario
Without debt simplification:
- **Alex** owes **Bob** \$20
- **Bob** owes **Charlie** \$20
- **Charlie** owes **Alex** \$5
- **David** owes **Bob** \$15

**SplitMate Simplified Result:**
- **Alex** owes **Charlie** \$15
- **David** owes **Bob** \$15

*(Reduces 4 confusing transactions down to just 2 simple settlements!)*

---

## 🚀 Quick Start & Installation Guide

### Prerequisites
- **Android Studio**: Ladybug (2024.2.1+) or newer
- **JDK**: Java 17
- **Min SDK**: API Level 24 (Android 7.0 Nougat)
- **Target SDK**: API Level 36 (Android 15+)

### 1. Clone the Repository
```bash
git clone https://github.com/AsimNadeem213/expense_tracker.git
cd expense_tracker
```

### 2. Configure Firebase (Optional for Cloud Sync)
1. Create a project on the [Firebase Console](https://console.firebase.google.com/).
2. Add an Android app with the package name `com.asim.splitmate`.
3. Download `google-services.json` and place it in the `app/` folder.
4. Enable **Email/Password Authentication** and **Realtime Database** in your Firebase console.

*(Note: The app works fully offline out-of-the-box using local Room Database even without Firebase configured).*

### 3. Build & Run
Open the project in **Android Studio**, sync Gradle, and run the app on an Android Emulator or physical device:
```bash
./gradlew assembleDebug
```

---

## 🔍 SEO & Repository Optimization

To make your GitHub repository rank on the **first page** of GitHub Search and search engines like Google for keywords such as **"android expense tracker"**, **"splitwise alternative"**, and **"jetpack compose bill splitter"**, configure the following settings on your GitHub repository page:

### 📌 Recommended Repository Description
> 💸 Open source Android Group Expense Tracker & Bill Splitter app built with Kotlin, Jetpack Compose, Room, and Firebase. Features smart debt simplification, QR code sharing, and spending reports.

### 🏷️ Recommended GitHub Topics (Add these in Repo Settings -> About -> Topics)
```text
expense-tracker  bill-splitter  splitwise-alternative  jetpack-compose  kotlin-android  
android-app  clean-architecture  room-database  firebase-realtime-database  koin  
money-management  debt-simplification  android-studio  open-source
```

### 💡 Search Keywords Targeted
- Android Expense Tracker App
- Open Source Splitwise Clone / Alternative
- Jetpack Compose Expense Tracker Sample App
- Android Bill Splitter Kotlin
- Debt Simplification Algorithm Kotlin
- Clean Architecture Jetpack Compose Android
- Offline First Android App Room Firebase

---

## 🤝 Contributing

Contributions, feature requests, and bug reports are welcome! 

1. **Fork** the Repository
2. **Create** your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. **Commit** your Changes (`git commit -m 'Add some AmazingFeature'`)
4. **Push** to the Branch (`git push origin feature/AmazingFeature`)
5. Open a **Pull Request**

---

## 📜 License

Distributed under the **MIT License**. See `LICENSE` for more information.

---

<p align="center">
  Crafted with ❤️ by <a href="https://github.com/AsimNadeem213">Asim Nadeem</a>
</p>
