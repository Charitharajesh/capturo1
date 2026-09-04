"""
Create every Capturo table directly in the configured database (Supabase Postgres).

Run once after pointing DATABASE_URL at Supabase:

    cd backend
    python scripts/create_supabase_tables.py

Importing app.models registers all tables on Base.metadata; create_all() then
emits Postgres-correct DDL (VARCHAR + CHECK for the non-native Enums,
CURRENT_TIMESTAMP for func.now(), etc.). Existing tables are left untouched.
Pass --drop to drop and recreate everything (DESTRUCTIVE).
"""

import sys

import app.models  # noqa: F401 — registers all models on Base.metadata
from app.core.config import settings
from app.db.session import Base, engine


def main() -> None:
    target = engine.url.render_as_string(hide_password=True)
    print(f"Target database: {target}")

    if "--drop" in sys.argv:
        print("Dropping all Capturo tables first (--drop) ...")
        Base.metadata.drop_all(bind=engine)

    Base.metadata.create_all(bind=engine)

    print("\nTables ensured:")
    for name in sorted(Base.metadata.tables):
        print(f"  - {name}")
    print("\nDone. Open the Supabase Table Editor to see them.")


if __name__ == "__main__":
    if "supabase" not in settings.DATABASE_URL and "postgres" not in settings.DATABASE_URL:
        print(f"WARNING: DATABASE_URL does not look like Postgres:\n  {settings.DATABASE_URL}")
        print("Set it in backend/.env before running this script.")
        sys.exit(1)
    main()
