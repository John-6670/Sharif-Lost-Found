import hashlib

from django.contrib.auth.models import AbstractBaseUser, PermissionsMixin, BaseUserManager
from django.db import models
from django.utils import timezone
from django.contrib.auth.hashers import make_password, check_password


class UserManager(BaseUserManager):
    def create_user(self, email, name, password=None, verified=False):
        if not email:
            raise ValueError('Users must have an email address')

        email = self.normalize_email(email)
        user = self.model(
            email=email,
            name=name,
            is_verified=verified,
        )
        user.set_password(password)
        user.save(using=self._db)
        return user

    def create_superuser(self, email, name, password=None):
        user = self.create_user(email, name, password, verified=True)
        user.is_superuser = True
        user.save(using=self._db)
        return user


class User(AbstractBaseUser, PermissionsMixin):
    email = models.EmailField(unique=True, max_length=255)
    name = models.CharField(max_length=255)
    is_verified = models.BooleanField(default=False)
    last_seen = models.DateTimeField(null=True, blank=True)
    created_at = models.DateTimeField(auto_now_add=True)

    objects = UserManager()

    USERNAME_FIELD = 'email'
    REQUIRED_FIELDS = ['name']

    def __str__(self):
        return self.email

    def save(self, *args, **kwargs):
        if not self.last_seen:
            self.last_seen = timezone.now()
        super().save(*args, **kwargs)

    @property
    def is_staff(self):
        return self.verified


class RegistrationOTP(models.Model):
    email = models.EmailField()
    otp_hash = models.CharField(max_length=128)
    created_at = models.DateTimeField(auto_now_add=True)

    def set_otp(self, otp_plaintext):
        """Hash OTP before saving"""
        self.otp_hash = make_password(otp_plaintext)

    def verify_otp(self, otp_plaintext):
        """Compare provided OTP with stored hash"""
        return check_password(otp_plaintext, self.otp_hash)

    def is_expired(self):
        """Check if OTP is older than 2 minutes"""
        return timezone.now() > self.created_at + timezone.timedelta(minutes=2)


class UserProfile(models.Model):
    user = models.OneToOneField(User, on_delete=models.CASCADE, related_name="profile")
    profile_pic = models.ImageField(upload_to='profile_pics', null=True, blank=True)
    bio = models.TextField(null=True, blank=True)
    phone_number = models.CharField(max_length=15, null=True, blank=True)
    is_public = models.BooleanField(default=False)
    # items_reported_found = models.PositiveIntegerField(default=0)
    # items_reported_missing = models.PositiveIntegerField(default=0)
    preferred_contact_method = models.CharField(default='email', max_length=50,
                                        choices=[('email', 'Email'), ('phone', 'Phone'), ('social', 'Social Media')])
    social_media_links = models.JSONField(default=dict, blank=True)  # e.g. {"facebook": "url", "twitter": "url"}
    department = models.CharField(max_length=100, null=True, blank=True)

    def __str__(self):
        return f"Profile of {self.user.email}"
