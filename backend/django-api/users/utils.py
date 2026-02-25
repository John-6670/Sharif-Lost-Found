import random
import hashlib
from django.core.mail import send_mail
from django.conf import settings

def generate_otp():
    return str(random.randint(100000, 999999))

def hash_otp(otp: str):
    return hashlib.sha256(otp.encode()).hexdigest()

def send_otp(email, otp, name='User'):
    subject = "Lost & Found System – One-Time Verification Code"

    message = f"""
    Dear {name},

    Your one-time verification code for the Sharif Lost & Found System is:

    {otp}

    This code is valid for 2 minutes.
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


def send_new_message_notification(recipient_email, recipient_name, sender_name, is_new_conversation=False):
    """
    Send email notification when a user receives a new message.
    
    Args:
        recipient_email: Email of the user receiving the message
        recipient_name: Name of the user receiving the message
        sender_name: Name of the user sending the message
        is_new_conversation: Whether this is from a new person (True) or after 2+ days of silence (False)
    """
    if is_new_conversation:
        subject = "Lost & Found System – New Message from a User"
        intro_text = f"You have a new message from {sender_name}."
    else:
        subject = "Lost & Found System – Message from Recent Contact"
        intro_text = f"{sender_name} has sent you a new message after a while."

    message = f"""
    Dear {recipient_name},

    {intro_text}

    Please log in to the Lost & Found System to view the message and reply.

    Best regards,
    Lost & Found System
    """.strip()

    send_mail(
        subject=subject,
        message=message,
        from_email=settings.DEFAULT_FROM_EMAIL,
        recipient_list=[recipient_email],
        fail_silently=False,
    )
