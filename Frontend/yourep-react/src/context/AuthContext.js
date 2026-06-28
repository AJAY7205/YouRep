import React, { createContext, useContext, useState, useEffect, useCallback } from 'react';

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

    if (storedToken && storedUsername) {
      setToken(storedToken);
      setUser({
        id: storedId,
        username: storedUsername,
        email: storedEmail,
        roles: storedRoles ? JSON.parse(storedRoles) : [],
      });
    }
    setLoading(false);
  }, []);

  const login = useCallback((token, username, email, roles, id) => {
    localStorage.setItem('authToken', token);
    localStorage.setItem('userId', id);
    localStorage.setItem('username', username);
    localStorage.setItem('userEmail', email);
    localStorage.setItem('userRoles', JSON.stringify(roles));
    setToken(token);
    setUser({ id, username, email, roles });
  }, []);

  const logout = useCallback(() => {
    localStorage.removeItem('authToken');
    localStorage.removeItem('userId');
    localStorage.removeItem('username');
    localStorage.removeItem('userEmail');
    localStorage.removeItem('userRoles');
    setToken(null);
    setUser(null);
  }, []);

  const isAuthenticated = !!token && !!user;

  const value = {
    user,
    token,
    loading,
    isAuthenticated,
    login,
    logout,
  };

  return (
    <AuthContext.Provider value={value}>
      {children}
    </AuthContext.Provider>
  );
};

export default AuthContext;
