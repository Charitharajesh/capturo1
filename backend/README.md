# 📸 Capturo Backend API Engine

Welcome to the backend engine of **Capturo**, a production-grade, highly structured REST API platform designed for creator search, bookings, dynamic messaging, and payment processing. 

Built with **FastAPI**, **SQLAlchemy 2.0**, and **Alembic**, this engine is designed with a strictly decoupled, highly modular layered architecture that separates business logic, database mutations, and input/output schema validation.

---

## 🛠️ Tech Stack & Key Integrations

*   **Framework**: [FastAPI](https://fastapi.tiangolo.com/) (Asynchronous, High-Performance, ASGI)
*   **Database ORM**: [SQLAlchemy 2.0](https://www.sqlalchemy.org/) (Strictly typed, decoupled session management)
*   **Database Migration**: [Alembic](https://alembic.sqlalchemy.org/) (Version-controlled database schema management)
*   **Database Driver**: [PyMySQL](https://pymysql.readthedocs.io/) (Asynchronous connection pools pointing to MySQL)
*   **Data Validation & Serialization**: [Pydantic v2](https://docs.pydantic.dev/) (Type-safe input/output models)
*   **Security & Encryption**: OAuth2 with JWT (JSON Web Tokens), `passlib` with `bcrypt` password hashing
*   **Structured Logging**: [Structlog](https://www.structlog.org/) (JSON formatted async logs)
*   **Rate Limiting**: [SlowAPI](https://github.com/laurentS/slowapi) (IP-based resource throttling)
*   **Payment Processor**: [Razorpay](https://razorpay.com/) (SDK integration for payment verification and webhooks)
*   **Push Notifications**: [Firebase Admin SDK](https://firebase.google.com/docs/admin) (Instant creator-client notification relays)
*   **Testing**: [Pytest](https://docs.pytest.org/) with `pytest-asyncio` and `HTTPX` for integration validations

---

## 📂 Layered Directory Structure

```text
backend/
├── app/
│   ├── api/             # Versioned REST Controllers (v1 Endpoints)
│   │   └── v1/          # Auth, Creators, Bookings, Messages, Payments, Reviews
│   ├── core/            # Global Settings, Security Configs & rate limits
│   ├── crud/            # Reusable CRUD utilities (DB helper queries)
│   ├── db/              # SQLAlchemy DB Engine & thread-safe Session Factory
│   ├── models/          # Declarative SQLAlchemy Database Models
│   ├── schemas/         # Decoupled Pydantic request/response schemas
│   ├── services/        # Service layer (Complex business logic & 3rd party APIs)
│   ├── utils/           # Helper scripts (e.g. file manager, token generators)
│   └── main.py          # ASGI Application entry point & Middlewares setup
├── alembic/             # Alembic database version control migrations
├── scripts/             # Admin, seeder, and housekeeping scripts
├── tests/               # Pytest integration & endpoint validation scripts
├── uploads/             # Mounted directory for persistent media storage
└── requirements.txt     # Locked production/development dependencies
```

---

## 🚀 Getting Started

### 1. Prerequisites
Ensure you have the following installed on your machine:
*   Python **3.10** or higher
*   **MySQL Server** (Running locally or hosted)

### 2. Environment Configurations
Create a copy of `.env.example` named `.env` inside the `backend` directory:
```bash
cp .env.example .env
```
Update your credentials inside `.env`:
*   **`DATABASE_URL`**: Set your MySQL database connection URI in the format: `mysql+pymysql://<user>:<password>@<host>:<port>/<db_name>`
*   **`SECRET_KEY`**: Provide a securely generated long string for JWT signing.
*   **SMTP Configuration**: Furnish details for transactional email services (e.g., Gmail App Passwords).

### 3. Create Virtual Environment & Install Dependencies
From the `backend` directory, create and activate a Python virtual environment, then install requirements:

**On Windows (PowerShell):**
```powershell
python -m venv .venv
.venv\Scripts\Activate
python -m pip install --upgrade pip
pip install -r requirements.txt
```

**On macOS/Linux:**
```bash
python3 -m venv .venv
source .venv/bin/activate
python -m pip install --upgrade pip
pip install -r requirements.txt
```

### 4. Automated / Custom CLI Setup & Run (Recommended)
Instead of running commands manually, use the premium **Capturo CLI Orchestrator Engine** (`start.py`):

*   **Initialize Everything (Migrations + Seed Data)**:
    ```bash
    python start.py setup
    ```
*   **Run Development Server**:
    ```bash
    python start.py start
    ```
*   **Check Platform Status Dashboard**:
    ```bash
    python start.py status
    ```
*   **Run Alembic Migrations Only**:
    ```bash
    python start.py migrate
    ```
*   **Seed Database Only**:
    ```bash
    python start.py seed
    ```
*   **Run Test Suite**:
    ```bash
    python start.py test
    ```

---

## 📚 API Documentation

Once the server is running, the interactive documentation is instantly accessible:
*   **Swagger UI (Interactive Playground)**: [http://localhost:8000/docs](http://localhost:8000/docs)
*   **ReDoc (Structured Specs)**: [http://localhost:8000/redoc](http://localhost:8000/redoc)

---

## 🧪 Running Integration Tests

To execute the Pytest test suite and verify all API boundaries and secure endpoints function correctly:
```bash
python -m pytest -v
```

> [!TIP]
> **Emulators and Android Testing**:
> When running the backend locally for Android Emulator verification, the emulator should point to `http://10.0.2.2:8000` (which loops back to the host machine's `localhost:8000`).
