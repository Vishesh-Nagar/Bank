import React, { useState } from "react";
import type { AccountDto } from "../../../types";

type Props = {
    visible: boolean;
    mode: "deposit" | "withdraw" | null;
    account: AccountDto | null;
    amount: string;
    setAmount: (s: string) => void;
    onCancel: () => void;
    onConfirm: () => void | Promise<void>;
    error: string;
    setError: (s: string) => void;
};

/* Shared input style */
const inputClass =
    "w-full px-4 py-[14px] bg-[rgba(15,23,42,0.6)] border border-white/10 rounded-[10px] text-white text-[15px] transition-all duration-300 " +
    "focus:outline-none focus:border-blue-500 focus:shadow-[0_0_0_3px_rgba(59,130,246,0.1)] focus:bg-[rgba(15,23,42,0.8)] " +
    "placeholder:text-slate-500";

const TransactionModal: React.FC<Props> = ({
    visible,
    mode,
    account,
    amount,
    setAmount,
    onCancel,
    onConfirm,
    error,
    setError,
}) => {
    if (!visible || !account || !mode) return null;
    const isWithdraw = mode === "withdraw";
    const [submitting, setSubmitting] = useState(false);

    const handleConfirm = async () => {
        if (submitting) return;
        setSubmitting(true);
        try {
            const res = onConfirm();
            if (res && typeof (res as any).then === "function") await res;
        } finally {
            setTimeout(() => setSubmitting(false), 800);
        }
    };

    return (
        /* Overlay */
        <div
            className="fixed inset-0 bg-black/85 backdrop-blur-[8px] flex justify-center items-center z-[1000] animate-modal-fade"
            onClick={onCancel}
        >
            {/* Modal */}
            <div
                className="bg-gradient-to-br from-[#1e293b] to-[#0f172a] p-8 rounded-[20px] min-w-[450px] max-w-[550px] w-full shadow-[0_20px_60px_rgba(0,0,0,0.6)] border border-white/10 animate-modal-slide"
                onClick={(e) => e.stopPropagation()}
            >
                <h2 className="m-0 mb-6 text-2xl font-bold">
                    {isWithdraw ? "💸 Withdraw Money" : "💰 Deposit Money"}
                </h2>

                {/* Transaction info */}
                <div className="bg-[rgba(15,23,42,0.5)] p-5 rounded-xl mb-6 border border-white/[0.05]">
                    <p className="my-2 text-sm text-slate-300">
                        <strong className="text-white font-semibold">Account:</strong>{" "}
                        {account.accountHolderName}
                    </p>
                    <p className="my-2 text-sm text-slate-300">
                        <strong className="text-white font-semibold">Type:</strong>{" "}
                        {account.accountType}
                    </p>
                    <p className="my-2 text-sm text-slate-300">
                        <strong className="text-white font-semibold">Current Balance:</strong>{" "}
                        ${account.balance.toFixed(2)}
                    </p>
                </div>

                {/* Amount input */}
                <div className="mb-6">
                    <label htmlFor="transactionAmount" className="block mb-2 text-sm font-semibold text-slate-300">
                        Amount:
                    </label>
                    <input
                        id="transactionAmount"
                        type="text"
                        inputMode="decimal"
                        value={amount}
                        onChange={(e) => {
                            const value = e.target.value;
                            if (value === "" || /^\d*\.?\d{0,2}$/.test(value)) {
                                setAmount(value);
                                if (isWithdraw) {
                                    const v = parseFloat(value);
                                    if (!isNaN(v) && v > account.balance) {
                                        setError("Cannot withdraw amount more than current balance");
                                    } else if (error === "Cannot withdraw amount more than current balance") {
                                        setError("");
                                    }
                                } else if (error === "Cannot withdraw amount more than current balance") {
                                    setError("");
                                }
                            }
                        }}
                        placeholder="Enter amount"
                        autoFocus
                        className={inputClass}
                    />
                </div>

                {/* Error */}
                {isWithdraw && error && (
                    <div className="bg-red-500/10 border border-red-500/30 text-red-300 px-5 py-4 rounded-xl mb-6 flex justify-between items-center animate-error-slide">
                        <span>⚠️ {error}</span>
                    </div>
                )}

                {/* Balance preview */}
                {amount && !isNaN(parseFloat(amount)) && parseFloat(amount) > 0 && (
                    <div className="bg-emerald-500/10 p-4 rounded-[10px] border border-emerald-500/20 mt-4">
                        <p className="m-0 text-base font-semibold text-emerald-500">
                            New Balance: $
                            {isWithdraw
                                ? (account.balance - parseFloat(amount)).toFixed(2)
                                : (account.balance + parseFloat(amount)).toFixed(2)}
                        </p>
                    </div>
                )}

                {/* Actions */}
                <div className="flex gap-3 mt-8">
                    <button
                        onClick={handleConfirm}
                        className={`btn flex-1 ${isWithdraw ? "btn-warning" : "btn-success"}`}
                        disabled={
                            submitting ||
                            !amount ||
                            isNaN(parseFloat(amount)) ||
                            parseFloat(amount) <= 0 ||
                            (isWithdraw && parseFloat(amount) > account.balance)
                        }
                    >
                        {isWithdraw ? "Withdraw" : "Deposit"}
                    </button>
                    <button onClick={onCancel} className="btn btn-secondary flex-1">
                        Cancel
                    </button>
                </div>
            </div>
        </div>
    );
};

export default TransactionModal;
