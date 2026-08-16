import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';
import { logoutApi } from '../services/api/auth.service';

const AuthContext = createContext(null);

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};

export const AuthProvider = ({ children }) => {
  const [user, setUser] = useState(null);
  const [token, setToken] = useState(null);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const storedToken = localStorage.getItem('authToken');
    const storedUsername = localStorage.getItem('username');
    const storedRoles = localStorage.getItem('userRoles');
    const storedEmail = localStorage.getItem('userEmail');
    const storedId = localStorage.getItem('userId');
    const storedVerified = localStorage.getItem('emailVerified') === 'true';

    if (storedToken && storedUsername) {
      setToken(storedToken);
      setUser({
        id: storedId,
        username: storedUsername,
        email: storedEmail,
        roles: storedRoles ? JSON.parse(storedRoles) : [],
        emailVerified: storedVerified,
      });
    }
    setLoading(false);
  }, []);

  const login = useCallback((token, username, email, roles, id, emailVerified) => {
    localStorage.setItem('authToken', token);
    localStorage.setItem('userId', id);
    localStorage.setItem('username', username);
    localStorage.setItem('userEmail', email);
    localStorage.setItem('userRoles', JSON.stringify(roles));
    localStorage.setItem('emailVerified', String(!!emailVerified));
    setToken(token);
    setUser({ id, username, email, roles, emailVerified: !!emailVerified });
  }, []);

  const logout = useCallback(async () => {
    await logoutApi();
    localStorage.removeItem('authToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userRoles');
    localStorage.removeItem('emailVerified');
    setToken(null);
    setUser(null);
  }, []);

  const refreshVerified = useCallback(() => {
    setUser((prev) => (prev ? { ...prev, emailVerified: true } : prev));
    localStorage.setItem('emailVerified', 'true');
  }, []);

  const isAuthenticated = !!token && !!user;

  const value = {
    user,
    token,
    loading,
    isAuthenticated,
    login,
    logout,
    refreshVerified,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;
