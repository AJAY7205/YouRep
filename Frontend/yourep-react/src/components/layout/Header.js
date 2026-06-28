import React, { useState } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../../context/AuthContext';

const Header = () => {
  const { isAuthenticated, user, logout } = useAuth();
  const navigate = useNavigate();
  const [menuOpen, setMenuOpen] = useState(false);

  const handleLogout = () => {
    logout();
    navigate('/login');
  };

  return (
    <header className="header">
      <div className="header-container">
        <Link to="/" className="header-logo">
          <span className="logo-icon">▶</span>
          <span className="logo-text">YouRep</span>
        </Link>

        <nav className={`header-nav ${menuOpen ? 'open' : ''}`}>
          <Link to="/" className="nav-link" onClick={() => setMenuOpen(false)}>
            Home
          </Link>
          {isAuthenticated && (
            <Link to="/upload" className="nav-link" onClick={() => setMenuOpen(false)}>
              Upload
            </Link>
          )}
        </nav>

        <div className="header-auth">
          {isAuthenticated ? (
            <div className="user-menu">
              <div className="user-avatar">
                {user?.username?.charAt(0).toUpperCase()}
              </div>
              <span className="user-greeting">
                {user?.username}
              </span>
              <button onClick={handleLogout} className="btn btn-logout">
                Logout
              </button>
            </div>
          ) : (
            <div className="auth-buttons">
              <Link to="/login" className="btn btn-login">Sign In</Link>
              <Link to="/signup" className="btn btn-signup">Sign Up</Link>
            </div>
          )}
        </div>

        <button
          className="hamburger"
          onClick={() => setMenuOpen(!menuOpen)}
          aria-label="Toggle menu"
        >
          <span className={`hamburger-line ${menuOpen ? 'open' : ''}`} />
          <span className={`hamburger-line ${menuOpen ? 'open' : ''}`} />
          <span className={`hamburger-line ${menuOpen ? 'open' : ''}`} />
        </button>
      </div>
    </header>
  );
};

export default Header;
