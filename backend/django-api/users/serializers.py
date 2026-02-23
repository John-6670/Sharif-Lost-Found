from rest_framework import serializers
from django.contrib.auth import authenticate
from rest_framework_simplejwt.serializers import TokenObtainPairSerializer
from rest_framework_simplejwt.exceptions import AuthenticationFailed

from .models import User, UserProfile


class UserRegistrationSerializer(serializers.ModelSerializer):
    password = serializers.CharField(write_only=True, min_length=8)

    class Meta:
        model = User
        fields = ['id', 'email', 'name', 'password']


class UserPublicSerializer(serializers.ModelSerializer):
    class Meta:
        model = User
        fields = ['id', 'email', 'name', 'created_at', 'is_verified']


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
        if not user.is_verified:
            raise serializers.ValidationError('User not verified')
        attrs['user'] = user
        return attrs


class UserProfileSerializer(serializers.ModelSerializer):
    user_name = serializers.CharField(source='user.name', required=False)

    class Meta:
        model = UserProfile
        fields = ['profile_pic', 'phone_number', 'social_media_links',
                  'user_name', 'preferred_contact_method', 'bio', 'department',
                  'is_public']

    def update(self, instance, validated_data):
        user_data = validated_data.pop('user', {})
        user = instance.user

        # Update user fields
        if 'name' in user_data:
            user.name = user_data['name']
            user.save()

        # Update profile fields
        return super().update(instance, validated_data)


class PublicUserProfileSerializer(serializers.ModelSerializer):
    user_id = serializers.IntegerField(source='user.id', read_only=True)
    user_name = serializers.CharField(source='user.name', read_only=True)
    email = serializers.SerializerMethodField()
    phone_number = serializers.SerializerMethodField()

    class Meta:
        model = UserProfile
        fields = ['user_id', 'user_name', 'profile_pic', 'bio', 'department',
                  'social_media_links', 'preferred_contact_method', 'is_public',
                  'email', 'phone_number']

    def get_email(self, obj):
        return obj.user.email if obj.is_public else None

    def get_phone_number(self, obj):
        return obj.phone_number if obj.is_public else None


class CustomTokenObtainPairSerializer(TokenObtainPairSerializer):
    @classmethod
    def get_token(cls, user):
        token = super().get_token(user)
        token['email'] = user.email
        token['name'] = user.name
        token['is_verified'] = user.is_verified
        return token

    def validate(self, attrs):
        # first, authenticate normally
        data = super().validate(attrs)

        # replace `is_active` check with `is_verified`
        user = self.user
        if not getattr(user, "is_verified", False):
            raise AuthenticationFailed("Account is not verified.", "no_active_account")

        data["user_id"] = str(user.id)
        data["email"] = user.email
        data["name"] = user.name

        return data


class PasswordResetSerializer(serializers.Serializer):
    email = serializers.EmailField()
    otp = serializers.CharField(max_length=6)
    new_password = serializers.CharField(write_only=True, min_length=8)


class EmailChangeSerializer(serializers.Serializer):
    new_email = serializers.EmailField()
    current_password = serializers.CharField(write_only=True, min_length=8)

    def validate(self, attrs):
        user = self.context.get('user')
        if not user or not user.check_password(attrs['current_password']):
            raise serializers.ValidationError('Invalid password')

        new_email = attrs['new_email']
        if User.objects.filter(email=new_email).exclude(id=user.id).exists():
            raise serializers.ValidationError('Email already in use')

        return attrs
