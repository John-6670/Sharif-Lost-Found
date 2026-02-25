from rest_framework import generics, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.db.models import Q, Max, Count
from django.shortcuts import get_object_or_404
from rest_framework.views import APIView
from rest_framework.pagination import PageNumberPagination

from .models import Conversation, Message
from .serializers import MessageSerializer, ConversationSerializer

from users.models import User


class ConversationListView(generics.ListAPIView):
    """
    Get all conversations for the authenticated user with latest message info
    """
    permission_classes = [IsAuthenticated]
    
    def get(self, request):
        user = request.user
        
        conversations = Conversation.objects.filter(
            Q(user1=user) | Q(user2=user)
        ).annotate(
            last_message_time=Max('messages__created_at')
        ).order_by('-last_message_time')

        serializer = ConversationSerializer(
            conversations, 
            many=True, 
            context={'request': request}
        )
        
        return Response(serializer.data, status=status.HTTP_200_OK)


class ConversationDetailView(generics.RetrieveAPIView):
    """
    Get a specific conversation with all its messages
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        user = request.user
        conversation = get_object_or_404(Conversation, id=conversation_id)

        # Verify user is part of conversation
        if user not in [conversation.user1, conversation.user2]:
            return Response(
                {'error': 'You are not part of this conversation'},
                status=status.HTTP_403_FORBIDDEN
            )

        # Mark messages as read
        Message.objects.filter(
            conversation=conversation,
            is_read=False
        ).exclude(sender=user).update(is_read=True)

        # Get messages
        messages = conversation.messages.select_related('sender').order_by('created_at')
        message_serializer = MessageSerializer(messages, many=True)

        other_user = conversation.user2 if conversation.user1 == user else conversation.user1

        return Response({
            'id': conversation.id,
            'other_user': {
                'id': other_user.id,
                'name': other_user.name,
                'email': other_user.email,
            },
            'messages': message_serializer.data,
            'created_at': conversation.created_at,
        }, status=status.HTTP_200_OK)


class ConversationCreateView(APIView):
    """
    Create a new conversation or return existing one
    """
    permission_classes = [IsAuthenticated]

    def post(self, request):
        user = request.user
        other_user_id = request.data.get('user_id')

        if not other_user_id:
            return Response(
                {'error': 'user_id is required'},
                status=status.HTTP_400_BAD_REQUEST
            )

        if int(other_user_id) == user.id:
            return Response(
                {'error': 'Cannot create conversation with yourself'},
                status=status.HTTP_400_BAD_REQUEST
            )

        other_user = get_object_or_404(User, id=other_user_id)

        # Check if conversation already exists (in either direction)
        conversation = Conversation.objects.filter(
            Q(user1=user, user2=other_user) |
            Q(user1=other_user, user2=user)
        ).first()

        if conversation:
            return Response({
                'id': conversation.id,
                'created': False,
                'other_user': {
                    'id': other_user.id,
                    'name': other_user.name,
                    'email': other_user.email,
                },
                'created_at': conversation.created_at,
            }, status=status.HTTP_200_OK)

        # Create new conversation
        conversation = Conversation.objects.create(
            user1=user,
            user2=other_user
        )

        return Response({
            'id': conversation.id,
            'created': True,
            'other_user': {
                'id': other_user.id,
                'name': other_user.name,
                'email': other_user.email,
            },
            'created_at': conversation.created_at,
        }, status=status.HTTP_201_CREATED)


class UnreadCountView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        unread_count = Message.objects.filter(
            conversation__user1=user,
            is_read=False
        ).exclude(sender=user).count() + Message.objects.filter(
            conversation__user2=user,
            is_read=False
        ).exclude(sender=user).count()

        return Response({
            'unread_count': unread_count
        }, status=status.HTTP_200_OK)


class MessagePagination(PageNumberPagination):
    page_size = 20
    page_size_query_param = 'page_size'
    max_page_size = 100


class ConversationMessagesView(APIView):
    """
    GET: Get paginated messages for a conversation
    POST: Send a new message to a conversation
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        user = request.user
        conversation = get_object_or_404(Conversation, id=conversation_id)

        # Verify user is part of conversation
        if user not in [conversation.user1, conversation.user2]:
            return Response(
                {'error': 'You are not part of this conversation'},
                status=status.HTTP_403_FORBIDDEN
            )

        # Get messages with pagination
        messages = conversation.messages.select_related('sender').order_by('-created_at')
        
        paginator = MessagePagination()
        paginated_messages = paginator.paginate_queryset(messages, request)
        serializer = MessageSerializer(paginated_messages, many=True)

        return paginator.get_paginated_response(serializer.data)

    def post(self, request, conversation_id):
        user = request.user
        conversation = get_object_or_404(Conversation, id=conversation_id)

        # Verify user is part of conversation
        if user not in [conversation.user1, conversation.user2]:
            return Response(
                {'error': 'You are not part of this conversation'},
                status=status.HTTP_403_FORBIDDEN
            )

        body = request.data.get('body') or request.data.get('message')
        
        if not body or not body.strip():
            return Response(
                {'error': 'Message body cannot be empty'},
                status=status.HTTP_400_BAD_REQUEST
            )

        # Create message
        message = Message.objects.create(
            conversation=conversation,
            sender=user,
            body=body.strip()
        )

        serializer = MessageSerializer(message)
        return Response(serializer.data, status=status.HTTP_201_CREATED)


class MarkMessagesReadView(APIView):
    """
    Mark all messages in a conversation as read
    """
    permission_classes = [IsAuthenticated]

    def post(self, request, conversation_id):
        user = request.user
        conversation = get_object_or_404(Conversation, id=conversation_id)

        # Verify user is part of conversation
        if user not in [conversation.user1, conversation.user2]:
            return Response(
                {'error': 'You are not part of this conversation'},
                status=status.HTTP_403_FORBIDDEN
            )

        # Mark messages as read
        updated_count = Message.objects.filter(
            conversation=conversation,
            is_read=False
        ).exclude(sender=user).update(is_read=True)

        return Response({
            'message': 'Messages marked as read',
            'count': updated_count
        }, status=status.HTTP_200_OK)
