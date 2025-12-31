from rest_framework.throttling import SimpleRateThrottle


class OTPIPRateThrottle(SimpleRateThrottle):
    scope = 'otp_ip'

    def get_cache_key(self, request, view):
        ident = self.get_ident(request)
        if not ident:
            return None

        return self.cache_format % {
            'scope': self.scope,
            'ident': ident,
        }


class OTPEmailRateThrottle(SimpleRateThrottle):
    scope = 'otp_email'

    def get_cache_key(self, request, view):
        email = request.data.get('email')
        if not email:
            return None

        return self.cache_format % {
            'scope': self.scope,
            'ident': email.lower(),
        }


class OTPVerifyRateThrottle(SimpleRateThrottle):
    scope = 'otp_verify'

    def get_cache_key(self, request, view):
        email = request.data.get('email')
        if not email:
            return None

        return self.cache_format % {
            'scope': self.scope,
            'ident': email.lower(),
        }

