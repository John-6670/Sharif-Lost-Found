from django.test import TestCase
from django.utils import timezone
from django.contrib.auth import authenticate
from rest_framework.test import APITestCase, APIClient
from rest_framework import status
from unittest.mock import patch

from .models import User, UserProfile, RegistrationOTP
from .serializers import (UserPublicSerializer, OTPRequestSerializer, OTPVerifySerializer,
    ResendOTPSerializer, LoginSerializer, UserProfileSerializer, PublicUserProfileSerializer,
    PasswordResetSerializer, EmailChangeSerializer
)
from .utils import generate_otp, hash_otp, send_otp


# ============================================================================
# MODEL TESTS
# ============================================================================

class UserModelTests(TestCase):
    """Test cases for the User model"""

    def setUp(self):
        """Set up test fixtures"""
        self.user = User.objects.create_user(
            email='test@example.com',
            name='Test User',
            password='securepassword123',
            is_verified=False
        )

    def test_user_creation(self):
        """Test basic user creation"""
        self.assertEqual(self.user.email, 'test@example.com')
        self.assertEqual(self.user.name, 'Test User')
        self.assertFalse(self.user.is_verified)

    def test_user_email_unique(self):
        """Test that email must be unique"""
        with self.assertRaises(Exception):
            User.objects.create_user(
                email='test@example.com',
                name='Another User',
                password='password123'
            )

    def test_user_password_hashing(self):
        """Test that password is hashed"""
        self.assertNotEqual(self.user.password, 'securepassword123')
        self.assertTrue(self.user.check_password('securepassword123'))

    def test_user_without_email_raises_error(self):
        """Test that creating user without email raises error"""
        with self.assertRaises(ValueError):
            User.objects.create_user(
                email='',
                name='Test User',
                password='password123'
            )

    def test_user_string_representation(self):
        """Test user __str__ method"""
        self.assertEqual(str(self.user), 'test@example.com')

    def test_user_last_seen_auto_set(self):
        """Test that last_seen is set on user creation"""
        self.assertIsNotNone(self.user.last_seen)

    def test_user_created_at_timestamp(self):
        """Test that created_at timestamp is set"""
        self.assertIsNotNone(self.user.created_at)

    def test_superuser_creation(self):
        """Test superuser creation"""
        superuser = User.objects.create_superuser(
            email='admin@example.com',
            name='Admin User',
            password='adminpass123'
        )
        self.assertTrue(superuser.is_superuser)
        self.assertTrue(superuser.is_verified)

    def test_is_staff_property(self):
        """Test is_staff property returns verified status"""
        self.assertFalse(self.user.is_staff)
        self.user.is_verified = True
        self.assertTrue(self.user.is_staff)

    def test_username_field_is_email(self):
        """Test that USERNAME_FIELD is set to email"""
        self.assertEqual(User.USERNAME_FIELD, 'email')

    def test_required_fields(self):
        """Test required fields for user creation"""
        self.assertIn('name', User.REQUIRED_FIELDS)


