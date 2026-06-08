import React from "react";
import { render, screen, fireEvent, waitFor } from "@testing-library/react";
import { describe, it, expect, vi, beforeEach } from "vitest";
import PaymentModal from "./PaymentModal";
import * as paymentService from "../../../services/paymentService";
import { AccountDto } from "../../../types";

vi.mock("../../../services/paymentService");

describe("PaymentModal", () => {
    const mockAccount: AccountDto = {
        id: 1,
        accountHolderName: "Test User",
        accountType: "SAVINGS",
        balance: 1000,
        transactions: [],
    };

    const mockOnClose = vi.fn();
    const mockOnQueued = vi.fn();

    beforeEach(() => {
        vi.clearAllMocks();
    });

    it("should not render if not visible", () => {
        const { container } = render(
            <PaymentModal
                visible={false}
                sourceAccount={mockAccount}
                onClose={mockOnClose}
                onQueued={mockOnQueued}
            />
        );
        expect(container.firstChild).toBeNull();
    });

    it("should render correctly when visible", () => {
        render(
            <PaymentModal
                visible={true}
                sourceAccount={mockAccount}
                onClose={mockOnClose}
                onQueued={mockOnQueued}
            />
        );
        expect(screen.getByRole("heading", { name: "💳 Send Payment" })).toBeInTheDocument();
        expect(screen.getByText("Test User")).toBeInTheDocument();
        expect(screen.getByText("$1000.00")).toBeInTheDocument();
    });

    it("should show error when sending to same account", async () => {
        render(
            <PaymentModal
                visible={true}
                sourceAccount={mockAccount}
                onClose={mockOnClose}
                onQueued={mockOnQueued}
            />
        );

        fireEvent.change(screen.getByLabelText(/Target Account ID/i), {
            target: { value: "1" },
        });
        fireEvent.change(screen.getByLabelText(/Amount/i), {
            target: { value: "100" },
        });

        fireEvent.click(screen.getByRole("button", { name: "💳 Send Payment" }));

        expect(
            await screen.findByText("⚠️ Cannot send a payment to the same account. Use transfer instead.")
        ).toBeInTheDocument();
        expect(paymentService.initiatePayment).not.toHaveBeenCalled();
    });

    it("should show error when amount exceeds balance", async () => {
        render(
            <PaymentModal
                visible={true}
                sourceAccount={mockAccount}
                onClose={mockOnClose}
                onQueued={mockOnQueued}
            />
        );

        fireEvent.change(screen.getByLabelText(/Target Account ID/i), {
            target: { value: "2" },
        });
        fireEvent.change(screen.getByLabelText(/Amount/i), {
            target: { value: "1500" },
        });

        fireEvent.click(screen.getByRole("button", { name: "💳 Send Payment" }));

        expect(
            await screen.findByText("⚠️ Amount exceeds your current balance.")
        ).toBeInTheDocument();
        expect(paymentService.initiatePayment).not.toHaveBeenCalled();
    });

    it("should initiate payment and call onClose/onQueued on success", async () => {
        (paymentService.initiatePayment as any).mockResolvedValue({});

        render(
            <PaymentModal
                visible={true}
                sourceAccount={mockAccount}
                onClose={mockOnClose}
                onQueued={mockOnQueued}
            />
        );

        fireEvent.change(screen.getByLabelText(/Target Account ID/i), {
            target: { value: "2" },
        });
        fireEvent.change(screen.getByLabelText(/Amount/i), {
            target: { value: "100" },
        });

        fireEvent.click(screen.getByRole("button", { name: "💳 Send Payment" }));

        await waitFor(() => {
            expect(paymentService.initiatePayment).toHaveBeenCalledWith({
                sourceAccountId: 1,
                targetAccountId: 2,
                amount: 100,
            });
        });

        expect(mockOnClose).toHaveBeenCalled();
        expect(mockOnQueued).toHaveBeenCalled();
    });
});
