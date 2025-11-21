import api from "./api";
import type {
    UserCreateDto,
    UserDto,
    LoginDto,
    LoginResponseDto,
} from "../types";

// Register new user
export const register = async (user: UserCreateDto): Promise<UserDto> => {
    const response = await api.post("/users", user);
    return response.data;
};

// Login user - stores credentials for session
export const login = async (
    credentials: LoginDto
): Promise<LoginResponseDto> => {
    try {
        const response = await api.post("/users/login", credentials);

        // Store user data in localStorage
        if (response.data && response.data.user) {
            localStorage.setItem("user", JSON.stringify(response.data));
        }

        return response.data;
    } catch (error: any) {
        console.error("Login error:", error);
        throw error;
    }
};

// Logout user
export const logout = (): void => {
    localStorage.removeItem("user");
    sessionStorage.clear();
};

// Check if user is authenticated
export const isAuthenticated = (): boolean => {
    const user = localStorage.getItem("user");
    return !!user;
};

// Get current user from localStorage
export const getCurrentUser = (): UserDto | null => {
    const user = localStorage.getItem("user");
    if (user) {
        try {
            const data = JSON.parse(user);
            return data.user || null;
        } catch (e) {
            return null;
        }
    }
    return null;
};

// Get user by ID
export const getUserById = async (id: number): Promise<UserDto> => {
    const response = await api.get(`/users/${id}`);
    return response.data;
};

// Get all users
export const getAllUsers = async (): Promise<UserDto[]> => {
    const response = await api.get("/users");
    return response.data;
};

// Update user
export const updateUser = async (
    id: number,
    user: UserDto
): Promise<UserDto> => {
    const response = await api.put(`/users/${id}`, user);
    return response.data;
};

// Delete user
export const deleteUser = async (id: number): Promise<string> => {
    const response = await api.delete(`/users/${id}`);
    return response.data;
};
