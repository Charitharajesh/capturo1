import sys
import os
import datetime
from decimal import Decimal

# Add backend folder to path
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from sqlalchemy.orm import Session
from app.db.session import SessionLocal, engine
from app.core.security import get_password_hash
from app.models.user import User
from app.models.creator import CreatorProfile
from app.models.booking import Booking
from app.models.payment import Payment
from app.models.message import Message
from app.models.review import Review
from app.models.gallery import GalleryItem
from app.models.notification import Notification

# Geo-located demo creators spread across Bangalore localities so the nearby
# map and search return real, well-distributed pins around the city centre
# (Sarah Connor's seeded coords: 12.9716, 77.5946).
DEMO_CREATORS = [
    {
        "full_name": "Priya Nair", "email": "priya.nair@example.com", "phone": "+919800000001",
        "profile_pic": "https://images.unsplash.com/photo-1544005313-94ddf0286df2?auto=format&fit=crop&w=150&h=150&q=80",
        "specializations": ["wedding", "portrait"], "hourly_rate": "2000.00", "minimum_hours": 3,
        "bio": "Candid wedding storyteller from Indiranagar with a soft, filmic style.",
        "years_experience": 8, "equipment": ["Canon EOS R5", "RF 50mm f/1.2 L", "RF 28-70mm f/2"],
        "availability_status": "available", "latitude": "12.9719", "longitude": "77.6412",
        "service_radius_km": 20, "is_featured": True, "avg_rating": "4.90",
        "total_reviews": 128, "total_bookings": 96, "on_time_rate": "98.50",
        "portfolio": [("https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=800&q=80",
                       "https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=300&q=80",
                       "Golden Hour Vows", "Candid couple portrait at sunset")],
    },
    {
        "full_name": "Rahul Verma", "email": "rahul.verma@example.com", "phone": "+919800000002",
        "profile_pic": "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?auto=format&fit=crop&w=150&h=150&q=80",
        "specializations": ["corporate", "product"], "hourly_rate": "1800.00", "minimum_hours": 2,
        "bio": "Corporate and product shooter based in Koramangala. Clean, commercial lighting.",
        "years_experience": 6, "equipment": ["Sony A7 IV", "FE 24-70mm f/2.8 GM II", "Godox strobes"],
        "availability_status": "available", "latitude": "12.9352", "longitude": "77.6245",
        "service_radius_km": 15, "is_featured": False, "avg_rating": "4.70",
        "total_reviews": 74, "total_bookings": 61, "on_time_rate": "96.00",
        "portfolio": [("https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=800&q=80",
                       "https://images.unsplash.com/photo-1521737604893-d14cc237f11d?auto=format&fit=crop&w=300&q=80",
                       "Boardroom Session", "Corporate team headshots")],
    },
    {
        "full_name": "Ananya Rao", "email": "ananya.rao@example.com", "phone": "+919800000003",
        "profile_pic": "https://images.unsplash.com/photo-1438761681033-6461ffad8d80?auto=format&fit=crop&w=150&h=150&q=80",
        "specializations": ["portrait", "fashion"], "hourly_rate": "1600.00", "minimum_hours": 2,
        "bio": "Fashion and portrait photographer in Jayanagar. Editorial, bold, colour-forward.",
        "years_experience": 5, "equipment": ["Nikon Z8", "Z 85mm f/1.2 S", "Profoto B10"],
        "availability_status": "busy", "latitude": "12.9250", "longitude": "77.5938",
        "service_radius_km": 12, "is_featured": True, "avg_rating": "4.80",
        "total_reviews": 91, "total_bookings": 70, "on_time_rate": "97.20",
        "portfolio": [("https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=800&q=80",
                       "https://images.unsplash.com/photo-1524504388940-b1c1722653e1?auto=format&fit=crop&w=300&q=80",
                       "Studio Editorial", "High-key fashion portrait")],
    },
    {
        "full_name": "Vikram Singh", "email": "vikram.singh@example.com", "phone": "+919800000004",
        "profile_pic": "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?auto=format&fit=crop&w=150&h=150&q=80",
        "specializations": ["event", "sports"], "hourly_rate": "1400.00", "minimum_hours": 3,
        "bio": "Fast-paced event and sports photographer covering Whitefield and east Bangalore.",
        "years_experience": 7, "equipment": ["Canon EOS R6 II", "RF 70-200mm f/2.8", "RF 100-500mm"],
        "availability_status": "available", "latitude": "12.9698", "longitude": "77.7500",
        "service_radius_km": 25, "is_featured": False, "avg_rating": "4.60",
        "total_reviews": 58, "total_bookings": 49, "on_time_rate": "94.80",
        "portfolio": [("https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=800&q=80",
                       "https://images.unsplash.com/photo-1461896836934-ffe607ba8211?auto=format&fit=crop&w=300&q=80",
                       "Match Point", "Live sports action capture")],
    },
    {
        "full_name": "Meera Iyer", "email": "meera.iyer@example.com", "phone": "+919800000005",
        "profile_pic": "https://images.unsplash.com/photo-1573496359142-b8d87734a5a2?auto=format&fit=crop&w=150&h=150&q=80",
        "specializations": ["wedding", "maternity"], "hourly_rate": "2200.00", "minimum_hours": 4,
        "bio": "Luxury wedding and maternity photographer in Malleshwaram. Timeless and elegant.",
        "years_experience": 10, "equipment": ["Sony A1", "FE 35mm f/1.4 GM", "FE 135mm f/1.8 GM"],
        "availability_status": "available", "latitude": "13.0035", "longitude": "77.5647",
        "service_radius_km": 18, "is_featured": True, "avg_rating": "5.00",
        "total_reviews": 156, "total_bookings": 132, "on_time_rate": "99.10",
        "portfolio": [("https://images.unsplash.com/photo-1606216794074-735e91aa2c92?auto=format&fit=crop&w=800&q=80",
                       "https://images.unsplash.com/photo-1606216794074-735e91aa2c92?auto=format&fit=crop&w=300&q=80",
                       "First Dance", "Elegant reception moment")],
    },
    {
        "full_name": "Arjun Kapoor", "email": "arjun.kapoor@example.com", "phone": "+919800000006",
        "profile_pic": "https://images.unsplash.com/photo-1506794778202-cad84cf45f1d?auto=format&fit=crop&w=150&h=150&q=80",
        "specializations": ["product", "food"], "hourly_rate": "1300.00", "minimum_hours": 2,
        "bio": "Product and food photographer in HSR Layout. Crisp macro and appetising light.",
        "years_experience": 4, "equipment": ["Fujifilm GFX 100S", "GF 120mm f/4 Macro"],
        "availability_status": "offline", "latitude": "12.9121", "longitude": "77.6446",
        "service_radius_km": 10, "is_featured": False, "avg_rating": "4.50",
        "total_reviews": 43, "total_bookings": 38, "on_time_rate": "95.50",
        "portfolio": [("https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=800&q=80",
                       "https://images.unsplash.com/photo-1504674900247-0877df9cc836?auto=format&fit=crop&w=300&q=80",
                       "Plated", "Restaurant menu food shoot")],
    },
    {
        "full_name": "Sneha Reddy", "email": "sneha.reddy@example.com", "phone": "+919800000007",
        "profile_pic": "https://images.unsplash.com/photo-1487412720507-e7ab37603c6f?auto=format&fit=crop&w=150&h=150&q=80",
        "specializations": ["fashion", "portrait"], "hourly_rate": "1700.00", "minimum_hours": 2,
        "bio": "Portrait and fashion photographer in Marathahalli. Natural light specialist.",
        "years_experience": 6, "equipment": ["Sony A7R V", "FE 85mm f/1.4 GM", "FE 50mm f/1.2 GM"],
        "availability_status": "available", "latitude": "12.9591", "longitude": "77.6974",
        "service_radius_km": 15, "is_featured": False, "avg_rating": "4.70",
        "total_reviews": 67, "total_bookings": 54, "on_time_rate": "96.80",
        "portfolio": [("https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=800&q=80",
                       "https://images.unsplash.com/photo-1529626455594-4ff0802cfb7e?auto=format&fit=crop&w=300&q=80",
                       "Natural Light", "Outdoor lifestyle portrait")],
    },
    {
        "full_name": "Karan Malhotra", "email": "karan.malhotra@example.com", "phone": "+919800000008",
        "profile_pic": "https://images.unsplash.com/photo-1519085360753-af0119f7cbe7?auto=format&fit=crop&w=150&h=150&q=80",
        "specializations": ["corporate", "event"], "hourly_rate": "1500.00", "minimum_hours": 3,
        "bio": "Corporate event photographer covering Electronic City tech parks and conferences.",
        "years_experience": 5, "equipment": ["Nikon Z6 III", "Z 24-70mm f/2.8 S", "SB-5000 flash"],
        "availability_status": "available", "latitude": "12.8452", "longitude": "77.6602",
        "service_radius_km": 22, "is_featured": False, "avg_rating": "4.40",
        "total_reviews": 39, "total_bookings": 33, "on_time_rate": "93.90",
        "portfolio": [("https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=800&q=80",
                       "https://images.unsplash.com/photo-1540575467063-178a50c2df87?auto=format&fit=crop&w=300&q=80",
                       "Keynote", "Tech conference stage coverage")],
    },
]


