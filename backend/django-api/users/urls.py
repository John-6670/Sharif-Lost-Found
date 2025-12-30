from django.urls import path

from .views import RegisterRequestView, RegisterVerifyView


urlpatterns = [
    path('register/request/', RegisterRequestView.as_view(), name='register_request'),
    path('register/verify/', RegisterVerifyView.as_view(), name='register_verify'),
]
