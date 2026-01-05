import { useState, useEffect, useCallback } from "react";
import { Link, useNavigate, useSearchParams } from "react-router-dom";
import { resetPassword, forgotPassword } from "../../services/api";
import OtpInput from "../../Components/OtpInput/OtpInput";
import {
  validateUniversityEmail,
  validatePassword,
  validateConfirmPassword,
  validateOtp,
  checkPasswordRequirements,
  getPasswordStrength,
  normalizeEmail,
} from "../../utils/validation";
import "./ResetPassword.css";

const OTP_LENGTH = 6;
const RESEND_COOLDOWN = 60; // seconds

export default function ResetPassword() {
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  
  // Form state
  const [formData, setFormData] = useState({
    email: searchParams.get("email") || "",
    otp: "",
    password: "",
    confirmPassword: "",
  });

  // UI state
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [showConfirmPassword, setShowConfirmPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [isSuccess, setIsSuccess] = useState(false);
  
  // Resend cooldown
  const [resendCooldown, setResendCooldown] = useState(RESEND_COOLDOWN);
  const [isResending, setIsResending] = useState(false);

  // Password requirements state (live update)
  const [passwordRequirements, setPasswordRequirements] = useState(
    checkPasswordRequirements("", { email: "" })
  );

  // Update password requirements on password/email change
  useEffect(() => {
    setPasswordRequirements(
      checkPasswordRequirements(formData.password, { email: formData.email })
    );
  }, [formData.password, formData.email]);

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
      case "email": {
        const result = validateUniversityEmail(value);
        return result.error;
      }
      
      case "otp": {
        const result = validateOtp(value);
        return result.error;
      }
      
      case "password": {
        const result = validatePassword(value, { email: allData.email });
        return result.error;
      }
      
      case "confirmPassword": {
        const result = validateConfirmPassword(allData.password, value);
        return result.error;
      }
      
      default:
        return "";
    }
  }, [formData]);

  // Handlers
  const handleChange = (e) => {
    const { name, value } = e.target;
    
    setFormData((prev) => ({ ...prev, [name]: value }));
    setSubmitError("");
    
    if (touched[name]) {
      const error = validateField(name, value, { ...formData, [name]: value });
      setErrors((prev) => ({ ...prev, [name]: error }));
    }
    
    // Also revalidate confirm password when password changes
    if (name === "password" && touched.confirmPassword) {
      const confirmError = validateField("confirmPassword", formData.confirmPassword, { 
        ...formData, 
        password: value 
      });
      setErrors((prev) => ({ ...prev, confirmPassword: confirmError }));
    }
  };

  const handleOtpChange = (value) => {
    setFormData((prev) => ({ ...prev, otp: value }));
    setSubmitError("");
    
    if (touched.otp) {
      const result = validateOtp(value);
      setErrors((prev) => ({ ...prev, otp: result.error }));
    }
  };

  const handleBlur = (e) => {
    const { name, value } = e.target;
    
    setTouched((prev) => ({ ...prev, [name]: true }));
    const error = validateField(name, value);
    setErrors((prev) => ({ ...prev, [name]: error }));
  };

  const handleOtpBlur = () => {
    setTouched((prev) => ({ ...prev, otp: true }));
    const result = validateOtp(formData.otp);
    setErrors((prev) => ({ ...prev, otp: result.error }));
  };

  const handleResendCode = async () => {
    if (resendCooldown > 0 || isResending) return;
    
    const emailResult = validateUniversityEmail(formData.email);
    if (!emailResult.valid) {
      setErrors((prev) => ({ ...prev, email: emailResult.error }));
      setTouched((prev) => ({ ...prev, email: true }));
      return;
    }
    
    setIsResending(true);
    
    try {
      await forgotPassword({ email: normalizeEmail(formData.email) });
      setResendCooldown(RESEND_COOLDOWN);
    } catch {
      // Privacy-friendly: don't show errors
      setResendCooldown(RESEND_COOLDOWN);
    } finally {
      setIsResending(false);
    }
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Mark all fields as touched
    setTouched({
      email: true,
      otp: true,
      password: true,
      confirmPassword: true,
    });
    
    // Validate all fields
    const formErrors = {};
    Object.keys(formData).forEach((key) => {
      const error = validateField(key, formData[key], formData);
      if (error) formErrors[key] = error;
    });
    
    setErrors(formErrors);
    
    if (Object.keys(formErrors).length > 0) return;
    
    setIsSubmitting(true);
    setSubmitError("");
    
    try {
      await resetPassword({
        email: normalizeEmail(formData.email),
        otp: formData.otp,
        newPassword: formData.password,
      });
      
      setIsSuccess(true);
      setTimeout(() => {
        navigate("/login");
      }, 2500);
    } catch (error) {
      setSubmitError(error.message || "کد تأیید نامعتبر است یا منقضی شده است.");
    } finally {
      setIsSubmitting(false);
    }
  };

  const passwordStrength = getPasswordStrength(formData.password);
  
  // Check if form is valid
  const isFormValid = 
    validateUniversityEmail(formData.email).valid &&
    validateOtp(formData.otp).valid &&
    validatePassword(formData.password, { email: formData.email }).valid &&
    validateConfirmPassword(formData.password, formData.confirmPassword).valid;

  if (isSuccess) {
    return (
      <div className="reset-container">
        <div className="reset-card success-card">
          <div className="success-icon">✓</div>
          <h2>رمز عبور با موفقیت تغییر کرد!</h2>
          <p>اکنون می‌توانید با رمز عبور جدید وارد شوید.</p>
          <p className="redirect-text">در حال انتقال به صفحه ورود...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="reset-container">
      <div className="reset-card">
        <div className="reset-header">
          <div className="logo-icon">🔐</div>
          <h1>بازیابی رمز عبور</h1>
          <p>کد تأیید ارسال شده و رمز عبور جدید را وارد کنید</p>
        </div>

        {submitError && (
          <div className="alert alert-error" role="alert">
            <span className="alert-icon">⚠</span>
            {submitError}
          </div>
        )}

        <form onSubmit={handleSubmit} noValidate>
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

          {/* OTP Input */}
          <div className="form-group">
            <label>
              کد تأیید
              <span className="required">*</span>
            </label>
            <div onBlur={handleOtpBlur}>
              <OtpInput
                value={formData.otp}
                onChange={handleOtpChange}
                hasError={!!(errors.otp && touched.otp)}
                length={OTP_LENGTH}
              />
            </div>
            {errors.otp && touched.otp && (
              <span className="error-message otp-error" role="alert">
                {errors.otp}
              </span>
            )}
            
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
                  onClick={handleResendCode}
                  disabled={isResending}
                >
                  {isResending ? "در حال ارسال..." : "ارسال مجدد کد"}
                </button>
              )}
            </div>
          </div>

          {/* New Password */}
          <div className="form-group">
            <label htmlFor="password">
              رمز عبور جدید
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
                placeholder="حداقل ۱۰ کاراکتر"
                dir="ltr"
                aria-describedby="password-requirements"
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
            
            {/* Password Strength Indicator */}
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

            {/* Password Requirements Checklist */}
            {(formData.password || touched.password) && (
              <div className="password-requirements" id="password-requirements">
                <p className="requirements-title">الزامات رمز عبور:</p>
                <ul className="requirements-list">
                  <li className={passwordRequirements.minLength.met ? "met" : ""}>
                    <span className="check-icon">{passwordRequirements.minLength.met ? "✓" : "○"}</span>
                    {passwordRequirements.minLength.label}
                  </li>
                  <li className={passwordRequirements.categoriesCount.met ? "met" : ""}>
                    <span className="check-icon">{passwordRequirements.categoriesCount.met ? "✓" : "○"}</span>
                    {passwordRequirements.categoriesCount.label}
                    <ul className="sub-requirements">
                      <li className={passwordRequirements.hasLowercase.met ? "met" : ""}>
                        <span className="check-icon">{passwordRequirements.hasLowercase.met ? "✓" : "○"}</span>
                        {passwordRequirements.hasLowercase.label}
                      </li>
                      <li className={passwordRequirements.hasUppercase.met ? "met" : ""}>
                        <span className="check-icon">{passwordRequirements.hasUppercase.met ? "✓" : "○"}</span>
                        {passwordRequirements.hasUppercase.label}
                      </li>
                      <li className={passwordRequirements.hasDigit.met ? "met" : ""}>
                        <span className="check-icon">{passwordRequirements.hasDigit.met ? "✓" : "○"}</span>
                        {passwordRequirements.hasDigit.label}
                      </li>
                      <li className={passwordRequirements.hasSymbol.met ? "met" : ""}>
                        <span className="check-icon">{passwordRequirements.hasSymbol.met ? "✓" : "○"}</span>
                        {passwordRequirements.hasSymbol.label}
                      </li>
                    </ul>
                  </li>
                  <li className={passwordRequirements.noLeadingTrailingSpaces.met ? "met" : ""}>
                    <span className="check-icon">{passwordRequirements.noLeadingTrailingSpaces.met ? "✓" : "○"}</span>
                    {passwordRequirements.noLeadingTrailingSpaces.label}
                  </li>
                  <li className={passwordRequirements.noPersonalInfo.met ? "met" : ""}>
                    <span className="check-icon">{passwordRequirements.noPersonalInfo.met ? "✓" : "○"}</span>
                    {passwordRequirements.noPersonalInfo.label}
                  </li>
                </ul>
              </div>
            )}
            
            {errors.password && touched.password && (
              <span className="error-message" id="password-error" role="alert">
                {errors.password}
              </span>
            )}
          </div>

          {/* Confirm Password */}
          <div className="form-group">
            <label htmlFor="confirmPassword">
              تکرار رمز عبور جدید
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

          {/* Submit Button */}
          <button
            type="submit"
            className="submit-btn"
            disabled={!isFormValid || isSubmitting}
          >
            {isSubmitting ? (
              <>
                <span className="spinner" />
                در حال تغییر رمز عبور...
              </>
            ) : (
              "تغییر رمز عبور"
            )}
          </button>
        </form>

        <div className="reset-footer">
          <p>
            <Link to="/login">بازگشت به صفحه ورود</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
