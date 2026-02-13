import { useState, useEffect, useRef } from "react";
import { Link, useNavigate } from "react-router-dom";
import { isAuthenticated, getUserData, clearAuth } from "../../services/auth";
import "./Home.css";

// Mock data - TODO: Replace with actual API call when backend is ready
const MOCK_ITEMS = [
  {
    id: 1,
    name: "لپتاپ ASUS",
    category: "electronics",
    location: "کتابخانه مرکزی",
    timestamp: "2025-01-01T09:30:00",
  },
  {
    id: 2,
    name: "جزوه ریاضی ۲",
    category: "documents",
    location: "دانشکده مهندسی",
    timestamp: "2025-01-01T14:10:00",
  },
  {
    id: 3,
    name: "کیف پول چرمی",
    category: "other",
    location: "سلف سرویس",
    timestamp: "2025-01-01T18:20:00",
  },
  {
    id: 4,
    name: "ژاکت مشکی",
    category: "clothing",
    location: "ساختمان آموزش",
    timestamp: "2025-01-01T11:45:00",
  },
];

// Mock stats - TODO: Replace with actual API call
const MOCK_STATS = {
  todayItems: 12,
  totalItems: 248,
  resolvedItems: 186,
};

// Category config
const CATEGORIES = {
  electronics: { label: "الکترونیک", icon: "💻", color: "#2563eb" },
  documents: { label: "مدارک", icon: "📄", color: "#16a34a" },
  clothing: { label: "پوشاک", icon: "👕", color: "#db2777" },
  other: { label: "سایر", icon: "📦", color: "#f59e0b" },
};

// TODO: Replace with actual API service
async function fetchRecentItems() {
  await new Promise((resolve) => setTimeout(resolve, 800));
  return MOCK_ITEMS;
}

async function fetchStats() {
  await new Promise((resolve) => setTimeout(resolve, 500));
  return MOCK_STATS;
}

/* ========== Item Card Component ========== */
function ItemCard({ item }) {
  const category = CATEGORIES[item.category] || CATEGORIES.other;
  const formattedDate = new Date(item.timestamp).toLocaleDateString("fa-IR", {
    day: "numeric",
    month: "long",
    hour: "2-digit",
    minute: "2-digit",
  });

  return (
    <Link to={`/items/${item.id}`} className="home-item-card">
      <div className="item-icon" style={{ backgroundColor: category.color }}>
        {category.icon}
      </div>
      <div className="item-content">
        <span className="item-category" style={{ color: category.color }}>
          {category.label}
        </span>
        <h4 className="item-name">{item.name}</h4>
        <p className="item-location">
          <span>📍</span> {item.location}
        </p>
        <p className="item-time">{formattedDate}</p>
      </div>
    </Link>
  );
}

/* ========== Skeleton Card ========== */
function SkeletonCard() {
  return (
    <div className="home-item-card skeleton">
      <div className="item-icon skeleton-icon" />
      <div className="item-content">
        <div className="skeleton-line short" />
        <div className="skeleton-line" />
        <div className="skeleton-line medium" />
      </div>
    </div>
  );
}

