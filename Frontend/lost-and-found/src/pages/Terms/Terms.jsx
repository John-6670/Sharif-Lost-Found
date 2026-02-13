import { useNavigate } from "react-router-dom";
import { TERMS_CONTENT } from "./terms.content";
import "./Terms.css";

export default function Terms() {
  const navigate = useNavigate();

  const handleBack = () => {
    // Go back to previous page, or to signup if no history
    if (window.history.length > 1) {
      navigate(-1);
    } else {
      navigate("/signup");
    }
  };

  const scrollToSection = (id) => {
    const element = document.getElementById(id);
    if (element) {
      element.scrollIntoView({ behavior: "smooth", block: "start" });
    }
  };

  return (
    <div className="terms-container">
      <main className="terms-card">
        {/* Header */}
        <header className="terms-header">
          <div className="terms-icon">📜</div>
          <h1>{TERMS_CONTENT.title}</h1>
          <p className="terms-updated">{TERMS_CONTENT.lastUpdated}</p>
        </header>

        {/* Table of Contents */}
        <nav className="terms-toc" aria-label="فهرست مطالب">
          <h2 className="toc-title">{TERMS_CONTENT.tableOfContents.title}</h2>
          <ul className="toc-list">
            {TERMS_CONTENT.tableOfContents.items.map((item) => (
              <li key={item.id}>
                <button
                  type="button"
                  className="toc-link"
                  onClick={() => scrollToSection(item.id)}
                >
                  {item.label}
                </button>
              </li>
            ))}
          </ul>
        </nav>

        {/* Sections */}
        <div className="terms-content">
          {TERMS_CONTENT.sections.map((section, index) => (
            <section
              key={section.id}
              id={section.id}
              className="terms-section"
            >
              <h2 className="section-title">{section.title}</h2>
              <div className="section-content">
                {section.content.split("\n\n").map((paragraph, pIndex) => (
                  <p key={pIndex}>{paragraph}</p>
                ))}
              </div>
              {index < TERMS_CONTENT.sections.length - 1 && (
                <div className="section-divider" />
              )}
            </section>
          ))}
        </div>

        {/* Footer */}
        <footer className="terms-footer">
          <button
            type="button"
            className="back-btn"
            onClick={handleBack}
          >
            <span className="back-arrow">←</span>
            بازگشت
          </button>
          <p className="terms-acceptance">
            با استفاده از سامانه، شما این قوانین را پذیرفته‌اید.
          </p>
        </footer>
      </main>
    </div>
  );
}