class UserProfileModelTests(TestCase):
    """Test cases for the UserProfile model"""

    def setUp(self):
        """Set up test fixtures"""
        self.user = User.objects.create_user(
            email='profile@example.com',
            name='Profile User',
            password='password123'
        )
        self.profile = UserProfile.objects.create(
            user=self.user,
            bio='Test bio',
            phone_number='03001234567',
            is_public=True,
            department='Computer Science'
        )

    def test_profile_creation(self):
        """Test basic profile creation"""
        self.assertEqual(self.profile.user, self.user)
        self.assertEqual(self.profile.bio, 'Test bio')
        self.assertEqual(self.profile.phone_number, '03001234567')

    def test_profile_one_to_one_relationship(self):
        """Test one-to-one relationship between User and UserProfile"""
        self.assertEqual(self.user.profile, self.profile)

    def test_profile_default_values(self):
        """Test default values for profile fields"""
        new_user = User.objects.create_user(
            email='default@example.com',
            name='Default User',
            password='password123'
        )
        new_profile = UserProfile.objects.create(user=new_user)
        
        self.assertEqual(new_profile.preferred_contact_method, 'email')
        self.assertFalse(new_profile.is_public)
        self.assertEqual(new_profile.social_media_links, {})

    def test_profile_string_representation(self):
        """Test profile __str__ method"""
        expected = f"Profile of {self.user.email}"
        self.assertEqual(str(self.profile), expected)

    def test_social_media_links_json_field(self):
        """Test social media links JSON field"""
        self.profile.social_media_links = {
            'facebook': 'https://facebook.com/user',
            'twitter': 'https://twitter.com/user'
        }
        self.profile.save()
        
        self.assertEqual(
            self.profile.social_media_links['facebook'],
            'https://facebook.com/user'
        )

    def test_profile_contact_method_choices(self):
        """Test preferred contact method choices"""
        self.assertIn(self.profile.preferred_contact_method, ['email', 'phone', 'social'])
        
        self.profile.preferred_contact_method = 'phone'
        self.profile.save()
        self.assertEqual(self.profile.preferred_contact_method, 'phone')

    def test_profile_is_public_toggle(self):
        """Test is_public toggle"""
        self.assertTrue(self.profile.is_public)
        self.profile.is_public = False
        self.profile.save()
        self.assertFalse(self.profile.is_public)


class RegistrationOTPModelTests(TestCase):
    """Test cases for the RegistrationOTP model"""

    def setUp(self):
        """Set up test fixtures"""
        self.otp_record = RegistrationOTP.objects.create(email='otp@example.com')
        self.test_otp = '123456'
        self.otp_record.set_otp(self.test_otp)
        self.otp_record.save()

    def test_otp_creation(self):
        """Test OTP record creation"""
        self.assertEqual(self.otp_record.email, 'otp@example.com')
        self.assertIsNotNone(self.otp_record.otp_hash)

    def test_otp_hashing(self):
        """Test that OTP is hashed"""
        self.assertNotEqual(self.otp_record.otp_hash, self.test_otp)

    def test_otp_verification_success(self):
        """Test successful OTP verification"""
        self.assertTrue(self.otp_record.verify_otp(self.test_otp))

    def test_otp_verification_failure(self):
        """Test failed OTP verification with wrong code"""
        self.assertFalse(self.otp_record.verify_otp('000000'))

    def test_otp_expiration_check_valid(self):
        """Test OTP is not expired when fresh"""
        self.assertFalse(self.otp_record.is_expired())

    def test_otp_expiration_check_expired(self):
        """Test OTP expiration after 2 minutes"""
        # Mock timezone.now() to be 3 minutes in the future
        old_created_at = timezone.now() - timezone.timedelta(minutes=3)
        self.otp_record.created_at = old_created_at
        self.otp_record.save()
        
        self.assertTrue(self.otp_record.is_expired())

    def test_otp_created_at_timestamp(self):
        """Test that created_at is set on creation"""
        self.assertIsNotNone(self.otp_record.created_at)


# ============================================================================
# SERIALIZER TESTS
# ============================================================================

class OTPRequestSerializerTests(TestCase):
    """Test cases for OTPRequestSerializer"""

    def test_valid_otp_request(self):
        """Test valid OTP request data"""
        data = {
            'email': 'test@example.com',
            'name': 'Test User',
            'password': 'securepass123'
        }
        serializer = OTPRequestSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_invalid_email(self):
        """Test invalid email validation"""
        data = {
            'email': 'invalid-email',
            'name': 'Test User',
            'password': 'securepass123'
        }
        serializer = OTPRequestSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('email', serializer.errors)

    def test_missing_required_fields(self):
        """Test missing required fields"""
        data = {'email': 'test@example.com'}
        serializer = OTPRequestSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('name', serializer.errors)
        self.assertIn('password', serializer.errors)

    def test_short_password(self):
        """Test password minimum length validation"""
        data = {
            'email': 'test@example.com',
            'name': 'Test User',
            'password': 'short'
        }
        serializer = OTPRequestSerializer(data=data)
        self.assertFalse(serializer.is_valid())


