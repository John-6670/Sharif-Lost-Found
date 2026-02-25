import json
from channels.generic.websocket import AsyncWebsocketConsumer
from channels.db import database_sync_to_async
from django.contrib.auth import get_user_model
from rest_framework_simplejwt.tokens import AccessToken
from rest_framework_simplejwt.exceptions import InvalidToken, TokenError

from .models import Conversation, Message
from .serializers import MessageSerializer

User = get_user_model()


class ChatConsumer(AsyncWebsocketConsumer):
    """
    WebSocket consumer for handling real-time chat messages.
    Users connect to a specific conversation and can send/receive messages.
    """
    
    async def connect(self):
        """Handle WebSocket connection"""
        self.conversation_id = self.scope['url_route']['kwargs']['conversation_id']
        self.room_group_name = f'chat_{self.conversation_id}'
        
        # Authenticate user from token
        self.user = await self.get_user_from_token()
        
        if self.user is None:
            await self.close(code=4001)
            return
        
        # Verify user is part of this conversation
        is_participant = await self.verify_conversation_participant()
        if not is_participant:
            await self.close(code=4003)
            return
        
        # Join room group
        await self.channel_layer.group_add(
            self.room_group_name,
            self.channel_name
        )
        
        await self.accept()
        
        # Mark messages as read
        await self.mark_messages_as_read()

    async def disconnect(self, close_code):
        """Handle WebSocket disconnection"""
        # Leave room group
        if hasattr(self, 'room_group_name'):
            await self.channel_layer.group_discard(
                self.room_group_name,
                self.channel_name
            )

    async def receive(self, text_data):
        """Receive message from WebSocket"""
        try:
            data = json.loads(text_data)
            message_body = data.get('message', '').strip()
            
            if not message_body:
                await self.send(text_data=json.dumps({
                    'error': 'Message body cannot be empty'
                }))
                return
            
            # Save message to database
            message = await self.save_message(message_body)
            
            # Broadcast message to room group
            await self.channel_layer.group_send(
                self.room_group_name,
                {
                    'type': 'chat_message',
                    'message': {
                        'id': message['id'],
                        'conversation': message['conversation'],
                        'sender': message['sender'],
                        'sender_name': message['sender_name'],
                        'body': message['body'],
                        'created_at': message['created_at'],
                        'is_read': message.get('is_read', False)
                    }
                }
            )
        except json.JSONDecodeError:
            await self.send(text_data=json.dumps({
                'error': 'Invalid JSON format'
            }))
        except Exception as e:
            await self.send(text_data=json.dumps({
                'error': str(e)
            }))

    async def chat_message(self, event):
        # Send message to WebSocket
        message = event['message']
        
        await self.send(text_data=json.dumps({
            'message': event['message'],
            'sender_id': event['sender_id'],
            'sender_name': event['sender_name'],
            'message_id': event['message_id'],
            'created_at': event['created_at'],
        }))
        
        # Mark as read if the receiver is connected
        if message['sender'] != self.user.id:
            await self.mark_message_as_read(message['id'])
    
    @database_sync_to_async
    def get_user_from_token(self):
        """
        Authenticate user from JWT token in query string
        """
        try:
            # Get token from query string
            query_string = self.scope.get('query_string', b'').decode('utf-8')
            token = None
            
            for param in query_string.split('&'):
                if param.startswith('token='):
                    token = param.split('=')[1]
                    break
            
            if not token:
                return None
            
            # Validate token
            access_token = AccessToken(token)
            user_id = access_token['user_id']
            user = User.objects.get(id=user_id)
            return user
            
        except (InvalidToken, TokenError, User.DoesNotExist, KeyError):
            return None

    @database_sync_to_async
    def verify_conversation_participant(self):
        """
        Verify that the user is a participant in this conversation
        """
        try:
            conversation = Conversation.objects.get(id=self.conversation_id)
            return conversation.user1 == self.user or conversation.user2 == self.user
        except Conversation.DoesNotExist:
            return False

    @database_sync_to_async
    def save_message(self, body):
        """
        Save message to database and return serialized data
        """
        conversation = Conversation.objects.get(id=self.conversation_id)
        message = Message.objects.create(
            conversation=conversation,
            sender=self.user,
            body=body
        )
        serializer = MessageSerializer(message)
        return serializer.data

    @database_sync_to_async
    def mark_messages_as_read(self):
        """
        Mark all messages in this conversation as read for the current user
        """
        Message.objects.filter(
            conversation_id=self.conversation_id
        ).exclude(
            sender=self.user
        ).update(is_read=True)

    @database_sync_to_async
    def mark_message_as_read(self, message_id):
        """
        Mark a specific message as read
        """
        Message.objects.filter(id=message_id).update(is_read=True)