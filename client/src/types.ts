export const AccountType = {
    SAVINGS: "SAVINGS",
    CURRENT: "CURRENT",
} as const;

export type AccountType = (typeof AccountType)[keyof typeof AccountType];

export interface UserCreateDto {
    username: string;
    password?: string;
    email: string;
}

export interface UserDto {
    id: number;
    username: string;
    email: string;
}

export interface AccountCreateDto {
    accountHolderName: string;
    balance: number;
    accountType: AccountType;
}

export interface AccountDto {
    id: number;
    accountHolderName: string;
    balance: number;
    accountType: AccountType;
}

export interface LoginDto {
    username: string;
    password?: string;
}

export interface LoginResponseDto {
    user: UserDto;
}