class OTPVerifySerializerTests(TestCase):
    """Test cases for OTPVerifySerializer"""

    def test_valid_otp_verify(self):
        """Test valid OTP verification data"""
        data = {
            'email': 'test@example.com',
            'otp': '123456'
        }
        serializer = OTPVerifySerializer(data=data)
        self.assertTrue(serializer.is_valid())


class ResendOTPSerializerTests(TestCase):
    """Test cases for ResendOTPSerializer"""

    def test_valid_resend_otp_request(self):
        """Test valid resend OTP request"""
        data = {'email': 'test@example.com'}
        serializer = ResendOTPSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_invalid_email_format(self):
        """Test invalid email format"""
        data = {'email': 'not-an-email'}
        serializer = ResendOTPSerializer(data=data)
        self.assertFalse(serializer.is_valid())


class LoginSerializerTests(TestCase):
    """Test cases for LoginSerializer"""

    def setUp(self):
        """Set up test fixtures"""
        self.user = User.objects.create_user(
            email='login@example.com',
            name='Login User',
            password='password123',
            is_verified=True
        )

    def test_valid_login(self):
        """Test valid login credentials"""
        data = {
            'email': 'login@example.com',
            'password': 'password123'
        }
        serializer = LoginSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_invalid_password(self):
        """Test login with wrong password"""
        data = {
            'email': 'login@example.com',
            'password': 'wrongpassword'
        }
        serializer = LoginSerializer(data=data)
        self.assertFalse(serializer.is_valid())

    def test_unverified_user_cannot_login(self):
        """Test that unverified user cannot login"""
        unverified_user = User.objects.create_user(
            email='unverified@example.com',
            name='Unverified User',
            password='password123',
            is_verified=False
        )
        data = {
            'email': 'unverified@example.com',
            'password': 'password123'
        }
        serializer = LoginSerializer(data=data)
        self.assertFalse(serializer.is_valid())
        self.assertIn('non_field_errors', serializer.errors)


class UserPublicSerializerTests(TestCase):
    """Test cases for UserPublicSerializer"""

    def setUp(self):
        """Set up test fixtures"""
        self.user = User.objects.create_user(
            email='public@example.com',
            name='Public User',
            password='password123'
        )

    def test_serializer_fields(self):
        """Test that serializer contains correct fields"""
        serializer = UserPublicSerializer(self.user)
        expected_fields = {'id', 'email', 'name', 'created_at', 'is_verified'}
        self.assertEqual(set(serializer.data.keys()), expected_fields)

    def test_serializer_data(self):
        """Test serialized data"""
        serializer = UserPublicSerializer(self.user)
        self.assertEqual(serializer.data['email'], 'public@example.com')
        self.assertEqual(serializer.data['name'], 'Public User')
        self.assertFalse(serializer.data['is_verified'])


class UserProfileSerializerTests(TestCase):
    """Test cases for UserProfileSerializer"""

    def setUp(self):
        """Set up test fixtures"""
        self.user = User.objects.create_user(
            email='profileuser@example.com',
            name='Profile User',
            password='password123'
        )
        self.profile = UserProfile.objects.create(
            user=self.user,
            bio='Test bio',
            phone_number='03001234567'
        )

    def test_serializer_read(self):
        """Test reading profile with serializer"""
        serializer = UserProfileSerializer(self.profile)
        self.assertEqual(serializer.data['bio'], 'Test bio')
        self.assertEqual(serializer.data['phone_number'], '03001234567')

    def test_serializer_update(self):
        """Test updating profile with serializer"""
        data = {
            'bio': 'Updated bio',
            'phone_number': '03009876543'
        }
        serializer = UserProfileSerializer(self.profile, data=data, partial=True)
        self.assertTrue(serializer.is_valid())
        serializer.save()
        
        self.profile.refresh_from_db()
        self.assertEqual(self.profile.bio, 'Updated bio')
        self.assertEqual(self.profile.phone_number, '03009876543')


