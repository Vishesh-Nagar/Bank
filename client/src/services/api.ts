import axios from "axios";

// const API_URL = import.meta.env.VITE_BACKEND_URL || "/api";
const API_URL = "http://localhost:8080/api";

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
        // If response is HTML (Spring Security login page), treat as 401
        if (error.response?.headers["content-type"]?.includes("text/html")) {
            console.log(
                "Received HTML instead of JSON - user not authenticated"
            );
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
                }
            } catch (e) {
                console.error("Error parsing user data:", e);
            }
        }
        return config;
    },
    (error) => {
        return Promise.reject(error);
    }
);

export default api;
