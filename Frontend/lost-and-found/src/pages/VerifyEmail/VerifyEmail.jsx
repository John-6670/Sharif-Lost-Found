import { useState, useEffect } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { resendRegistrationOtp, verifyRegistrationOtp } from "../../services/api";
import OtpInput from "../../Components/OtpInput/OtpInput";
import "./VerifyEmail.css";

const OTP_LENGTH = 6;
const RESEND_COOLDOWN = 120; // seconds (2 minutes - matches backend OTP expiry)

// Simple email validation
function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

// Normalize email (lowercase, trim)
function normalizeEmail(email) {
  return email.trim().toLowerCase();
}

export default function VerifyEmail() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  
  // Get email from URL params if available (from login redirect)
  const initialEmail = searchParams.get("email") || "";
  
  // Step state: 'email' or 'verify'
  const [step, setStep] = useState(initialEmail ? "verify" : "email");
  
  // Form state
  const [email, setEmail] = useState(initialEmail);
  const [otp, setOtp] = useState("");
  
  // Timer state
  const [resendCooldown, setResendCooldown] = useState(initialEmail ? RESEND_COOLDOWN : 0);
  const [isResending, setIsResending] = useState(false);
  
  // UI state
  const [emailError, setEmailError] = useState("");
  const [emailTouched, setEmailTouched] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [submitSuccess, setSubmitSuccess] = useState(false);

  // Auto-send OTP if email is provided via URL
  useEffect(() => {
    if (initialEmail && isValidEmail(initialEmail)) {
      handleResendOtp(initialEmail);
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  // Cooldown timer
  useEffect(() => {
    if (resendCooldown > 0) {
      const timer = setTimeout(() => {
        setResendCooldown((prev) => prev - 1);
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [resendCooldown]);

  // Email validation
  const validateEmail = (value) => {
    if (!value.trim()) return "ایمیل الزامی است";
    if (!isValidEmail(value.trim())) return "فرمت ایمیل نامعتبر است";
    return "";
  };

  // Handlers
  const handleEmailChange = (e) => {
    const value = e.target.value;
    setEmail(value);
    setSubmitError("");
    
    if (emailTouched) {
      setEmailError(validateEmail(value));
    }
  };

  const handleEmailBlur = () => {
    setEmailTouched(true);
    setEmailError(validateEmail(email));
  };

  // Request OTP
  const handleResendOtp = async (emailToUse = email) => {
    if (resendCooldown > 0 || isResending) return;
    
    setIsResending(true);
    setSubmitError("");
    
    try {
      const normalizedEmail = normalizeEmail(emailToUse);
      await resendRegistrationOtp({ email: normalizedEmail });
      setResendCooldown(RESEND_COOLDOWN);
      setStep("verify");
    } catch (error) {
      if (error.status === 429) {
        setSubmitError("تعداد درخواست‌ها بیش از حد مجاز است. لطفاً کمی صبر کنید.");
        setResendCooldown(RESEND_COOLDOWN);
      } else if (error.status === 404) {
        setSubmitError("این ایمیل در سیستم ثبت نشده است. لطفاً ابتدا ثبت‌نام کنید.");
      } else if (error.status === 400 && error.message?.toLowerCase().includes("verified")) {
        setSubmitError("این حساب قبلاً تأیید شده است. می‌توانید وارد شوید.");
      } else {
        setSubmitError(error.message || "خطا در ارسال کد تأیید. لطفاً دوباره تلاش کنید.");
      }
    } finally {
      setIsResending(false);
    }
  };

  // Submit email form
  const handleEmailSubmit = async (e) => {
    e.preventDefault();
    
    setEmailTouched(true);
    const error = validateEmail(email);
    setEmailError(error);
    
    if (error) return;
    
    await handleResendOtp();
  };

  // Handle OTP change
  const handleOtpChange = (value) => {
    setOtp(value);
    setSubmitError("");
  };

  // Go back to email step
  const handleEditEmail = () => {
    setStep("email");
    setOtp("");
    setSubmitError("");
    setResendCooldown(0);
  };

  // Verify OTP
  const handleVerifySubmit = async (e) => {
    e.preventDefault();
    
    if (otp.length !== OTP_LENGTH) {
      setSubmitError("لطفاً کد تأیید ۶ رقمی را وارد کنید");
      return;
    }
    
    setIsSubmitting(true);
    setSubmitError("");
    
    try {
      const normalizedEmail = normalizeEmail(email);
      
      await verifyRegistrationOtp({
        email: normalizedEmail,
        otp: otp,
      });
      
      setSubmitSuccess(true);
      setTimeout(() => {
        navigate("/login");
      }, 2000);
    } catch (error) {
      if (error.status === 429) {
        setSubmitError("تعداد تلاش‌ها بیش از حد مجاز است. لطفاً کمی صبر کنید.");
      } else if (error.status === 400) {
        setSubmitError("کد تأیید نامعتبر یا منقضی شده است. کد پس از ۲ دقیقه منقضی می‌شود.");
      } else {
        setSubmitError(error.message || "خطا در تأیید. لطفاً دوباره تلاش کنید.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  const isEmailValid = isValidEmail(email.trim());
  const isOtpValid = otp.length === OTP_LENGTH && /^\d+$/.test(otp);

  // Success state
  if (submitSuccess) {
    return (
      <div className="verify-email-container">
        <div className="verify-email-card success-card">
          <div className="success-icon">✓</div>
          <h2>ایمیل با موفقیت تأیید شد!</h2>
          <p>در حال انتقال به صفحه ورود...</p>
        </div>
      </div>
    );
  }

  // Step 2: OTP Verification
  if (step === "verify") {
    return (
      <div className="verify-email-container">
        <div className="verify-email-card">
          <div className="verify-email-header">
            <div className="logo-icon">📧</div>
            <h1>تأیید ایمیل</h1>
            <p>کد تأیید ۶ رقمی به ایمیل زیر ارسال شد</p>
            <p className="otp-expiry-note">کد پس از ۲ دقیقه منقضی می‌شود</p>
          </div>

          {/* Email display */}
          <div className="email-display">
            <span className="email-text" dir="ltr">{normalizeEmail(email)}</span>
            <button 
              type="button" 
              className="edit-email-btn"
              onClick={handleEditEmail}
            >
              ویرایش
            </button>
          </div>

          {submitError && (
            <div className="alert alert-error" role="alert">
              <span className="alert-icon">⚠</span>
              {submitError}
            </div>
          )}

          <form onSubmit={handleVerifySubmit} noValidate>
            {/* OTP Input */}
            <div className="form-group otp-group">
              <label>کد تأیید</label>
              <OtpInput
                value={otp}
                onChange={handleOtpChange}
                hasError={!!submitError}
                length={OTP_LENGTH}
              />
            </div>

            {/* Resend Code */}
            <div className="resend-section">
              {resendCooldown > 0 ? (
                <span className="resend-cooldown">
                  ارسال مجدد کد ({Math.floor(resendCooldown / 60)}:{String(resendCooldown % 60).padStart(2, '0')})
                </span>
              ) : (
                <button
                  type="button"
                  className="resend-btn"
                  onClick={() => handleResendOtp()}
                  disabled={isResending}
                >
                  {isResending ? "در حال ارسال..." : "ارسال مجدد کد"}
                </button>
              )}
            </div>

            {/* Submit Button */}
            <button
              type="submit"
              className="submit-btn"
              disabled={!isOtpValid || isSubmitting}
            >
              {isSubmitting ? (
                <>
                  <span className="spinner" />
                  در حال تأیید...
                </>
              ) : (
                "تأیید ایمیل"
              )}
            </button>
          </form>

          <div className="verify-email-footer">
            <p>
              <button 
                type="button" 
                className="back-link"
                onClick={handleEditEmail}
              >
                تغییر ایمیل
              </button>
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Step 1: Email Form
  return (
    <div className="verify-email-container">
      <div className="verify-email-card">
        <div className="verify-email-header">
          <div className="logo-icon">✉️</div>
          <h1>تأیید حساب کاربری</h1>
          <p>ایمیلی که با آن ثبت‌نام کرده‌اید را وارد کنید</p>
        </div>

        {submitError && (
          <div className="alert alert-error" role="alert">
            <span className="alert-icon">⚠</span>
            <div className="alert-content">
              <span>{submitError}</span>
              {submitError.includes("ثبت‌نام") && (
                <Link to="/signup" className="alert-link">
                  ثبت‌نام کنید
                </Link>
              )}
              {submitError.includes("وارد شوید") && (
                <Link to="/login" className="alert-link">
                  ورود به حساب
                </Link>
              )}
            </div>
          </div>
        )}

        <form onSubmit={handleEmailSubmit} noValidate>
          {/* Email */}
          <div className="form-group">
            <label htmlFor="email">
              ایمیل
              <span className="required">*</span>
            </label>
            <input
              type="email"
              id="email"
              name="email"
              value={email}
              onChange={handleEmailChange}
              onBlur={handleEmailBlur}
              className={emailError && emailTouched ? "input-error" : ""}
              placeholder="example@gmail.com"
              dir="ltr"
              aria-describedby={emailError ? "email-error" : undefined}
              aria-invalid={emailError && emailTouched ? "true" : "false"}
              autoComplete="email"
              autoFocus
            />
            {emailError && emailTouched && (
              <span className="error-message" id="email-error" role="alert">
                {emailError}
              </span>
            )}
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            className="submit-btn"
            disabled={!isEmailValid || isResending}
          >
            {isResending ? (
              <>
                <span className="spinner" />
                در حال ارسال کد...
              </>
            ) : (
              "دریافت کد تأیید"
            )}
          </button>
        </form>

        <div className="verify-email-footer">
          <p>
            <Link to="/login">بازگشت به صفحه ورود</Link>
          </p>
          <p className="footer-divider">یا</p>
          <p>
            هنوز ثبت‌نام نکرده‌اید؟{" "}
            <Link to="/signup">ثبت‌نام کنید</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

