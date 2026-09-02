import sys
from sqlalchemy import text
from app.db.session import SessionLocal

def clean():
    print("=== CLEARING ALL DATA FROM DATABASE ===")
    db = SessionLocal()
    try:
        # Disable foreign key checks to allow truncating tables with relationships
        db.execute(text("SET FOREIGN_KEY_CHECKS = 0;"))
        
        # Truncate tables to completely remove all data rows
        db.execute(text("TRUNCATE TABLE notifications;"))
        db.execute(text("TRUNCATE TABLE gallery_items;"))
        db.execute(text("TRUNCATE TABLE reviews;"))
        db.execute(text("TRUNCATE TABLE messages;"))
        db.execute(text("TRUNCATE TABLE payments;"))
        db.execute(text("TRUNCATE TABLE bookings;"))
        db.execute(text("TRUNCATE TABLE creator_profiles;"))
        db.execute(text("TRUNCATE TABLE users;"))
        
        # Re-enable foreign key constraints
        db.execute(text("SET FOREIGN_KEY_CHECKS = 1;"))
        
        db.commit()
        print("Database successfully cleared of all rows!")
    except Exception as e:
        db.rollback()
        print(f"Error clearing database: {str(e)}")
        sys.exit(1)
    finally:
        db.close()

if __name__ == "__main__":
    clean()
