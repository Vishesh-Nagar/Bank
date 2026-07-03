import axios from "axios";

const API_URL = import.meta.env.VITE_BACKEND_URL || "/api/v1";

const api = axios.create({
    baseURL: API_URL,
    withCredentials: true,
    headers: {
        "Content-Type": "application/json",
    },
});

let isRefreshing = false;
let failedQueue: Array<{ resolve: (value?: unknown) => void, reject: (reason?: any) => void }> = [];

const processQueue = (error: any, token: string | null = null) => {
    failedQueue.forEach(prom => {
        if (error) {
            prom.reject(error);
        } else {
            prom.resolve(token);
        }
    });
    failedQueue = [];
};

import { globalToast } from "../contexts/ToastContext";

// Add response interceptor to handle authentication errors
api.interceptors.response.use(
    (response) => response,
    async (error) => {
        // Extract unified error message if available
        if (error.response?.data?.error?.message) {
            error.message = error.response.data.error.message;
            error.response.data.message = error.response.data.error.message; // Ensure frontend catch blocks read it
        } else if (error.response?.data?.message) {
            error.message = error.response.data.message;
        }

        const originalRequest = error.config;
        
        if (error.response?.status === 401 && !originalRequest._retry) {
            const currentPath = window.location.pathname;
            if (currentPath === "/login" || currentPath === "/register") {
                return Promise.reject(error);
            }

            if (isRefreshing) {
                return new Promise(function(resolve, reject) {
                    failedQueue.push({ resolve, reject });
                }).then(token => {
                    originalRequest.headers['Authorization'] = 'Bearer ' + token;
                    return api(originalRequest);
                }).catch(err => {
                    return Promise.reject(err);
                });
            }

            originalRequest._retry = true;
            isRefreshing = true;

            const userStr = localStorage.getItem("user");
            if (!userStr) {
                window.location.href = "/login";
                return Promise.reject(error);
            }

            try {
                const userData = JSON.parse(userStr);
                const response = await axios.post(`${API_URL}/users/refresh`, {
                    refreshToken: userData.refreshToken
                });

                const newTokens = response.data.data;
                localStorage.setItem("user", JSON.stringify(newTokens));
                
                originalRequest.headers['Authorization'] = `Bearer ${newTokens.accessToken}`;
                processQueue(null, newTokens.accessToken);
                return api(originalRequest);
            } catch (refreshError) {
                processQueue(refreshError, null);
                console.log("Session expired - redirecting to login");
                localStorage.removeItem("user");
                sessionStorage.clear();
                window.location.href = "/login";
                return Promise.reject(refreshError);
            } finally {
                isRefreshing = false;
            }
        }
        
        if (error.response?.status === 403) {
            // Might be an unverified email or locked account. Let the component handle it or redirect.
            return Promise.reject(error);
        }

        // Display global toast for server errors (500+)
        if (error.response?.status >= 500) {
            globalToast.show(error.message, "error");
        } else if (error.message === "Network Error") {
            globalToast.show("Unable to connect to the server. Please check your internet connection.", "error");
        }

        return Promise.reject(error);
    }
);

// Add request interceptor to attach auth token if available
api.interceptors.request.use(
    (config) => {
        const user = localStorage.getItem("user");
        if (user) {
            try {
                const userData = JSON.parse(user);
                // If you have an access token, add it to headers
                if (userData.accessToken) {
                    config.headers.set("Authorization", `Bearer ${userData.accessToken}`);
                } else if (userData.token) {
                     // Fallback for old sessions
                    config.headers.set("Authorization", `Bearer ${userData.token}`);
                } else {
                    config.headers.set("Authorization", "Bearer dummy");
                }
            } catch (e) {
                console.error("Error parsing user data:", e);
            }
        } else {
            config.headers.set("Authorization", "Bearer dummy");
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default api;
