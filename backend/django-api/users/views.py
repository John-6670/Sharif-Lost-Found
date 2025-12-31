from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.permissions import AllowAny
from django.utils import timezone
from datetime import timedelta

from .models import RegistrationOTP, User
from .serializers import OTPRequestSerializer, OTPVerifySerializer
from .utils import generate_otp, send_otp
from .throttles import OTPIPRateThrottle, OTPEmailRateThrottle, OTPVerifyRateThrottle


class RegisterRequestView(APIView):
    permission_classes = [AllowAny]
    throttle_classes = [
        OTPIPRateThrottle,
        OTPEmailRateThrottle,
    ]

    def post(self, request):
        serializer = OTPRequestSerializer(data=request.data)
        if serializer.is_valid():
            email = serializer.validated_data['email']
            name = serializer.validated_data['name']
            password = serializer.validated_data['password']

            # Create user
            user = User.objects.create_user(email=email, name=name, password=password)

            # Delete any old OTP for this email
            RegistrationOTP.objects.filter(email=email).delete()

            # Generate and hash OTP
            otp = generate_otp()
            otp_obj, _ = RegistrationOTP.objects.update_or_create(email=email)
            otp_obj.set_otp(otp)
            otp_obj.save()

            send_otp(email, otp, name)
            return Response({'message': 'OTP sent to email'}, status=status.HTTP_200_OK)

        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class RegisterVerifyView(APIView):
    permission_classes = [AllowAny]
    throttle_classes = [OTPVerifyRateThrottle]

    def post(self, request):
        serializer = OTPVerifySerializer(data=request.data)
        if serializer.is_valid():
            email = serializer.validated_data['email']
            otp = serializer.validated_data['otp']

            try:
                otp_obj = RegistrationOTP.objects.get(email=email)
            except RegistrationOTP.DoesNotExist:
                return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

            # Check expiration
            if otp_obj.is_verified():
                return Response({'error': 'OTP has expired'}, status=status.HTTP_400_BAD_REQUEST)

            # Verify OTP hash
            if not otp_obj.verify_otp(otp):
                return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

            # Mark user as verified
            user = User.objects.filter(email=email).first()
            if not user:
                return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

            user.verified = True
            user.save()

            otp_obj.delete()  # Delete OTP after use
            return Response({'message': 'User registered and verified'}, status=status.HTTP_201_CREATED)

        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class ResendOTPView(APIView):
    permission_classes = [AllowAny]
    throttle_classes = [
        OTPIPRateThrottle,
        OTPEmailRateThrottle,
    ]

    def post(self, request):
        email = request.data.get('email')
        if not email:
            return Response({'error': 'Email is required'}, status=status.HTTP_400_BAD_REQUEST)

        user = User.objects.filter(email=email).first()
        if not user or user.verified:
            return Response({'error': 'User not found or already verified'}, status=status.HTTP_400_BAD_REQUEST)

        # Generate and hash new OTP
        otp = generate_otp()
        otp_obj, _ = RegistrationOTP.objects.update_or_create(email=email)
        otp_obj.set_otp(otp)
        otp_obj.save()

        send_otp(email, otp, user.name)
        return Response({'message': 'OTP resent to email'}, status=status.HTTP_200_OK)
