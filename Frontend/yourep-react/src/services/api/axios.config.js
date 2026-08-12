import axios from 'axios';
import axiosRetry from 'axios-retry';

const API_BASE_URL = 'https://yourep-api.austriaeast.cloudapp.azure.com/api' || 'http://localhost:8080/api';
console.log('API_BASE_URL:', API_BASE_URL); // Log the API base URL for debugging
const api = axios.create({
  baseURL: API_BASE_URL,
  headers: {
    'Content-Type': 'application/json',
  },
});

axiosRetry(api, {
  retries: 3,
  retryDelay: axiosRetry.exponentialDelay,
  retryCondition: (error) => {
    return !error.response || error.response.status >= 500;
  },
  onRetry: (retryCount, error) => {
    console.warn(`Retry ${retryCount}/3 for ${error.config?.url}`, error.message);
  },
});

api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('authToken');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    if (['post', 'put', 'patch'].includes(config.method?.toLowerCase())) {
      if(!config.headers['Idempotency-Key']) {
        config.headers['Idempotency-Key'] = crypto.randomUUID();
      }
    }
    return config;
  },
  (error) => Promise.reject(error)
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 || error.response?.status === 403) {
      localStorage.clear();
      window.location.href = '/login';
    }
    return Promise.reject(error);
  }
);

export default api;
