import { useState, useEffect } from "react";
import { Link, useNavigate } from "react-router-dom";
import { isAuthenticated, getUserData, clearAuth } from "../../services/auth";
import "./Profile.css";

// Mock user data - TODO: Replace with actual API call
const MOCK_USER_PROFILE = {
  name: "علی محمدی",
  email: "ali.mohammadi@sharif.edu",
  studentId: "99101234",
  department: "مهندسی کامپیوتر",
  joinedDate: "1403/08/15",
  itemsReported: 5,
  itemsFound: 3,
  avatar: null,
};

// Mock user items - TODO: Replace with actual API call
const MOCK_USER_ITEMS = [
  {
    id: 1,
    name: "کیف لپتاپ مشکی",
    type: "lost",
    status: "active",
    date: "1404/10/01",
    location: "کتابخانه مرکزی",
  },
  {
    id: 2,
    name: "فلش مموری ۱۶ گیگ",
    type: "found",
    status: "resolved",
    date: "1404/09/25",
    location: "سلف دانشجویی",
  },
  {
    id: 3,
    name: "عینک طبی",
    type: "lost",
    status: "active",
    date: "1404/09/20",
    location: "دانشکده برق",
  },
];

export default function Profile() {
  const navigate = useNavigate();
  const [activeTab, setActiveTab] = useState("info");
  const [isLoading, setIsLoading] = useState(true);
  const [userProfile, setUserProfile] = useState(null);
  const [userItems, setUserItems] = useState([]);

  // Check authentication
  useEffect(() => {
    if (!isAuthenticated()) {
      navigate("/login");
      return;
    }

    // Simulate API call
    const loadData = async () => {
      await new Promise((resolve) => setTimeout(resolve, 500));
      
      // Get stored user data and merge with mock
      const storedUser = getUserData();
      setUserProfile({
        ...MOCK_USER_PROFILE,
        email: storedUser?.email || MOCK_USER_PROFILE.email,
      });
      setUserItems(MOCK_USER_ITEMS);
      setIsLoading(false);
    };

    loadData();
  }, [navigate]);

  const handleLogout = () => {
    clearAuth();
    navigate("/login");
  };

  if (isLoading) {
    return (
      <div className="profile-container">
        <div className="profile-loading">
          <div className="loading-spinner" />
          <p>در حال بارگذاری...</p>
        </div>
      </div>
    );
  }

  return (
    <div className="profile-container">
      {/* Header */}
      <header className="profile-header">
        <div className="header-content">
          <Link to="/" className="back-btn">
            <span>→</span>
            بازگشت
          </Link>
          <h1>پروفایل کاربری</h1>
          <div className="header-spacer" />
        </div>
      </header>

      <main className="profile-main">
        {/* Profile Card */}
        <section className="profile-card">
          <div className="profile-avatar">
            {userProfile?.avatar ? (
              <img src={userProfile.avatar} alt={userProfile.name} />
            ) : (
              <span className="avatar-placeholder">👤</span>
            )}
          </div>
          <div className="profile-info">
            <h2 className="profile-name">{userProfile?.name}</h2>
            <p className="profile-email" dir="ltr">{userProfile?.email}</p>
            <div className="profile-badges">
              <span className="badge badge-blue">
                📚 {userProfile?.department}
              </span>
              <span className="badge badge-gray">
                🎓 {userProfile?.studentId}
              </span>
            </div>
          </div>
          <div className="profile-stats">
            <div className="profile-stat">
              <span className="stat-number">{userProfile?.itemsReported}</span>
              <span className="stat-text">گزارش شده</span>
            </div>
            <div className="profile-stat">
              <span className="stat-number">{userProfile?.itemsFound}</span>
              <span className="stat-text">پیدا شده</span>
            </div>
            <div className="profile-stat">
              <span className="stat-number">{userProfile?.joinedDate}</span>
              <span className="stat-text">تاریخ عضویت</span>
            </div>
          </div>
        </section>

        {/* Tabs */}
        <div className="profile-tabs">
          <button
            className={`tab-btn ${activeTab === "info" ? "active" : ""}`}
            onClick={() => setActiveTab("info")}
          >
            اطلاعات حساب
          </button>
          <button
            className={`tab-btn ${activeTab === "items" ? "active" : ""}`}
            onClick={() => setActiveTab("items")}
          >
            اشیاء من
          </button>
        </div>

        {/* Tab Content */}
        <div className="tab-content">
          {activeTab === "info" && (
            <section className="info-section">
              <div className="info-card">
                <h3>اطلاعات شخصی</h3>
                <div className="info-grid">
                  <div className="info-item">
                    <span className="info-label">نام و نام خانوادگی</span>
                    <span className="info-value">{userProfile?.name}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">ایمیل</span>
                    <span className="info-value" dir="ltr">{userProfile?.email}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">شماره دانشجویی</span>
                    <span className="info-value">{userProfile?.studentId}</span>
                  </div>
                  <div className="info-item">
                    <span className="info-label">دانشکده</span>
                    <span className="info-value">{userProfile?.department}</span>
                  </div>
                </div>
              </div>

              <div className="info-card">
                <h3>عملیات حساب</h3>
                <div className="actions-list">
                  <Link to="/change-password" className="action-item">
                    <span className="action-icon">🔑</span>
                    <span className="action-text">تغییر رمز عبور</span>
                    <span className="action-arrow">←</span>
                  </Link>
                  <button className="action-item logout" onClick={handleLogout}>
                    <span className="action-icon">🚪</span>
                    <span className="action-text">خروج از حساب</span>
                    <span className="action-arrow">←</span>
                  </button>
                </div>
              </div>
            </section>
          )}

          {activeTab === "items" && (
            <section className="items-section">
              {userItems.length === 0 ? (
                <div className="empty-state">
                  <span className="empty-icon">📦</span>
                  <h3>هنوز شیئی ثبت نکرده‌اید</h3>
                  <p>اولین شیء گم‌شده یا پیداشده خود را ثبت کنید</p>
                  <Link to="/items/new" className="btn btn-primary">
                    ثبت شیء جدید
                  </Link>
                </div>
              ) : (
                <div className="items-list">
                  {userItems.map((item) => (
                    <Link key={item.id} to={`/items/${item.id}`} className="item-card">
                      <div className={`item-type ${item.type}`}>
                        {item.type === "lost" ? "🔴 گم‌شده" : "🟢 پیداشده"}
                      </div>
                      <div className="item-details">
                        <h4 className="item-name">{item.name}</h4>
                        <p className="item-meta">
                          <span>📍 {item.location}</span>
                          <span>📅 {item.date}</span>
                        </p>
                      </div>
                      <div className={`item-status ${item.status}`}>
                        {item.status === "active" ? "فعال" : "حل شده"}
                      </div>
                    </Link>
                  ))}
                </div>
              )}
            </section>
          )}
        </div>
      </main>
    </div>
  );
}

