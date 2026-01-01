from django.urls import path

from .views import (
    RegisterRequestView,
    RegisterVerifyView,
    ResendOTPView,
    LoginView,
    CustomTokenObtainPairView,
    CustomTokenRefreshView
)


urlpatterns = [
    path('register/request/', RegisterRequestView.as_view(), name='register_request'),
    path('register/verify/', RegisterVerifyView.as_view(), name='register_verify'),
    path('register/resend-otp/', ResendOTPView.as_view(), name='register_resend_otp'),
    path('login/', LoginView.as_view(), name='login'),
    path('token/', CustomTokenObtainPairView.as_view(), name='token_obtain_pair'),
    path('token/refresh/', CustomTokenRefreshView.as_view(), name='token_refresh'),
]
