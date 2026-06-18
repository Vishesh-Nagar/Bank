import api from "./api";
import type { AccountCreateDto, AccountDto, ApiResponse, Page } from "../types";

// Create account
export const createAccount = async (
    account: AccountCreateDto
): Promise<AccountDto> => {
    const response = await api.post<ApiResponse<AccountDto>>("/accounts", account);
    return response.data.data;
};

// Get all accounts for authenticated user
export const getAllAccounts = async (): Promise<AccountDto[]> => {
    const response = await api.get<ApiResponse<Page<AccountDto>>>("/accounts");
    return response.data.data.content;
};

// Get account by ID
export const getAccountById = async (id: number): Promise<AccountDto> => {
    const response = await api.get<ApiResponse<AccountDto>>(`/accounts/${id}`);
    return response.data.data;
};

// Deposit money
export const deposit = async (
    id: number,
    amount: number
): Promise<AccountDto> => {
    const response = await api.post<ApiResponse<AccountDto>>(`/accounts/${id}/deposit`, { amount });
    return response.data.data;
};

// Withdraw money
export const withdraw = async (
    id: number,
    amount: number
): Promise<AccountDto> => {
    const response = await api.post<ApiResponse<AccountDto>>(`/accounts/${id}/withdraw`, { amount });
    return response.data.data;
};

// Delete account
export const deleteAccount = async (id: number): Promise<void> => {
    await api.delete(`/accounts/${id}`);
};
