from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
from drf_yasg.utils import swagger_auto_schema
from rest_framework_simplejwt.views import TokenObtainPairView, TokenRefreshView
from rest_framework.permissions import IsAuthenticated

from .models import RegistrationOTP, User, UserProfile
from .serializers import (
    OTPRequestSerializer,
    OTPVerifySerializer,
    ResendOTPSerializer,
    UserPublicSerializer,
    LoginSerializer,
    CustomTokenObtainPairSerializer,
    PasswordResetSerializer,
    UserProfileSerializer
)
from .utils import generate_otp, send_otp
from .throttles import OTPIPRateThrottle, OTPEmailRateThrottle, OTPVerifyRateThrottle


class RegisterRequestView(APIView):
    throttle_classes = [OTPIPRateThrottle, OTPEmailRateThrottle]

    @swagger_auto_schema(
        request_body=OTPRequestSerializer,
        responses={200: 'OTP sent to email', 400: 'Validation error'},
    )
    def post(self, request):
        serializer = OTPRequestSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        email = serializer.validated_data['email']
        name = serializer.validated_data['name']
        password = serializer.validated_data['password']

        user, _ = User.objects.get_or_create(email=email, defaults={'name': name})
        user.set_password(password)
        user.save()

        RegistrationOTP.objects.filter(email=email).delete()
        otp = generate_otp()
        otp_obj, _ = RegistrationOTP.objects.update_or_create(email=email)
        otp_obj.set_otp(otp)
        otp_obj.save()

        send_otp(email, otp, name)
        return Response({'message': 'OTP sent to email'}, status=status.HTTP_200_OK)


class RegisterVerifyView(APIView):
    throttle_classes = [OTPVerifyRateThrottle]

    @swagger_auto_schema(
        request_body=OTPVerifySerializer,
        responses={201: UserPublicSerializer, 400: 'Invalid or expired OTP'},
    )
    def post(self, request):
        serializer = OTPVerifySerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        email = serializer.validated_data['email']
        otp = serializer.validated_data['otp']

        try:
            otp_obj = RegistrationOTP.objects.get(email=email)
        except RegistrationOTP.DoesNotExist:
            return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

        if otp_obj.is_expired():
            otp_obj.delete()
            return Response({'error': 'OTP has expired'}, status=status.HTTP_400_BAD_REQUEST)

        if not otp_obj.verify_otp(otp):
            return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

        user = User.objects.filter(email=email).first()
        if not user:
            return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

        user.verified = True
        user.save()
        otp_obj.delete()
        return Response(UserPublicSerializer(user).data, status=status.HTTP_201_CREATED)


class ResendOTPView(APIView):
    throttle_classes = [OTPIPRateThrottle, OTPEmailRateThrottle]

    @swagger_auto_schema(
        request_body=ResendOTPSerializer,
        responses={200: 'OTP resent to email', 400: 'User not found or already verified'},
    )
    def post(self, request):
        serializer = ResendOTPSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        email = serializer.validated_data['email']
        user = User.objects.filter(email=email).first()
        if not user or user.verified:
            return Response({'error': 'User not found or already verified'}, status=status.HTTP_400_BAD_REQUEST)

        otp = generate_otp()
        otp_obj, _ = RegistrationOTP.objects.update_or_create(email=email)
        otp_obj.set_otp(otp)
        otp_obj.save()

        send_otp(email, otp, user.name)
        return Response({'message': 'OTP resent to email'}, status=status.HTTP_200_OK)


class LoginView(APIView):
    @swagger_auto_schema(
        request_body=LoginSerializer,
        responses={200: UserPublicSerializer, 400: 'Invalid credentials'}
    )
    def post(self, request):
        serializer = LoginSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        user = serializer.validated_data['user']
        return Response(UserPublicSerializer(user).data, status=status.HTTP_200_OK)


class CustomTokenObtainPairView(TokenObtainPairView):
    serializer_class = CustomTokenObtainPairSerializer


class CustomTokenRefreshView(TokenRefreshView):
    pass


class PasswordResetRequestView(APIView):
    throttle_classes = [OTPIPRateThrottle, OTPEmailRateThrottle]

    def post(self, request):
        serializer = ResendOTPSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        email = serializer.validated_data['email']
        user = User.objects.filter(email=email).first()
        if not user:
            return Response({'error': 'User not found'}, status=status.HTTP_400_BAD_REQUEST)

        # generate OTP
        otp = generate_otp()
        otp_obj, _ = RegistrationOTP.objects.update_or_create(email=email)
        otp_obj.set_otp(otp)
        otp_obj.save()

        send_otp(email, otp, getattr(user, "name", ""))
        return Response({'message': 'Password reset OTP sent'}, status=status.HTTP_200_OK)


class PasswordResetConfirmView(APIView):
    throttle_classes = [OTPVerifyRateThrottle]

    def post(self, request):
        serializer = PasswordResetSerializer(data=request.data)
        if not serializer.is_valid():
            return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)

        email = serializer.validated_data['email']
        otp = serializer.validated_data['otp']
        new_password = serializer.validated_data['new_password']

        try:
            otp_obj = RegistrationOTP.objects.get(email=email)
        except RegistrationOTP.DoesNotExist:
            return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

        if otp_obj.is_expired():
            otp_obj.delete()
            return Response({'error': 'OTP has expired'}, status=status.HTTP_400_BAD_REQUEST)

        if not otp_obj.verify_otp(otp):
            return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

        # reset password
        user = User.objects.filter(email=email).first()
        if not user:
            return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

        user.set_password(new_password)
        user.save()
        otp_obj.delete()

        return Response({'message': 'Password has been reset successfully'}, status=status.HTTP_200_OK)


class UserProfileView(APIView):
    permission_classes = [IsAuthenticated]

    def get(self, request):
        profile, _ = UserProfile.objects.get_or_create(user=request.user)
        serializer = UserProfileSerializer(profile)
        return Response(serializer.data, status=status.HTTP_200_OK)

    def put(self, request):
        profile, _ = UserProfile.objects.get_or_create(user=request.user)
        serializer = UserProfileSerializer(profile, data=request.data, partial=True)
        if serializer.is_valid():
            serializer.save()
            return Response(serializer.data, status=status.HTTP_200_OK)
        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)
