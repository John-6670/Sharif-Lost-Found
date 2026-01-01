from rest_framework import serializers
from django.contrib.auth import authenticate

from .models import User


class UserRegistrationSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = User
        fields = ['id', 'email', 'name', 'password']


class UserPublicSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'email', 'name', 'created_at', 'verified']


class OTPRequestSerializer(serializers.Serializer):
    email = serializers.EmailField()
    name = serializers.CharField(max_length=255)
    password = serializers.CharField(min_length=8)


class OTPVerifySerializer(serializers.Serializer):
    email = serializers.EmailField()
    otp = serializers.CharField(max_length=6)


class ResendOTPSerializer(serializers.Serializer):
    email = serializers.EmailField()


class LoginSerializer(serializers.Serializer):
    email = serializers.EmailField()
    password = serializers.CharField(write_only=True, min_length=8)

    def validate(self, attrs):
        user = authenticate(username=attrs['email'], password=attrs['password'])
        if not user:
            raise serializers.ValidationError('Invalid credentials')
        if not user.verified:
            raise serializers.ValidationError('User not verified')
        attrs['user'] = user
        return attrs
