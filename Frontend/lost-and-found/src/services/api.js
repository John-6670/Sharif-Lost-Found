/**
 * API Service for Lost & Found Platform
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8000";

/**
 * Generic fetch wrapper with error handling
 */
async function fetchAPI(endpoint, options = {}) {
  const url = `${API_BASE_URL}${endpoint}`;
  
  const config = {
    headers: {
      "Content-Type": "application/json",
      ...options.headers,
    },
    ...options,
  };

  try {
    const response = await fetch(url, config);
    
    // Handle empty response (204 No Content)
    if (response.status === 204) {
      return null;
    }
    
    let data;
    try {
      data = await response.json();
    } catch {
      // If response is not JSON, create a generic response
      data = {};
    }

    if (!response.ok) {
      // Handle backend error format: { error: "..." } or { detail: "..." }
      const errorMessage = data.error || data.detail || data.message || getErrorMessage(response.status);
      const error = new Error(errorMessage);
      error.status = response.status;
      error.data = data;
      throw error;
    }

    return data;
  } catch (error) {
    if (error instanceof TypeError && error.message === "Failed to fetch") {
      throw new Error("خطا در برقراری ارتباط با سرور. لطفاً اتصال اینترنت خود را بررسی کنید.");
    }
    throw error;
  }
}

/**
 * Get user-friendly error message based on HTTP status
 */
function getErrorMessage(status) {
  const messages = {
    400: "اطلاعات وارد شده نامعتبر است.",
    401: "دسترسی غیرمجاز.",
    403: "شما اجازه دسترسی به این بخش را ندارید.",
    404: "منبع مورد نظر یافت نشد.",
    409: "این ایمیل قبلاً ثبت شده است.",
    422: "اطلاعات وارد شده نامعتبر است.",
    429: "تعداد درخواست‌ها بیش از حد مجاز است. لطفاً کمی صبر کنید.",
    500: "خطای سرور. لطفاً بعداً تلاش کنید.",
    502: "سرور در دسترس نیست.",
    503: "سرویس موقتاً در دسترس نیست.",
  };
  return messages[status] || "خطای ناشناخته رخ داده است.";
}

// ========== Authentication API ==========

/**
 * Request registration - Creates unverified user and sends OTP
 * Backend endpoint: POST /api/users/register/request/
 * 
 * @param {Object} userData - User registration data
 * @param {string} userData.email - User email address
 * @param {string} userData.name - User's full name
 * @param {string} userData.password - Password (min 8 chars)
 * @returns {Promise<{ message: string }>} - Success message
 */
export async function registerRequest(userData) {
  return fetchAPI("/api/users/register/request/", {
    method: "POST",
    body: JSON.stringify({
      email: userData.email,
      name: userData.name,
      password: userData.password,
    }),
  });
}

/**
 * Resend OTP for registration
 * Backend endpoint: POST /api/users/register/resend-otp/
 * 
 * @param {Object} data - Request data
 * @param {string} data.email - Email of unverified user
 * @returns {Promise<{ message: string }>} - Success message
 */
export async function resendRegistrationOtp(data) {
  return fetchAPI("/api/users/register/resend-otp/", {
    method: "POST",
    body: JSON.stringify({
      email: data.email,
    }),
  });
}

/**
 * Verify OTP and complete registration
 * Backend endpoint: POST /api/users/register/verify/
 * Note: OTP expires after 2 minutes
 * 
 * @param {Object} data - Verification data
 * @param {string} data.email - Email used during registration
 * @param {string} data.otp - 6-digit OTP code
 * @returns {Promise<{ message: string }>} - Success message
 */
export async function verifyRegistrationOtp(data) {
  return fetchAPI("/api/users/register/verify/", {
    method: "POST",
    body: JSON.stringify({
      email: data.email,
      otp: data.otp,
    }),
  });
}

// Legacy aliases for backwards compatibility
export const signupRequestOtp = registerRequest;
export const signupVerifyOtp = verifyRegistrationOtp;
export const resendSignupOtp = resendRegistrationOtp;

/**
 * Log in a user
 * @param {Object} credentials - Login credentials
 * @param {string} credentials.email - User email
 * @param {string} credentials.password - User password
 * @returns {Promise<Object>} - Response with auth token
 */
export async function login(credentials) {
  return fetchAPI("/api/auth/login", {
    method: "POST",
    body: JSON.stringify(credentials),
  });
}

/**
 * Log out the current user
 * @returns {Promise<void>}
 */
export async function logout() {
  const token = localStorage.getItem("authToken") || sessionStorage.getItem("authToken");
  if (token) {
    try {
      await fetchAPI("/api/auth/logout", {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
        },
      });
    } catch {
      // Ignore logout errors, still clear tokens
    }
  }
  localStorage.removeItem("authToken");
  sessionStorage.removeItem("authToken");
  localStorage.removeItem("user");
  sessionStorage.removeItem("user");
}

/**
 * Get current user profile
 * @returns {Promise<Object>} - User profile data
 */
export async function getCurrentUser() {
  const token = localStorage.getItem("authToken") || sessionStorage.getItem("authToken");
  if (!token) {
    throw new Error("کاربر وارد نشده است.");
  }

  return fetchAPI("/api/auth/me", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

/**
 * Request password reset OTP
 * @param {Object} data - Request data
 * @param {string} data.email - User email
 * @returns {Promise<Object>} - Response from server
 */
export async function forgotPassword(data) {
  return fetchAPI("/api/auth/forgot-password", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

/**
 * Reset password with OTP verification
 * @param {Object} data - Reset data
 * @param {string} data.email - User email
 * @param {string} data.otp - OTP code received
 * @param {string} data.newPassword - New password to set
 * @returns {Promise<Object>} - Response from server
 */
export async function resetPassword(data) {
  return fetchAPI("/api/auth/reset-password", {
    method: "POST",
    body: JSON.stringify(data),
  });
}
