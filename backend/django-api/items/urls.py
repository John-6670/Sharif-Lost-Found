from django.urls import path

from .views import (ItemListCreateView, ItemDetailView, ReportItemView,
                    CommentListCreateView, ReportCommentView)


urlpatterns = [
    path('', ItemListCreateView.as_view(), name='item_list_create'),
    path('<int:item_id>/', ItemDetailView.as_view(), name='item_detail'),
    path('<int:item_id>/report/', ReportItemView.as_view(), name='report_item'),
    path('<int:item_id>/comments', CommentListCreateView.as_view(), name='comment_list_create'),
    path('<int:item_id>/comments/<int:comment_id>/report/', ReportCommentView.as_view(), name='report_comment')
]
