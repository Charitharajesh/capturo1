# 📸 Capturo Platform

Welcome to **Capturo**, a premium, production-grade service booking and media delivery platform designed for creators (photographers, videographers) and clients. Capturo simplifies the booking workflow, payment handling, and photo/video delivery in one integrated system.

---

## 📂 Project Repository Structure

This repository is split into three main modules:

1. **`database/`**: Contains the SQL schema definitions (`schema.sql`) for initialization.
2. **`backend/`**: A production-grade REST API backend built using **FastAPI**, **SQLAlchemy 2.0**, and **Alembic** database migration version control.
3. **`frontend/`**: A native Android client application built using **Kotlin**, following modern **MVVM architecture**, **Jetpack Navigation**, and **Material Design 3**.

---

## 🛠️ Tech Stack Overview

### Backend Engine
- **Framework**: FastAPI (Asynchronous ASGI server)
- **Database ORM**: SQLAlchemy 2.0 & PyMySQL
- **Schema Management**: Alembic
- **Integrations**: 
  - **Razorpay**: Safe checkout & signature verification
  - **Firebase Admin**: Realtime push notification relays
  - **ReportLab**: Dynamic PDF invoice generation
- **Rate Limiting**: SlowAPI

### Frontend Android Client
- **Language**: Kotlin
- **Architecture**: MVVM (Model-View-ViewModel) & Clean Architecture
- **Dependency Injection**: Hilt / Dagger
- **Networking**: Retrofit2 & OkHttp
- **Local DB**: Room Database
- **UI Components**: XML Layouts, Material Design 3, ViewBinding, TabLayout, ViewPager2

---

## 🚀 Getting Started

To spin up the platform locally, follow these steps in order:

### 1. Database Setup
1. Spin up a **MySQL 8.x** instance.
2. Run the SQL statements inside [schema.sql](file:///e:/Capturo/database/schema.sql) to initialize the database:
   ```sql
   SOURCE database/schema.sql;
   ```

### 2. Backend Setup
1. Navigate to the `backend/` directory:
   ```bash
   cd backend
   ```
2. Create your local virtual environment:
   ```bash
   python -m venv .venv
   source .venv/bin/activate  # Or .venv\Scripts\Activate on Windows
   ```
3. Install dependencies:
   ```bash
   pip install -r requirements.txt
   ```
4. Setup environment configurations (`.env`):
   ```bash
   cp .env.example .env
   ```
   *Edit `.env` to supply database connection details, secret keys, and email details.*
5. Bootstrap/run the backend service:
   ```bash
   python start.py setup
   python start.py start
   ```

### 3. Frontend Android App
1. Open the [frontend](file:///e:/Capturo/frontend) directory in Android Studio.
2. Android Gradle dependencies will sync automatically.
3. Build and launch the app in an emulator or real device.
   *Ensure the base URL in local networking configuration is pointed to `http://10.0.2.2:8000` when running inside the Android Emulator.*