class PublicUserProfileSerializerTests(TestCase):
    """Test cases for PublicUserProfileSerializer"""

    def setUp(self):
        """Set up test fixtures"""
        self.user = User.objects.create_user(
            email='publicprofile@example.com',
            name='Public Profile User',
            password='password123'
        )
        self.profile = UserProfile.objects.create(
            user=self.user,
            bio='Public bio',
            phone_number='03001234567',
            is_public=True
        )

    def test_public_profile_shows_email(self):
        """Test that email is shown for public profiles"""
        serializer = PublicUserProfileSerializer(self.profile)
        self.assertEqual(serializer.data['email'], 'publicprofile@example.com')

    def test_private_profile_hides_email(self):
        """Test that email is hidden for private profiles"""
        self.profile.is_public = False
        self.profile.save()
        
        serializer = PublicUserProfileSerializer(self.profile)
        self.assertIsNone(serializer.data['email'])

    def test_public_profile_shows_phone(self):
        """Test that phone is shown for public profiles"""
        serializer = PublicUserProfileSerializer(self.profile)
        self.assertEqual(serializer.data['phone_number'], '03001234567')

    def test_private_profile_hides_phone(self):
        """Test that phone is hidden for private profiles"""
        self.profile.is_public = False
        self.profile.save()
        
        serializer = PublicUserProfileSerializer(self.profile)
        self.assertIsNone(serializer.data['phone_number'])


class PasswordResetSerializerTests(TestCase):
    """Test cases for PasswordResetSerializer"""

    def test_valid_password_reset_data(self):
        """Test valid password reset data"""
        data = {
            'email': 'reset@example.com',
            'otp': '123456',
            'new_password': 'newpassword123'
        }
        serializer = PasswordResetSerializer(data=data)
        self.assertTrue(serializer.is_valid())

    def test_short_new_password(self):
        """Test new password minimum length"""
        data = {
            'email': 'reset@example.com',
            'otp': '123456',
            'new_password': 'short'
        }
        serializer = PasswordResetSerializer(data=data)
        self.assertFalse(serializer.is_valid())


class EmailChangeSerializerTests(TestCase):
    """Test cases for EmailChangeSerializer"""

    def setUp(self):
        """Set up test fixtures"""
        self.user = User.objects.create_user(
            email='changemail@example.com',
            name='Email Change User',
            password='password123'
        )

    def test_valid_email_change(self):
        """Test valid email change request"""
        data = {
            'new_email': 'newemail@example.com',
            'current_password': 'password123'
        }
        serializer = EmailChangeSerializer(data=data, context={'user': self.user})
        self.assertTrue(serializer.is_valid())

    def test_invalid_current_password(self):
        """Test email change with wrong password"""
        data = {
            'new_email': 'newemail@example.com',
            'current_password': 'wrongpassword'
        }
        serializer = EmailChangeSerializer(data=data, context={'user': self.user})
        self.assertFalse(serializer.is_valid())

    def test_duplicate_email(self):
        """Test email change to existing email"""
        User.objects.create_user(
            email='existing@example.com',
            name='Existing User',
            password='password123'
        )
        
        data = {
            'new_email': 'existing@example.com',
            'current_password': 'password123'
        }
        serializer = EmailChangeSerializer(data=data, context={'user': self.user})
        self.assertFalse(serializer.is_valid())


# ============================================================================
# UTILITY TESTS
# ============================================================================

class GenerateOTPTests(TestCase):
    """Test cases for OTP generation"""

    def test_otp_format(self):
        """Test that OTP is 6 digits"""
        otp = generate_otp()
        self.assertEqual(len(otp), 6)
        self.assertTrue(otp.isdigit())

    def test_otp_range(self):
        """Test that OTP is in valid range"""
        for _ in range(100):
            otp = int(generate_otp())
            self.assertGreaterEqual(otp, 100000)
            self.assertLessEqual(otp, 999999)

    def test_otp_uniqueness(self):
        """Test that generated OTPs are different"""
        otps = [generate_otp() for _ in range(10)]
        # Not guaranteed to be unique, but highly likely
        # This is just a sanity check


