import api from "./api";
import type {
    PaymentRequestDto,
    PaymentResponseDto,
    PaymentStatusDto,
} from "../types";

// Initiate a cross-user payment — returns 202 Accepted with QUEUED status
export const initiatePayment = async (
    request: PaymentRequestDto
): Promise<PaymentResponseDto> => {
    const response = await api.post("/payments", request);
    return response.data;
};

// Poll the status of a specific payment by ID
export const getPaymentStatus = async (
    paymentId: string
): Promise<PaymentStatusDto> => {
    const response = await api.get(`/payments/${paymentId}`);
    return response.data;
};

// Get all payments where the given account was sender or receiver
export const getPaymentHistory = async (
    accountId: number
): Promise<PaymentStatusDto[]> => {
    const response = await api.get(`/accounts/${accountId}/payments`);
    return response.data;
};
