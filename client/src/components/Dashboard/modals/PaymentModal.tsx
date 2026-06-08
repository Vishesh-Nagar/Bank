import React, { useState } from "react";
import type { AccountDto } from "../../../types";
import { initiatePayment } from "../../../services/paymentService";
import "./PaymentModal.css";

type Props = {
    visible: boolean;
    sourceAccount: AccountDto | null;
    onClose: () => void;
    onQueued: (message: string) => void;
};

const PaymentModal: React.FC<Props> = ({
    visible,
    sourceAccount,
    onClose,
    onQueued,
}) => {
    const [targetAccountId, setTargetAccountId] = useState("");
    const [amount, setAmount] = useState("");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    if (!visible || !sourceAccount) return null;

    const reset = () => {
        setTargetAccountId("");
        setAmount("");
        setError("");
        setSubmitting(false);
    };

    const handleClose = () => {
        reset();
        onClose();
    };

    const handleSubmit = async () => {
        const parsedAmount = parseFloat(amount);
        const parsedTarget = parseInt(targetAccountId, 10);

        if (!targetAccountId || isNaN(parsedTarget) || parsedTarget <= 0) {
            setError("Please enter a valid target account ID.");
            return;
        }
        if (parsedTarget === sourceAccount.id) {
            setError("Cannot send a payment to the same account. Use transfer instead.");
            return;
        }
        if (isNaN(parsedAmount) || parsedAmount <= 0) {
            setError("Please enter a valid positive amount.");
            return;
        }
        if (parsedAmount > sourceAccount.balance) {
            setError("Amount exceeds your current balance.");
            return;
        }

        setSubmitting(true);
        setError("");

        try {
            await initiatePayment({
                sourceAccountId: sourceAccount.id,
                targetAccountId: parsedTarget,
                amount: parsedAmount,
            });
            reset();
            onClose();
            onQueued(
                `Payment of $${parsedAmount.toFixed(2)} to Account #${parsedTarget} has been queued. You'll be notified when it completes.`
            );
        } catch (err: any) {
            setError(
                err.response?.data?.message ||
                    "Failed to initiate payment. Please try again."
            );
            setSubmitting(false);
        }
    };

    const parsedAmt = parseFloat(amount);
    const previewBalance =
        !isNaN(parsedAmt) && parsedAmt > 0
            ? sourceAccount.balance - parsedAmt
            : null;

    return (
        <div className="modal-overlay" onClick={handleClose}>
            <div className="modal payment-modal" onClick={(e) => e.stopPropagation()}>
                <div className="payment-modal__header">
                    <h2>💳 Send Payment</h2>
                    <button className="payment-modal__close" onClick={handleClose} aria-label="Close">×</button>
                </div>

                <div className="transaction-info">
                    <p>
                        <strong>From:</strong> {sourceAccount.accountHolderName}{" "}
                        <span className="payment-modal__account-type">
                            ({sourceAccount.accountType})
                        </span>
                    </p>
                    <p>
                        <strong>Account ID:</strong> #{sourceAccount.id}
                    </p>
                    <p>
                        <strong>Available Balance:</strong>{" "}
                        <span className="payment-modal__balance">
                            ${sourceAccount.balance.toFixed(2)}
                        </span>
                    </p>
                </div>

                <div className="form-group">
                    <label htmlFor="targetAccountId">Target Account ID</label>
                    <input
                        id="targetAccountId"
                        type="number"
                        min="1"
                        value={targetAccountId}
                        onChange={(e) => {
                            setTargetAccountId(e.target.value);
                            setError("");
                        }}
                        placeholder="e.g. 7"
                        autoFocus
                        disabled={submitting}
                    />
                </div>

                <div className="form-group">
                    <label htmlFor="paymentAmount">Amount ($)</label>
                    <input
                        id="paymentAmount"
                        type="text"
                        inputMode="decimal"
                        value={amount}
                        onChange={(e) => {
                            const val = e.target.value;
                            if (val === "" || /^\d*\.?\d{0,4}$/.test(val)) {
                                setAmount(val);
                                setError("");
                            }
                        }}
                        placeholder="0.00"
                        disabled={submitting}
                    />
                </div>

                {previewBalance !== null && (
                    <div
                        className={`transaction-preview ${previewBalance < 0 ? "transaction-preview--danger" : ""}`}
                    >
                        <p>
                            Balance after payment:{" "}
                            <strong>${previewBalance.toFixed(2)}</strong>
                        </p>
                    </div>
                )}

                {error && (
                    <div className="error-message" style={{ marginTop: "16px" }}>
                        <span>⚠️ {error}</span>
                    </div>
                )}

                <div className="payment-modal__notice">
                    <span>⚡</span>
                    <span>Payments are processed asynchronously. You'll receive a notification once it's completed.</span>
                </div>

                <div className="modal-actions">
                    <button
                        className="btn btn-primary"
                        onClick={handleSubmit}
                        disabled={
                            submitting ||
                            !targetAccountId ||
                            !amount ||
                            isNaN(parseFloat(amount)) ||
                            parseFloat(amount) <= 0
                        }
                    >
                        {submitting ? "⏳ Sending..." : "💳 Send Payment"}
                    </button>
                    <button
                        className="btn btn-secondary"
                        onClick={handleClose}
                        disabled={submitting}
                    >
                        Cancel
                    </button>
                </div>
            </div>
        </div>
    );
};

export default PaymentModal;
