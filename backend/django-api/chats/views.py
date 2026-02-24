from rest_framework import generics, status
from rest_framework.decorators import api_view, permission_classes
from rest_framework.response import Response
from rest_framework.permissions import IsAuthenticated
from django.db.models import Q, Max, Count
from django.shortcuts import get_object_or_404
from rest_framework.views import APIView

from .models import Conversation, Message
from .serializers import MessageSerializer

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
            last_message_time=Max('messages__created_at'),
            unread_count=Count(
                'messages',
                filter=Q(messages__is_read=False) & ~Q(messages__sender=user)
                # Changed from Q(messages__sender__ne=user)
                filter=Q(messages__is_read=False) & ~Q(messages__sender=user)  # Changed from Q(messages__sender__ne=user)
            )
        ).order_by('-last_message_time')

        data = []
        for conv in conversations:
            other_user = conv.user2 if conv.user1 == user else conv.user1
            last_message = conv.messages.order_by('-created_at').first()

            data.append({
                'id': conv.id,
                'other_user': {
                    'id': other_user.id,
                    'name': other_user.name,
                },
                'last_message': {
                    'body': last_message.body if last_message else None,
                    'created_at': last_message.created_at if last_message else None,
                    'sender_id': last_message.sender.id if last_message else None,
                } if last_message else None,
                'unread_count': conv.unread_count,
                'created_at': conv.created_at,
            })

        return Response(data, status=status.HTTP_200_OK)


class ConversationDetailView(generics.RetrieveAPIView):
    """
    Get a specific conversation with all its messages
    """
    permission_classes = [IsAuthenticated]

    def get(self, request, conversation_id):
        user = request.user
        conversation = get_object_or_404(
            Conversation,
            id=conversation_id
        )

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
        ).exclude(sender=user).update(is_read=True)  # Changed from filter with sender__ne

        # Get messages
        messages = conversation.messages.select_related('sender').order_by('created_at')
        serializer = MessageSerializer(messages, many=True)

        other_user = conversation.user2 if conversation.user1 == user else conversation.user1

        return Response({
            'id': conversation.id,
            'other_user': {
                'id': other_user.id,
                'name': other_user.name,
            },
            'messages': serializer.data,
        }, status=status.HTTP_200_OK)


class ConversationCreateView(APIView):
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

        # Check if conversation already exists (in either direction)
        conversation = Conversation.objects.filter(
            Q(user1=user, user2_id=other_user_id) |
            Q(user1_id=other_user_id, user2=user)
        ).first()

        if conversation:
            return Response({
                'id': conversation.id,
                'message': 'Conversation already exists'
            }, status=status.HTTP_200_OK)

        # Create new conversation

        other_user = get_object_or_404(User, id=other_user_id)

        from users.models import User
        other_user = get_object_or_404(User, id=other_user_id)

        conversation = Conversation.objects.create(
            user1=user,
            user2=other_user
        )

        return Response({
            'id': conversation.id,
            'message': 'Conversation created successfully'
        }, status=status.HTTP_201_CREATED)


class UnreadCountView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        user = request.user
        unread_count = Message.objects.filter(
            conversation__user1=user,
            is_read=False
        ).exclude(sender=user).count() + Message.objects.filter(  # Changed from filter with sender__ne
            conversation__user2=user,
            is_read=False
        ).exclude(sender=user).count()  # Changed from filter with sender__ne

        return Response({
            'unread_count': unread_count
        }, status=status.HTTP_200_OK)
