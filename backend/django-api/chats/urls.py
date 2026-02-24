from django.urls import path
from . import views

urlpatterns = [
    path('conversations/', views.ConversationListView.as_view(), name='conversation-list'),
    path('conversations/<int:conversation_id>/', views.ConversationDetailView.as_view(), name='conversation-detail'),
    path('conversations/create/', views.ConversationCreateView.as_view(), name='conversation-create'),
    path('unread-count/', views.UnreadCountView.as_view(), name='unread-count'),
]