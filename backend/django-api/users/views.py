from rest_framework import status
from rest_framework.response import Response
from rest_framework.views import APIView
from rest_framework.permissions import AllowAny
from django.utils import timezone

from datetime import timedelta

from .models import RegistrationOTP, User
from .serializers import OTPRequestSerializer, OTPVerifySerializer
from .utils import generate_otp, send_otp


class RegisterRequestView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        serializer = OTPRequestSerializer(data=request.data)
        if serializer.is_valid():
            email = serializer.validated_data['email']
            name = serializer.validated_data['name']
            password = serializer.validated_data['password']
            user = User.objects.create_user(email=email, name=name, password=password)

            old_otp = RegistrationOTP.objects.filter(email=email).first()
            if old_otp:
                old_otp.delete()

            otp = generate_otp()
            RegistrationOTP.objects.update_or_create(email=email, defaults={'otp': otp})
            send_otp(email, otp, name)
            return Response({'message': 'OTP sent to email'}, status=status.HTTP_200_OK)

        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class RegisterVerifyView(APIView):
    def post(self, request):
        serializer = OTPVerifySerializer(data=request.data)
        if serializer.is_valid():
            email = serializer.validated_data['email']
            otp = serializer.validated_data['otp']
            try:
                otp_obj = RegistrationOTP.objects.get(email=email, otp=otp)
            except RegistrationOTP.DoesNotExist:
                return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

            # Check if OTP is expired (older than 2 minutes)
            if timezone.now() - otp_obj.created_at > timedelta(minutes=2):
                otp_obj.delete()
                return Response({'error': 'OTP expired'}, status=status.HTTP_400_BAD_REQUEST)

            user = User.objects.filter(email=email).first()
            if not user:
                return Response({'error': 'Invalid OTP'}, status=status.HTTP_400_BAD_REQUEST)

            user.verified = True
            user.save()
            otp_obj.delete()
            return Response({'message': 'User registered and verified'}, status=status.HTTP_201_CREATED)

        return Response(serializer.errors, status=status.HTTP_400_BAD_REQUEST)


class ResendOTPView(APIView):
    permission_classes = [AllowAny]

    def post(self, request):
        email = request.data.get('email')
        if not email:
            return Response({'error': 'Email is required'}, status=status.HTTP_400_BAD_REQUEST)
        user = User.objects.filter(email=email).first()
        if not user or user.verified:
            return Response({'error': 'User not found or already verified'}, status=status.HTTP_400_BAD_REQUEST)
        otp = generate_otp()
        RegistrationOTP.objects.update_or_create(email=email, defaults={'otp': otp})
        send_otp(email, otp, user.name)
        return Response({'message': 'OTP resent to email'}, status=status.HTTP_200_OK)

