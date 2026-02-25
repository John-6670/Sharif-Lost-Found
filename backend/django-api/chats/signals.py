import logging
import requests
from django.db.models.signals import post_save
from django.dispatch import receiver
from django.utils import timezone
from datetime import timedelta

from .models import Message, Conversation
from users.utils import send_new_message_notification

logger = logging.getLogger(__name__)


@receiver(post_save, sender=Message)
def send_message_notification(sender, instance, created, **kwargs):
    """
    Send email notification to the recipient when a new message is created.
    Conditions:
    - Recipient has notification preference enabled
    - Either it's a completely new conversation OR last message was 2+ days ago
    """
    if not created:
        return

    try:
        conversation = instance.conversation
        sender = instance.sender
        
        # Determine the recipient (the other user in the conversation)
        if conversation.user1 == sender:
            recipient = conversation.user2
        else:
            recipient = conversation.user1
        
        # Check if recipient has notifications enabled
        if not recipient.notification_on_new_messages:
            return
        
        # Get the last message before this one (excluding current message)
        previous_messages = Message.objects.filter(
            conversation=conversation
        ).exclude(id=instance.id).order_by('-created_at')
        
        # Check if this is a new conversation (no previous messages)
        is_new_conversation = not previous_messages.exists()
        
        if is_new_conversation:
            # New conversation - send notification
            send_new_message_notification(
                recipient_email=recipient.email,
                recipient_name=recipient.name,
                sender_name=sender.name,
                is_new_conversation=True
            )
            logger.info(f"Sent new conversation notification to {recipient.email}")
        else:
            # Get the last message time
            last_message = previous_messages.first()
            time_since_last_message = timezone.now() - last_message.created_at
            
            # If more than 2 days have passed, send notification
            if time_since_last_message >= timedelta(days=2):
                send_new_message_notification(
                    recipient_email=recipient.email,
                    recipient_name=recipient.name,
                    sender_name=sender.name,
                    is_new_conversation=False
                )
                logger.info(f"Sent renewed conversation notification to {recipient.email}")
    
    except Exception as e:
        logger.error(f"Error sending message notification: {str(e)}")
