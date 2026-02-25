from django.contrib.auth import get_user_model
from rest_framework import serializers

from .models import *
from .models import Conversation, Message

User = get_user_model()


class ConversationSerializer(serializers.ModelSerializer):
    other_user = serializers.SerializerMethodField()
    last_message = serializers.SerializerMethodField()
    unread_count = serializers.SerializerMethodField()

    class Meta:
        model = Conversation
        fields = [
            "id",
            "other_user",
            "last_message",
            "unread_count",
            "created_at",
        ]

    def get_other_user(self, obj):
        request = self.context.get("request")
        user = request.user

        other = obj.user2 if obj.user1 == user else obj.user1

        return {
            "id": other.id,
            "name": other.name,
            "email": other.email,
        }

    def get_last_message(self, obj):
        last_msg = obj.messages.order_by("-created_at").first()

        if not last_msg:
            return None
        return MessageSerializer(last_msg, context=self.context).data

    def get_unread_count(self, obj):
        request = self.context.get("request")
        user = request.user

        return obj.messages.filter(is_read=False).exclude(sender=user).count()


class MessageSerializer(serializers.ModelSerializer):
    sender_name = serializers.CharField(source="sender.name", read_only=True)
    sender_id = serializers.IntegerField(source="sender.id", read_only=True)

    class Meta:
        model = Message
        fields = ["id", "conversation", "sender", "sender_id", "sender_name", "body", "created_at", "is_read"]
        read_only_fields = ["sender", "sender_id", "sender_name", "created_at", "is_read"]


class GlobalMessageSerializer(serializers.ModelSerializer):
    sender_name = serializers.CharField(source="sender.name", read_only=True)

    class Meta:
        model = GlobalMessage
        fields = ["id", "sender", "sender_name", "body", "created_at"]
        read_only_fields = ["sender"]