class HashOTPTests(TestCase):
    """Test cases for OTP hashing"""

    def test_hash_consistency(self):
        """Test that same input produces same hash"""
        otp = '123456'
        hash1 = hash_otp(otp)
        hash2 = hash_otp(otp)
        self.assertEqual(hash1, hash2)

    def test_different_otps_different_hashes(self):
        """Test that different OTPs produce different hashes"""
        hash1 = hash_otp('123456')
        hash2 = hash_otp('654321')
        self.assertNotEqual(hash1, hash2)


class SendOTPTests(TestCase):
    """Test cases for sending OTP"""

    @patch('users.utils.send_mail')
    def test_send_otp_called(self, mock_send_mail):
        """Test that send_mail is called when sending OTP"""
        send_otp('test@example.com', '123456', 'Test User')
        mock_send_mail.assert_called_once()

    @patch('users.utils.send_mail')
    def test_send_otp_parameters(self, mock_send_mail):
        """Test that send_otp is called with correct parameters"""
        send_otp('test@example.com', '123456', 'Test User')
        
        call_args = mock_send_mail.call_args
        self.assertIn('test@example.com', call_args[1]['recipient_list'])
        self.assertIn('123456', call_args[1]['message'])


# ============================================================================
# API VIEW TESTS
# ============================================================================

