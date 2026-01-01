/**
 * Validation Utilities for Lost & Found Platform
 */

// ========== Email Validation ==========

/**
 * Get allowed email domains from environment
 * @returns {string[]} Array of allowed domains, empty if all allowed
 */
export function getAllowedEmailDomains() {
  const domainsEnv = import.meta.env.VITE_ALLOWED_EMAIL_DOMAINS || "";
  if (!domainsEnv.trim()) return [];
  return domainsEnv
    .split(",")
    .map((d) => d.trim().toLowerCase())
    .filter(Boolean);
}

/**
 * Basic email format validation (RFC-like)
 * @param {string} email - Email to validate
 * @returns {boolean} True if valid format
 */
export function isValidEmailFormat(email) {
  if (!email || typeof email !== "string") return false;
  const trimmed = email.trim();
  // RFC 5322 simplified pattern
  const emailRegex = /^[a-zA-Z0-9.!#$%&'*+/=?^_`{|}~-]+@[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?(?:\.[a-zA-Z0-9](?:[a-zA-Z0-9-]{0,61}[a-zA-Z0-9])?)+$/;
  return emailRegex.test(trimmed);
}

/**
 * Get domain from email
 * @param {string} email - Email address
 * @returns {string} Domain part
 */
export function getEmailDomain(email) {
  if (!email || !email.includes("@")) return "";
  return email.split("@")[1]?.toLowerCase() || "";
}

/**
 * Get local part from email (before @)
 * @param {string} email - Email address
 * @returns {string} Local part
 */
export function getEmailLocalPart(email) {
  if (!email || !email.includes("@")) return "";
  return email.split("@")[0] || "";
}

/**
 * Validate university email
 * @param {string} email - Email to validate
 * @param {string[]} allowedDomains - List of allowed domains (empty = all allowed)
 * @returns {{ valid: boolean, error: string }} Validation result
 */
export function validateUniversityEmail(email, allowedDomains = null) {
  const domains = allowedDomains ?? getAllowedEmailDomains();
  const trimmed = (email || "").trim();

  if (!trimmed) {
    return { valid: false, error: "ایمیل الزامی است" };
  }

  if (!isValidEmailFormat(trimmed)) {
    return { valid: false, error: "فرمت ایمیل نامعتبر است" };
  }

  // Check domain restriction if domains are specified
  if (domains.length > 0) {
    const emailDomain = getEmailDomain(trimmed);
    if (!domains.includes(emailDomain)) {
      const exampleDomain = domains[0];
      return {
        valid: false,
        error: `لطفاً از ایمیل دانشگاهی استفاده کنید (example@${exampleDomain})`,
      };
    }
  }

  return { valid: true, error: "" };
}

/**
 * Normalize email (lowercase, trim)
 * @param {string} email - Email to normalize
 * @returns {string} Normalized email
 */
export function normalizeEmail(email) {
  return (email || "").trim().toLowerCase();
}

// ========== Password Validation ==========

const PASSWORD_MIN_LENGTH = 10;
const PASSWORD_REQUIRED_CATEGORIES = 3;

/**
 * Check password requirements
 * @param {string} password - Password to check
 * @param {{ email?: string }} options - Options for personal info check
 * @returns {Object} Requirements status
 */
export function checkPasswordRequirements(password, options = {}) {
  const pwd = password || "";
  const { email = "" } = options;
  const emailLocalPart = getEmailLocalPart(email).toLowerCase();

  const requirements = {
    minLength: {
      met: pwd.length >= PASSWORD_MIN_LENGTH,
      label: `حداقل ${PASSWORD_MIN_LENGTH} کاراکتر`,
    },
    hasLowercase: {
      met: /[a-z]/.test(pwd),
      label: "حروف کوچک انگلیسی (a-z)",
    },
    hasUppercase: {
      met: /[A-Z]/.test(pwd),
      label: "حروف بزرگ انگلیسی (A-Z)",
    },
    hasDigit: {
      met: /\d/.test(pwd),
      label: "عدد (0-9)",
    },
    hasSymbol: {
      met: /[^a-zA-Z0-9]/.test(pwd),
      label: "نماد (!@#$%...)",
    },
    noLeadingTrailingSpaces: {
      met: pwd.length === 0 || (pwd === pwd.trim()),
      label: "بدون فاصله در ابتدا/انتها",
    },
    noPersonalInfo: {
      met: !emailLocalPart || emailLocalPart.length < 3 || !pwd.toLowerCase().includes(emailLocalPart),
      label: "بدون اطلاعات شخصی (ایمیل)",
    },
  };

  // Count categories met (lowercase, uppercase, digit, symbol)
  const categoriesMet = [
    requirements.hasLowercase.met,
    requirements.hasUppercase.met,
    requirements.hasDigit.met,
    requirements.hasSymbol.met,
  ].filter(Boolean).length;

  requirements.categoriesCount = {
    met: categoriesMet >= PASSWORD_REQUIRED_CATEGORIES,
    label: `حداقل ${PASSWORD_REQUIRED_CATEGORIES} دسته از ۴ دسته کاراکتر`,
    count: categoriesMet,
  };

  return requirements;
}

/**
 * Validate password meets policy
 * @param {string} password - Password to validate
 * @param {{ email?: string }} options - Options
 * @returns {{ valid: boolean, error: string, requirements: Object }} Validation result
 */
export function validatePassword(password, options = {}) {
  const pwd = password || "";
  const requirements = checkPasswordRequirements(pwd, options);

  if (!pwd) {
    return { valid: false, error: "رمز عبور الزامی است", requirements };
  }

  if (!requirements.minLength.met) {
    return {
      valid: false,
      error: `رمز عبور باید حداقل ${PASSWORD_MIN_LENGTH} کاراکتر باشد`,
      requirements,
    };
  }

  if (!requirements.noLeadingTrailingSpaces.met) {
    return {
      valid: false,
      error: "رمز عبور نباید با فاصله شروع یا تمام شود",
      requirements,
    };
  }

  if (!requirements.categoriesCount.met) {
    return {
      valid: false,
      error: `رمز عبور باید شامل حداقل ${PASSWORD_REQUIRED_CATEGORIES} دسته از کاراکترها باشد`,
      requirements,
    };
  }

  if (!requirements.noPersonalInfo.met) {
    return {
      valid: false,
      error: "رمز عبور نباید شامل بخشی از ایمیل شما باشد",
      requirements,
    };
  }

  return { valid: true, error: "", requirements };
}

/**
 * Calculate password strength
 * @param {string} password - Password to check
 * @returns {{ level: number, label: string, color: string }} Strength info
 */
export function getPasswordStrength(password) {
  if (!password) return { level: 0, label: "", color: "" };

  let score = 0;
  
  // Length points
  if (password.length >= 10) score += 1;
  if (password.length >= 14) score += 1;
  if (password.length >= 18) score += 1;
  
  // Category points
  if (/[a-z]/.test(password)) score += 1;
  if (/[A-Z]/.test(password)) score += 1;
  if (/\d/.test(password)) score += 1;
  if (/[^a-zA-Z0-9]/.test(password)) score += 1;
  
  // Variety bonus
  const uniqueChars = new Set(password).size;
  if (uniqueChars >= password.length * 0.7) score += 1;

  if (score <= 3) return { level: 1, label: "ضعیف", color: "#ef4444" };
  if (score <= 5) return { level: 2, label: "متوسط", color: "#f59e0b" };
  if (score <= 7) return { level: 3, label: "خوب", color: "#3b82f6" };
  return { level: 4, label: "قوی", color: "#16a34a" };
}

/**
 * Validate confirm password matches
 * @param {string} password - Original password
 * @param {string} confirmPassword - Confirmation password
 * @returns {{ valid: boolean, error: string }} Validation result
 */
export function validateConfirmPassword(password, confirmPassword) {
  if (!confirmPassword) {
    return { valid: false, error: "تکرار رمز عبور الزامی است" };
  }
  if (password !== confirmPassword) {
    return { valid: false, error: "رمز عبور و تکرار آن مطابقت ندارند" };
  }
  return { valid: true, error: "" };
}

// ========== Full Name Validation ==========

/**
 * Validate full name
 * @param {string} name - Name to validate
 * @returns {{ valid: boolean, error: string }} Validation result
 */
export function validateFullName(name) {
  const trimmed = (name || "").trim();
  
  if (!trimmed) {
    return { valid: false, error: "نام و نام خانوادگی الزامی است" };
  }
  
  if (trimmed.length < 3) {
    return { valid: false, error: "نام باید حداقل ۳ کاراکتر باشد" };
  }
  
  return { valid: true, error: "" };
}

// ========== OTP Validation ==========

const OTP_LENGTH = 6;

/**
 * Validate OTP code
 * @param {string} otp - OTP to validate
 * @returns {{ valid: boolean, error: string }} Validation result
 */
export function validateOtp(otp) {
  if (!otp) {
    return { valid: false, error: "کد تأیید الزامی است" };
  }
  
  if (!/^\d+$/.test(otp)) {
    return { valid: false, error: "کد تأیید باید عددی باشد" };
  }
  
  if (otp.length !== OTP_LENGTH) {
    return { valid: false, error: `کد تأیید باید ${OTP_LENGTH} رقم باشد` };
  }
  
  return { valid: true, error: "" };
}

