import api from "./api";
import type {
    UserCreateDto,
    UserDto,
    LoginDto,
    LoginResponseDto,
    ApiResponse,
    Page,
} from "../types";

// Register new user: send plain password (backend will hash with BCrypt)
export const register = async (user: UserCreateDto): Promise<UserDto> => {
    const response = await api.post<ApiResponse<UserDto>>("/users", user);
    return response.data.data;
};

// Login user: send plain password (backend will verify with BCrypt)
export const login = async (
    credentials: LoginDto
): Promise<LoginResponseDto> => {
    const response = await api.post<ApiResponse<LoginResponseDto>>("/users/login", credentials);
    const loginData = response.data.data;
    localStorage.setItem("user", JSON.stringify(loginData));
    return loginData;
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
        return data.user || data; // fallback for old format
    } catch {
        return null;
    }
};

// Get user by ID
export const getUserById = async (id: number): Promise<UserDto> => {
    const response = await api.get<ApiResponse<UserDto>>(`/users/${id}`);
    return response.data.data;
};

// Get all users
export const getAllUsers = async (): Promise<UserDto[]> => {
    const response = await api.get<ApiResponse<Page<UserDto>>>("/users");
    return response.data.data.content;
};

// Update user
export const updateUser = async (
    id: number,
    user: UserDto
): Promise<UserDto> => {
    const response = await api.put<ApiResponse<UserDto>>(`/users/${id}`, user);
    return response.data.data;
};

// Delete user
export const deleteUser = async (id: number): Promise<void> => {
    await api.delete(`/users/${id}`);
};
