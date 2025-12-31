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
            verified=verified,
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
    verified = models.BooleanField(default=False)
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
