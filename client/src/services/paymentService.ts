import api from "./api";
import type {
    PaymentRequestDto,
    PaymentResponseDto,
    PaymentStatusDto,
    ApiResponse,
    Page,
} from "../types";

// Initiate a cross-user payment — returns 202 Accepted with PENDING status
export const initiatePayment = async (
    request: PaymentRequestDto
): Promise<PaymentResponseDto> => {
    const response = await api.post<ApiResponse<PaymentResponseDto>>("/payments", request);
    return response.data.data;
};

// Poll the status of a specific payment by ID
export const getPaymentStatus = async (
    paymentId: string
): Promise<PaymentStatusDto> => {
    const response = await api.get<ApiResponse<PaymentStatusDto>>(`/payments/${paymentId}`);
    return response.data.data;
};

// Get all payments where the given account was sender or receiver
export const getPaymentHistory = async (
    accountId: number
): Promise<PaymentStatusDto[]> => {
    const response = await api.get<ApiResponse<Page<PaymentStatusDto>>>(`/payments?accountId=${accountId}`);
    return response.data.data.content;
};
