import api from "./api";
import type { AccountCreateDto, AccountDto } from "../types";

// Create account
export const createAccount = async (
    account: AccountCreateDto
): Promise<AccountDto> => {
    const response = await api.post("/accounts", account);
    return response.data;
};

// Get all accounts for authenticated user
export const getAllAccounts = async (): Promise<AccountDto[]> => {
    const response = await api.get("/accounts");
    return response.data;
};

// Get account by ID
export const getAccountById = async (id: number): Promise<AccountDto> => {
    const response = await api.get(`/accounts/${id}`);
    return response.data;
};

// Deposit money
export const deposit = async (
    id: number,
    amount: number
): Promise<AccountDto> => {
    const response = await api.put(`/accounts/${id}/deposit`, { amount });
    return response.data;
};

// Withdraw money
export const withdraw = async (
    id: number,
    amount: number
): Promise<AccountDto> => {
    const response = await api.put(`/accounts/${id}/withdraw`, { amount });
    return response.data;
};

// Delete account
export const deleteAccount = async (id: number): Promise<string> => {
    const response = await api.delete(`/accounts/${id}`);
    return response.data;
};
