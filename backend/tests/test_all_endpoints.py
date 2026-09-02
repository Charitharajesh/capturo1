import random
import string
import datetime
from decimal import Decimal
from unittest.mock import MagicMock
from fastapi.testclient import TestClient
from app.main import app
from app.services.payment_service import payment_service
from app.services.upload_service import upload_service
from app.db.session import SessionLocal
from app.models.user import User

client = TestClient(app)

def random_string(length=8):
    return "".join(random.choices(string.ascii_lowercase + string.digits, k=length))

def test_all_endpoints():
    print("\n=== STARTING END-TO-END ENDPOINT TESTS ===")

    # Generate random credentials for this run to avoid conflict
    rand = random_string(6)
    attendee_email = f"attendee_{rand}@example.com"
    creator_email = f"creator_{rand}@example.com"
    password = "SecurePassword123!"
    phone_attendee = "+1" + "".join(random.choices(string.digits, k=10))
    phone_creator = "+1" + "".join(random.choices(string.digits, k=10))

    pay_id = f"pay_{rand}"
    order_id = f"order_{rand}"

    # Mock payment_service.client
    payment_service.client = MagicMock()
    # Mock order.create
    payment_service.client.order.create.return_value = {
        "id": order_id,
        "amount": 199800,
        "currency": "INR",
        "receipt": "booking_test",
        "status": "created"
    }
    # Mock verify_payment_signature to always return True
    payment_service.verify_payment_signature = MagicMock(return_value=True)

    # 1. AUTH ENDPOINTS
    print("\n1. Testing Auth Endpoints...")
    # Register Attendee
    resp = client.post("/api/v1/auth/register", json={
        "full_name": "Test Attendee",
        "email": attendee_email,
        "password": password,
        "role": "attendee",
        "phone": phone_attendee
    })
    assert resp.status_code == 201, resp.text
    attendee_data = resp.json()["data"]
    attendee_id = attendee_data["id"]

    # Register Creator
    resp = client.post("/api/v1/auth/register", json={
        "full_name": "Test Creator",
        "email": creator_email,
        "password": password,
        "role": "creator",
        "phone": phone_creator
    })
    assert resp.status_code == 201, resp.text
    creator_data = resp.json()["data"]
    creator_user_id = creator_data["id"]

    # Login Attendee
    resp = client.post("/api/v1/auth/login", json={
        "email": attendee_email,
        "password": password
    })
    assert resp.status_code == 200, resp.text
    attendee_token = resp.json()["data"]["access_token"]
    attendee_headers = {"Authorization": f"Bearer {attendee_token}"}

    # Login Creator
    resp = client.post("/api/v1/auth/login", json={
        "email": creator_email,
        "password": password
    })
    assert resp.status_code == 200, resp.text
    creator_token = resp.json()["data"]["access_token"]
    creator_headers = {"Authorization": f"Bearer {creator_token}"}

    # Verify Token Refresh
    refresh_token = resp.json()["data"]["refresh_token"]
    resp = client.post("/api/v1/auth/refresh", json={"refresh_token": refresh_token})
    assert resp.status_code == 200, resp.text

    # 2. USER PROFILE ENDPOINTS
    print("\n2. Testing User Profile Endpoints...")
    resp = client.get("/api/v1/users/me", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    resp = client.patch("/api/v1/users/me", headers=attendee_headers, json={
        "full_name": "Test Attendee Updated"
    })
    assert resp.status_code == 200, resp.text
    assert resp.json()["data"]["full_name"] == "Test Attendee Updated"

    # 3. CREATOR PROFILE ENDPOINTS
    print("\n3. Testing Creator Profile Endpoints...")
    # Delete auto-created creator profile so we can test POST /creators/profile
    db = SessionLocal()
    try:
        from app.models.creator import CreatorProfile
        db.query(CreatorProfile).filter(CreatorProfile.user_id == creator_user_id).delete()
        db.commit()
    finally:
        db.close()

    # Create Creator Profile
    resp = client.post("/api/v1/creators/profile", headers=creator_headers, json={
        "specializations": ["wedding", "portrait"],
        "hourly_rate": 999.00,
        "minimum_hours": 2,
        "bio": "Experienced wedding photographer",
        "years_experience": 5,
        "equipment": ["Sony A7III", "85mm Lens"],
        "availability_status": "available",
        "latitude": 12.9716,
        "longitude": 77.5946,
        "service_radius_km": 15
    })
    assert resp.status_code == 201, resp.text
    creator_profile_id = resp.json()["data"]["id"]

    # Get Own Creator Profile
    resp = client.get("/api/v1/creators/me", headers=creator_headers)
    assert resp.status_code == 200, resp.text

    # Update Own Creator Profile
    resp = client.patch("/api/v1/creators/me", headers=creator_headers, json={
        "bio": "Top Wedding photographer in Bangalore"
    })
    assert resp.status_code == 200, resp.text

    # List Creators
    resp = client.get("/api/v1/creators")
    assert resp.status_code == 200, resp.text

    # Get Nearby Creators
    resp = client.get("/api/v1/creators/nearby?lat=12.9700&lon=77.5900&radius_km=10")
    assert resp.status_code == 200, resp.text

    # Get Public Creator Detail
    resp = client.get(f"/api/v1/creators/{creator_user_id}")
    assert resp.status_code == 200, resp.text

    # Get Availability
    resp = client.get(f"/api/v1/creators/{creator_user_id}/availability?date={(datetime.date.today() + datetime.timedelta(days=1)).isoformat()}&start_time=14:00:00", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Get Creator Stats
    resp = client.get(f"/api/v1/creators/{creator_user_id}/stats", headers=creator_headers)
    assert resp.status_code == 200, resp.text

    # 4. BOOKINGS
    print("\n4. Testing Booking Endpoints...")
    # Create Booking
    tomorrow = (datetime.date.today() + datetime.timedelta(days=1)).isoformat()
    resp = client.post("/api/v1/bookings", headers=attendee_headers, json={
        "creator_id": creator_user_id,
        "event_type": "wedding",
        "location": "UB City, Bangalore",
        "event_date": tomorrow,
        "start_time": "14:00:00",
        "duration_hours": 2.0,
        "special_notes": "Please arrive 15 mins early"
    })
    assert resp.status_code == 201, resp.text
    booking_id = resp.json()["data"]["booking_id"]

    # List Bookings
    resp = client.get("/api/v1/bookings", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Get Booking Detail
    resp = client.get(f"/api/v1/bookings/{booking_id}", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Update Booking details
    resp = client.patch(f"/api/v1/bookings/{booking_id}", headers=attendee_headers, json={
        "location": "MG Road, Bangalore"
    })
    assert resp.status_code == 200, resp.text

    # 5. PAYMENTS
    print("\n5. Testing Payment Endpoints...")
    # Create payment order
    resp = client.post("/api/v1/payments/create-order", headers=attendee_headers, json={
        "booking_id": booking_id
    })
    assert resp.status_code == 200, resp.text

    # Verify payment
    resp = client.post("/api/v1/payments/verify", headers=attendee_headers, json={
        "order_id": order_id,
        "payment_id": pay_id,
        "signature": "mock_signature"
    })
    assert resp.status_code == 200, resp.text

    # Confirm Booking
    resp = client.post(f"/api/v1/bookings/{booking_id}/confirm", headers=attendee_headers, json={
        "payment_id": pay_id,
        "payment_signature": "mock_signature"
    })
    assert resp.status_code == 200, resp.text

    # Get payment detail
    # Let's search payment db record or query history
    resp = client.get("/api/v1/payments/history", headers=attendee_headers)
    assert resp.status_code == 200, resp.text
    payments_list = resp.json()["items"]
    assert len(payments_list) > 0
    payment_id = payments_list[0]["id"]

    # Get specific payment details
    resp = client.get(f"/api/v1/payments/{payment_id}", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # 6. MESSAGES (CHAT)
    print("\n6. Testing Message Endpoints...")
    # Send message from Attendee to Creator
    resp = client.post("/api/v1/messages", headers=attendee_headers, json={
        "receiver_id": creator_user_id,
        "booking_id": booking_id,
        "message_text": "Hey there! Looking forward to the shoot."
    })
    assert resp.status_code == 201, resp.text
    message_id = resp.json()["data"]["id"]

    # Get Unread message count for creator
    resp = client.get("/api/v1/messages/unread-count", headers=creator_headers)
    assert resp.status_code == 200, resp.text

    # Get chat history
    resp = client.get(f"/api/v1/messages/{booking_id}", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Mark message as read
    resp = client.patch(f"/api/v1/messages/{message_id}/read", headers=creator_headers)
    assert resp.status_code == 200, resp.text

    # Delete message
    resp = client.delete(f"/api/v1/messages/{message_id}", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # 7. GALLERY & FILE UPLOAD
    print("\n7. Testing Gallery and Upload Endpoints...")
    # Mock upload_service.save_uploaded_file
    async def mock_save(*args, **kwargs):
        return "http://localhost:8000/uploads/test.jpg", "http://localhost:8000/uploads/test_thumb.jpg"
    upload_service.save_uploaded_file = mock_save

    # Mock File upload
    dummy_file = ("test.jpg", b"dummy content", "image/jpeg")
    resp = client.post("/api/v1/uploads/file", headers=creator_headers, files={"file": dummy_file}, data={"booking_id": booking_id})
    assert resp.status_code == 201, resp.text

    # Gallery upload
    resp = client.post("/api/v1/gallery/upload", headers=creator_headers, files={"file": dummy_file}, data={
        "is_portfolio": "true",
        "is_client_delivery": "true",
        "booking_id": booking_id,
        "title": "Test Title",
        "description": "Test Description"
    })
    assert resp.status_code == 201, resp.text
    gallery_item_id = resp.json()["data"]["id"]

    # Public Creator portfolio gallery
    resp = client.get(f"/api/v1/gallery/creator/{creator_user_id}")
    assert resp.status_code == 200, resp.text

    # Client delivery gallery
    resp = client.get(f"/api/v1/gallery/delivery/{booking_id}", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Patch Gallery item
    resp = client.patch(f"/api/v1/gallery/{gallery_item_id}", headers=creator_headers, json={
        "title": "Stunning Wedding Shot Updated"
    })
    assert resp.status_code == 200, resp.text

    # Share Link
    resp = client.post(f"/api/v1/gallery/delivery/{booking_id}/share", headers=creator_headers, json={
        "expires_in_days": 30
    })
    assert resp.status_code == 200, resp.text
    share_link_url = resp.json()["data"]["share_url"]

    # Download Delivery Zip
    resp = client.get(f"/api/v1/gallery/delivery/{booking_id}/download", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Delete Gallery item (we will test at the end or test now and check)
    # resp = client.delete(f"/api/v1/gallery/{gallery_item_id}", headers=creator_headers)
    # assert resp.status_code == 200, resp.text

    # 8. NOTIFICATIONS
    print("\n8. Testing Notification Endpoints...")
    # List notifications
    resp = client.get("/api/v1/notifications", headers=attendee_headers)
    assert resp.status_code == 200, resp.text
    notifications_list = resp.json()["items"]
    assert len(notifications_list) > 0
    notification_id = notifications_list[0]["id"]

    # Unread notifications count
    resp = client.get("/api/v1/notifications/unread-count", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Mark notification as read
    resp = client.patch(f"/api/v1/notifications/{notification_id}/read", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Delete notification
    resp = client.delete(f"/api/v1/notifications/{notification_id}", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # 9. COMPLETE BOOKING, DISPUTES & REVIEWS
    print("\n9. Testing Booking Completion, Reviews & Disputes...")
    # Creator completes booking
    resp = client.post(f"/api/v1/bookings/{booking_id}/complete", headers=creator_headers)
    assert resp.status_code == 200, resp.text

    # Create Review
    resp = client.post("/api/v1/reviews", headers=attendee_headers, json={
        "booking_id": booking_id,
        "rating": 5,
        "comment": "Absolutely brilliant photos! Prompt and professional."
    })
    assert resp.status_code == 201, resp.text
    review_id = resp.json()["data"]["id"]

    # Get review list for creator
    resp = client.get(f"/api/v1/reviews/creator/{creator_user_id}")
    assert resp.status_code == 200, resp.text

    # Get review detail
    resp = client.get(f"/api/v1/reviews/{review_id}")
    assert resp.status_code == 200, resp.text

    # Update review
    resp = client.patch(f"/api/v1/reviews/{review_id}", headers=attendee_headers, json={
        "comment": "Changed comment: Outstanding work!"
    })
    assert resp.status_code == 200, resp.text

    # Delete review
    resp = client.delete(f"/api/v1/reviews/{review_id}", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # 10. ADMIN DASHBOARD & CONTROLS
    print("\n10. Testing Admin Endpoints...")
    # Set Attendee as Admin in DB to check admin routes
    db = SessionLocal()
    try:
        user = db.query(User).filter(User.id == attendee_id).first()
        user.role = "admin"
        db.commit()
    finally:
        db.close()

    # Now attendee_headers works as an Admin!
    # Admin stats
    resp = client.get("/api/v1/admin/dashboard/stats", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Admin bookings list
    resp = client.get("/api/v1/admin/bookings", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Admin users list
    resp = client.get("/api/v1/admin/users", headers=attendee_headers)
    assert resp.status_code == 200, resp.text

    # Toggle user status
    resp = client.patch(f"/api/v1/admin/users/{creator_user_id}/status", headers=attendee_headers, json={
        "is_active": False
    })
    assert resp.status_code == 200, resp.text

    # Clean up and confirm
    print("\n=== ALL ENDPOINT TESTS PASSED SUCCESSFULLY! ===")

if __name__ == "__main__":
    test_all_endpoints()
