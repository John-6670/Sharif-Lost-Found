import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { forgotPassword } from "../../services/api";
import "./ForgotPassword.css";

// Email validation
function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

export default function ForgotPassword() {
  const navigate = useNavigate();
  
  // Form state
  const [email, setEmail] = useState("");
  
  // UI state
  const [error, setError] = useState("");
  const [touched, setTouched] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [isSuccess, setIsSuccess] = useState(false);

  // Validation
  const validateEmail = (value) => {
    if (!value.trim()) return "ایمیل الزامی است";
    if (!isValidEmail(value.trim())) {
      return "فرمت ایمیل نامعتبر است";
    }
    return "";
  };

  // Handlers
  const handleChange = (e) => {
    const value = e.target.value;
    setEmail(value);
    setSubmitError("");
    
    if (touched) {
      setError(validateEmail(value));
    }
  };

  const handleBlur = () => {
    setTouched(true);
    setError(validateEmail(email));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    setTouched(true);
    const validationError = validateEmail(email);
    setError(validationError);
    
    if (validationError) return;
    
    setIsSubmitting(true);
    setSubmitError("");
    
    try {
      await forgotPassword({ email: email.trim() });
      setIsSuccess(true);
      
      // Navigate to reset password after short delay
      setTimeout(() => {
        navigate(`/reset-password?email=${encodeURIComponent(email.trim())}`);
      }, 2000);
    } catch (err) {
      // Privacy-friendly: don't reveal if account exists
      // Still show success message
      setIsSuccess(true);
      setTimeout(() => {
        navigate(`/reset-password?email=${encodeURIComponent(email.trim())}`);
      }, 2000);
    } finally {
      setIsSubmitting(false);
    }
  };

  const isFormValid = isValidEmail(email.trim());

  if (isSuccess) {
    return (
      <div className="forgot-container">
        <div className="forgot-card success-card">
          <div className="success-icon">📧</div>
          <h2>کد تأیید ارسال شد</h2>
          <p>
            در صورت وجود حساب کاربری، کد تأیید به ایمیل شما ارسال شده است.
          </p>
          <p className="redirect-text">در حال انتقال به صفحه بازیابی...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="forgot-container">
      <div className="forgot-card">
          <div className="forgot-header">
          <div className="logo-icon">🔑</div>
          <h1>فراموشی رمز عبور</h1>
          <p>ایمیل خود را وارد کنید تا کد تأیید برایتان ارسال شود</p>
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
              value={email}
              onChange={handleChange}
              onBlur={handleBlur}
              className={error && touched ? "input-error" : ""}
              placeholder="example@gmail.com"
              dir="ltr"
              aria-describedby={error ? "email-error" : undefined}
              aria-invalid={error && touched ? "true" : "false"}
              autoComplete="email"
              autoFocus
            />
            {error && touched && (
              <span className="error-message" id="email-error" role="alert">
                {error}
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
                در حال ارسال...
              </>
            ) : (
              "ارسال کد تأیید"
            )}
          </button>
        </form>

        <div className="forgot-footer">
          <p>
            رمز عبور خود را به خاطر آوردید؟{" "}
            <Link to="/login">بازگشت به ورود</Link>
          </p>
        </div>
      </div>
    </div>
  );
}

