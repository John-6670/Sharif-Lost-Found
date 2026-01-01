import { useState, useCallback, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { signupRequestOtp, signupVerifyOtp } from "../../services/api";
import OtpInput from "../../components/OtpInput/OtpInput";
import "./Signup.css";

const OTP_LENGTH = 6;
const RESEND_COOLDOWN = 60; // seconds

// Password strength calculator
function getPasswordStrength(password) {
  if (!password) return { level: 0, label: "", color: "" };
  
  let score = 0;
  if (password.length >= 8) score++;
  if (password.length >= 12) score++;
  if (/[a-z]/.test(password) && /[A-Z]/.test(password)) score++;
  if (/\d/.test(password)) score++;
  if (/[^a-zA-Z0-9]/.test(password)) score++;

  if (score <= 1) return { level: 1, label: "ضعیف", color: "#ef4444" };
  if (score <= 2) return { level: 2, label: "متوسط", color: "#f59e0b" };
  if (score <= 3) return { level: 3, label: "خوب", color: "#3b82f6" };
  return { level: 4, label: "قوی", color: "#16a34a" };
}

// Email validation
function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

export default function Signup() {
  const navigate = useNavigate();
  
  // Step state: 'form' or 'verify'
  const [step, setStep] = useState("form");
  
  // Form state
  const [formData, setFormData] = useState({
    fullName: "",
    email: "",
    password: "",
    confirmPassword: "",
    acceptTerms: false,
  });

  // OTP state
  const [otp, setOtp] = useState("");
  const [resendCooldown, setResendCooldown] = useState(0);
  const [isResending, setIsResending] = useState(false);

  // UI state
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [submitSuccess, setSubmitSuccess] = useState(false);

  // Cooldown timer
  useEffect(() => {
    if (resendCooldown > 0) {
      const timer = setTimeout(() => {
        setResendCooldown((prev) => prev - 1);
      }, 1000);
      return () => clearTimeout(timer);
    }
  }, [resendCooldown]);

  // Validation
  const validateField = useCallback((name, value, allData = formData) => {
    switch (name) {
      case "fullName":
        if (!value.trim()) return "نام و نام خانوادگی الزامی است";
        if (value.trim().length < 3) return "نام باید حداقل ۳ کاراکتر باشد";
        return "";
      
      case "email":
        if (!value.trim()) return "ایمیل الزامی است";
        if (!isValidEmail(value)) return "فرمت ایمیل نامعتبر است";
        return "";
      
      case "password":
        if (!value) return "رمز عبور الزامی است";
        if (value.length < 8) return "رمز عبور باید حداقل ۸ کاراکتر باشد";
        return "";
      
      case "confirmPassword":
        if (!value) return "تکرار رمز عبور الزامی است";
        if (value !== allData.password) return "رمز عبور و تکرار آن مطابقت ندارند";
        return "";
      
      case "acceptTerms":
        if (!value) return "پذیرش قوانین الزامی است";
        return "";
      
      default:
        return "";
    }
  }, [formData]);

  const validateForm = useCallback(() => {
    const newErrors = {};
    Object.keys(formData).forEach((key) => {
      const error = validateField(key, formData[key], formData);
      if (error) newErrors[key] = error;
    });
    return newErrors;
  }, [formData, validateField]);

  // Handlers
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    const newValue = type === "checkbox" ? checked : value;
    
    setFormData((prev) => ({ ...prev, [name]: newValue }));
    setSubmitError("");
    
    // Real-time validation for touched fields
    if (touched[name]) {
      const error = validateField(name, newValue, { ...formData, [name]: newValue });
      setErrors((prev) => ({ ...prev, [name]: error }));
    }
  };

  const handleBlur = (e) => {
    const { name, value, type, checked } = e.target;
    const newValue = type === "checkbox" ? checked : value;
    
    setTouched((prev) => ({ ...prev, [name]: true }));
    const error = validateField(name, newValue);
    setErrors((prev) => ({ ...prev, [name]: error }));
  };

  // Step 1: Submit form and request OTP
  const handleFormSubmit = async (e) => {
    e.preventDefault();
    
    // Mark all fields as touched
    const allTouched = {};
    Object.keys(formData).forEach((key) => (allTouched[key] = true));
    setTouched(allTouched);
    
    // Validate all fields
    const formErrors = validateForm();
    setErrors(formErrors);
    
    if (Object.keys(formErrors).length > 0) return;
    
    setIsSubmitting(true);
    setSubmitError("");
    
    try {
      await signupRequestOtp({ email: formData.email });
      setStep("verify");
      setResendCooldown(RESEND_COOLDOWN);
    } catch (error) {
      setSubmitError(error.message || "خطا در ارسال کد تأیید. لطفاً دوباره تلاش کنید.");
    } finally {
      setIsSubmitting(false);
    }
  };

  // Handle OTP change
  const handleOtpChange = (value) => {
    setOtp(value);
    setSubmitError("");
  };

  // Resend OTP
  const handleResendOtp = async () => {
    if (resendCooldown > 0 || isResending) return;
    
    setIsResending(true);
    
    try {
      await signupRequestOtp({ email: formData.email });
      setResendCooldown(RESEND_COOLDOWN);
    } catch {
      // Silently fail or show brief message
      setResendCooldown(RESEND_COOLDOWN);
    } finally {
      setIsResending(false);
    }
  };

  // Go back to form step
  const handleEditEmail = () => {
    setStep("form");
    setOtp("");
    setSubmitError("");
  };

  // Step 2: Verify OTP and complete signup
  const handleVerifySubmit = async (e) => {
    e.preventDefault();
    
    if (otp.length !== OTP_LENGTH) {
      setSubmitError("لطفاً کد تأیید ۶ رقمی را وارد کنید");
      return;
    }
    
    setIsSubmitting(true);
    setSubmitError("");
    
    try {
      await signupVerifyOtp({
        email: formData.email,
        otp: otp,
        fullName: formData.fullName,
        password: formData.password,
      });
      
      setSubmitSuccess(true);
      setTimeout(() => {
        navigate("/login");
      }, 2000);
    } catch (error) {
      setSubmitError(error.message || "کد تأیید نامعتبر یا منقضی شده است.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const passwordStrength = getPasswordStrength(formData.password);
  
  // Check if form is valid - simplified check
  const isFormValid = 
    formData.fullName.trim().length >= 3 &&
    isValidEmail(formData.email) &&
    formData.password.length >= 8 &&
    formData.confirmPassword === formData.password &&
    formData.acceptTerms;

  const isOtpValid = otp.length === OTP_LENGTH && /^\d+$/.test(otp);

  // Success state
  if (submitSuccess) {
    return (
      <div className="signup-container">
        <div className="signup-card success-card">
          <div className="success-icon">✓</div>
          <h2>ثبت‌نام با موفقیت انجام شد!</h2>
          <p>در حال انتقال به صفحه ورود...</p>
        </div>
      </div>
    );
  }

  // Step 2: OTP Verification
  if (step === "verify") {
    return (
      <div className="signup-container">
        <div className="signup-card">
          <div className="signup-header">
            <div className="logo-icon">📧</div>
            <h1>تأیید ایمیل</h1>
            <p>کد تأیید ۶ رقمی به ایمیل زیر ارسال شد</p>
          </div>

          {/* Email display */}
          <div className="email-display">
            <span className="email-text" dir="ltr">{formData.email}</span>
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
                  ارسال مجدد کد ({resendCooldown} ثانیه)
                </span>
              ) : (
                <button
                  type="button"
                  className="resend-btn"
                  onClick={handleResendOtp}
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
                "تأیید و تکمیل ثبت‌نام"
              )}
            </button>
          </form>

          <div className="signup-footer">
            <p>
              <button 
                type="button" 
                className="back-link"
                onClick={handleEditEmail}
              >
                بازگشت به فرم ثبت‌نام
              </button>
            </p>
          </div>
        </div>
      </div>
    );
  }

  // Step 1: Registration Form
  return (
    <div className="signup-container">
      <div className="signup-card">
        <div className="signup-header">
          <div className="logo-icon">🎓</div>
          <h1>ثبت‌نام دانشجو</h1>
          <p>به سامانه اشیاء گم‌شده دانشگاه خوش آمدید</p>
        </div>

        {submitError && (
          <div className="alert alert-error" role="alert">
            <span className="alert-icon">⚠</span>
            {submitError}
          </div>
        )}

        <form onSubmit={handleFormSubmit} noValidate>
          {/* Full Name */}
          <div className="form-group">
            <label htmlFor="fullName">
              نام و نام خانوادگی
              <span className="required">*</span>
            </label>
            <input
              type="text"
              id="fullName"
              name="fullName"
              value={formData.fullName}
              onChange={handleChange}
              onBlur={handleBlur}
              className={errors.fullName && touched.fullName ? "input-error" : ""}
              placeholder="مثال: علی محمدی"
              aria-describedby={errors.fullName ? "fullName-error" : undefined}
              aria-invalid={errors.fullName && touched.fullName ? "true" : "false"}
              autoComplete="name"
            />
            {errors.fullName && touched.fullName && (
              <span className="error-message" id="fullName-error" role="alert">
                {errors.fullName}
              </span>
            )}
          </div>

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
              value={formData.email}
              onChange={handleChange}
              onBlur={handleBlur}
              className={errors.email && touched.email ? "input-error" : ""}
              placeholder="example@gmail.com"
              dir="ltr"
              aria-describedby={errors.email ? "email-error" : undefined}
              aria-invalid={errors.email && touched.email ? "true" : "false"}
              autoComplete="email"
            />
            {errors.email && touched.email && (
              <span className="error-message" id="email-error" role="alert">
                {errors.email}
              </span>
            )}
          </div>

          {/* Password */}
          <div className="form-group">
            <label htmlFor="password">
              رمز عبور
              <span className="required">*</span>
            </label>
            <div className="password-wrapper">
              <input
                type={showPassword ? "text" : "password"}
                id="password"
                name="password"
                value={formData.password}
                onChange={handleChange}
                onBlur={handleBlur}
                className={errors.password && touched.password ? "input-error" : ""}
                placeholder="حداقل ۸ کاراکتر"
                dir="ltr"
                aria-describedby={errors.password ? "password-error" : "password-hint"}
                aria-invalid={errors.password && touched.password ? "true" : "false"}
                autoComplete="new-password"
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowPassword(!showPassword)}
                aria-label={showPassword ? "پنهان کردن رمز عبور" : "نمایش رمز عبور"}
              >
                {showPassword ? "🙈" : "👁"}
              </button>
            </div>
            
            {formData.password && (
              <div className="password-strength">
                <div className="strength-bar">
                  <div
                    className="strength-fill"
                    style={{
                      width: `${(passwordStrength.level / 4) * 100}%`,
                      backgroundColor: passwordStrength.color,
                    }}
                  />
                </div>
                <span className="strength-label" style={{ color: passwordStrength.color }}>
                  {passwordStrength.label}
                </span>
              </div>
            )}
            
            {errors.password && touched.password && (
              <span className="error-message" id="password-error" role="alert">
                {errors.password}
              </span>
            )}
            
            {!errors.password && (
              <span className="hint" id="password-hint">
                ترکیب حروف بزرگ، کوچک، اعداد و نمادها امنیت بیشتری دارد
              </span>
            )}
          </div>

          {/* Confirm Password */}
          <div className="form-group">
            <label htmlFor="confirmPassword">
              تکرار رمز عبور
              <span className="required">*</span>
            </label>
            <div className="password-wrapper">
              <input
                type={showConfirmPassword ? "text" : "password"}
                id="confirmPassword"
                name="confirmPassword"
                value={formData.confirmPassword}
                onChange={handleChange}
                onBlur={handleBlur}
                className={errors.confirmPassword && touched.confirmPassword ? "input-error" : ""}
                placeholder="رمز عبور را مجدداً وارد کنید"
                dir="ltr"
                aria-describedby={errors.confirmPassword ? "confirmPassword-error" : undefined}
                aria-invalid={errors.confirmPassword && touched.confirmPassword ? "true" : "false"}
                autoComplete="new-password"
              />
              <button
                type="button"
                className="password-toggle"
                onClick={() => setShowConfirmPassword(!showConfirmPassword)}
                aria-label={showConfirmPassword ? "پنهان کردن رمز عبور" : "نمایش رمز عبور"}
              >
                {showConfirmPassword ? "🙈" : "👁"}
              </button>
            </div>
            {errors.confirmPassword && touched.confirmPassword && (
              <span className="error-message" id="confirmPassword-error" role="alert">
                {errors.confirmPassword}
              </span>
            )}
          </div>

          {/* Terms Checkbox */}
          <div className="form-group checkbox-group">
            <label className="checkbox-label">
              <input
                type="checkbox"
                name="acceptTerms"
                checked={formData.acceptTerms}
                onChange={handleChange}
                onBlur={handleBlur}
                aria-describedby={errors.acceptTerms ? "terms-error" : undefined}
              />
              <span className="checkbox-custom" />
              <span>
                <a href="/terms" target="_blank" rel="noopener noreferrer">
                  قوانین و مقررات
                </a>{" "}
                را مطالعه کردم و می‌پذیرم
              </span>
            </label>
            {errors.acceptTerms && touched.acceptTerms && (
              <span className="error-message" id="terms-error" role="alert">
                {errors.acceptTerms}
              </span>
            )}
          </div>

          {/* Submit Button */}
          <button
            type="submit"
            className="submit-btn"
            disabled={!isFormValid || isSubmitting}
          >
            {isSubmitting ? (
              <>
                <span className="spinner" />
                در حال ارسال کد تأیید...
              </>
            ) : (
              "ادامه و دریافت کد تأیید"
            )}
          </button>
        </form>

        <div className="signup-footer">
          <p>
            قبلاً ثبت‌نام کرده‌اید؟{" "}
            <Link to="/login">وارد شوید</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