class RegisterRequestViewTests(APITestCase):
    """Test cases for RegisterRequestView"""

    def setUp(self):
        """Set up test fixtures"""
        self.client = APIClient()
        self.url = '/api/users/register/request/'

    @patch('users.views.send_otp')
    def test_register_request_success(self, mock_send_otp):
        """Test successful registration request"""
        data = {
            'email': 'newuser@example.com',
            'name': 'New User',
            'password': 'newpassword123'
        }
        response = self.client.post(self.url, data, format='json')
        
        # Should return 200 OK
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('message', response.data)
        
        # Should create user
        self.assertTrue(
            User.objects.filter(email='newuser@example.com').exists()
        )
        
        # Should create OTP
        self.assertTrue(
            RegistrationOTP.objects.filter(email='newuser@example.com').exists()
        )
        
        # Should call send_otp
        mock_send_otp.assert_called_once()

    @patch('users.views.send_otp')
    def test_register_request_invalid_email(self, mock_send_otp):
        """Test registration request with invalid email"""
        data = {
            'email': 'invalid-email',
            'name': 'New User',
            'password': 'newpassword123'
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertFalse(User.objects.filter(email='invalid-email').exists())

    @patch('users.views.send_otp')
    def test_register_request_missing_fields(self, mock_send_otp):
        """Test registration request with missing fields"""
        data = {'email': 'test@example.com'}
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    @patch('users.views.send_otp')
    def test_register_request_short_password(self, mock_send_otp):
        """Test registration request with short password"""
        data = {
            'email': 'test@example.com',
            'name': 'Test User',
            'password': 'short'
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class RegisterVerifyViewTests(APITestCase):
    """Test cases for RegisterVerifyView"""

    def setUp(self):
        """Set up test fixtures"""
        self.client = APIClient()
        self.url = '/api/users/register/verify/'
        
        # Create user and OTP
        self.user = User.objects.create_user(
            email='verify@example.com',
            name='Verify User',
            password='password123',
            is_verified=False
        )
        self.otp_obj = RegistrationOTP.objects.create(email='verify@example.com')
        self.test_otp = '123456'
        self.otp_obj.set_otp(self.test_otp)
        self.otp_obj.save()

    def test_verify_otp_success(self):
        """Test successful OTP verification"""
        data = {
            'email': 'verify@example.com',
            'otp': self.test_otp
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_201_CREATED)
        
        # User should be verified
        self.user.refresh_from_db()
        self.assertTrue(self.user.is_verified)
        
        # OTP should be deleted
        self.assertFalse(
            RegistrationOTP.objects.filter(email='verify@example.com').exists()
        )

    def test_verify_otp_invalid(self):
        """Test OTP verification with wrong OTP"""
        data = {
            'email': 'verify@example.com',
            'otp': '000000'
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('error', response.data)

    def test_verify_otp_expired(self):
        """Test OTP verification with expired OTP"""
        # Make OTP expired
        old_created_at = timezone.now() - timezone.timedelta(minutes=3)
        self.otp_obj.created_at = old_created_at
        self.otp_obj.save()
        
        data = {
            'email': 'verify@example.com',
            'otp': self.test_otp
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('error', response.data)

    def test_verify_otp_not_found(self):
        """Test OTP verification when OTP record doesn't exist"""
        data = {
            'email': 'nonexistent@example.com',
            'otp': '123456'
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class ResendOTPViewTests(APITestCase):
    """Test cases for ResendOTPView"""

    def setUp(self):
        """Set up test fixtures"""
        self.client = APIClient()
        self.url = '/api/users/register/resend-otp/'
        
        self.user = User.objects.create_user(
            email='resend@example.com',
            name='Resend User',
            password='password123',
            is_verified=False
        )

    @patch('users.views.send_otp')
    def test_resend_otp_success(self, mock_send_otp):
        """Test successful OTP resend"""
        data = {'email': 'resend@example.com'}
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('message', response.data)
        mock_send_otp.assert_called_once()

    @patch('users.views.send_otp')
    def test_resend_otp_user_not_found(self, mock_send_otp):
        """Test resend OTP for non-existent user"""
        data = {'email': 'nonexistent@example.com'}
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('error', response.data)

    @patch('users.views.send_otp')
    def test_resend_otp_user_already_verified(self, mock_send_otp):
        """Test resend OTP for already verified user"""
        self.user.is_verified = True
        self.user.save()
        
        data = {'email': 'resend@example.com'}
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)
        self.assertIn('error', response.data)


class LoginViewTests(APITestCase):
    """Test cases for LoginView"""

    def setUp(self):
        """Set up test fixtures"""
        self.client = APIClient()
        self.url = '/api/users/login/'
        
        self.user = User.objects.create_user(
            email='login@example.com',
            name='Login User',
            password='loginpass123',
            is_verified=True
        )

    def test_login_success(self):
        """Test successful login"""
        data = {
            'email': 'login@example.com',
            'password': 'loginpass123'
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['email'], 'login@example.com')

    def test_login_invalid_password(self):
        """Test login with wrong password"""
        data = {
            'email': 'login@example.com',
            'password': 'wrongpassword'
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)

    def test_login_unverified_user(self):
        """Test login with unverified user"""
        unverified_user = User.objects.create_user(
            email='unverified@example.com',
            name='Unverified User',
            password='password123',
            is_verified=False
        )
        
        data = {
            'email': 'unverified@example.com',
            'password': 'password123'
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class PasswordResetRequestViewTests(APITestCase):
    """Test cases for PasswordResetRequestView"""

    def setUp(self):
        """Set up test fixtures"""
        self.client = APIClient()
        self.url = '/api/users/password-reset/request/'
        
        self.user = User.objects.create_user(
            email='reset@example.com',
            name='Reset User',
            password='password123'
        )

    @patch('users.views.send_otp')
    def test_password_reset_request_success(self, mock_send_otp):
        """Test successful password reset request"""
        data = {'email': 'reset@example.com'}
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('message', response.data)
        mock_send_otp.assert_called_once()


class PasswordResetConfirmViewTests(APITestCase):
    """Test cases for PasswordResetConfirmView"""

    def setUp(self):
        """Set up test fixtures"""
        self.client = APIClient()
        self.url = '/api/users/password-reset/confirm/'
        
        self.user = User.objects.create_user(
            email='resetconfirm@example.com',
            name='Reset Confirm User',
            password='oldpassword123'
        )
        
        self.otp_obj = RegistrationOTP.objects.create(email='resetconfirm@example.com')
        self.test_otp = '123456'
        self.otp_obj.set_otp(self.test_otp)
        self.otp_obj.save()

    def test_password_reset_confirm_success(self):
        """Test successful password reset"""
        data = {
            'email': 'resetconfirm@example.com',
            'otp': self.test_otp,
            'new_password': 'newpassword123'
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        
        # Password should be reset
        self.user.refresh_from_db()
        self.assertTrue(self.user.check_password('newpassword123'))
        self.assertFalse(self.user.check_password('oldpassword123'))

    def test_password_reset_confirm_invalid_otp(self):
        """Test password reset with invalid OTP"""
        data = {
            'email': 'resetconfirm@example.com',
            'otp': '000000',
            'new_password': 'newpassword123'
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_password_reset_confirm_expired_otp(self):
        """Test password reset with expired OTP"""
        old_created_at = timezone.now() - timezone.timedelta(minutes=3)
        self.otp_obj.created_at = old_created_at
        self.otp_obj.save()
        
        data = {
            'email': 'resetconfirm@example.com',
            'otp': self.test_otp,
            'new_password': 'newpassword123'
        }
        response = self.client.post(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)


class UserProfileViewTests(APITestCase):
    """Test cases for UserProfileView"""

    def setUp(self):
        """Set up test fixtures"""
        self.client = APIClient()
        self.url = '/api/users/profile/'
        
        self.user = User.objects.create_user(
            email='profile@example.com',
            name='Profile User',
            password='password123'
        )
        self.client.force_authenticate(user=self.user)

    def test_get_profile(self):
        """Test getting user profile"""
        response = self.client.get(self.url)
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertIn('bio', response.data)

    def test_get_profile_creates_if_not_exists(self):
        """Test that getting profile creates it if doesn't exist"""
        UserProfile.objects.filter(user=self.user).delete()
        
        response = self.client.get(self.url)
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertTrue(UserProfile.objects.filter(user=self.user).exists())

    def test_update_profile(self):
        """Test updating user profile"""
        data = {
            'bio': 'Updated bio',
            'phone_number': '03001234567'
        }
        response = self.client.put(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        
        profile = self.user.profile
        self.assertEqual(profile.bio, 'Updated bio')
        self.assertEqual(profile.phone_number, '03001234567')

    def test_update_profile_requires_authentication(self):
        """Test that profile update requires authentication"""
        self.client.force_authenticate(user=None)
        
        data = {'bio': 'Updated bio'}
        response = self.client.put(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class PublicUserProfileViewTests(APITestCase):
    """Test cases for PublicUserProfileView"""

    def setUp(self):
        """Set up test fixtures"""
        self.client = APIClient()
        
        self.user1 = User.objects.create_user(
            email='user1@example.com',
            name='User 1',
            password='password123'
        )
        self.profile1 = UserProfile.objects.create(
            user=self.user1,
            bio='User 1 bio',
            is_public=True
        )
        
        self.user2 = User.objects.create_user(
            email='user2@example.com',
            name='User 2',
            password='password123'
        )
        self.client.force_authenticate(user=self.user2)

    def test_get_public_profile(self):
        """Test getting public user profile"""
        url = f'/api/users/profile/public/{self.user1.id}/'
        response = self.client.get(url)
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        self.assertEqual(response.data['user_id'], self.user1.id)

    def test_get_nonexistent_user_profile(self):
        """Test getting profile of non-existent user"""
        url = '/api/users/profile/public/99999/'
        response = self.client.get(url)
        
        self.assertEqual(response.status_code, status.HTTP_404_NOT_FOUND)

    def test_get_profile_requires_authentication(self):
        """Test that getting profile requires authentication"""
        self.client.force_authenticate(user=None)
        
        url = f'/api/users/profile/public/{self.user1.id}/'
        response = self.client.get(url)
        
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


class EmailChangeViewTests(APITestCase):
    """Test cases for EmailChangeView"""

    def setUp(self):
        """Set up test fixtures"""
        self.client = APIClient()
        self.url = '/api/users/profile/email/'
        
        self.user = User.objects.create_user(
            email='oldemail@example.com',
            name='Email Change User',
            password='password123'
        )
        self.client.force_authenticate(user=self.user)

    def test_email_change_success(self):
        """Test successful email change"""
        data = {
            'new_email': 'newemail@example.com',
            'current_password': 'password123'
        }
        response = self.client.put(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_200_OK)
        
        self.user.refresh_from_db()
        self.assertEqual(self.user.email, 'newemail@example.com')

    def test_email_change_invalid_password(self):
        """Test email change with wrong password"""
        data = {
            'new_email': 'newemail@example.com',
            'current_password': 'wrongpassword'
        }
        response = self.client.put(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_email_change_duplicate_email(self):
        """Test email change to existing email"""
        User.objects.create_user(
            email='existing@example.com',
            name='Existing User',
            password='password123'
        )
        
        data = {
            'new_email': 'existing@example.com',
            'current_password': 'password123'
        }
        response = self.client.put(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_400_BAD_REQUEST)

    def test_email_change_requires_authentication(self):
        """Test that email change requires authentication"""
        self.client.force_authenticate(user=None)
        
        data = {
            'new_email': 'newemail@example.com',
            'current_password': 'password123'
        }
        response = self.client.put(self.url, data, format='json')
        
        self.assertEqual(response.status_code, status.HTTP_401_UNAUTHORIZED)


# ============================================================================
# INTEGRATION TESTS
# ============================================================================

class UserRegistrationFlowTests(APITestCase):
    """Integration tests for complete user registration flow"""

    def setUp(self):
        """Set up test fixtures"""
        self.client = APIClient()

    @patch('users.views.send_otp')
    def test_complete_registration_flow(self, mock_send_otp):
        """Test complete registration flow: request -> verify"""
        # Step 1: Register request
        register_request_url = '/api/users/register/request/'
        register_data = {
            'email': 'integration@example.com',
            'name': 'Integration User',
            'password': 'integrationpass123'
        }
        register_response = self.client.post(register_request_url, register_data, format='json')
        self.assertEqual(register_response.status_code, status.HTTP_200_OK)
        
        # Step 2: Get the OTP that was "sent"
        otp_obj = RegistrationOTP.objects.get(email='integration@example.com')
        test_otp = '123456'
        otp_obj.set_otp(test_otp)
        otp_obj.save()
        
        # Step 3: Verify OTP
        verify_url = '/api/users/register/verify/'
        verify_data = {
            'email': 'integration@example.com',
            'otp': test_otp
        }
        verify_response = self.client.post(verify_url, verify_data, format='json')
        self.assertEqual(verify_response.status_code, status.HTTP_201_CREATED)
        
        # Step 4: Verify user is now verified
        user = User.objects.get(email='integration@example.com')
        self.assertTrue(user.is_verified)

    @patch('users.views.send_otp')
    def test_registration_to_login_flow(self, mock_send_otp):
        """Test flow from registration to login"""
        # Register user
        register_url = '/api/users/register/request/'
        register_data = {
            'email': 'flowtest@example.com',
            'name': 'Flow Test User',
            'password': 'flowpass123'
        }
        self.client.post(register_url, register_data, format='json')
        
        # Verify user
        user = User.objects.get(email='flowtest@example.com')
        user.is_verified = True
        user.save()
        
        # Login
        login_url = '/api/users/login/'
        login_data = {
            'email': 'flowtest@example.com',
            'password': 'flowpass123'
        }
        login_response = self.client.post(login_url, login_data, format='json')
        
        self.assertEqual(login_response.status_code, status.HTTP_200_OK)
        self.assertEqual(login_response.data['email'], 'flowtest@example.com')
