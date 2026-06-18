import React, { useState } from "react";
import type { AccountDto } from "../../../types";
import { initiatePayment } from "../../../services/paymentService";

type Props = {
    visible: boolean;
    sourceAccount: AccountDto | null;
    onClose: () => void;
    onQueued: (message: string) => void;
};

/* Shared input style */
const inputClass =
    "w-full px-4 py-[14px] bg-[rgba(15,23,42,0.6)] border border-white/10 rounded-[10px] text-white text-[15px] transition-all duration-300 " +
    "focus:outline-none focus:border-blue-500 focus:shadow-[0_0_0_3px_rgba(59,130,246,0.1)] focus:bg-[rgba(15,23,42,0.8)] " +
    "placeholder:text-slate-500 disabled:opacity-60";

const PaymentModal: React.FC<Props> = ({
    visible,
    sourceAccount,
    onClose,
    onQueued,
}) => {
    const [accountIdStr, setAccountIdStr] = useState("");
    const [amount, setAmount] = useState("");
    const [error, setError] = useState("");
    const [submitting, setSubmitting] = useState(false);

    if (!visible || !sourceAccount) return null;

    const reset = () => {
        setAccountIdStr("");
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
        const parsedTarget = parseInt(accountIdStr, 10);

        if (!accountIdStr || isNaN(parsedTarget) || parsedTarget <= 0) {
            setError("Please enter a valid target account ID.");
            return;
        }
        if (parsedTarget === sourceAccount.id) {
            setError("You cannot send a payment to yourself.");
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
            // Show optimistic queued notification immediately so it always appears first
            onQueued(
                `Payment of ₹${parsedAmount.toFixed(2)} to Account #${parsedTarget} is initiating...`
            );

            await initiatePayment({
                sourceAccountId: sourceAccount.id,
                targetAccountId: parsedTarget,
                amount: parsedAmount,
            });
            reset();
            onClose();
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
        /* Overlay */
        <div
            className="fixed inset-0 bg-black/85 backdrop-blur-[8px] flex justify-center items-center z-[1000] animate-modal-fade"
            onClick={handleClose}
        >
            {/* Modal */}
            <div
                className="bg-gradient-to-br from-[#1e293b] to-[#0f172a] p-8 rounded-[20px] w-full max-w-[500px] shadow-[0_20px_60px_rgba(0,0,0,0.6)] border border-white/10 animate-modal-slide"
                onClick={(e) => e.stopPropagation()}
            >
                {/* Header */}
                <div className="flex justify-between items-center mb-6">
                    <h2 className="m-0 text-2xl font-bold">💳 Send Payment</h2>
                    <button
                        className="bg-transparent border-none text-slate-500 text-[28px] leading-none cursor-pointer px-1 transition-colors duration-200 hover:text-slate-200"
                        onClick={handleClose}
                        aria-label="Close"
                    >
                        ×
                    </button>
                </div>

                {/* Transaction info */}
                <div className="bg-[rgba(15,23,42,0.5)] p-5 rounded-xl mb-6 border border-white/[0.05]">
                    <p className="my-2 text-sm text-slate-300">
                        <strong className="text-white font-semibold">From:</strong>{" "}
                        {sourceAccount.accountHolderName}{" "}
                        <span className="text-slate-400 text-[13px] font-normal">
                            ({sourceAccount.accountType})
                        </span>
                    </p>
                    <p className="my-2 text-sm text-slate-300">
                        <strong className="text-white font-semibold">Account ID:</strong>{" "}
                        #{sourceAccount.id}
                    </p>
                    <p className="my-2 text-sm text-slate-300">
                        <strong className="text-white font-semibold">Available Balance:</strong>{" "}
                        <span className="text-emerald-500 font-bold">
                            ${sourceAccount.balance.toFixed(2)}
                        </span>
                    </p>
                </div>

                {/* Target Account ID */}
                <div className="mb-6">
                    <label htmlFor="targetAccountId" className="block mb-2 text-sm font-semibold text-slate-300">
                        Target Account ID
                    </label>
                    <input
                        id="targetAccountId"
                        type="text"
                        inputMode="numeric"
                        value={accountIdStr}
                        onChange={(e) => {
                            const val = e.target.value;
                            if (val === "" || /^\d+$/.test(val)) {
                                setAccountIdStr(val);
                                setError("");
                            }
                        }}
                        placeholder="e.g. 7"
                        autoFocus
                        disabled={submitting}
                        className={inputClass}
                    />
                </div>

                {/* Amount */}
                <div className="mb-6">
                    <label htmlFor="paymentAmount" className="block mb-2 text-sm font-semibold text-slate-300">
                        Amount ($)
                    </label>
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
                        className={inputClass}
                    />
                </div>

                {/* Balance preview */}
                {previewBalance !== null && (
                    <div
                        className={`p-4 rounded-[10px] mt-4 border ${
                            previewBalance < 0
                                ? "bg-red-500/10 border-red-500/25 [&>p]:text-red-400"
                                : "bg-emerald-500/10 border-emerald-500/20"
                        }`}
                    >
                        <p className={`m-0 font-semibold text-base ${previewBalance < 0 ? "text-red-400" : "text-emerald-500"}`}>
                            Balance after payment:{" "}
                            <strong>${previewBalance.toFixed(2)}</strong>
                        </p>
                    </div>
                )}

                {/* Error */}
                {error && (
                    <div className="bg-red-500/10 border border-red-500/30 text-red-300 px-5 py-4 rounded-xl mt-4 flex justify-between items-center animate-error-slide">
                        <span>⚠️ {error}</span>
                    </div>
                )}

                {/* Async notice */}
                <div className="flex items-start gap-2.5 bg-blue-500/[0.08] border border-blue-500/20 rounded-[10px] px-4 py-3 text-[13px] text-blue-300 mt-5 leading-relaxed">
                    <span>⚡</span>
                    <span>
                        Payments are processed asynchronously. You'll receive a notification once
                        it's completed.
                    </span>
                </div>

                {/* Actions */}
                <div className="flex gap-3 mt-8">
                    <button
                        className="btn btn-primary flex-1"
                        onClick={handleSubmit}
                        disabled={
                            submitting ||
                            !accountIdStr ||
                            !amount ||
                            isNaN(parseFloat(amount)) ||
                            parseFloat(amount) <= 0
                        }
                    >
                        {submitting ? "⏳ Sending..." : "💳 Send Payment"}
                    </button>
                    <button
                        className="btn btn-secondary flex-1"
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
