import api from "./api";
import { sha256 } from "js-sha256";
import type {
    UserCreateDto,
    UserDto,
    LoginDto,
    LoginResponseDto,
} from "../types";

// Register new user: hash password using SHA-256 before sending
export const register = async (user: UserCreateDto): Promise<UserDto> => {
    const hashedUser = { ...user, password: sha256(user.password) };
    const response = await api.post("/users", hashedUser);
    return response.data;
};

// Login user: hash password using SHA-256 before sending
export const login = async (
    credentials: LoginDto
): Promise<LoginResponseDto> => {
    const hashedCredentials = {
        ...credentials,
        password: sha256(credentials.password),
    };
    const response = await api.post("/users/login", hashedCredentials);
    localStorage.setItem("user", JSON.stringify(response.data));
    return response.data;
};

// Logout user
export const logout = (): void => {
    // Logout is handled server-side
    localStorage.clear();
    sessionStorage.clear();
};

// Check if user is authenticated
export const isAuthenticated = (): boolean => {
    // Check if user data exists in localStorage
    return getCurrentUser() !== null;
};

// Get current user
export const getCurrentUser = (): UserDto | null => {
    const user = localStorage.getItem("user");
    if (!user) return null;

    try {
        const data = JSON.parse(user);
        return data.user || null;
    } catch {
        return null;
    }
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
