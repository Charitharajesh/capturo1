"""
One-off helper: rebuild the SQLite schema directly from the SQLAlchemy models.

The bundled Alembic migrations hardcoded `now()` as a column server default,
which is a MySQL function that SQLite does not understand (it raises
"unknown function: now()" on INSERT). Importing app.models registers every
table on Base.metadata; create_all() then emits SQLite-correct defaults
(CURRENT_TIMESTAMP) for func.now().
"""

import app.models  # noqa: F401 — registers all models on Base.metadata
from app.db.session import Base, engine

Base.metadata.drop_all(bind=engine)
Base.metadata.create_all(bind=engine)

print("Rebuilt tables:")
for name in Base.metadata.tables:
    print(f"  - {name}")
