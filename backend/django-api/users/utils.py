import random
from django.core.mail import send_mail
from django.conf import settings

def generate_otp():
    return str(random.randint(100000, 999999))

def send_otp(email, otp, name='User'):
    subject = "Lost & Found System – One-Time Verification Code"

    message = f"""
    Dear {name},

    Your one-time verification code for the Sharif Lost & Found System is:

    {otp}

    Please do not share this code with anyone.

    If you did not request this verification, you can safely ignore this email.

    Best regards,
    Lost & Found System
    """.strip()

    send_mail(
        subject=subject,
        message=message,
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=[email],
        fail_silently=False,
    )