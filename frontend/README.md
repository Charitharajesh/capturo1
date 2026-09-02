# 📱 Capturo Android Application

This is the official native Android client application for **Capturo**, designed for booking creators and delivering high-quality media.

---

## 🛠️ Architecture & Core Libraries

- **Architecture**: Modern MVVM (Model-View-ViewModel) design pattern with ViewBinding.
- **Dependency Injection**: Hilt (Dagger Hilt) for clean, decoupled injectables.
- **Networking**: Retrofit 2 with OkHttp for asynchronous API resource consumption.
- **Local Cache**: Room Database for offline support.
- **UI Components**:
  - Material Design 3
  - XML Layout layouts
  - ViewPager2 & TabLayout
  - SwipeRefreshLayout

---

## 📂 Directory Structure

Inside `app/src/main/java/com/capturo/app/`:
- **`data/`**: Networking APIs (`api/`), local Database definitions (`local/`), and models (`model/`).
- **`di/`**: Hilt Dependency Injection modules.
- **`ui/`**: Feature-specific screens and components:
  - `auth/`: Login, Registration, and Role Selection
  - `booking/`: Bookings management, details, and creations
  - `chat/`: Dynamic chat system between creators and attendees
  - `creatorDashboard/`: Creator analytics, pending requests, and uploads
  - `gallery/`: Media grids, portfolios, and client delivery views
  - `profile/`: Payout history, settings, and profile edits

---

## 🚀 Building & Running

### 1. Configure Backend Target
Ensure you configured the API endpoints target url inside your network layer or constants helper file:
- For local emulator testing: Target should point to host localhost redirect ip `http://10.0.2.2:8000`.
- For real device testing: Use your host computer's local IP address or target domain.

### 2. Run gradle assemble
Run compilation via the Gradle wrapper to build the debug version:
```bash
./gradlew assembleDebug
```
Or open the `frontend` folder directly in Android Studio and click the **Run** button.
