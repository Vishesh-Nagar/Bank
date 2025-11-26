export const AccountType = {
    SAVINGS: "SAVINGS",
    CURRENT: "CURRENT",
} as const;

export type AccountType = (typeof AccountType)[keyof typeof AccountType];

// DTO for creating a user (registration)
export interface UserCreateDto {
    username: string;
    password: string;
    email: string;
}

// DTO for returning user data (do NOT expose password in response)
export interface UserDto {
    id: number;
    username: string;
    email: string;
}

// DTO for updating user (use same as create if you allow password change)
export interface UserUpdateDto {
    username?: string;
    password?: string;
    email?: string;
}

// DTO for registering or creating accounts
export interface AccountCreateDto {
    accountHolderName: string;
    balance: number;
    accountType: AccountType;
}

// DTO for returning account data
export interface AccountDto {
    id: number;
    accountHolderName: string;
    balance: number;
    accountType: AccountType;
}

// DTO for login
export interface LoginDto {
    username: string;
    password: string;
}

// DTO for login response (never send password in response)
export interface LoginResponseDto {
    user: UserDto;
    token: string;
}
