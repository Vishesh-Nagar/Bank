import { describe, it, expect, vi, beforeEach } from "vitest";
import { initiatePayment, getPaymentStatus, getPaymentHistory } from "./paymentService";
import api from "./api";
import { PaymentRequestDto } from "../types";

vi.mock("./api");

describe("paymentService", () => {
    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should initiate a payment and return the response", async () => {
        const mockRequest: PaymentRequestDto = {
            sourceAccountId: 1,
            targetAccountId: 2,
            amount: 100,
        };
        const mockResponse = { data: { paymentId: "uuid", status: "QUEUED" } };
        (api.post as any).mockResolvedValue(mockResponse);

        const result = await initiatePayment(mockRequest);

        expect(api.post).toHaveBeenCalledWith("/payments", mockRequest);
        expect(result).toEqual(mockResponse.data);
    });

    it("should get payment status", async () => {
        const mockResponse = { data: { paymentId: "uuid", status: "COMPLETED" } };
        (api.get as any).mockResolvedValue(mockResponse);

        const result = await getPaymentStatus("uuid");

        expect(api.get).toHaveBeenCalledWith("/payments/uuid");
        expect(result).toEqual(mockResponse.data);
    });

    it("should get payment history", async () => {
        const mockResponse = { data: [{ paymentId: "uuid", status: "COMPLETED" }] };
        (api.get as any).mockResolvedValue(mockResponse);

        const result = await getPaymentHistory(1);

        expect(api.get).toHaveBeenCalledWith("/accounts/1/payments");
        expect(result).toEqual(mockResponse.data);
    });
});
