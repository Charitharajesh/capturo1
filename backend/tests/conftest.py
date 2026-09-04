"""
Pytest bootstrap: forces the test run onto an isolated local SQLite
database instead of the production Supabase Postgres instance configured
in .env. Must set the env var before `app.core.config.settings` (and the
SQLAlchemy engine bound to it) are imported anywhere, so this needs to run
before any `from app...` import — hence the module-level code below,
which pytest executes while collecting this conftest.py, ahead of
collecting the test modules that import the app.
"""
import os
from pathlib import Path

_TEST_DB_PATH = Path(__file__).resolve().parent / "test_run.db"

os.environ["DATABASE_URL"] = f"sqlite:///{_TEST_DB_PATH}"
os.environ["AUTO_CREATE_TABLES"] = "True"
os.environ["DEBUG"] = "False"

try:
    if _TEST_DB_PATH.exists():
        _TEST_DB_PATH.unlink()
except OSError:
    pass  # e.g. still locked from a previous interrupted run on Windows — create_all() is idempotent

import pytest


@pytest.fixture(scope="session", autouse=True)
def _cleanup_test_database():
    yield
    try:
        from app.db.session import engine
        engine.dispose()
    except Exception:
        pass
    try:
        if _TEST_DB_PATH.exists():
            _TEST_DB_PATH.unlink()
    except OSError:
        pass  # e.g. Windows file lock — harmless, next run starts with a fresh path anyway
