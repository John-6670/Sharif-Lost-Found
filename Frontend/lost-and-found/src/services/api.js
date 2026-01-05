/**
 * API Service for Lost & Found Platform
 * With JWT authentication and automatic token refresh
 */

import {
  getAccessToken,
  getRefreshToken,
  setTokens,
  updateAccessToken,
  setUserData,
  clearAuth,
  setRememberMe,
} from "./auth";

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "https://lostfound1.pythonanywhere.com";

// Track ongoing refresh to prevent multiple simultaneous refreshes
let isRefreshing = false;
let refreshSubscribers = [];

/**
 * Subscribe to token refresh completion
 * @param {Function} callback - Callback to execute with new token
 */
function subscribeTokenRefresh(callback) {
  refreshSubscribers.push(callback);
}

/**
 * Notify all subscribers that refresh is complete
 * @param {string} token - New access token
 */
function onTokenRefreshed(token) {
  refreshSubscribers.forEach((callback) => callback(token));
  refreshSubscribers = [];
}

/**
 * Notify all subscribers that refresh failed
 * @param {Error} error - The error that occurred
 */
function onRefreshFailed(error) {
  refreshSubscribers.forEach((callback) => callback(null, error));
  refreshSubscribers = [];
}

/**
 * Get user-friendly error message based on HTTP status
 * @param {number} status - HTTP status code
 * @returns {string} Localized error message
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

/**
 * Refresh the access token using refresh token
 * @returns {Promise<string>} New access token
 */
async function refreshAccessToken() {
  const refreshToken = getRefreshToken();
  
  if (!refreshToken) {
    throw new Error("No refresh token available");
  }

  const response = await fetch(`${API_BASE_URL}/api/users/token-refresh/`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify({ refresh: refreshToken }),
  });

  const data = await response.json();

  if (!response.ok) {
    const error = new Error(data.detail || "Token refresh failed");
    error.status = response.status;
    error.code = data.code;
    throw error;
  }

  return data.access;
}

/**
 * Generic fetch wrapper with authentication and auto-refresh
 * @param {string} endpoint - API endpoint
 * @param {Object} options - Fetch options
 * @param {boolean} skipAuth - Skip authentication header
 * @param {boolean} isRetry - Is this a retry after token refresh
 * @returns {Promise<any>} Response data
 */
async function fetchAPI(endpoint, options = {}, skipAuth = false, isRetry = false) {
  const url = `${API_BASE_URL}${endpoint}`;
  
  const headers = {
    "Content-Type": "application/json",
    ...options.headers,
  };

  // Add authorization header if we have a token and shouldn't skip auth
  if (!skipAuth) {
    const accessToken = getAccessToken();
    if (accessToken) {
      headers["Authorization"] = `Bearer ${accessToken}`;
    }
  }

  const config = {
    ...options,
    headers,
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
      data = {};
    }

    // Handle 401 - try to refresh token
    if (response.status === 401 && !skipAuth && !isRetry) {
      const refreshToken = getRefreshToken();
      
      // Check if this is a "not verified" error - don't try to refresh
      if (data.code === "no_active_account") {
        const error = new Error(data.detail || "دسترسی غیرمجاز.");
        error.status = 401;
        error.code = data.code;
        error.data = data;
        throw error;
      }
      
      if (refreshToken) {
        // If already refreshing, wait for it to complete
        if (isRefreshing) {
          return new Promise((resolve, reject) => {
            subscribeTokenRefresh((newToken, error) => {
              if (error) {
                reject(error);
                return;
              }
              if (newToken) {
                // Retry with new token
                options.headers = {
                  ...options.headers,
                  Authorization: `Bearer ${newToken}`,
                };
                fetchAPI(endpoint, options, false, true)
                  .then(resolve)
                  .catch(reject);
              } else {
                reject(new Error("Token refresh failed"));
              }
            });
          });
        }

        // Start refreshing
        isRefreshing = true;

        try {
          const newAccessToken = await refreshAccessToken();
          updateAccessToken(newAccessToken);
          isRefreshing = false;
          onTokenRefreshed(newAccessToken);
          
          // Retry the original request with new token
          return fetchAPI(endpoint, options, false, true);
        } catch (refreshError) {
          isRefreshing = false;
          onRefreshFailed(refreshError);
          
          // If refresh failed with token_not_valid, logout and redirect
          if (refreshError.code === "token_not_valid" || refreshError.status === 401) {
            clearAuth();
            window.location.href = "/login";
          }
          
          throw refreshError;
        }
      } else {
        // No refresh token, clear auth and redirect
        clearAuth();
        window.location.href = "/login";
      }
    }

    if (!response.ok) {
      const errorMessage = data.error || data.detail || data.message || getErrorMessage(response.status);
      const error = new Error(errorMessage);
      error.status = response.status;
      error.code = data.code;
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

// ========== Authentication API ==========

/**
 * Log in a user
 * Backend endpoint: POST /api/users/login/
 * 
 * @param {Object} credentials - Login credentials
 * @param {string} credentials.email - User email
 * @param {string} credentials.password - User password
 * @param {boolean} rememberMe - Whether to persist tokens
 * @returns {Promise<Object>} - Response with tokens and user info
 * @returns {string} response.access - JWT access token
 * @returns {string} response.refresh - JWT refresh token
 * @returns {string} response.user_id - User ID
 * @returns {string} response.email - User email
 */
export async function login(credentials, rememberMe = false) {
  // Set remember me preference before storing tokens
  setRememberMe(rememberMe);
  
  const response = await fetchAPI("/api/users/login/", {
    method: "POST",
    body: JSON.stringify({
      email: credentials.email,
      password: credentials.password,
    }),
  }, true); // Skip auth for login
  
  // Store tokens and user data
  setTokens({
    access: response.access,
    refresh: response.refresh,
  });
  
  setUserData({
    user_id: response.user_id,
    email: response.email,
  });
  
  return response;
}

/**
 * Log out the current user
 * Clears all stored tokens and user data
 */
export function logout() {
  clearAuth();
}

/**
 * Refresh access token
 * Backend endpoint: POST /api/users/token-refresh/
 * 
 * @returns {Promise<string>} New access token
 */
export async function refreshToken() {
  const newToken = await refreshAccessToken();
  updateAccessToken(newToken);
  return newToken;
}

// ========== Registration API ==========

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
  }, true); // Skip auth for registration
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
  }, true); // Skip auth
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
  }, true); // Skip auth
}

// Legacy aliases for backwards compatibility
export const signupRequestOtp = registerRequest;
export const signupVerifyOtp = verifyRegistrationOtp;
export const resendSignupOtp = resendRegistrationOtp;

// ========== User API ==========

/**
 * Get current user profile
 * @returns {Promise<Object>} - User profile data
 */
export async function getCurrentUser() {
  return fetchAPI("/api/users/me/", {
    method: "GET",
  });
}

// ========== Password Reset API ==========

/**
 * Request password reset OTP
 * @param {Object} data - Request data
 * @param {string} data.email - User email
 * @returns {Promise<Object>} - Response from server
 */
export async function forgotPassword(data) {
  return fetchAPI("/api/users/password-reset/request/", {
    method: "POST",
    body: JSON.stringify(data),
  }, true); // Skip auth
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
  return fetchAPI("/api/users/password-reset/verify/", {
    method: "POST",
    body: JSON.stringify({
      email: data.email,
      otp: data.otp,
      new_password: data.newPassword,
    }),
  }, true); // Skip auth
}

// ========== Export auth utilities ==========
export { getAccessToken, getRefreshToken, isAuthenticated, getUserData, clearAuth } from "./auth";
