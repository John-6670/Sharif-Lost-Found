from django.urls import path
from django.conf import settings
from django.conf.urls.static import static

from .views import (RegisterRequestView, RegisterVerifyView, ResendOTPView,
    LoginView, CustomTokenObtainPairView, CustomTokenRefreshView, PasswordResetRequestView,
    PasswordResetConfirmView, UserProfileView, PublicUserProfileView, EmailChangeView,
)


urlpatterns = [
    path('register/request/', RegisterRequestView.as_view(), name='register_request'),
    path('register/verify/', RegisterVerifyView.as_view(), name='register_verify'),
    path('register/resend-otp/', ResendOTPView.as_view(), name='register_resend_otp'),
    path('login/', CustomTokenObtainPairView.as_view(), name='token_obtain_pair'),
    path('token-refresh/', CustomTokenRefreshView.as_view(), name='token_refresh'),
    path('password-reset/request/', PasswordResetRequestView.as_view(), name='password_reset_request'),
    path('password-reset/confirm/', PasswordResetConfirmView.as_view(), name='password_reset_confirm'),
    path('profile/', UserProfileView.as_view(), name='user_profile'),
    path('profile/public/<int:user_id>/', PublicUserProfileView.as_view(), name='public_user_profile'),
    path('profile/email/', EmailChangeView.as_view(), name='email_change'),
]

urlpatterns += static(settings.MEDIA_URL, document_root=settings.MEDIA_ROOT)
