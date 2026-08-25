import axios from 'axios';

const backendUrl = import.meta.env.VITE_API_URL ?? 'http://localhost:8080';

const api = axios.create({
  baseURL: `${backendUrl}/api`,
  withCredentials: true, // cookies are the auth - never store a token
  // Frontend (5173) and backend (8080) are different origins in dev, so axios's
  // same-origin-only default for echoing the XSRF-TOKEN cookie must be overridden.
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
  }
);

export default api;
