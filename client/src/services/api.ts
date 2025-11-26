import axios from "axios";

const API_URL = import.meta.env.VITE_BACKEND_URL;

const api = axios.create({
    baseURL: API_URL,
    withCredentials: true,
    headers: {
        "Content-Type": "application/json",
    },
});

// Add response interceptor to handle authentication errors
api.interceptors.response.use(
    (response) => response,
    (error) => {
        if (error.response?.status === 401 || error.response?.status === 403) {
            console.log("Authentication error - redirecting to login");
            // Clear any stored user data
            localStorage.removeItem("user");
            sessionStorage.clear();
            // Redirect to login
            window.location.href = "/login";
            return Promise.reject({
                response: { status: 401, data: "Not authenticated" },
            });
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
                // If you have a token, add it to headers
                if (userData.token) {
                    config.headers.Authorization = `Bearer ${userData.token}`;
                } else {
                    // Add dummy auth header to prevent browser basic auth popup
                    config.headers.Authorization = "Bearer dummy";
                }
            } catch (e) {
                console.error("Error parsing user data:", e);
            }
        } else {
            // Add dummy auth header to prevent browser basic auth popup
            config.headers.Authorization = "Bearer dummy";
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default api;