def seed():
    print("=== SEEDING REALISTIC TEST DATA FOR TWO USERS ===")
    db = SessionLocal()
    try:
        # 1. Clean up existing seed data to allow repeated runs
        client_email = "client@example.com"
        creator_email = "creator@example.com"
        
        # Delete old users (cascades to bookings, messages, reviews, profiles, gallery)
        demo_emails = [d["email"] for d in DEMO_CREATORS]
        db.query(User).filter(
            User.email.in_([client_email, creator_email] + demo_emails)
        ).delete(synchronize_session=False)
        db.commit()
        print("Cleared previous seed users.")

        # 2. Insert Client/Attendee User
        client_pwd_hash = get_password_hash("Password123!")
        client_user = User(
            full_name="Alex Mercer (Client)",
            email=client_email,
            phone="+919876543210",
            hashed_password=client_pwd_hash,
            role="attendee",
            is_active=True,
            is_verified=True,
            profile_pic_url="https://images.unsplash.com/photo-1535713875002-d1d0cf377fde?auto=format&fit=crop&w=150&h=150&q=80"
        )
        db.add(client_user)
        db.commit()
        db.refresh(client_user)
        print(f"Created Attendee Client: {client_user.email} (ID: {client_user.id})")

        # 3. Insert Creator User
        creator_pwd_hash = get_password_hash("Password123!")
        creator_user = User(
            full_name="Sarah Connor (Photographer)",
            email=creator_email,
            phone="+919876543211",
            hashed_password=creator_pwd_hash,
            role="creator",
            is_active=True,
            is_verified=True,
            profile_pic_url="https://images.unsplash.com/photo-1494790108377-be9c29b29330?auto=format&fit=crop&w=150&h=150&q=80"
        )
        db.add(creator_user)
        db.commit()
        db.refresh(creator_user)
        print(f"Created Creator User: {creator_user.email} (ID: {creator_user.id})")

        # 4. Creator Profile (Sarah Connor is a professional photographer)
        # Note: auth_service.register_user automatically creates a profile, but since we created the model directly,
        # we will insert/create a detailed profile.
        creator_profile = CreatorProfile(
            user_id=creator_user.id,
            specializations=["wedding", "portrait", "corporate"],
            hourly_rate=Decimal("1500.00"),
            minimum_hours=2,
            bio="Award-winning portrait and wedding photographer based in Bangalore. Capturing precious moments with top-tier equipment.",
            years_experience=6,
            equipment=["Sony A7R V", "FE 24-70mm f/2.8 GM II", "FE 85mm f/1.4 GM"],
            availability_status="available",
            latitude=Decimal("12.9716"),
            longitude=Decimal("77.5946"),
            service_radius_km=15,
            is_featured=True,
            avg_rating=Decimal("4.85"),
            total_reviews=1,
            total_bookings=1,
            on_time_rate=Decimal("100.00")
        )
        db.add(creator_profile)
        db.commit()
        print("Created Creator Profile.")

        # 5. Seed a Booking between Alex and Sarah
        tomorrow = datetime.date.today() + datetime.timedelta(days=1)
        booking = Booking(
            attendee_id=client_user.id,
            creator_id=creator_user.id,
            event_type="wedding",
            location="Leela Palace, Bangalore",
            event_date=tomorrow,
            start_time=datetime.time(14, 0, 0),
            duration_hours=Decimal("4.0"),
            total_amount=Decimal("6000.00"),  # 1500 * 4 hours
            status="confirmed",
            special_notes="Need raw files uploaded to client delivery gallery within 3 days."
        )
        db.add(booking)
        db.commit()
        db.refresh(booking)
        print(f"Created Booking (ID: {booking.id})")

        # 6. Seed Payment Record
        payment = Payment(
            booking_id=booking.id,
            payer_id=client_user.id,
            amount=Decimal("6000.00"),
            refund_amount=Decimal("0.00"),
            currency="INR",
            gateway="razorpay",
            gateway_order_id="order_seeded_123",
            gateway_payment_id="pay_seeded_123",
            status="captured",
            captured_at=datetime.datetime.now(datetime.timezone.utc)
        )
        db.add(payment)
        db.commit()
        print("Created Payment Record.")

        # 7. Seed Chat Messages
        messages = [
            Message(
                booking_id=booking.id,
                sender_id=client_user.id,
                receiver_id=creator_user.id,
                content="Hi Sarah! Looking forward to the shoot tomorrow at Leela Palace.",
                message_type="text"
            ),
            Message(
                booking_id=booking.id,
                sender_id=creator_user.id,
                receiver_id=client_user.id,
                content="Hi Alex! Yes, absolutely. I will arrive 30 minutes early to scout locations.",
                message_type="text"
            ),
            Message(
                booking_id=booking.id,
                sender_id=client_user.id,
                receiver_id=creator_user.id,
                content="Great, thank you! See you there.",
                message_type="text"
            )
        ]
        db.add_all(messages)
        db.commit()
        print("Seeded Chat History.")

        # 8. Seed Gallery Items
        gallery_items = [
            # Portfolio Item
            GalleryItem(
                creator_id=creator_user.id,
                file_url="https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=800&q=80",
                thumbnail_url="https://images.unsplash.com/photo-1519741497674-611481863552?auto=format&fit=crop&w=300&q=80",
                file_type="photo",
                file_size_bytes=1543200,
                title="Sangeet Ceremony Portrait",
                description="Traditional Indian wedding ceremony shoot",
                is_portfolio=True,
                is_client_delivery=False,
                views_count=42
            ),
            # Client Delivery Item for this Booking
            GalleryItem(
                creator_id=creator_user.id,
                booking_id=booking.id,
                file_url="https://images.unsplash.com/photo-1606800052052-a08af7148866?auto=format&fit=crop&w=800&q=80",
                thumbnail_url="https://images.unsplash.com/photo-1606800052052-a08af7148866?auto=format&fit=crop&w=300&q=80",
                file_type="photo",
                file_size_bytes=1845100,
                title="Couple Portrait Seeding",
                description="Seeded delivery photo",
                is_portfolio=False,
                is_client_delivery=True,
                views_count=1
            )
        ]
        db.add_all(gallery_items)
        db.commit()
        print("Seeded Portfolio & Client Delivery Gallery.")

        # 9. Seed a completed Review from another (fictional) booking
        # We will create a review for Sarah from the client Alex
        review = Review(
            booking_id=booking.id,
            reviewer_id=client_user.id,
            creator_id=creator_user.id,
            rating=5,
            comment="Outstanding work! The portrait shots were breathtaking.",
            is_verified=True
        )
        db.add(review)
        db.commit()
        print("Seeded Review.")

        # 10. Seed Notifications
        notifications = [
            Notification(
                user_id=creator_user.id,
                title="Booking Confirmed",
                body="Alex Mercer has booked you for tomorrow.",
                notification_type="booking_confirmed",
                reference_id=booking.id,
                reference_type="booking",
                is_read=False
            ),
            Notification(
                user_id=client_user.id,
                title="Payment Successful",
                body="Your payment of ₹6000.00 was successfully processed.",
                notification_type="payment_captured",
                reference_id=booking.id,
                reference_type="payment",
                is_read=False
            )
        ]
        db.add_all(notifications)
        db.commit()
        print("Seeded Notifications.")

        # 11. Seed geo-located demo creators spread across Bangalore
        demo_pwd_hash = get_password_hash("Password123!")
        for d in DEMO_CREATORS:
            demo_user = User(
                full_name=d["full_name"],
                email=d["email"],
                phone=d["phone"],
                hashed_password=demo_pwd_hash,
                role="creator",
                is_active=True,
                is_verified=True,
                profile_pic_url=d["profile_pic"],
            )
            db.add(demo_user)
            db.commit()
            db.refresh(demo_user)

            demo_profile = CreatorProfile(
                user_id=demo_user.id,
                specializations=d["specializations"],
                hourly_rate=Decimal(d["hourly_rate"]),
                minimum_hours=d["minimum_hours"],
                bio=d["bio"],
                years_experience=d["years_experience"],
                equipment=d["equipment"],
                availability_status=d["availability_status"],
                latitude=Decimal(d["latitude"]),
                longitude=Decimal(d["longitude"]),
                service_radius_km=d["service_radius_km"],
                is_featured=d["is_featured"],
                avg_rating=Decimal(d["avg_rating"]),
                total_reviews=d["total_reviews"],
                total_bookings=d["total_bookings"],
                on_time_rate=Decimal(d["on_time_rate"]),
            )
            db.add(demo_profile)
            db.commit()

            portfolio_items = [
                GalleryItem(
                    creator_id=demo_user.id,
                    file_url=full_url,
                    thumbnail_url=thumb_url,
                    file_type="photo",
                    file_size_bytes=1500000,
                    title=title,
                    description=desc,
                    is_portfolio=True,
                    is_client_delivery=False,
                    views_count=25,
                )
                for (full_url, thumb_url, title, desc) in d["portfolio"]
            ]
            db.add_all(portfolio_items)
            db.commit()
        print(f"Seeded {len(DEMO_CREATORS)} geo-located demo creators around Bangalore.")

        print("\n=== DATABASE SEEDING COMPLETED SUCCESSFULLY! ===")
        print(f"Attendee Username: {client_email}")
        print(f"Creator Username:  {creator_email}")
        print("Password:          Password123!")

    except Exception as e:
        db.rollback()
        print(f"Error during seeding: {str(e)}")
        sys.exit(1)
    finally:
        db.close()

if __name__ == "__main__":
    seed()
