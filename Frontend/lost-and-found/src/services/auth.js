/**
 * Authentication Service for Lost & Found Platform
 * Handles JWT token storage and management
 */

// Storage keys
const ACCESS_TOKEN_KEY = "access_token";
const REFRESH_TOKEN_KEY = "refresh_token";
const USER_DATA_KEY = "user_data";
const REMEMBER_ME_KEY = "remember_me";

/**
 * Get the appropriate storage based on "remember me" preference
 * @returns {Storage} localStorage or sessionStorage
 */
function getStorage() {
  const rememberMe = localStorage.getItem(REMEMBER_ME_KEY) === "true";
  return rememberMe ? localStorage : sessionStorage;
}

/**
 * Set remember me preference
 * @param {boolean} remember - Whether to persist across sessions
 */
export function setRememberMe(remember) {
  if (remember) {
    localStorage.setItem(REMEMBER_ME_KEY, "true");
  } else {
    localStorage.removeItem(REMEMBER_ME_KEY);
  }
}

/**
 * Get access token
 * @returns {string|null} Access token or null
 */
export function getAccessToken() {
  // Check both storages (localStorage for remember me, sessionStorage for session only)
  return localStorage.getItem(ACCESS_TOKEN_KEY) || sessionStorage.getItem(ACCESS_TOKEN_KEY);
}

/**
 * Get refresh token
 * @returns {string|null} Refresh token or null
 */
export function getRefreshToken() {
  return localStorage.getItem(REFRESH_TOKEN_KEY) || sessionStorage.getItem(REFRESH_TOKEN_KEY);
}

/**
 * Store authentication tokens
 * @param {Object} tokens - Token data
 * @param {string} tokens.access - Access token
 * @param {string} tokens.refresh - Refresh token
 */
export function setTokens({ access, refresh }) {
  const storage = getStorage();
  
  // Clear from both storages first to avoid duplicates
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  
  // Store in appropriate storage
  storage.setItem(ACCESS_TOKEN_KEY, access);
  storage.setItem(REFRESH_TOKEN_KEY, refresh);
}

/**
 * Update only the access token (used after refresh)
 * @param {string} access - New access token
 */
export function updateAccessToken(access) {
  const storage = getStorage();
  
  // Clear from both storages first
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  
  storage.setItem(ACCESS_TOKEN_KEY, access);
}

/**
 * Store user data
 * @param {Object} user - User data
 * @param {string} user.user_id - User ID
 * @param {string} user.email - User email
 */
export function setUserData({ user_id, email }) {
  const storage = getStorage();
  
  // Clear from both storages first
  localStorage.removeItem(USER_DATA_KEY);
  sessionStorage.removeItem(USER_DATA_KEY);
  
  storage.setItem(USER_DATA_KEY, JSON.stringify({ user_id, email }));
}

/**
 * Get stored user data
 * @returns {Object|null} User data or null
 */
export function getUserData() {
  const data = localStorage.getItem(USER_DATA_KEY) || sessionStorage.getItem(USER_DATA_KEY);
  if (!data) return null;
  
  try {
    return JSON.parse(data);
  } catch {
    return null;
  }
}

/**
 * Clear all authentication data (logout)
 */
export function clearAuth() {
  // Clear from both storages
  localStorage.removeItem(ACCESS_TOKEN_KEY);
  localStorage.removeItem(REFRESH_TOKEN_KEY);
  localStorage.removeItem(USER_DATA_KEY);
  localStorage.removeItem(REMEMBER_ME_KEY);
  
  sessionStorage.removeItem(ACCESS_TOKEN_KEY);
  sessionStorage.removeItem(REFRESH_TOKEN_KEY);
  sessionStorage.removeItem(USER_DATA_KEY);
  
  // Also clear legacy keys if they exist
  localStorage.removeItem("authToken");
  localStorage.removeItem("user");
  sessionStorage.removeItem("authToken");
  sessionStorage.removeItem("user");
}

/**
 * Check if user is authenticated
 * @returns {boolean} True if access token exists
 */
export function isAuthenticated() {
  return !!getAccessToken();
}

