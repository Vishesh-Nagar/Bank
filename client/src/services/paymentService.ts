import api from "./api";
import type {
    PaymentRequestDto,
    PaymentResponseDto,
    PaymentStatusDto,
    ApiResponse,
    Page,
    ScheduledPaymentDto,
} from "../types";

// Initiate a payment
export const initiatePayment = async (
    paymentRequest: PaymentRequestDto
): Promise<PaymentResponseDto> => {
    const response = await api.post<ApiResponse<PaymentResponseDto>>("/payments", paymentRequest);
    return response.data.data;
};

// Polling status
export const getPaymentStatus = async (paymentId: string): Promise<PaymentStatusDto> => {
    const response = await api.get<ApiResponse<PaymentStatusDto>>(`/payments/${paymentId}`);
    return response.data.data;
};

// Payment history
export const getPaymentHistory = async (accountId: number, page: number = 0, size: number = 20): Promise<PaymentStatusDto[]> => {
    const response = await api.get<ApiResponse<Page<PaymentStatusDto>>>("/payments", {
        params: { accountId, page, size }
    });
    return response.data.data.content;
};

// Dispute / Cancel payment
export const disputePayment = async (paymentId: string): Promise<PaymentStatusDto> => {
    const response = await api.post<ApiResponse<PaymentStatusDto>>(`/payments/${paymentId}/dispute`);
    return response.data.data;
};

// Scheduled Payments
export const createScheduledPayment = async (payment: ScheduledPaymentDto): Promise<ScheduledPaymentDto> => {
    const response = await api.post<ApiResponse<ScheduledPaymentDto>>("/scheduled-payments", payment);
    return response.data.data;
};

export const getScheduledPayments = async (accountId: number): Promise<ScheduledPaymentDto[]> => {
    const response = await api.get<ApiResponse<ScheduledPaymentDto[]>>("/scheduled-payments", {
        params: { accountId }
    });
    return response.data.data;
};

export const cancelScheduledPayment = async (id: number): Promise<void> => {
    await api.delete(`/scheduled-payments/${id}`);
};
