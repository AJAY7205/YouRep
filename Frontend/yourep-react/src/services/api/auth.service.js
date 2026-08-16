import api from './axios.config';

export const signin = async (username, password) => {
  const response = await api.post('/auth/signin', { username, password });
  return response.data;
};

export const signup = async (username, email, password) => {
  const response = await api.post('/auth/signup', {
    username,
    email,
    password,
    roles: ['user'],
  });
  return response.data;
};

export const logoutApi = async () => {
  try {
    await api.post('/auth/logout');
  } catch {
    // ignore — token might already be invalid
  }
};

export const sendVerificationCode = async (email) => {
  const response = await api.post('/auth/send-verification-code', { email });
  return response.data;
};

export const verifyEmail = async (email, code) => {
  const response = await api.post('/auth/verify-email', { email, code });
  return response.data;
};

export const logout = () => {
  localStorage.removeItem('authToken');
  localStorage.removeItem('userId');
  localStorage.removeItem('username');
  localStorage.removeItem('userEmail');
  localStorage.removeItem('userRoles');
  localStorage.removeItem('emailVerified');
};
