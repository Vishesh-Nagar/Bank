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
    accessToken: string;
    refreshToken: string;
}

// Payment Status Enum
export const PaymentStatus = {
    PENDING: "PENDING",
    PROCESSING: "PROCESSING",
    COMPLETED: "COMPLETED",
    FAILED: "FAILED",
    QUEUED: "QUEUED",
} as const;

export type PaymentStatus = (typeof PaymentStatus)[keyof typeof PaymentStatus];

// DTO for initiating a payment (cross-user)
export interface PaymentRequestDto {
    sourceAccountId: number;
    targetAccountId: number;
    amount: number;
}

// DTO returned immediately when a payment is queued
export interface PaymentResponseDto {
    paymentId: string;
    status: string; // "QUEUED"
    sourceAccountId: number;
    targetAccountId: number;
    amount: number;
    submittedAt: string;
}

// DTO for polling payment status or listing history
export interface PaymentStatusDto {
    paymentId: string;
    status: PaymentStatus;
    failureReason: string | null;
    sourceAccountId: number;
    targetAccountId: number;
    amount: number;
    submittedAt: string;
    completedAt: string | null;
}

// Notification pushed via WebSocket
export interface NotificationDto {
    type: "PAYMENT_COMPLETED" | "PAYMENT_RECEIVED" | "PAYMENT_FAILED" | "BALANCE_CHANGED" | "PAYMENT_QUEUED";
    message: string;
    payment: PaymentStatusDto;
}

// Generic API Response Wrapper
export interface ApiResponse<T> {
    success: boolean;
    data: T;
    meta: {
        requestId: string;
        timestamp: string;
        pagination?: {
            pageNumber: number;
            pageSize: number;
            totalElements: number;
            totalPages: number;
            hasNext: boolean;
            hasPrevious: boolean;
        };
    };
    error?: {
        code: string;
        message: string;
        details?: Record<string, string>;
    };
}

// Generic Spring Page Wrapper
export interface Page<T> {
    content: T[];
    pageable: any;
    totalElements: number;
    totalPages: number;
    last: boolean;
    size: number;
    number: number;
    sort: any;
    numberOfElements: number;
    first: boolean;
    empty: boolean;
}

export type RecurrenceType = "DAILY" | "WEEKLY" | "MONTHLY";

export interface ScheduledPaymentDto {
    id?: number;
    sourceAccountId: number;
    targetAccountId: number;
    amount: number;
    recurrenceType: RecurrenceType;
    status?: "ACTIVE" | "CANCELLED" | "COMPLETED";
    nextExecutionTime: string;
    createdAt?: string;
}
