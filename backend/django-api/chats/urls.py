from django.urls import path
from . import views

urlpatterns = [
    # Conversation endpoints
    path('conversations/', views.ConversationListView.as_view(), name='conversation-list'),
    path('conversations/<int:conversation_id>/', views.ConversationDetailView.as_view(), name='conversation-detail'),
    path('conversations/create/', views.ConversationCreateView.as_view(), name='conversation-create'),
    
    # Message endpoints
    path('conversations/<int:conversation_id>/messages/', views.ConversationMessagesView.as_view(), name='conversation-messages'),
    path('conversations/<int:conversation_id>/mark-read/', views.MarkMessagesReadView.as_view(), name='mark-messages-read'),
    
    # Utility endpoints
    path('unread-count/', views.UnreadCountView.as_view(), name='unread-count'),
]