import React, { useState } from "react";
import type { AccountDto } from "../../../types";
import "./TransactionModal.css";

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
        <div className="modal-overlay" onClick={onCancel}>
            <div className="modal" onClick={(e) => e.stopPropagation()}>
                <h2>{isWithdraw ? "💸 Withdraw Money" : "💰 Deposit Money"}</h2>

                <div className="transaction-info">
                    <p><strong>Account:</strong> {account.accountHolderName}</p>
                    <p><strong>Type:</strong> {account.accountType}</p>
                    <p><strong>Current Balance:</strong> ${account.balance.toFixed(2)}</p>
                </div>

                <div className="form-group">
                    <label htmlFor="transactionAmount">Amount:</label>
                    <input
                        id="transactionAmount"
                        type="text"
                        inputMode="decimal"
                        value={amount}
                        onChange={(e) => {
                            const value = e.target.value;
                            // Allow only numbers and one decimal point, up to 2 decimal places
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
                    />
                </div>

                {isWithdraw && error && (
                    <div className="error-message">
                        <span>⚠️ {error}</span>
                    </div>
                )}

                {amount && !isNaN(parseFloat(amount)) && parseFloat(amount) > 0 && (
                    <div className="transaction-preview">
                        <p>
                            New Balance: $
                            {isWithdraw
                                ? (account.balance - parseFloat(amount)).toFixed(2)
                                : (account.balance + parseFloat(amount)).toFixed(2)}
                        </p>
                    </div>
                )}

                <div className="modal-actions">
                    <button
                        onClick={handleConfirm}
                        className={`btn ${isWithdraw ? "btn-warning" : "btn-success"}`}
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
                    <button onClick={onCancel} className="btn btn-secondary">Cancel</button>
                </div>
            </div>
        </div>
    );
};

export default TransactionModal;
