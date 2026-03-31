import axios from 'axios';
import { store } from '../store';
import { clearCredentials } from '../store/slices/authSlice';
import { requestStarted, requestFinished } from '../store/slices/uiSlice';
import { showSessionExpiredToast } from '../utils/errorHandler';

const api = axios.create({
  baseURL: '/api/v1',
});

api.interceptors.request.use((config) => {
  const token = localStorage.getItem('pharma_cx_token');
  if (token) {
    config.headers.Authorization = `Bearer ${token}`;
  }
  store.dispatch(requestStarted());
  return config;
});

api.interceptors.response.use(
  (response) => {
    store.dispatch(requestFinished());
    return response;
  },
  (error) => {
    store.dispatch(requestFinished());
    if (error.response?.status === 401) {
      store.dispatch(clearCredentials());
      showSessionExpiredToast();
      setTimeout(() => { window.location.href = '/login'; }, 1500);
    }
    return Promise.reject(error);
  }
);

export default api;
