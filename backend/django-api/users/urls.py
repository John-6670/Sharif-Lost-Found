from django.urls import path

from .views import RegisterRequestView, RegisterVerifyView, ResendOTPView, LoginView


urlpatterns = [
    path('register/request/', RegisterRequestView.as_view(), name='register_request'),
    path('register/verify/', RegisterVerifyView.as_view(), name='register_verify'),
    path('register/resend-otp/', ResendOTPView.as_view(), name='register_resend_otp'),
    path('login/', LoginView.as_view(), name='login'),
]
