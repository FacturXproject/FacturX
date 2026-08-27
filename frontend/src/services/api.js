import axios from 'axios';

const backendUrl = import.meta.env.VITE_API_URL ?? '';

const api = axios.create({
  baseURL: `${backendUrl}/api`,
  withCredentials: true,
  withXSRFToken: true,
});

let unauthorizedHandler = null;

export function setUnauthorizedHandler(handler) {
  unauthorizedHandler = handler;
}

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401 && unauthorizedHandler) {
      unauthorizedHandler();
    }
    return Promise.reject(error);
  },
);

export default api;
