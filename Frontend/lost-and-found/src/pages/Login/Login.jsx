import { useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { login } from "../../services/api";
import "./Login.css";

// Email validation
function isValidEmail(email) {
  const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
  return emailRegex.test(email);
}

export default function Login() {
  const navigate = useNavigate();
  
  // Form state
  const [formData, setFormData] = useState({
    email: "",
    password: "",
    rememberMe: false,
  });

  // UI state
  const [errors, setErrors] = useState({});
  const [touched, setTouched] = useState({});
  const [showPassword, setShowPassword] = useState(false);
  const [isSubmitting, setIsSubmitting] = useState(false);
  const [submitError, setSubmitError] = useState("");
  const [showVerifyLink, setShowVerifyLink] = useState(false);

  // Validation
  const validateField = (name, value) => {
    switch (name) {
      case "email":
        if (!value.trim()) return "ایمیل الزامی است";
        if (!isValidEmail(value.trim())) {
          return "فرمت ایمیل نامعتبر است";
        }
        return "";
      
      case "password":
        if (!value) return "رمز عبور الزامی است";
        if (value.length < 8) return "رمز عبور باید حداقل ۸ کاراکتر باشد";
        return "";
      
      default:
        return "";
    }
  };

  // Handlers
  const handleChange = (e) => {
    const { name, value, type, checked } = e.target;
    const newValue = type === "checkbox" ? checked : value;
    
    setFormData((prev) => ({ ...prev, [name]: newValue }));
    setSubmitError("");
    setShowVerifyLink(false);
    
    // Real-time validation for touched fields
    if (touched[name]) {
      const error = validateField(name, newValue);
      setErrors((prev) => ({ ...prev, [name]: error }));
    }
  };

  const handleBlur = (e) => {
    const { name, value } = e.target;
    
    setTouched((prev) => ({ ...prev, [name]: true }));
    const error = validateField(name, value);
    setErrors((prev) => ({ ...prev, [name]: error }));
  };

  const handleSubmit = async (e) => {
    e.preventDefault();
    
    // Mark all fields as touched
    setTouched({ email: true, password: true });
    
    // Validate all fields
    const emailError = validateField("email", formData.email);
    const passwordError = validateField("password", formData.password);
    
    const formErrors = {};
    if (emailError) formErrors.email = emailError;
    if (passwordError) formErrors.password = passwordError;
    
    setErrors(formErrors);
    
    if (Object.keys(formErrors).length > 0) return;
    
    setIsSubmitting(true);
    setSubmitError("");
    setShowVerifyLink(false);
    
    try {
      const credentials = {
        email: formData.email.trim(),
        password: formData.password,
      };
      
      await login(credentials, formData.rememberMe);
      
      // Navigate to main page
      navigate("/home");
    } catch (error) {
      // Handle specific error cases
      if (error.code === "no_active_account") {
        // Check if it's specifically about verification
        if (error.data?.detail?.toLowerCase().includes("not verified")) {
          setSubmitError("حساب شما تأیید نشده است. لطفاً ابتدا ایمیل خود را تأیید کنید.");
          setShowVerifyLink(true);
        } else {
          setSubmitError("ایمیل یا رمز عبور اشتباه است.");
        }
      } else if (error.status === 401) {
        setSubmitError("ایمیل یا رمز عبور اشتباه است.");
      } else if (error.status === 429) {
        setSubmitError("تعداد تلاش‌ها بیش از حد مجاز است. لطفاً کمی صبر کنید.");
      } else {
        setSubmitError(error.message || "خطا در ورود. لطفاً دوباره تلاش کنید.");
      }
    } finally {
      setIsSubmitting(false);
    }
  };

  // Check if form is valid
  const isFormValid = 
    isValidEmail(formData.email.trim()) &&
    formData.password.length >= 8;

  return (
    <div className="login-container">
      <div className="login-card">
        <div className="login-header">
          <div className="logo-icon">🔐</div>
          <h1>ورود به حساب کاربری</h1>
          <p>به سامانه اشیاء گم‌شده دانشگاه خوش آمدید</p>
        </div>

        {submitError && (
          <div className="alert alert-error" role="alert">
            <span className="alert-icon">⚠</span>
            <div className="alert-content">
              <span>{submitError}</span>
              {showVerifyLink && (
                <Link to="/verify-email" className="verify-link">
                  تأیید حساب کاربری
                </Link>
              )}
            </div>
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
              autoFocus
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
                placeholder="رمز عبور خود را وارد کنید"
                dir="ltr"
                aria-describedby={errors.password ? "password-error" : undefined}
                aria-invalid={errors.password && touched.password ? "true" : "false"}
                autoComplete="current-password"
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
            {errors.password && touched.password && (
              <span className="error-message" id="password-error" role="alert">
                {errors.password}
              </span>
            )}
          </div>

          {/* Remember Me & Forgot Password Row */}
          <div className="form-options">
            <label className="checkbox-label">
              <input
                type="checkbox"
                name="rememberMe"
                checked={formData.rememberMe}
                onChange={handleChange}
              />
              <span className="checkbox-custom" />
              <span>مرا به خاطر بسپار</span>
            </label>
            
            <Link to="/forgot-password" className="forgot-link">
              رمز عبور را فراموش کردم
            </Link>
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
                در حال ورود...
              </>
            ) : (
              "ورود"
            )}
          </button>
        </form>

        <div className="login-footer">
          <p>
            حساب کاربری ندارید؟{" "}
            <Link to="/signup">ثبت‌نام کنید</Link>
          </p>
        </div>
      </div>
    </div>
  );
}
