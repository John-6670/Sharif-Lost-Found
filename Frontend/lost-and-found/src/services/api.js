/**
 * API Service for Lost & Found Platform
 */

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:3000/api";

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
    const data = await response.json();

    if (!response.ok) {
      // Handle specific error messages from backend
      throw new Error(data.message || data.error || getErrorMessage(response.status));
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
    409: "این ایمیل یا شماره دانشجویی قبلاً ثبت شده است.",
    422: "اطلاعات وارد شده نامعتبر است.",
    429: "تعداد درخواست‌ها بیش از حد مجاز است. لطفاً کمی صبر کنید.",
    500: "خطای سرور. لطفاً بعداً تلاش کنید.",
    502: "سرور در دسترس نیست.",
    503: "سرویس موقتاً در دسترس نیست.",
  };
  return messages[status] || "خطای ناشناخته رخ داده است.";
}

/**
 * Sign up a new student
 * @param {Object} userData - User registration data
 * @param {string} userData.fullName - Student's full name
 * @param {string} userData.studentId - Student ID number
 * @param {string} userData.email - University email
 * @param {string} userData.password - Password
 * @returns {Promise<Object>} - Response from server
 */
export async function signup(userData) {
  return fetchAPI("/auth/signup", {
    method: "POST",
    body: JSON.stringify(userData),
  });
}

/**
 * Log in a user
 * @param {Object} credentials - Login credentials
 * @param {string} credentials.email - User email
 * @param {string} credentials.password - User password
 * @returns {Promise<Object>} - Response with auth token
 */
export async function login(credentials) {
  return fetchAPI("/auth/login", {
    method: "POST",
    body: JSON.stringify(credentials),
  });
}

/**
 * Log out the current user
 * @returns {Promise<void>}
 */
export async function logout() {
  const token = localStorage.getItem("authToken");
  if (token) {
    await fetchAPI("/auth/logout", {
      method: "POST",
      headers: {
        Authorization: `Bearer ${token}`,
      },
    });
  }
  localStorage.removeItem("authToken");
}

/**
 * Get current user profile
 * @returns {Promise<Object>} - User profile data
 */
export async function getCurrentUser() {
  const token = localStorage.getItem("authToken");
  if (!token) {
    throw new Error("کاربر وارد نشده است.");
  }

  return fetchAPI("/auth/me", {
    headers: {
      Authorization: `Bearer ${token}`,
    },
  });
}

/**
 * Request password reset OTP
 * @param {Object} data - Request data
 * @param {string} data.identifier - User email or student ID
 * @returns {Promise<Object>} - Response from server
 */
export async function forgotPassword(data) {
  return fetchAPI("/auth/forgot-password", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

/**
 * Reset password with OTP verification
 * @param {Object} data - Reset data
 * @param {string} data.identifier - User email or student ID
 * @param {string} data.otp - OTP code received
 * @param {string} data.newPassword - New password to set
 * @returns {Promise<Object>} - Response from server
 */
export async function resetPassword(data) {
  return fetchAPI("/auth/reset-password", {
    method: "POST",
    body: JSON.stringify(data),
  });
}