/* ========== User Menu Component ========== */
function UserMenu({ user, onLogout }) {
  const [isOpen, setIsOpen] = useState(false);
  const menuRef = useRef(null);

  // Close menu when clicking outside
  useEffect(() => {
    function handleClickOutside(event) {
      if (menuRef.current && !menuRef.current.contains(event.target)) {
        setIsOpen(false);
      }
    }
    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, []);

  // Close menu on escape key
  useEffect(() => {
    function handleEscape(event) {
      if (event.key === "Escape") {
        setIsOpen(false);
      }
    }
    document.addEventListener("keydown", handleEscape);
    return () => document.removeEventListener("keydown", handleEscape);
  }, []);

  return (
    <div className="user-menu" ref={menuRef}>
      <button
        className="user-menu-trigger"
        onClick={() => setIsOpen(!isOpen)}
        aria-expanded={isOpen}
        aria-haspopup="true"
      >
        <span className="user-avatar">👤</span>
        <span className="user-name">{user?.email?.split("@")[0] || "کاربر"}</span>
        <span className={`dropdown-arrow ${isOpen ? "open" : ""}`}>▼</span>
      </button>
      
      {isOpen && (
        <div className="user-dropdown" role="menu">
          <div className="dropdown-header">
            <span className="dropdown-email">{user?.email || "کاربر"}</span>
          </div>
          <div className="dropdown-divider" />
          <Link 
            to="/profile" 
            className="dropdown-item"
            role="menuitem"
            onClick={() => setIsOpen(false)}
          >
            <span className="dropdown-icon">👤</span>
            پروفایل من
          </Link>
          <button 
            className="dropdown-item logout-item"
            role="menuitem"
            onClick={() => {
              setIsOpen(false);
              onLogout();
            }}
          >
            <span className="dropdown-icon">🚪</span>
            خروج از حساب
          </button>
        </div>
      )}
    </div>
  );
}

/* ========== Main Home Component ========== */
export default function Home() {
  const navigate = useNavigate();
  const [items, setItems] = useState([]);
  const [stats, setStats] = useState(null);
  const [loadingItems, setLoadingItems] = useState(true);
  const [loadingStats, setLoadingStats] = useState(true);
  
  // Auth state
  const [isLoggedIn, setIsLoggedIn] = useState(false);
  const [userData, setUserData] = useState(null);

  // Check authentication status on mount
  useEffect(() => {
    const checkAuth = () => {
      const authenticated = isAuthenticated();
      setIsLoggedIn(authenticated);
      if (authenticated) {
        setUserData(getUserData());
      }
    };
    checkAuth();
  }, []);

  useEffect(() => {
    fetchRecentItems()
      .then(setItems)
      .finally(() => setLoadingItems(false));

    fetchStats()
      .then(setStats)
      .finally(() => setLoadingStats(false));
  }, []);

  const handleLogout = () => {
    clearAuth();
    setIsLoggedIn(false);
    setUserData(null);
    navigate("/");
  };

  return (
    <div className="home-container">
      {/* Header */}
      <header className="home-header">
        <div className="header-content">
          <Link to="/" className="header-brand">
            <span className="brand-icon">🎓</span>
            <span className="brand-text">شریف گم‌شده</span>
          </Link>
          <nav className="header-nav">
            <Link to="/map" className="nav-link">نقشه</Link>
            
            {isLoggedIn ? (
              <UserMenu user={userData} onLogout={handleLogout} />
            ) : (
              <>
                <Link to="/signup" className="nav-link">ثبت‌نام</Link>
                <Link to="/login" className="nav-link nav-btn">ورود</Link>
              </>
            )}
          </nav>
        </div>
      </header>

      <main className="home-main">
        {/* Hero Section */}
        <section className="hero-section">
          <div className="hero-card">
            <div className="hero-badge">🔍 سامانه گم‌شده و پیداشده</div>
            <h1 className="hero-title">
              شریف <span className="highlight">گم‌شده و پیداشده</span>
            </h1>
            <p className="hero-subtitle">
              شیء گم‌شده‌ای پیدا کردید؟ یا چیزی گم کرده‌اید؟
              <br />
              با کمک نقشه تعاملی، اشیاء گم‌شده را ثبت و جستجو کنید.
            </p>
            <div className="hero-actions">
              <Link to="/map" className="btn btn-primary">
                <span>🗺️</span> مشاهده نقشه
              </Link>
              {!isLoggedIn && (
                <Link to="/signup" className="btn btn-secondary">
                  <span>👤</span> ثبت‌نام
                </Link>
              )}
              {isLoggedIn && (
                <Link to="/items/new" className="btn btn-secondary">
                  <span>📝</span> ثبت شیء جدید
                </Link>
              )}
            </div>
          </div>
        </section>

        {/* Stats Section */}
        <section className="stats-section">
          <div className="stats-grid">
            <div className="stat-card">
              <div className="stat-icon blue">📦</div>
              <div className="stat-info">
                {loadingStats ? (
                  <div className="stat-skeleton" />
                ) : (
                  <span className="stat-value">
                    {stats?.todayItems?.toLocaleString("fa-IR") || "۰"}
                  </span>
                )}
                <span className="stat-label">اشیاء امروز</span>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon green">📊</div>
              <div className="stat-info">
                {loadingStats ? (
                  <div className="stat-skeleton" />
                ) : (
                  <span className="stat-value">
                    {stats?.totalItems?.toLocaleString("fa-IR") || "۰"}
                  </span>
                )}
                <span className="stat-label">کل ثبت‌شده</span>
              </div>
            </div>
            <div className="stat-card">
              <div className="stat-icon pink">✅</div>
              <div className="stat-info">
                {loadingStats ? (
                  <div className="stat-skeleton" />
                ) : (
                  <span className="stat-value">
                    {stats?.resolvedItems?.toLocaleString("fa-IR") || "۰"}
                  </span>
                )}
                <span className="stat-label">بازگردانده‌شده</span>
              </div>
            </div>
          </div>
        </section>

        {/* Quick Actions */}
        <section className="actions-section">
          <h2 className="section-title">دسترسی سریع</h2>
          <div className="actions-grid">
            <Link to="/map" className="action-card">
              <span className="action-icon">🗺️</span>
              <h3>جستجو در نقشه</h3>
              <p>اشیاء گم‌شده را روی نقشه ببینید</p>
            </Link>
            <Link to="/items/new" className="action-card">
              <span className="action-icon">📝</span>
              <h3>ثبت شیء پیداشده</h3>
              <p>شیء پیدا کردید؟ اینجا ثبت کنید</p>
            </Link>
            <Link to="/items" className="action-card">
              <span className="action-icon">📋</span>
              <h3>مرور همه اشیاء</h3>
              <p>لیست کامل اشیاء ثبت‌شده</p>
            </Link>
          </div>
        </section>

        {/* Recent Items */}
        <section className="recent-section">
          <div className="section-header">
            <h2 className="section-title">آخرین اشیاء</h2>
            <Link to="/items" className="view-all">
              مشاهده همه ←
            </Link>
          </div>
          <div className="items-grid">
            {loadingItems
              ? [1, 2, 3, 4].map((i) => <SkeletonCard key={i} />)
              : items.map((item) => <ItemCard key={item.id} item={item} />)}
          </div>
        </section>
      </main>

      {/* Footer */}
      <footer className="home-footer">
        <div className="footer-content">
          <div className="footer-brand">
            <span>🎓</span> سامانه گم‌شده و پیداشده دانشگاه شریف
          </div>
          <nav className="footer-nav">
            <Link to="/map">نقشه</Link>
            <Link to="/terms">قوانین</Link>
            <Link to="/about">درباره ما</Link>
            <Link to="/contact">تماس</Link>
          </nav>
          <p className="footer-copy">© ۱۴۰۴ - دانشگاه صنعتی شریف</p>
        </div>
      </footer>
    </div>
  );
}
