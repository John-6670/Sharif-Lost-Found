from django.urls import path

from .views import ItemListCreateView, ItemDetailView, ReportItemView


urlpatterns = [
    path('', ItemListCreateView.as_view(), name='item_list_create'),
    path('<int:item_id>/', ItemDetailView.as_view(), name='item_detail'),
    path('<int:item_id>/report/', ReportItemView.as_view(), name='report_item'),
]