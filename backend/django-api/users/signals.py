import logging
import requests
from django.conf import settings
from django.db.models.signals import post_save
from django.dispatch import receiver
from .models import User

logger = logging.getLogger(__name__)


def prepare_user_data(user_instance):
    print("Preparing user data for synchronization:", user_instance.email)

    return {
        'id': user_instance.id,
        'email': user_instance.email,
        'name': user_instance.name,
        'is_verified': user_instance.is_verified,
        'last_seen': user_instance.last_seen.isoformat() if user_instance.last_seen else None,
        'created_at': user_instance.created_at.isoformat() if user_instance.created_at else None,
    }


def sync_user_register(user_instance):
    print("Syncing new user registration to external API:", user_instance.email)
    external_api_url = getattr(settings, 'EXTERNAL_USER_SYNC_API', None)
    
    if not external_api_url:
        logger.warning("EXTERNAL_USER_SYNC_API not configured in settings. Skipping user registration sync.")
        return
    
    user_data = prepare_user_data(user_instance)
    headers = {'Content-Type': 'application/json'}
    
    try:
        response = requests.post(
            external_api_url,
            json=user_data,
            headers=headers,
            timeout=10
        )
        
        if response.status_code in [200, 201]:
            logger.info(f"Successfully registered user {user_instance.email} to external API")
        else:
            logger.error(
                f"Failed to register user {user_instance.email} to external API. "
                f"Status: {response.status_code}, Response: {response.text}"
            )
    except requests.exceptions.RequestException as e:
        logger.error(f"Error registering user {user_instance.email} to external API: {str(e)}")
    except Exception as e:
        logger.error(f"Unexpected error registering user {user_instance.email}: {str(e)}")


def sync_user_update(user_instance):
    print("Syncing user update to external API:", user_instance.email)
    external_api_url = getattr(settings, 'EXTERNAL_USER_SYNC_API', None)
    
    if not external_api_url:
        logger.warning("EXTERNAL_USER_SYNC_API not configured in settings. Skipping user update sync.")
        return
    
    user_data = prepare_user_data(user_instance)
    headers = {'Content-Type': 'application/json'}
    
    try:
        response = requests.put(
            external_api_url,
            json=user_data,
            headers=headers,
            timeout=10
        )
        
        if response.status_code in [200, 204]:
            logger.info(f"Successfully updated user {user_instance.email} in external API")
        else:
            logger.error(
                f"Failed to update user {user_instance.email} in external API. "
                f"Status: {response.status_code}, Response: {response.text}"
            )
    except requests.exceptions.RequestException as e:
        logger.error(f"Error updating user {user_instance.email} in external API: {str(e)}")
    except Exception as e:
        logger.error(f"Unexpected error updating user {user_instance.email}: {str(e)}")


@receiver(post_save, sender=User)
def sync_user_to_external_api(sender, instance, created, **kwargs):
    if created:
        # New user registration - use POST
        sync_user_register(instance)
    else:
        # Existing user update - use PUT
        sync_user_update(instance)
